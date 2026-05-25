package top.tobyprime.mcedia.player.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class McediaExecutors {
    public static ExecutorService ioExecutor =
            Executors.newFixedThreadPool(4, new ThreadFactory() {
                private final AtomicInteger index = new AtomicInteger(1);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setName("mcedia-io-executor-" + index.getAndIncrement());
                    return t;
                }
            });
    public static ThreadFactory decoderThreadFactory = new ThreadFactory() {
        private final AtomicInteger index = new AtomicInteger(1);

        public Thread newThread(@NotNull Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("mcedia-decoder-thread-" + index.getAndIncrement());
            return t;
        }
    };
}
