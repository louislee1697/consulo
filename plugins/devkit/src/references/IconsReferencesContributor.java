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
package org.jetbrains.idea.devkit.references;

import com.intellij.find.FindModel;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceUtil;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.*;

/**
 * @author Konstantin Bulenkov
 */
public class IconsReferencesContributor extends PsiReferenceContributor implements QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
  @Override
  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    final StringPattern methodName = string().oneOf("findIcon", "getIcon");
    final PsiMethodPattern method = psiMethod().withName(methodName).definedInClass(IconLoader.class.getName());
    final PsiJavaElementPattern.Capture<PsiLiteralExpression> javaFile
      = literalExpression().and(psiExpression().methodCallParameter(0, method));

    final XmlAttributeValuePattern pluginXml = XmlPatterns.xmlAttributeValue().withLocalName("icon");

    registrar.registerReferenceProvider(or(javaFile, pluginXml), new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull ProcessingContext context) {
        if (!isIdeaProject(element.getProject())) return PsiReference.EMPTY_ARRAY;
        return new FileReferenceSet(element) {
          @Override
          protected Collection<PsiFileSystemItem> getExtraContexts() {
            final Module icons = ModuleManager.getInstance(element.getProject()).findModuleByName("icons");
            if (icons != null) {
              final ArrayList<PsiFileSystemItem> result = new ArrayList<PsiFileSystemItem>();
              final VirtualFile[] roots = ModuleRootManager.getInstance(icons).getSourceRoots();
              final PsiManager psiManager = element.getManager();
              for (VirtualFile root : roots) {
                final PsiDirectory directory = psiManager.findDirectory(root);
                if (directory != null) {
                  result.add(directory);
                }
              }
              return result;
            }
            return super.getExtraContexts();
          }
        }.getAllReferences();
      }
    });
  }

  public static boolean isIdeaProject(@Nullable Project project) {
    final VirtualFile baseDir;
    return project != null
           && ("IDEA".equals(project.getName()) || "community".equals(project.getName()))
           && (baseDir = project.getBaseDir()) != null
           && baseDir.findFileByRelativePath("plugins") != null;
  }

  @Override
  public boolean execute(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<PsiReference> consumer) {
    final PsiElement file = queryParameters.getElementToSearch();
    if (file instanceof PsiBinaryFile) {
      final Module module = ModuleUtil.findModuleForPsiElement(file);
      final VirtualFile image = ((PsiBinaryFile)file).getVirtualFile();
      if (isImage(image) && isIconsModule(module)) {
        final Project project = file.getProject();
        final FindModel model = new FindModel();
        final String path = getPathToImage(image, module);
        if (path == null) return true;
        model.setStringToFind(path);
        model.setCaseSensitive(true);
        model.setFindAll(true);
        final List<UsageInfo> usages = FindInProjectUtil.findUsages(model, FindInProjectUtil.getPsiDirectory(model, project), project, false);
        if (!usages.isEmpty()) {
          for (UsageInfo usage : usages) {
            final PsiElement element = usage.getElement();

            final ProperTextRange textRange = usage.getRangeInElement();
            if (element != null && textRange != null) {
              final PsiElement start = element.findElementAt(textRange.getStartOffset());
              final PsiElement end = element.findElementAt(textRange.getEndOffset());
              if (start != null && end != null) {
                PsiElement value = PsiTreeUtil.findCommonParent(start, end);
                if (value instanceof PsiJavaToken) {
                  value = value.getParent();
                }
                if (value != null) {
                  final FileReference reference = FileReferenceUtil.findFileReference(value);
                  if (reference != null) {
                    consumer.process(reference);
                  }
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  @Nullable
  private static String getPathToImage(VirtualFile image, Module module) {
    final String path = ModuleRootManager.getInstance(module).getSourceRoots()[0].getPath();
    return "/" + FileUtil.getRelativePath(path, image.getPath(), '/');
  }

  private static boolean isIconsModule(Module module) {
    return module != null && "icons".equals(module.getName())
           && ModuleRootManager.getInstance(module).getSourceRoots().length == 1;
  }

  private static boolean isImage(VirtualFile image) {
    final FileTypeManager mgr = FileTypeManager.getInstance();
    return image != null && mgr.getFileTypeByFile(image) == mgr.getFileTypeByExtension("png");
  }
}