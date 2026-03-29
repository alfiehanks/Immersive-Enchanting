package me.alfie.immersiveenchanting.gui.core.tab.enchanting.node;

import net.minecraft.resources.ResourceLocation;

public enum NodeType {
        BASIC(
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/basic_node_unobtained.png"),
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/basic_node_obtained.png")
        ),
        ADVANCED(
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/advanced_node_unobtained.png"),
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/sprites/advanced_node_obtained.png")
        ),
        ELITE(
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/elite_node_unobtained.png"),
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/elite_node_obtained.png")
        ),
        LOCKED(
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/basic_node_locked.png"),
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/basic_node_locked.png")
        ),
        ALERT(
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/alert_node.png"),
                new ResourceLocation("immersiveenchanting", "textures/gui/sprites/node/alert_node.png")
        );

    private final ResourceLocation unobtainedTexture;
    private final ResourceLocation obtainedTexture;

    NodeType(ResourceLocation offTexture, ResourceLocation onTexture) {
        this.unobtainedTexture = offTexture;
        this.obtainedTexture = onTexture;
    }

    public ResourceLocation getUnobtainedTexture() {
        return unobtainedTexture;
    }

    public ResourceLocation getObtainedTexture() {
        return obtainedTexture;
    }
}
