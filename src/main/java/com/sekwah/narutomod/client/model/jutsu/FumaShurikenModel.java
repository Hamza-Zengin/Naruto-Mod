package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 3D Model for Giant 4-Bladed Fūma Shuriken
 */
public class FumaShurikenModel extends Model<EntityRenderState> {

    public FumaShurikenModel(ModelPart modelPart) {
        super(modelPart, RenderTypes::entityCutout);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("hub",
                CubeListBuilder.create()
                        // Central metallic ring hub
                        .texOffs(0, 0)
                        .addBox(-3.0F, -0.5F, -3.0F, 6, 1, 6)
                        // Blade 1 (+Z)
                        .texOffs(0, 8)
                        .addBox(-2.0F, -0.5F, 3.0F, 4, 1, 9)
                        .texOffs(26, 8)
                        .addBox(-1.0F, -0.5F, 12.0F, 2, 1, 3)
                        // Blade 2 (-Z)
                        .texOffs(0, 8)
                        .addBox(-2.0F, -0.5F, -12.0F, 4, 1, 9)
                        .texOffs(26, 8)
                        .addBox(-1.0F, -0.5F, -15.0F, 2, 1, 3)
                        // Blade 3 (+X)
                        .texOffs(0, 18)
                        .addBox(3.0F, -0.5F, -2.0F, 9, 1, 4)
                        .texOffs(26, 18)
                        .addBox(12.0F, -0.5F, -1.0F, 3, 1, 2)
                        // Blade 4 (-X)
                        .texOffs(0, 18)
                        .addBox(-12.0F, -0.5F, -2.0F, 9, 1, 4)
                        .texOffs(26, 18)
                        .addBox(-15.0F, -0.5F, -1.0F, 3, 1, 2),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }
}
