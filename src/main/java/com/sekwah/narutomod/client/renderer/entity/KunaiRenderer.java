package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

public class KunaiRenderer extends ArrowRenderer<KunaiEntity, ArrowRenderState> {
   public static final Identifier RES_ARROW = Identifier.fromNamespaceAndPath("narutomod", "textures/entity/projectiles/kunai.png");

   public KunaiRenderer(EntityRendererProvider.Context manager) {
      super(manager);
   }

   @Override
   public ArrowRenderState createRenderState() {
      return new ArrowRenderState();
   }

   @Override
   protected Identifier getTextureLocation(ArrowRenderState state) {
      return RES_ARROW;
   }
}
