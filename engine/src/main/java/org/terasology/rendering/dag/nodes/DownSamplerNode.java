/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.nodes;

import org.terasology.assets.ResourceUrn;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.dag.ConditionDependentNode;
import org.terasology.rendering.dag.stateChanges.BindFBO;
import org.terasology.rendering.dag.stateChanges.EnableMaterial;
import org.terasology.rendering.dag.stateChanges.SetInputTextureFromFBO;
import org.terasology.rendering.dag.stateChanges.SetViewportToSizeOf;
import org.terasology.rendering.opengl.BaseFBOsManager;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.FBOConfig;
import org.terasology.rendering.opengl.FBOManagerSubscriber;

import static org.terasology.rendering.dag.stateChanges.SetInputTextureFromFBO.FboTexturesTypes.ColorTexture;
import static org.terasology.rendering.opengl.OpenGLUtils.renderFullscreenQuad;

/**
 * Instances of this class take the content of the color attachment of an input FBO
 * and downsamples it into the color attachment of a smaller output FBO.
 */
public class DownSamplerNode extends ConditionDependentNode implements FBOManagerSubscriber {
    private static final String TEXTURE_NAME = "tex";
    private static final int SLOT_0 = 0;
    private static final ResourceUrn DOWN_SAMPLER_MATERIAL = new ResourceUrn("engine:prog.downSampler");

    private String performanceMonitorLabel;
    private BaseFBOsManager outputFboManager;
    private ResourceUrn outputFboUrn;
    private FBO outputFbo;
    private Material downSampler;

    /**
     * Throws a RuntimeException if invoked. Use initialise(...) instead.
     */
    @Override
    public void initialise() {
        throw new RuntimeException("Please do not use initialise(). For this class use initialise(...) instead.");
    }

    /**
     * Initializes the DownSamplerNode instance. This method is meant to be called once, shortly after instantiation.
     *
     * @param inputFboConfig an FBOConfig instance describing the input FBO, to be retrieved from the FBO manager
     * @param inputFboManager the FBO manager from which to retrieve the input FBO
     * @param outputFboConfig an FBOConfig instance describing the output FBO, to be retrieved from the FBO manager
     * @param outputFboManager the FBO manager from which to retrieve the output FBO
     * @param aLabel a String to label the instance's entry in output generated by the PerformanceMonitor
     */
    public void initialise(FBOConfig inputFboConfig, BaseFBOsManager inputFboManager,
                           FBOConfig outputFboConfig, BaseFBOsManager outputFboManager,
                           String aLabel) {
        this.outputFboManager = outputFboManager;
        this.outputFboUrn = outputFboConfig.getName();

        requiresFBO(inputFboConfig, inputFboManager);
        outputFbo = requiresFBO(outputFboConfig, outputFboManager);

        addDesiredStateChange(new BindFBO(outputFboConfig.getName(), outputFboManager));
        addDesiredStateChange(new SetViewportToSizeOf(outputFboConfig.getName(), outputFboManager));
        addDesiredStateChange(new SetInputTextureFromFBO(SLOT_0, inputFboConfig.getName(), ColorTexture, inputFboManager,
                DOWN_SAMPLER_MATERIAL, TEXTURE_NAME));

        setupConditions();

        addDesiredStateChange(new EnableMaterial(DOWN_SAMPLER_MATERIAL.toString()));
        downSampler = getMaterial(DOWN_SAMPLER_MATERIAL);

        this.performanceMonitorLabel = aLabel;

        outputFboManager.subscribe(this);
    }

    /**
     * This method does nothing. It is meant to be overridden by inheriting classes if needed.
     * On the other hand, there might be situations in which downsampling should always occur,
     * in which case this DownSamplerNode class is good as it is.
     */
    protected void setupConditions() { }

    /**
     * Processes the input FBO downsampling its color attachment into the color attachment of the output FBO.
     */
    @Override
    public void process() {
        PerformanceMonitor.startActivity(performanceMonitorLabel);

        downSampler.setFloat("size", outputFbo.width(), true);

        renderFullscreenQuad();

        PerformanceMonitor.endActivity();
    }

    @Override
    public void update() {
        outputFbo = outputFboManager.get(outputFboUrn);
    }
}
