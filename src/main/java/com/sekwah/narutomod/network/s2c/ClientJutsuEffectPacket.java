package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.events.ClientEffectHandler;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Lightweight S2C packet for instant client-side jutsu visual/particle bursts
 * without spawning server-ticked visual entities.
 */
public class ClientJutsuEffectPacket {

    private static final int FIXED_TAIL_BYTES = (3 * Double.BYTES) + (3 * Double.BYTES) + Float.BYTES + Integer.BYTES; // 56 bytes

    public final ResourceLocation effectId;
    public final Vec3 position;
    public final Vec3 direction;
    public final float scale;
    public final int colorHex;

    public ClientJutsuEffectPacket(ResourceLocation effectId, Vec3 position, Vec3 direction, float scale, int colorHex) {
        this.effectId = effectId;
        this.position = position;
        this.direction = direction;
        this.scale = scale;
        this.colorHex = colorHex;
    }

    public static void encode(ClientJutsuEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.effectId);
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
        ResourceLocation effectId = buf.readResourceLocation();
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

    public static class Handler {
        public static void handle(ClientJutsuEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEffectHandler.handleJutsuEffect(msg))
            );
            ctx.get().setPacketHandled(true);
        }
    }
}
