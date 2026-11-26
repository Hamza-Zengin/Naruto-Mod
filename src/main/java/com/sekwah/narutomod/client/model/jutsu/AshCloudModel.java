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
 * 14. KATON: HAISEKISHŌ — 3D Superheated Volcanic Ash Cloud &amp; Ember Vortex.
 */
public class AshCloudModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart corePlume;
    private final ModelPart swirlRing;
    private final ModelPart emberCrown;

    public AshCloudModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.corePlume = part.getChild("core_plume");
        this.swirlRing = part.getChild("swirl_ring");
        this.emberCrown = part.getChild("ember_crown");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Multi-lobed octagonal superheated ash dome & incandescent ember core
        PartDefinition core = root.addOrReplaceChild("core_plume",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-11.0F, -14.0F, -11.0F, 22.0F, 13.0F, 22.0F)
                        .texOffs(0, 35).addBox(-8.0F, -22.0F, -8.0F, 16.0F, 9.0F, 16.0F)
                        .texOffs(38, 35).addBox(-4.5F, -13.0F, -4.5F, 9.0F, 9.0F, 9.0F),
                PartPose.ZERO);
        core.addOrReplaceChild("core_oct",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-10.5F, -13.5F, -10.5F, 21.0F, 12.0F, 21.0F),
                PartPose.rotation(0.0F, 0.7854F, 0.0F));

        // 8 Billowing Perimeter Volcanic Ash Lobes
        PartDefinition swirl = root.addOrReplaceChild("swirl_ring", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI * 2.0 / 8.0);
            float radius = (i % 2 == 0) ? 18.0F : 15.0F;
            float px = Mth.cos(angle) * radius;
            float pz = Mth.sin(angle) * radius;
            float py = (i % 2 == 0) ? -7.5F : -11.5F;
            swirl.addOrReplaceChild("ash_lobe_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-6.5F, -5.5F, -6.5F, 13.0F, 10.0F, 13.0F)
                            .texOffs(38, 35).addBox(-3.0F, -3.5F, -3.0F, 6.0F, 6.0F, 6.0F),
                    PartPose.offsetAndRotation(px, py, pz, 0.12F, -angle, 0.10F));
        }

        // 6 Spiraling Crown Cinder & Flame Plumes
        PartDefinition crown = root.addOrReplaceChild("ember_crown", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 6; i++) {
            float angle = (float) (i * Math.PI * 2.0 / 6.0);
            float radius = 11.5F;
            float px = Mth.cos(angle) * radius;
            float pz = Mth.sin(angle) * radius;
            crown.addOrReplaceChild("ember_col_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 35).addBox(-3.2F, -12.0F, -3.2F, 6.4F, 12.0F, 6.4F)
                            .texOffs(38, 35).addBox(-1.6F, -18.0F, -1.6F, 3.2F, 7.0F, 3.2F),
                    PartPose.offsetAndRotation(px, -13.0F, pz, 0.22F, -angle + (float) (Math.PI / 2.0), 0.0F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age(), state.progress());
    }

    public void setupAnim(float age, float progress) {
        float rise = Mth.sin(age * 0.24F) * 1.2F;
        float expand = age < 7.0F ? (1.0F - (age / 7.0F)) * 10.0F : 0.0F;
        this.corePlume.y = rise + expand;
        this.corePlume.yRot = age * 0.08F;
        this.swirlRing.y = rise * 0.6F + expand;
        this.swirlRing.yRot = age * 0.16F;
        this.emberCrown.y = -rise + expand;
        this.emberCrown.yRot = -age * 0.24F;
    }
}
