/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import consulo.ui.Component;
import consulo.ui.DockLayout;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtDockLayoutImpl extends WGwtLayoutImpl<String> implements DockLayout {
  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout top(@NotNull Component component) {
    addChild((WGwtBaseComponent)component, "top");
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout bottom(@NotNull Component component) {
    addChild((WGwtBaseComponent)component, "bottom");
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout center(@NotNull Component component) {
    addChild((WGwtBaseComponent)component, "center");
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout left(@NotNull Component component) {
    addChild((WGwtBaseComponent)component, "left");
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout right(@NotNull Component component) {
    addChild((WGwtBaseComponent)component, "right");
    return this;
  }

  @Override
  protected void convertConstraint(Map<String, Serializable> map, String constraint) {
    map.put("side", constraint);
  }
}
