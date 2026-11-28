package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.ToadSageBossModel;
import com.sekwah.narutomod.entity.mob.SageTrialBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Custom 3D renderer for {@link SageTrialBossEntity}, using {@link ToadSageBossModel}
 * and the 128x128 {@code toad_sage_boss.png} texture scaled to ~2.25 blocks tall.
 */
public class ToadSageBossRenderer extends MobRenderer<SageTrialBossEntity, ToadSageBossRenderer.ToadBossRenderState, ToadSageBossModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NarutoMod.MOD_ID, "textures/entity/mob/toad_sage_boss.png");

    public static class ToadBossRenderState extends LivingEntityRenderState {
        public boolean isPhaseTwo;
        public boolean isDuelActive;
        public float attackAnim;
        public float leapPhase;
    }

    public ToadSageBossRenderer(EntityRendererProvider.Context context) {
        super(context, new ToadSageBossModel(context.bakeLayer(ToadSageBossModel.LAYER_LOCATION)), 1.15F);
    }

    @Override
    public ToadBossRenderState createRenderState() {
        return new ToadBossRenderState();
    }

    @Override
    public void extractRenderState(SageTrialBossEntity entity, ToadBossRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isPhaseTwo = entity.isPhaseTwo();
        state.isDuelActive = entity.isDuelActive();
        state.attackAnim = entity.getSwingAnimation(partialTick);
        state.leapPhase = entity.getLeapAnimation(partialTick);
    }

    @Override
    protected void scale(ToadBossRenderState state, PoseStack poseStack) {
        float bossScale = state.isPhaseTwo ? 1.58F : 1.52F;
        poseStack.scale(bossScale, bossScale, bossScale);
    }

    @Override
    public Identifier getTextureLocation(ToadBossRenderState state) {
        return TEXTURE;
    }
}
