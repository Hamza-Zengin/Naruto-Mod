package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.NarutoMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight S2C packet for instant client-side jutsu visual/particle bursts
 * without spawning server-ticked visual entities.
 */
public class ClientJutsuEffectPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientJutsuEffectPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "client_jutsu_effect"));

    public static final StreamCodec<FriendlyByteBuf, ClientJutsuEffectPacket> STREAM_CODEC =
            StreamCodec.ofMember(ClientJutsuEffectPacket::encode, ClientJutsuEffectPacket::decode);

    private static final int FIXED_TAIL_BYTES = (3 * Double.BYTES) + (3 * Double.BYTES) + Float.BYTES + Integer.BYTES; // 56 bytes

    public final Identifier effectId;
    public final Vec3 position;
    public final Vec3 direction;
    public final float scale;
    public final int colorHex;

    public ClientJutsuEffectPacket(Identifier effectId, Vec3 position, Vec3 direction, float scale, int colorHex) {
        this.effectId = effectId;
        this.position = position;
        this.direction = direction;
        this.scale = scale;
        this.colorHex = colorHex;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientJutsuEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeIdentifier(msg.effectId);
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
        buf.writeDouble(msg.direction.x);
        buf.writeDouble(msg.direction.y);
        buf.writeDouble(msg.direction.z);
        buf.writeFloat(msg.scale);
        buf.writeInt(msg.colorHex);
    }

    public static ClientJutsuEffectPacket decode(FriendlyByteBuf buf) {
        if (buf.readableBytes() < 1) {
            throw new DecoderException("Empty ClientJutsuEffectPacket buffer");
        }
        Identifier effectId = buf.readIdentifier();
        if (buf.readableBytes() < FIXED_TAIL_BYTES) {
            throw new DecoderException("Truncated ClientJutsuEffectPacket payload: expected "
                    + FIXED_TAIL_BYTES + " bytes, got " + buf.readableBytes());
        }
        double px = buf.readDouble();
        double py = buf.readDouble();
        double pz = buf.readDouble();
        double dx = buf.readDouble();
        double dy = buf.readDouble();
        double dz = buf.readDouble();
        float scale = buf.readFloat();
        int colorHex = buf.readInt();

        if (!Double.isFinite(px) || !Double.isFinite(py) || !Double.isFinite(pz)
                || !Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)
                || !Float.isFinite(scale) || scale < 0.0f) {
            throw new DecoderException("Invalid non-finite or negative values in ClientJutsuEffectPacket");
        }

        return new ClientJutsuEffectPacket(effectId, new Vec3(px, py, pz), new Vec3(dx, dy, dz), scale, colorHex);
    }
}
