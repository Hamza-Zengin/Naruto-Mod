package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.ClientCameraShakePacket;
import com.sekwah.narutomod.network.s2c.ClientFlashEffectPacket;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for S2C packet encode/decode round-trip accuracy and bounds checking.
 */
public class S2CPacketBoundsTest {

    @Test
    void testClientJutsuEffectPacketRoundTrip() {
        Identifier id = Identifier.fromNamespaceAndPath("narutomod", "kirin_impact");
        Vec3 pos = new Vec3(12.5, 64.0, -30.25);
        Vec3 dir = new Vec3(0.0, 1.0, 0.0);
        ClientJutsuEffectPacket original = new ClientJutsuEffectPacket(id, pos, dir, 1.5f, 0x66CCFF);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ClientJutsuEffectPacket.encode(original, buf);

        ClientJutsuEffectPacket decoded = ClientJutsuEffectPacket.decode(buf);
        assertEquals(id, decoded.effectId);
        assertEquals(pos.x, decoded.position.x, 1e-6);
        assertEquals(pos.y, decoded.position.y, 1e-6);
        assertEquals(pos.z, decoded.position.z, 1e-6);
        assertEquals(dir.y, decoded.direction.y, 1e-6);
        assertEquals(1.5f, decoded.scale, 1e-6);
        assertEquals(0x66CCFF, decoded.colorHex);
    }

    @Test
    void testClientJutsuEffectPacketRejectsEmptyOrTruncatedBuffer() {
        FriendlyByteBuf empty = new FriendlyByteBuf(Unpooled.buffer());
        assertThrows(DecoderException.class, () -> ClientJutsuEffectPacket.decode(empty));

        FriendlyByteBuf truncated = new FriendlyByteBuf(Unpooled.buffer());
        truncated.writeIdentifier(Identifier.fromNamespaceAndPath("narutomod", "hakke_kusho"));
        truncated.writeDouble(1.0); // Missing remaining fields
        assertThrows(DecoderException.class, () -> ClientJutsuEffectPacket.decode(truncated));
    }

    @Test
    void testClientCameraShakePacketRoundTrip() {
        ClientCameraShakePacket original = new ClientCameraShakePacket(1.25f, 16);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ClientCameraShakePacket.encode(original, buf);

        ClientCameraShakePacket decoded = ClientCameraShakePacket.decode(buf);
        assertEquals(1.25f, decoded.intensity, 1e-6);
        assertEquals(16, decoded.durationTicks);
    }

    @Test
    void testClientCameraShakePacketRejectsOutOfBoundsAndTruncated() {
        FriendlyByteBuf truncated = new FriendlyByteBuf(Unpooled.buffer());
        truncated.writeFloat(1.0f); // Missing int durationTicks
        assertThrows(DecoderException.class, () -> ClientCameraShakePacket.decode(truncated));

        FriendlyByteBuf negativeDuration = new FriendlyByteBuf(Unpooled.buffer());
        negativeDuration.writeFloat(1.0f);
        negativeDuration.writeInt(-10);
        assertThrows(DecoderException.class, () -> ClientCameraShakePacket.decode(negativeDuration));

        FriendlyByteBuf excessiveDuration = new FriendlyByteBuf(Unpooled.buffer());
        excessiveDuration.writeFloat(1.0f);
        excessiveDuration.writeInt(999999);
        assertThrows(DecoderException.class, () -> ClientCameraShakePacket.decode(excessiveDuration));
    }

    @Test
    void testClientFlashEffectPacketRoundTripAndClamping() {
        ClientFlashEffectPacket clampedHigh = new ClientFlashEffectPacket(2.5f, 5000);
        assertEquals(1.0f, clampedHigh.intensity(), 1e-6);
        assertEquals(1200, clampedHigh.durationTicks());

        ClientFlashEffectPacket clampedLow = new ClientFlashEffectPacket(-0.5f, -15);
        assertEquals(0.0f, clampedLow.intensity(), 1e-6);
        assertEquals(0, clampedLow.durationTicks());

        ClientFlashEffectPacket original = new ClientFlashEffectPacket(0.85f, 70);
        ByteBuf buf = Unpooled.buffer();
        ClientFlashEffectPacket.STREAM_CODEC.encode(buf, original);
        ClientFlashEffectPacket decoded = ClientFlashEffectPacket.STREAM_CODEC.decode(buf);
        assertEquals(0.85f, decoded.intensity(), 1e-6);
        assertEquals(70, decoded.durationTicks());
    }
}
