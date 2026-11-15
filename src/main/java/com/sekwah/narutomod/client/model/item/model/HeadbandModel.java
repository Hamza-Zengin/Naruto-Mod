package com.sekwah.narutomod.client.model.item.model;

import com.sekwah.sekclib.util.ModelUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class HeadbandModel extends HumanoidModel<HumanoidRenderState>
{
    public HeadbandModel(ModelPart modelPart)
    {
        super(modelPart);
    }

    public static LayerDefinition createLayer() {

        float yOffset = 1.6F;

        MeshDefinition definition = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = definition.getRoot();
        PartDefinition partdefinition1 = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                , PartPose.ZERO);

        partdefinition1.addOrReplaceChild("front",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(0, 0)
                        .addBox(0F, 0F, 0F, 8, 2, 1)
                , PartPose.offset(-4F, -8F + yOffset, -4.233333F));

        partdefinition1.addOrReplaceChild("back",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(0, 0)
                        .addBox(0F, 0F, 0F, 8, 2, 1)
                , PartPose.offset(-4F, -8F + yOffset, 3.2F));

        partdefinition1.addOrReplaceChild("right",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(0, 0)
                        .addBox(0F, 0F, 0F, 8, 2, 1)
                , PartPose.offsetAndRotation(3.1F, -8F + yOffset, 4F, 0F, 1.570796F, 0F));

        partdefinition1.addOrReplaceChild("left",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(0, 0)
                        .addBox(0F, 0F, 0F, 8, 2, 1)
                , PartPose.offsetAndRotation(-4.2F, -8F + yOffset, 4F, 0F, 1.570796F, 0F));
        partdefinition1.addOrReplaceChild("metal_plate",
                CubeListBuilder.create()
                        .texOffs(19, 0)
                        .addBox(0F, 0F, 0F, 3, 2, 1,
                                new CubeDeformation(0.01F))
                , PartPose.offset(-1.5F, -8F + yOffset, -4.533333F));

        partdefinition1.addOrReplaceChild("knot_right",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(0, 3)
                        .addBox(-1F, 0F, 0F, 1, 3, 1)
                , PartPose.offsetAndRotation(0F, -6.333333F + yOffset, 3.6F,
                        0.4833219F, 1.226894F, 0F));

        partdefinition1.addOrReplaceChild("knot_left",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(0, 3)
                        .addBox(0F, 0F, 0F, 1, 3, 1)
                , PartPose.offsetAndRotation(0F, -7.066667F + yOffset, 4.7F,
                        -0.5576792F, 1.953013F, 0F));

        partdefinition1.addOrReplaceChild("knot",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(0, 0)
                        .addBox(0F, 0F, 0F, 1, 1, 1)
                , PartPose.offsetAndRotation(0.2666667F, -7.533333F + yOffset, 3.933333F,
                        0.4833219F, -0.3717861F, 0.8551081F));

        return LayerDefinition.create(definition, 64, 32);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        this.head.xScale = 1.1F;
        this.head.yScale = 1.1F;
        this.head.zScale = 1.1F;
    }

}
