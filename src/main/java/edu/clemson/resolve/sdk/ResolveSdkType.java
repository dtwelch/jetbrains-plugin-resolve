package edu.clemson.resolve.sdk;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.NotSupportedException;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ObjectUtils;
import edu.clemson.resolve.ResolveConstants;
import edu.clemson.resolve.ResolveIcons;
import edu.clemson.resolve.ResolveModuleType;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ResolveSdkType extends SdkType {

    public ResolveSdkType() {
        super(ResolveConstants.SDK_TYPE_ID);
    }

    @NotNull
    public static ResolveSdkType getInstance() {
        return SdkType.findInstance(ResolveSdkType.class);
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return ResolveIcons.TOOL_ICON;
    }

    @NotNull
    @Override
    public Icon getIconForAddAction() {
        return getIcon();
    }

    @Nullable
    @Override
    public String suggestHomePath() {
        VirtualFile suggestSdkDirectory = ResolveSdkUtil.suggestSdkDirectory();
        return suggestSdkDirectory != null ? suggestSdkDirectory.getPath() : null;
    }

    @Override
    public boolean isValidSdkHome(@NotNull String sdkHomePath) {
        //note that we don't explicitly validate the resolve compiler jar
        //exists here---no need, as this will return null if it doesn't find a
        //suitable version.
        // (the version is embedded in it)
        if (getVersionString(sdkHomePath) == null) {
            ResolveSdkService.LOG.debug("Cannot retrieve version for sdk (or the compiler-jar): " + sdkHomePath);
            return false;
        }
        return true;
    }

    @Override
    public String suggestSdkName(String currentSdkName, String sdkHome) {
        String version = getVersionString(sdkHome);
        if (version == null) {
            return "Unknown RESOLVE version at " + sdkHome;
        }
        return "RESOLVE " + version;
    }

    @Nullable
    @Override
    public String getVersionString(@NotNull String sdkHome) {
        return RESOLVESdkUtil.retrieveRESOLVEVersion(sdkHome);
    }

    @Nullable
    @Override
    public String getDefaultDocumentationUrl(@NotNull Sdk sdk) {
        return null;
    }

    @Nullable
    @Override
    public AdditionalDataConfigurable createAdditionalDataConfigurable(@NotNull SdkModel sdkModel,
                                                                       @NotNull SdkModificator sdkModificator) {
        return null;
    }

    @Override
    public void saveAdditionalData(@NotNull SdkAdditionalData additionalData, @NotNull Element additional) {
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "RESOLVE SDK";
    }

    @Override
    public void setupSdkPaths(@NotNull Sdk sdk) {
        String versionString = sdk.getVersionString();
        if (versionString == null) {
            throw new RuntimeException("SDK version is not defined");
        }
        SdkModificator modificator = sdk.getSdkModificator();
        String path = sdk.getHomePath();
        if (path == null) return;
        modificator.setHomePath(path);

        for (VirtualFile file : RESOLVESdkUtil.getSdkDirectoriesToAttach(path, versionString)) {
            modificator.addRoot(file, OrderRootType.CLASSES);
            modificator.addRoot(file, OrderRootType.SOURCES);
        }
        modificator.commitChanges();
    }

    public static class ResolveSdkService extends SimpleModificationTracker {
        static final Logger LOG = Logger.getInstance(ResolveSdkService.class);

        @NotNull
        final Project project;

        protected ResolveSdkService(@NotNull Project project) {
            this.project = project;
        }

        public static ResolveSdkService getInstance(@NotNull Project project) {
            return ServiceManager.getService(project, ResolveSdkService.class);
        }

        @Nullable
        public String getSdkHomePath(@Nullable Module module) {
            throw new NotSupportedException("Not yet implemented");
        }

        @Nullable
        public String getSdkCompilerJarPath(@Nullable Module module) {
            throw new NotSupportedException("Not yet implemented");
        }

        static String libraryRootToSdkPath(@NotNull VirtualFile root) {
            return VfsUtilCore.urlToPath(StringUtil.trimEnd(
                    StringUtil.trimEnd(StringUtil.trimEnd(root.getUrl(),
                            "src/pkg"), "src"), "/"));
        }

        @Nullable
        public String getSdkVersion(@Nullable Module module) {
            ComponentManager holder = ObjectUtils.notNull(module, project);
            return CachedValuesManager.getManager(project).getCachedValue(
                    holder, () -> {
                        String result = null;
                        String sdkHomePath = getSdkHomePath(module);
                        if (sdkHomePath != null) {
                            result = ResolveSdkUtil.retrieveRESOLVEVersion(sdkHomePath);
                        }
                        return CachedValueProvider.Result.create(
                                result, ResolveSdkService.this);
                    });
        }

        public void chooseAndSetSdk(@Nullable Module module) {

        }

        /**
         * Use this method in order to check whether the method is appropriate
         * for providing RESOLVE-specific code insight.
         */
        @Contract("null -> false")
        public boolean isRESOLVEModule(@Nullable Module module) {
            return module != null && !module.isDisposed() &&
                    ModuleType.get(module) == ResolveModuleType.getInstance();
        }

        public static boolean isRESOLVESdkLibRoot(@NotNull VirtualFile root) {
            return root.isInLocalFileSystem() &&
                    root.isDirectory() &&
                    ResolveSdkUtil.retrieveResolveVersion(
                            root.getPath()) != null;
        }
    }
}