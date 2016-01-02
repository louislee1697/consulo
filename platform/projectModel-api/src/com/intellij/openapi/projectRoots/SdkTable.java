/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.util.messages.Topic;
import org.consulo.lombok.annotations.ApplicationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.DeprecationInfo;
import org.mustbe.consulo.Exported;
import org.mustbe.consulo.RequiredWriteAction;

import java.util.ArrayList;
import java.util.List;

@ApplicationService
public abstract class SdkTable implements SdkModel{
  public static Topic<SdkTableListener> SDK_TABLE_TOPIC = Topic.create("Sdk table", SdkTableListener.class);

  @Override
  @Nullable
  public abstract Sdk findSdk(String name);

  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #getSdks()", until = "1.0")
  public final Sdk[] getAllSdks() {
    return getSdks();
  }

  @NotNull
  @Exported
  public List<Sdk> getSdksOfType(SdkTypeId type) {
    List<Sdk> result = new ArrayList<Sdk>();
    for (Sdk sdk : getSdks()) {
      if (sdk.getSdkType() == type) {
        result.add(sdk);
      }
    }
    return result;
  }

  @Nullable
  public Sdk findMostRecentSdkOfType(final SdkTypeId type) {
    return findMostRecentSdk(new Condition<Sdk>() {
      @Override
      public boolean value(Sdk sdk) {
        return sdk.getSdkType() == type;
      }
    });
  }

  @Nullable
  public Sdk findMostRecentSdk(@NotNull Condition<Sdk> condition) {
    Sdk found = null;
    for (Sdk each : getSdks()) {
      if (!condition.value(each)) continue;
      if (found == null) {
        found = each;
        continue;
      }
      if (Comparing.compare(each.getVersionString(), found.getVersionString()) > 0) found = each;
    }
    return found;
  }

  @Override
  @RequiredWriteAction
  public abstract void addSdk(@NotNull Sdk sdk);

  @RequiredWriteAction
  public abstract void removeSdk(@NotNull Sdk sdk);

  @RequiredWriteAction
  public abstract void updateSdk(@NotNull Sdk originalSdk, @NotNull Sdk modifiedSdk);

  public abstract SdkTypeId getDefaultSdkType();

  public abstract SdkTypeId getSdkTypeByName(String name);

  @Nullable
  @Exported
  public Sdk findPredefinedSdkByType(@NotNull SdkTypeId sdkType) {
    for (Sdk sdk : getSdks()) {
      if(sdk.isPredefined() && sdk.getSdkType() == sdkType) {
        return sdk;
      }
    }
    return null;
  }

  @NotNull
  public abstract Sdk createSdk(final String name, final SdkTypeId sdkType);
}
