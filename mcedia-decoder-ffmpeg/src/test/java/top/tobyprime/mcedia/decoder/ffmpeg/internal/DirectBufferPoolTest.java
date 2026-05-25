package top.tobyprime.mcedia.decoder.ffmpeg.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.*;

import static org.junit.jupiter.api.Assertions.*;

class DirectBufferPoolTest {
    @AfterEach
    void cleanupPool() {
        DirectBufferPool.clearPoolForTests();
    }

    @Test
    void allocBytesRoundsToTierAndUsesRequestedLimit() {
        ByteBuffer buf = DirectBufferPool.allocBytes(10);
        assertTrue(buf.capacity() >= 10);
        assertTrue(isPowerOfTwoOrZero(buf.capacity()));
        assertEquals(10, buf.limit());
        assertEquals(0, buf.position());
        assertEquals(ByteOrder.nativeOrder(), buf.order());

        DirectBufferPool.release(buf);
    }

    @Test
    void allocatedBytesUsesBufferTypeCapacity() {
        ByteBuffer bytes = DirectBufferPool.allocBytes(64);
        assertEquals(bytes.capacity(), DirectBufferPool.allocatedBytes(bytes));
        DirectBufferPool.release(bytes);

        ShortBuffer shorts = DirectBufferPool.allocShorts(7);
        assertTrue(shorts.capacity() >= 7);
        assertEquals(shorts.capacity() * Short.BYTES, DirectBufferPool.allocatedBytes(shorts));
        DirectBufferPool.release(shorts);

        IntBuffer ints = DirectBufferPool.allocInts(5);
        assertTrue(ints.capacity() >= 5);
        assertEquals(ints.capacity() * Integer.BYTES, DirectBufferPool.allocatedBytes(ints));
        DirectBufferPool.release(ints);

        FloatBuffer floats = DirectBufferPool.allocFloats(9);
        assertTrue(floats.capacity() >= 9);
        assertEquals(floats.capacity() * Float.BYTES, DirectBufferPool.allocatedBytes(floats));
        DirectBufferPool.release(floats);
    }

    @Test
    void allocatedBytesReturnsZeroForHeapAndNull() {
        assertEquals(0, DirectBufferPool.allocatedBytes(null));
        assertEquals(0, DirectBufferPool.allocatedBytes(ByteBuffer.allocate(16)));
        assertEquals(0, DirectBufferPool.allocatedBytes(ShortBuffer.allocate(8)));
    }

    @Test
    void releaseViewAndExternalDirectBufferDoNotThrow() {
        ShortBuffer pooledView = DirectBufferPool.allocShorts(32);
        assertDoesNotThrow(() -> DirectBufferPool.release(pooledView));

        long retainedBeforeExternal = DirectBufferPool.poolStatsForTests().retainedBytes();
        ByteBuffer externalDirect = ByteBuffer.allocateDirect(32);
        assertDoesNotThrow(() -> DirectBufferPool.release(externalDirect));
        assertEquals(retainedBeforeExternal, DirectBufferPool.poolStatsForTests().retainedBytes());
    }

    @Test
    void releaseNullIsNoop() {
        assertDoesNotThrow(() -> DirectBufferPool.release(null));
    }

    @Test
    void allocBytesRejectsNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> DirectBufferPool.allocBytes(-1));
    }

    @Test
    void typedAllocationsValidateElementOverflow() {
        assertThrows(ArithmeticException.class, () -> DirectBufferPool.allocShorts(Integer.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> DirectBufferPool.allocInts(Integer.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> DirectBufferPool.allocFloats(Integer.MAX_VALUE));
    }

    @Test
    void allocIntAndFloatViewsUseNativeOrderAndExpectedLimits() {
        IntBuffer ints = DirectBufferPool.allocInts(5);
        assertEquals(5, ints.limit());
        assertEquals(ByteOrder.nativeOrder(), ints.order());
        DirectBufferPool.release(ints);

        FloatBuffer floats = DirectBufferPool.allocFloats(7);
        assertEquals(7, floats.limit());
        assertEquals(ByteOrder.nativeOrder(), floats.order());
        DirectBufferPool.release(floats);
    }

    @Test
    void releasedBufferCanBeReusedInSameTier() {
        ByteBuffer first = DirectBufferPool.allocBytes(33); // tier -> 64
        int tierSize = first.capacity();
        DirectBufferPool.release(first);
        assertEquals(1, DirectBufferPool.poolStatsForTests().retainedBuffers());

        ByteBuffer second = DirectBufferPool.allocBytes(40); // same tier
        assertSame(first, second);
        assertEquals(tierSize, second.capacity());
        assertEquals(40, second.limit());
        DirectBufferPool.release(second);
    }

    @Test
    void releaseUntrackedAliasIsNoopAndOriginalStillReleases() {
        ShortBuffer original = DirectBufferPool.allocShorts(32);
        ShortBuffer alias = original.duplicate();

        assertDoesNotThrow(() -> DirectBufferPool.release(alias));
        assertEquals(0, DirectBufferPool.poolStatsForTests().retainedBuffers());

        assertDoesNotThrow(() -> DirectBufferPool.release(original));
        assertEquals(1, DirectBufferPool.poolStatsForTests().retainedBuffers());
    }

    @Test
    void staleHandleCannotReleaseNextLease() {
        ShortBuffer firstLease = DirectBufferPool.allocShorts(32);
        DirectBufferPool.release(firstLease);
        assertEquals(1, DirectBufferPool.poolStatsForTests().retainedBuffers());

        ShortBuffer secondLease = DirectBufferPool.allocShorts(32);
        assertEquals(0, DirectBufferPool.poolStatsForTests().retainedBuffers());

        // stale handle from previous lease must be ignored
        assertDoesNotThrow(() -> DirectBufferPool.release(firstLease));
        assertEquals(0, DirectBufferPool.poolStatsForTests().retainedBuffers());

        assertDoesNotThrow(() -> DirectBufferPool.release(secondLease));
        assertEquals(1, DirectBufferPool.poolStatsForTests().retainedBuffers());
    }

    @Test
    void zeroSizeAllocationsAreNotRetainedInPool() {
        ByteBuffer first = DirectBufferPool.allocBytes(0);
        DirectBufferPool.release(first);
        assertEquals(0, DirectBufferPool.poolStatsForTests().retainedBuffers());

        ByteBuffer second = DirectBufferPool.allocBytes(0);
        assertNotSame(first, second);
        DirectBufferPool.release(second);
        assertEquals(0, DirectBufferPool.poolStatsForTests().retainedBuffers());
    }

    private static boolean isPowerOfTwoOrZero(int value) {
        return value == 0 || (value & (value - 1)) == 0;
    }
}
