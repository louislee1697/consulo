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
package com.maddyhome.idea.copyright;

import com.intellij.openapi.fileTypes.FileTypeExtension;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.maddyhome.idea.copyright.psi.UpdateCopyrightsProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class CopyrightUpdaters extends FileTypeExtension<UpdateCopyrightsProvider> {
  public static CopyrightUpdaters INSTANCE = new CopyrightUpdaters();

  private CopyrightUpdaters() {
    super("com.intellij.copyright.updater");
  }

  public static boolean hasExtension(@NotNull PsiFile psiFile) {
    VirtualFile virtualFile = psiFile.getVirtualFile();
    return virtualFile != null && hasExtension(virtualFile);
  }

  public static boolean hasExtension(@NotNull VirtualFile virtualFile) {
    return INSTANCE.forFileType(virtualFile.getFileType()) != null;
  }
}
