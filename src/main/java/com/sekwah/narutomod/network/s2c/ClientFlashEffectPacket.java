package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.gui.FlashOverlayManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C packet triggering the client-side white flashbang screen overlay.
 */
public record ClientFlashEffectPacket(float intensity, int durationTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientFlashEffectPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "client_flash_effect"));

    public static final StreamCodec<ByteBuf, ClientFlashEffectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            ClientFlashEffectPacket::intensity,
            ByteBufCodecs.VAR_INT,
            ClientFlashEffectPacket::durationTicks,
            ClientFlashEffectPacket::new
    );

    public ClientFlashEffectPacket {
        if (!Float.isFinite(intensity)) {
            intensity = 0.0F;
        }
        intensity = Math.max(0.0F, Math.min(1.0F, intensity));
        durationTicks = Math.max(0, Math.min(1200, durationTicks));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ClientFlashEffectPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> FlashOverlayManager.triggerFlash(packet.intensity(), packet.durationTicks()));
    }
}
