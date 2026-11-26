package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * 16. CHAKURA NO MESU — 3D Teal Medical Chakra Scalpel Hand Blade.
 */
public class ChakraScalpelModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart scalpelCore;
    private final ModelPart edgeAura;

    public ChakraScalpelModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.scalpelCore = part.getChild("scalpel_core");
        this.edgeAura = part.getChild("edge_aura");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Ergonomic gauntlet sheath + tapered dual-bevel glowing teal chakra blade
        PartDefinition core = root.addOrReplaceChild("scalpel_core",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.2F, -1.6F, -4.0F, 4.4F, 3.2F, 8.0F)
                        .texOffs(0, 14).addBox(-1.4F, -1.1F, -13.0F, 2.8F, 2.2F, 10.0F)
                        .texOffs(26, 0).addBox(-0.85F, -0.75F, -20.0F, 1.7F, 1.5F, 8.0F)
                        .texOffs(26, 14).addBox(-0.35F, -0.4F, -24.5F, 0.7F, 0.8F, 5.0F),
                PartPose.ZERO);
        core.addOrReplaceChild("blade_fin_top",
                CubeListBuilder.create().texOffs(0, 28).addBox(-0.35F, -3.2F, -16.0F, 0.7F, 2.3F, 13.0F),
                PartPose.rotation(-0.08F, 0.0F, 0.0F));
        core.addOrReplaceChild("blade_fin_bottom",
                CubeListBuilder.create().texOffs(0, 28).addBox(-0.35F, 0.9F, -15.0F, 0.7F, 2.0F, 12.0F),
                PartPose.rotation(0.08F, 0.0F, 0.0F));

        // 4 Crackling Teal Surgical Chakra Ribbons Spiraling Around the Blade
        PartDefinition aura = root.addOrReplaceChild("edge_aura", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            float rotZ = (float) (i * Math.PI * 0.5D);
            aura.addOrReplaceChild("scalpel_arc_" + i,
                    CubeListBuilder.create().texOffs(28, 28).addBox(2.4F, -0.45F, -14.5F, 1.1F, 0.9F, 13.0F),
                    PartPose.rotation(0.10F, 0.0F, rotZ));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age());
    }

    public void setupAnim(float age) {
        this.scalpelCore.y = Mth.sin(age * 0.60F) * 0.35F;
        this.edgeAura.zRot = age * 0.55F;
    }
}
