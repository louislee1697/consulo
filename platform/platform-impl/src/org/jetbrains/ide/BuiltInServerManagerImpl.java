package org.jetbrains.ide;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ShutDownTracker;
import org.jboss.netty.channel.ChannelException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.io.WebServer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class BuiltInServerManagerImpl extends BuiltInServerManager {
  private static final Logger LOG = Logger.getInstance(BuiltInServerManager.class);

  @NonNls
  public static final String PROPERTY_RPC_PORT = "consulo.rpc.port";
  private static final int FIRST_PORT_NUMBER = 62242;
  private static final int PORTS_COUNT = 20;

  private volatile int myDetectedPortNumber = -1;
  private final AtomicBoolean myStarted = new AtomicBoolean(false);

  @Nullable
  private WebServer myServer;
  private boolean myEnabledInUnitTestMode;

  @Override
  public int getPort() {
    return myDetectedPortNumber == -1 ? getDefaultPort() : myDetectedPortNumber;
  }

  @Override
  public BuiltInServerManager waitForStart() {
    Future<?> serverStartFuture = startServerInPooledThread();
    if (serverStartFuture != null) {
      LOG.assertTrue(!ApplicationManager.getApplication().isDispatchThread());
      try {
        serverStartFuture.get();
      }
      catch (InterruptedException ignored) {
      }
      catch (ExecutionException ignored) {
      }
    }
    return this;
  }

  private static int getDefaultPort() {
    return System.getProperty(PROPERTY_RPC_PORT) == null ? FIRST_PORT_NUMBER : Integer.parseInt(System.getProperty(PROPERTY_RPC_PORT));
  }

  static final class MyPostStartupActivity implements StartupActivity, DumbAware {
    private boolean veryFirstProjectOpening = true;

    @Override
    public void runActivity(Project project) {
      if (!veryFirstProjectOpening) {
        return;
      }

      veryFirstProjectOpening = false;
      BuiltInServerManager builtInServerManager = BuiltInServerManager.getInstance();
      if (builtInServerManager instanceof BuiltInServerManagerImpl) {
        ((BuiltInServerManagerImpl)builtInServerManager).startServerInPooledThread();
      }
    }
  }

  private Future<?> startServerInPooledThread() {
    Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode() && !myEnabledInUnitTestMode) {
      return null;
    }

    if (!myStarted.compareAndSet(false, true)) {
      return null;
    }

    return application.executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        try {
          myServer = new WebServer();
        }
        catch (ChannelException e) {
          LOG.info(e);
          String groupDisplayId = "Web Server";
          Notifications.Bus.register(groupDisplayId, NotificationDisplayType.STICKY_BALLOON);
          new Notification(groupDisplayId, "Internal HTTP server disabled",
                           "Cannot start internal HTTP server. Some plugins may operate with errors. " +
                           "Please check your firewall settings and restart " + ApplicationNamesInfo.getInstance().getFullProductName(),
                           NotificationType.ERROR).notify(null);
          return;
        }

        Disposer.register(ApplicationManager.getApplication(), myServer);
        ShutDownTracker.getInstance().registerShutdownTask(new Runnable() {
          @Override
          public void run() {
            if (!Disposer.isDisposed(myServer)) {
              // something went wrong
              Disposer.dispose(myServer);
            }
          }
        });

        myDetectedPortNumber = myServer.start(getDefaultPort(), PORTS_COUNT, true);
        if (myDetectedPortNumber == -1) {
          LOG.info("web server cannot be started, cannot bind to port");
        }
        else {
          LOG.info("web server started, port " + myDetectedPortNumber);
        }
      }
    });
  }

  @Override
  @Nullable
  public Disposable getServerDisposable() {
    return myServer;
  }

  /**
   * Pass true to start the WebServerManager even in the {@link Application#isUnitTestMode() unit test mode}.
   */
  @TestOnly
  public void setEnabledInUnitTestMode(boolean enabled) {
    myEnabledInUnitTestMode = enabled;
  }

}