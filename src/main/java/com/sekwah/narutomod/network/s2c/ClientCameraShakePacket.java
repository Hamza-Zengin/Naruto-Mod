package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.NarutoMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Lightweight S2C packet to trigger a damped client-side camera shake.
 */
public class ClientCameraShakePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientCameraShakePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "client_camera_shake"));

    public static final StreamCodec<FriendlyByteBuf, ClientCameraShakePacket> STREAM_CODEC =
            StreamCodec.ofMember(ClientCameraShakePacket::encode, ClientCameraShakePacket::decode);

    private static final int PAYLOAD_BYTES = Float.BYTES + Integer.BYTES; // 8 bytes
    private static final int MAX_DURATION_TICKS = 1200; // 60 seconds cap

    public final float intensity;
    public final int durationTicks;

    public ClientCameraShakePacket(float intensity, int durationTicks) {
        this.intensity = intensity;
        this.durationTicks = durationTicks;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientCameraShakePacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.intensity);
        buf.writeInt(msg.durationTicks);
    }

    public static ClientCameraShakePacket decode(FriendlyByteBuf buf) {
        if (buf.readableBytes() < PAYLOAD_BYTES) {
            throw new DecoderException("Truncated ClientCameraShakePacket payload: expected "
                    + PAYLOAD_BYTES + " bytes, got " + buf.readableBytes());
        }
        float intensity = buf.readFloat();
        int durationTicks = buf.readInt();

        if (!Float.isFinite(intensity) || intensity < 0.0f || durationTicks < 0 || durationTicks > MAX_DURATION_TICKS) {
            throw new DecoderException("Out-of-bounds ClientCameraShakePacket values: intensity="
                    + intensity + ", durationTicks=" + durationTicks);
        }

        return new ClientCameraShakePacket(intensity, durationTicks);
    }
}
