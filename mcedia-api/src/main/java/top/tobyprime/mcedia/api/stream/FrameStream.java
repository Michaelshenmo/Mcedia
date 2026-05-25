package top.tobyprime.mcedia.api.stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * 帧流容器。
 * <p>
 * 所有权约定：
 * 1) 入队后由 FrameStream 持有帧；
 * 2) 调用 poll() / poll(condition) 成功出队后，所有权转移给调用方，调用方必须负责 close()；
 * 3) 仍留在队列中的帧由 clear()/close() 统一释放。
 */
public class FrameStream<TFrame extends Closeable> implements Closeable {
    private final LinkedBlockingQueue<TFrame> queue;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public FrameStream(int maxFrames) {
        queue = new LinkedBlockingQueue<>(maxFrames);
    }

    public long size() {
        return queue.size();
    }

    /**
     * 出队一帧并将所有权转移给调用方；返回非空时调用方必须 close()。
     */
    public @Nullable TFrame poll() {
        return queue.poll();
    }

    /**
     * 条件满足时出队并转移所有权给调用方；返回非空时调用方必须 close()。
     */
    public @Nullable TFrame poll(@NotNull Predicate<TFrame> condition) {
        synchronized (this) {
            var data = queue.peek();
            if (data == null) return null;
            if (condition.test(data)) {
                return queue.poll();
            }
            return null;
        }
    }

    public @Nullable TFrame peek() {
        return queue.peek();
    }

    public void put(@NotNull TFrame frame) throws InterruptedException {
        if (closed.get()) {
            throw new IllegalStateException("FrameStream is closed");
        }
        queue.put(frame);
        if (closed.get() && queue.remove(frame)) {
            closeQuietly(frame);
            throw new IllegalStateException("FrameStream is closed");
        }
    }

    public void clear() {
        synchronized (this) {
            while (true) {
                var frame = queue.poll();
                if (frame == null) break;
                closeQuietly(frame);
            }
            queue.clear();
        }
    }

    @Override
    public void close() {
        closed.set(true);
        clear();
    }

    private void closeQuietly(@NotNull TFrame frame) {
        try {
            frame.close();
        } catch (IOException ignored) {
        }
    }
}
