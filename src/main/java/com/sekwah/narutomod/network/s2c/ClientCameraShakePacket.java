package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.NarutoMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight S2C packet to trigger a damped client-side camera shake.
 */
public class ClientCameraShakePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientCameraShakePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "client_camera_shake"));

    public static final StreamCodec<FriendlyByteBuf, ClientCameraShakePacket> STREAM_CODEC =
            StreamCodec.ofMember(ClientCameraShakePacket::encode, ClientCameraShakePacket::decode);

    private static final int BASE_PAYLOAD_BYTES = Float.BYTES + Integer.BYTES + 1;
    private static final int ORIGIN_BYTES = Double.BYTES * 3 + Float.BYTES;
    private static final int MAX_DURATION_TICKS = 1200; // 60 seconds cap

    public final float intensity;
    public final int durationTicks;
    public final boolean hasOrigin;
    public final Vec3 origin;
    public final float maxRadius;

    public ClientCameraShakePacket(float intensity, int durationTicks) {
        this.intensity = intensity;
        this.durationTicks = durationTicks;
        this.hasOrigin = false;
        this.origin = Vec3.ZERO;
        this.maxRadius = 0.0f;
    }

    public ClientCameraShakePacket(float intensity, int durationTicks, Vec3 origin, float maxRadius) {
        this.intensity = intensity;
        this.durationTicks = durationTicks;
        this.hasOrigin = true;
        this.origin = origin;
        this.maxRadius = maxRadius;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientCameraShakePacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.intensity);
        buf.writeInt(msg.durationTicks);
        buf.writeBoolean(msg.hasOrigin);
        if (msg.hasOrigin) {
            buf.writeDouble(msg.origin.x);
            buf.writeDouble(msg.origin.y);
            buf.writeDouble(msg.origin.z);
            buf.writeFloat(msg.maxRadius);
        }
    }

    public static ClientCameraShakePacket decode(FriendlyByteBuf buf) {
        if (buf.readableBytes() < BASE_PAYLOAD_BYTES) {
            throw new DecoderException("Truncated ClientCameraShakePacket payload: expected "
                    + BASE_PAYLOAD_BYTES + " bytes, got " + buf.readableBytes());
        }
        float intensity = buf.readFloat();
        int durationTicks = buf.readInt();
        boolean hasOrigin = buf.readBoolean();

        if (!Float.isFinite(intensity) || intensity < 0.0f || durationTicks < 0 || durationTicks > MAX_DURATION_TICKS) {
            throw new DecoderException("Out-of-bounds ClientCameraShakePacket values: intensity="
                    + intensity + ", durationTicks=" + durationTicks);
        }

        if (hasOrigin) {
            if (buf.readableBytes() < ORIGIN_BYTES) {
                throw new DecoderException("Truncated ClientCameraShakePacket origin payload");
            }
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float maxRadius = buf.readFloat();
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(maxRadius) || maxRadius <= 0.0f) {
                throw new DecoderException("Invalid origin or maxRadius in ClientCameraShakePacket");
            }
            return new ClientCameraShakePacket(intensity, durationTicks, new Vec3(x, y, z), maxRadius);
        }

        return new ClientCameraShakePacket(intensity, durationTicks);
    }
}
