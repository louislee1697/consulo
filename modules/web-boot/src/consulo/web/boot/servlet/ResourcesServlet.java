/*
 * Copyright 2013-2017 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.web.boot.servlet;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.lang.UrlClassLoader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * @author VISTALL
 * @since 16-May-17
 */
public class ResourcesServlet extends HttpServlet {
  private UrlClassLoader myUrlClassLoader;

  public ResourcesServlet(UrlClassLoader urlClassLoader) {
    myUrlClassLoader = urlClassLoader;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String url = req.getRequestURI();

    url = url.replace("/res/", "/webResources/");


    URL resource = myUrlClassLoader.findResource(url);

    if (resource == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    try {
      String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
      String mimeType = Files.probeContentType(new File(fileName).toPath());

      if (mimeType != null) {
        resp.setHeader("Content-Type", mimeType);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    try (OutputStream stream = resp.getOutputStream(); InputStream inputStream = resource.openStream()) {
      StreamUtil.copyStreamContent(inputStream, stream);
    }
  }
}
