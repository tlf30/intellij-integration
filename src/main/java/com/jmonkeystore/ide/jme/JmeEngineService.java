package com.jmonkeystore.ide.jme;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.input.InputManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Spatial;
import com.jmonkeystore.ide.jme.impl.JmePanel;

public interface JmeEngineService {

    AssetManager getAssetManager();
    InputManager getInputManager();
    AppStateManager getStateManager();
    Renderer getRenderer();

    void enqueue(Runnable runnable);

    JmePanel getOrCreatePanel(String name);
    void removePanel(JmePanel jmePanel);

    Spatial loadExternalModel(String url);
    Spatial loadExternalModel(ModelKey modelKey);

}
