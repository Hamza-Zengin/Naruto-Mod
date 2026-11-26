package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 10. DOTON: DORYŪHEKI — 3D Sculpted Shinobi Earth Bulwark.
 */
public class EarthWallModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart wallBody;

    public EarthWallModel(ModelPart part) {
        super(part, RenderTypes::entityCutout);
        this.wallBody = part.getChild("wall_body");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Sculpted 3-block wide stone bulwark with chamfered battlements, side columns, and stone buttresses
        PartDefinition wall = root.addOrReplaceChild("wall_body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-22.0F, -45.0F, -5.5F, 44, 45, 11)
                        // Stepped stone plinth & top cornice ledge
                        .texOffs(0, 0).addBox(-24.0F, -6.0F, -7.0F, 48, 6, 14)
                        .texOffs(0, 0).addBox(-23.0F, -46.5F, -6.5F, 46, 3, 13),
                PartPose.ZERO);

        // Beveled octagonal flanking stone pillars
        wall.addOrReplaceChild("pillar_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -48.0F, -6.5F, 7, 48, 13),
                PartPose.offsetAndRotation(-21.5F, 0.0F, 0.0F, 0.0F, 0.22F, 0.0F));
        wall.addOrReplaceChild("pillar_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -48.0F, -6.5F, 7, 48, 13),
                PartPose.offsetAndRotation(21.5F, 0.0F, 0.0F, 0.0F, -0.22F, 0.0F));

        // Angled Front & Back Stone Buttress Ribs
        for (int i = -1; i <= 1; i++) {
            float bx = i * 11.5F;
            wall.addOrReplaceChild("buttress_f_" + (i + 1),
                    CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -40.0F, -3.0F, 6, 40, 6),
                    PartPose.offsetAndRotation(bx, 0.0F, -5.5F, -0.09F, 0.0F, 0.0F));
            wall.addOrReplaceChild("buttress_b_" + (i + 1),
                    CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -40.0F, -3.0F, 6, 40, 6),
                    PartPose.offsetAndRotation(bx, 0.0F, 5.5F, 0.09F, 0.0F, 0.0F));
        }

        // Sculpted Chamfered Top Battlement Merlons
        for (int i = 0; i < 4; i++) {
            float mx = -15.0F + i * 10.0F;
            PartDefinition merlon = wall.addOrReplaceChild("merlon_" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -5.0F, -5.0F, 7, 5, 10),
                    PartPose.offset(mx, -46.5F, 0.0F));
            merlon.addOrReplaceChild("merlon_cap_" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-2.6F, -2.0F, -4.0F, 5.2F, 2.0F, 8.0F),
                    PartPose.offset(0.0F, -5.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age(), state.progress());
    }

    public void setupAnim(float age, float progress) {
        float emerge = age < 6.0F ? (1.0F - (age / 6.0F)) * 48.0F : 0.0F;
        this.wallBody.y = emerge;
    }

    public void setupAnim(float progress) {
        float emerge = progress < 0.08F ? (1.0F - (progress / 0.08F)) * 48.0F : 0.0F;
        this.wallBody.y = emerge;
    }
}
