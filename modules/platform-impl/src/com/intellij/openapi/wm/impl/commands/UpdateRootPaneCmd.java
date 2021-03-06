/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.commands;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Vladimir Kondratyev
 */
public final class UpdateRootPaneCmd extends FinalizableCommand {
  private final JRootPane myRootPane;

  public UpdateRootPaneCmd(@NotNull final JRootPane rootPane, final Runnable finishCallBack) {
    super(finishCallBack);
    myRootPane = rootPane;
  }

  @Override
  public void run() {
    try {
      myRootPane.revalidate();
      myRootPane.repaint();
    }
    finally {
      finish();
    }
  }
}

