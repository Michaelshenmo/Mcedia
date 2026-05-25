package top.tobyprime.mcedia.api.stream;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameStreamTest {
    @Test
    void pollConditionOnlyConsumesWhenPredicateMatches() throws Exception {
        FrameStream<TestFrame> stream = new FrameStream<>(4);
        TestFrame frame = new TestFrame(100);
        stream.put(frame);

        assertNull(stream.poll(f -> f.time < 50));
        TestFrame out = stream.poll(f -> f.time == 100);
        assertSame(frame, out);
        out.close();
        assertEquals(1, frame.closedCount.get());
    }

    @Test
    void clearClosesQueuedFrames() throws Exception {
        FrameStream<TestFrame> stream = new FrameStream<>(4);
        TestFrame first = new TestFrame(1);
        TestFrame second = new TestFrame(2);
        stream.put(first);
        stream.put(second);

        stream.clear();

        assertEquals(1, first.closedCount.get());
        assertEquals(1, second.closedCount.get());
        assertEquals(0, stream.size());
    }

    @Test
    void closeClosesFramesAlreadyQueued() throws Exception {
        FrameStream<TestFrame> stream = new FrameStream<>(4);
        TestFrame first = new TestFrame(1);
        TestFrame second = new TestFrame(2);
        stream.put(first);
        stream.put(second);

        stream.close();

        assertEquals(1, first.closedCount.get());
        assertEquals(1, second.closedCount.get());
        assertEquals(0, stream.size());
    }

    @Test
    void closePreventsFuturePut() {
        FrameStream<TestFrame> stream = new FrameStream<>(1);
        stream.close();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> stream.put(new TestFrame(1)));
        assertEquals("FrameStream is closed", ex.getMessage());
    }

    @Test
    void closeWhilePutIsBlockedClosesTheLateFrame() throws Exception {
        FrameStream<TestFrame> stream = new FrameStream<>(1);
        TestFrame first = new TestFrame(1);
        TestFrame second = new TestFrame(2);
        stream.put(first);

        CountDownLatch attemptingPut = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                attemptingPut.countDown();
                stream.put(second);
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        thread.start();

        assertTrue(attemptingPut.await(1, TimeUnit.SECONDS));
        waitForBlockedPut(thread);

        stream.close();
        thread.join(1000);

        assertFalse(thread.isAlive());
        assertTrue(failure.get() instanceof IllegalStateException);
        assertEquals(1, first.closedCount.get());
        assertEquals(1, second.closedCount.get());
        assertEquals(0, stream.size());
    }

    @Test
    void clearContinuesWhenAFrameCloseFails() throws Exception {
        FrameStream<Closeable> stream = new FrameStream<>(4);
        stream.put(new ThrowingFrame());
        TestFrame second = new TestFrame(2);
        stream.put(second);

        stream.clear();

        assertEquals(1, second.closedCount.get());
        assertEquals(0, stream.size());
    }

    @Test
    void pollTransfersOwnershipToCaller() throws Exception {
        FrameStream<TestFrame> stream = new FrameStream<>(2);
        TestFrame frame = new TestFrame(7);
        stream.put(frame);

        TestFrame out = stream.poll();
        assertNotNull(out);
        assertEquals(0, frame.closedCount.get());

        out.close();
        assertEquals(1, frame.closedCount.get());
    }

    @Test
    void pollConditionDoesNotCloseUnmatchedHeadFrame() throws Exception {
        FrameStream<TestFrame> stream = new FrameStream<>(2);
        TestFrame frame = new TestFrame(7);
        stream.put(frame);

        assertNull(stream.poll(f -> f.time > 10));
        assertEquals(0, frame.closedCount.get());
        assertSame(frame, stream.peek());
        assertEquals(1, stream.size());
    }

    @Test
    void closeIsIdempotent() throws Exception {
        FrameStream<TestFrame> stream = new FrameStream<>(2);
        TestFrame frame = new TestFrame(7);
        stream.put(frame);

        stream.close();
        stream.close();

        assertEquals(1, frame.closedCount.get());
        assertEquals(0, stream.size());
    }

    private static void waitForBlockedPut(Thread thread) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING || state == Thread.State.BLOCKED) {
                return;
            }
            Thread.sleep(10);
        }
    }

    private static final class TestFrame implements Closeable {
        private final long time;
        private final AtomicInteger closedCount = new AtomicInteger(0);

        private TestFrame(long time) {
            this.time = time;
        }

        @Override
        public void close() throws IOException {
            closedCount.incrementAndGet();
        }
    }

    private static final class ThrowingFrame implements Closeable {
        @Override
        public void close() throws IOException {
            throw new IOException("close failed");
        }
    }
}
