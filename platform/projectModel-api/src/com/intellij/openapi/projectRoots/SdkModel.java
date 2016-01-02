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
package com.intellij.openapi.projectRoots;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * General interface for {@see com.intellij.openapi.projectRoots.SdkTable} and it wrapper while it editing in UI
 */
public interface SdkModel {

  /**
   * Returns the list of SDKs in the table.
   * @return the SDK list.
   */
  @NotNull
  Sdk[] getSdks();

  /**
   * Returns the SDK with the specified name, or null if one is not found.
   *
   * @param sdkName the name of the SDK to find.
   * @return the SDK instance or null.
   */
  @Nullable
  Sdk findSdk(String sdkName);

  /**
   * Adds the specified SDK the model.
   *
   * @param sdk the SDK to add
   */
  void addSdk(Sdk sdk);
}
