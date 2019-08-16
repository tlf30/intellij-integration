package com.jmonkeystore.ide;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.VetoableProjectManagerListener;
import com.jme3.asset.plugins.FileLocator;
import com.jmonkeystore.ide.jme.JmeEngineService;
import org.jetbrains.annotations.NotNull;

public class JmeVetoableProjectListener implements VetoableProjectManagerListener {

    private String getProjectResourcesDir(Project project) {
        return project.getBasePath() + "/src/main/resources";
    }

    @Override
    public boolean canClose(@NotNull Project project) {
        return true;
    }

    @Override
    public void projectOpened(@NotNull Project project) {
        ServiceManager.getService(JmeEngineService.class)
                .getAssetManager()
                .registerLocator(getProjectResourcesDir(project), FileLocator.class);
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        ServiceManager.getService(JmeEngineService.class)
                .getAssetManager()
                .unregisterLocator(getProjectResourcesDir(project), FileLocator.class);
    }

}
