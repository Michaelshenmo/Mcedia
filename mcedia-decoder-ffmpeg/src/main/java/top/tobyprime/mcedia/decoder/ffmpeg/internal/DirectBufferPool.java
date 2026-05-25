package top.tobyprime.mcedia.decoder.ffmpeg.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe tiered direct buffer pool for frame-level temporary buffers.
 */
public final class DirectBufferPool {
    private static final Object UNSAFE_INSTANCE;
    private static final Method UNSAFE_INVOKE_CLEANER;

    private static final long MAX_RETAINED_BYTES = 128L * 1024L * 1024L;
    private static final TieredPool POOL = new TieredPool(MAX_RETAINED_BYTES);

    static {
        Object unsafe = null;
        Method invokeCleaner = null;

        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = field.get(null);
            invokeCleaner = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
        } catch (ReflectiveOperationException ignored) {
        }

        UNSAFE_INSTANCE = unsafe;
        UNSAFE_INVOKE_CLEANER = invokeCleaner;
    }

    private DirectBufferPool() {
    }

    public static ByteBuffer allocBytes(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        return POOL.acquire(size);
    }

    public static ShortBuffer allocShorts(int elements) {
        int bytes = Math.multiplyExact(elements, Short.BYTES);
        ByteBuffer root = allocBytes(bytes);
        ShortBuffer view = root.order(ByteOrder.nativeOrder()).asShortBuffer();
        POOL.registerView(view, root);
        return view;
    }

    public static IntBuffer allocInts(int elements) {
        int bytes = Math.multiplyExact(elements, Integer.BYTES);
        ByteBuffer root = allocBytes(bytes);
        IntBuffer view = root.order(ByteOrder.nativeOrder()).asIntBuffer();
        POOL.registerView(view, root);
        return view;
    }

    public static FloatBuffer allocFloats(int elements) {
        int bytes = Math.multiplyExact(elements, Float.BYTES);
        ByteBuffer root = allocBytes(bytes);
        FloatBuffer view = root.order(ByteOrder.nativeOrder()).asFloatBuffer();
        POOL.registerView(view, root);
        return view;
    }

    public static int allocatedBytes(Buffer buffer) {
        if (buffer == null) {
            return 0;
        }

        return switch (buffer) {
            case ByteBuffer byteBuffer -> byteBuffer.isDirect() ? byteBuffer.capacity() : 0;
            case ShortBuffer shortBuffer -> shortBuffer.isDirect() ? shortBuffer.capacity() * Short.BYTES : 0;
            case IntBuffer intBuffer -> intBuffer.isDirect() ? intBuffer.capacity() * Integer.BYTES : 0;
            case FloatBuffer floatBuffer -> floatBuffer.isDirect() ? floatBuffer.capacity() * Float.BYTES : 0;
            default -> 0;
        };
    }

    public static void release(Buffer buffer) {
        if (buffer == null) {
            return;
        }
        POOL.release(buffer);
    }

    static void clearPoolForTests() {
        POOL.clearRetained();
    }

    static PoolStats poolStatsForTests() {
        return POOL.snapshot();
    }

    static final class PoolStats {
        private final long retainedBytes;
        private final int retainedBuffers;
        private final int ownedBuffers;

        PoolStats(long retainedBytes, int retainedBuffers, int ownedBuffers) {
            this.retainedBytes = retainedBytes;
            this.retainedBuffers = retainedBuffers;
            this.ownedBuffers = ownedBuffers;
        }

        long retainedBytes() {
            return retainedBytes;
        }

        int retainedBuffers() {
            return retainedBuffers;
        }

        int ownedBuffers() {
            return ownedBuffers;
        }
    }

    private enum BufferState {
        IN_USE,
        IN_POOL,
        DROPPED
    }

    private static final class BufferRecord {
        private final int tierSize;
        private BufferState state;
        private long leaseId;

        private BufferRecord(int tierSize) {
            this.tierSize = tierSize;
            this.state = BufferState.IN_USE;
        }
    }

    private static final class BufferHandle {
        private final ByteBuffer root;
        private final long leaseId;

        private BufferHandle(ByteBuffer root, long leaseId) {
            this.root = root;
            this.leaseId = leaseId;
        }
    }

    private static final class TieredPool {
        private static final int MAX_POWER_OF_TWO_TIER = 1 << 30;

        private final long maxRetainedBytes;
        private final ReentrantLock lock = new ReentrantLock();
        private final NavigableMap<Integer, ArrayDeque<ByteBuffer>> pooledByTier = new TreeMap<>();
        private final IdentityHashMap<ByteBuffer, BufferRecord> records = new IdentityHashMap<>();
        private final IdentityHashMap<Buffer, BufferHandle> handles = new IdentityHashMap<>();

        private long retainedBytes;
        private long nextLeaseId = 1L;

        private TieredPool(long maxRetainedBytes) {
            this.maxRetainedBytes = maxRetainedBytes;
        }

        private ByteBuffer acquire(int requestedBytes) {
            if (requestedBytes == 0) {
                return ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());
            }

            int tierSize = tierSizeFor(requestedBytes);
            ByteBuffer pooled = null;

            lock.lock();
            try {
                ArrayDeque<ByteBuffer> queue = pooledByTier.get(tierSize);
                while (queue != null) {
                    ByteBuffer candidate = queue.pollFirst();
                    if (candidate == null) {
                        break;
                    }

                    BufferRecord record = records.get(candidate);
                    if (record == null || record.state != BufferState.IN_POOL) {
                        continue;
                    }

                    record.state = BufferState.IN_USE;
                    retainedBytes -= record.tierSize;
                    assignLeaseLocked(candidate, record);
                    pooled = candidate;
                    break;
                }

                if (queue != null && queue.isEmpty()) {
                    pooledByTier.remove(tierSize);
                }
            } finally {
                lock.unlock();
            }

            if (pooled == null) {
                pooled = ByteBuffer.allocateDirect(tierSize).order(ByteOrder.nativeOrder());
                lock.lock();
                try {
                    BufferRecord record = new BufferRecord(tierSize);
                    records.put(pooled, record);
                    assignLeaseLocked(pooled, record);
                } finally {
                    lock.unlock();
                }
            }

            pooled.clear();
            pooled.limit(requestedBytes);
            pooled.order(ByteOrder.nativeOrder());
            return pooled;
        }

        private void registerView(Buffer view, ByteBuffer root) {
            lock.lock();
            try {
                BufferRecord record = records.get(root);
                if (record == null || record.state != BufferState.IN_USE) {
                    return;
                }
                handles.put(view, new BufferHandle(root, record.leaseId));
            } finally {
                lock.unlock();
            }
        }

        private void release(Buffer source) {
            List<ByteBuffer> toClean = null;
            lock.lock();
            try {
                BufferHandle handle = handles.remove(source);
                if (handle == null) {
                    return;
                }

                ByteBuffer root = handle.root;
                BufferRecord record = records.get(root);
                if (record == null || record.state != BufferState.IN_USE || record.leaseId != handle.leaseId) {
                    return;
                }

                // Drop all sibling handles from the same lease to prevent stale-view re-release.
                purgeLeaseHandlesLocked(root, handle.leaseId);

                root.clear();
                root.order(ByteOrder.nativeOrder());

                toClean = evictUntilFitsLocked(record.tierSize);
                if (record.tierSize > 0 && retainedBytes + record.tierSize <= maxRetainedBytes) {
                    pooledByTier.computeIfAbsent(record.tierSize, ignored -> new ArrayDeque<>()).offerFirst(root);
                    retainedBytes += record.tierSize;
                    record.state = BufferState.IN_POOL;
                } else {
                    records.remove(root);
                    record.state = BufferState.DROPPED;
                    if (toClean == null) {
                        toClean = new ArrayList<>();
                    }
                    toClean.add(root);
                }
            } finally {
                lock.unlock();
            }

            if (toClean != null) {
                for (ByteBuffer buffer : toClean) {
                    cleanDirectBuffer(buffer);
                }
            }
        }

        private List<ByteBuffer> evictUntilFitsLocked(int incomingBytes) {
            if (incomingBytes <= 0 || retainedBytes + incomingBytes <= maxRetainedBytes) {
                return null;
            }

            List<ByteBuffer> evicted = new ArrayList<>();
            while (retainedBytes + incomingBytes > maxRetainedBytes) {
                ByteBuffer victim = pollVictimLocked();
                if (victim == null) {
                    break;
                }

                BufferRecord victimRecord = records.remove(victim);
                if (victimRecord == null || victimRecord.state != BufferState.IN_POOL) {
                    continue;
                }

                victimRecord.state = BufferState.DROPPED;
                retainedBytes -= victimRecord.tierSize;
                evicted.add(victim);
            }

            return evicted.isEmpty() ? null : evicted;
        }

        private ByteBuffer pollVictimLocked() {
            Iterator<Map.Entry<Integer, ArrayDeque<ByteBuffer>>> iterator = pooledByTier.descendingMap().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, ArrayDeque<ByteBuffer>> entry = iterator.next();
                ArrayDeque<ByteBuffer> queue = entry.getValue();
                ByteBuffer victim = queue.pollLast();
                if (queue.isEmpty()) {
                    iterator.remove();
                }
                if (victim != null) {
                    return victim;
                }
            }
            return null;
        }

        private PoolStats snapshot() {
            lock.lock();
            try {
                int retainedBuffers = 0;
                for (ArrayDeque<ByteBuffer> queue : pooledByTier.values()) {
                    retainedBuffers += queue.size();
                }
                return new PoolStats(retainedBytes, retainedBuffers, records.size());
            } finally {
                lock.unlock();
            }
        }

        private void clearRetained() {
            List<ByteBuffer> toClean = new ArrayList<>();
            lock.lock();
            try {
                for (Iterator<Map.Entry<ByteBuffer, BufferRecord>> iterator = records.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<ByteBuffer, BufferRecord> entry = iterator.next();
                    BufferRecord record = entry.getValue();
                    if (record.state == BufferState.IN_POOL) {
                        record.state = BufferState.DROPPED;
                        toClean.add(entry.getKey());
                        iterator.remove();
                    }
                }

                for (Iterator<Map.Entry<Buffer, BufferHandle>> iterator = handles.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<Buffer, BufferHandle> entry = iterator.next();
                    BufferRecord record = records.get(entry.getValue().root);
                    if (record == null || record.state != BufferState.IN_USE) {
                        iterator.remove();
                    }
                }

                pooledByTier.clear();
                retainedBytes = 0;
            } finally {
                lock.unlock();
            }

            for (ByteBuffer buffer : toClean) {
                cleanDirectBuffer(buffer);
            }
        }

        private int tierSizeFor(int requestedBytes) {
            if (requestedBytes <= 0) {
                return 0;
            }
            if (requestedBytes > MAX_POWER_OF_TWO_TIER) {
                return requestedBytes;
            }

            int normalized = requestedBytes - 1;
            int shift = Integer.SIZE - Integer.numberOfLeadingZeros(normalized);
            return 1 << shift;
        }

        private void assignLeaseLocked(ByteBuffer root, BufferRecord record) {
            long leaseId = nextLeaseId++;
            record.leaseId = leaseId;
            handles.put(root, new BufferHandle(root, leaseId));
        }

        private void purgeLeaseHandlesLocked(ByteBuffer root, long leaseId) {
            for (Iterator<Map.Entry<Buffer, BufferHandle>> iterator = handles.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<Buffer, BufferHandle> entry = iterator.next();
                BufferHandle handle = entry.getValue();
                if (handle.root == root && handle.leaseId == leaseId) {
                    iterator.remove();
                }
            }
        }
    }

    private static void cleanDirectBuffer(ByteBuffer buffer) {
        if (!buffer.isDirect()) {
            return;
        }
        if (UNSAFE_INSTANCE == null || UNSAFE_INVOKE_CLEANER == null) {
            return;
        }
        try {
            UNSAFE_INVOKE_CLEANER.invoke(UNSAFE_INSTANCE, buffer);
        } catch (ReflectiveOperationException ignored) {
            // Best effort: if cleaner invoke fails, JVM GC will reclaim it later.
        }
    }
}
