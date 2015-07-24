package edu.clemson.resolve.plugin.sdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RESOLVESmallIDEsSdkService extends RESOLVESdkService {
    public static final String LIBRARY_NAME = "RESOLVE SDK";

    protected RESOLVESmallIDEsSdkService(@NotNull Project project) {
        super(project);
    }

    @Nullable @Override public String getSdkHomePath(@Nullable Module module) {
        ComponentManager holder = ObjectUtils.notNull(module, this.project);
        return CachedValuesManager.getManager(this.project)
                .getCachedValue(holder, new CachedValueProvider<String>() {

            @Nullable @Override public Result<String> compute() {
                return Result.create(ApplicationManager.getApplication()
                        .runReadAction(new Computable<String>() {
                            @Nullable
                            @Override
                            public String compute() {
                                LibraryTable table = LibraryTablesRegistrar
                                        .getInstance().getLibraryTable(project);
                                for (Library library : table.getLibraries()) {
                                    final String libraryName = library.getName();
                                    if (libraryName != null && libraryName
                                            .startsWith(LIBRARY_NAME)) {
                                        for (final VirtualFile root :
                                                library.getFiles(OrderRootType.CLASSES)) {
                                            if (isRESOLVESdkLibRoot(root)) {
                                                return libraryRootToSdkPath(root);
                                            }
                                        }
                                    }
                                }
                                return null;
                            }
                        }), RESOLVESmallIDEsSdkService.this);
            }
        });
    }

    @Nullable @Override public String getSdkVersion(@Nullable Module module) {
        ComponentManager holder = ObjectUtils.notNull(module, project);
        return CachedValuesManager.getManager(project)
                .getCachedValue(holder, new CachedValueProvider<String>() {
                    @Nullable
                    @Override
                    public Result<String> compute() {
                        String result = null;
                        final String sdkHomePath = getSdkHomePath(module);
                        if (sdkHomePath != null) {
                            result = RESOLVESdkUtil
                                    .retrieveRESOLVEVersion(sdkHomePath);
                        }
                        return Result.create(result,
                                RESOLVESmallIDEsSdkService.this);
                    }
                });
    }

    @Override public void chooseAndSetSdk(@Nullable final Module module) {
        Sdk projectSdk = ProjectSettingsService.getInstance(this.project)
                .chooseAndSetSdk();
        if (projectSdk == null && module != null) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override public void run() {
                    if (!module.isDisposed()) {
                        ModuleRootModificationUtil.setSdkInherited(module);
                    }
                }
            });
        }
    }

    @Override public boolean isRESOLVEModule(@Nullable Module module) {
        return super.isRESOLVEModule(module) && getSdkHomePath(module) != null;
    }

    public static boolean isRESOLVESdkLibRoot(@NotNull VirtualFile root) {
        return root.isInLocalFileSystem() && root.isDirectory();
    }
}
