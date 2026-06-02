package top.tobyprime.mcedia.player.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.tobyprime.mcedia.api.audio.AudioSource;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.Media;
import top.tobyprime.mcedia.api.player.MediaPlay;
import top.tobyprime.mcedia.api.player.MediaPlayer;
import top.tobyprime.mcedia.api.player.PlaybackState;
import top.tobyprime.mcedia.api.video.MediaTexture;
import top.tobyprime.mcedia.player.internal.MediaPlayImpl;
import top.tobyprime.mcedia.player.internal.processors.AudioProcessor;
import top.tobyprime.mcedia.player.internal.processors.VideoProcessor;
import top.tobyprime.mcedia.player.runtime.McediaExecutors;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class SingleMediaPlayer implements MediaPlayer {
    final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<@Nullable CompletableFuture<MediaPlay>> loadFuture = new AtomicReference<>(null);
    private final AtomicLong loadRequestId = new AtomicLong(0);
    private final AtomicLong seekRequestId = new AtomicLong(0);
    private final AtomicReference<CompletableFuture<Void>> seekFuture = new AtomicReference<>(CompletableFuture.completedFuture(null));
    private final AudioProcessor audioProcessor = new AudioProcessor();
    private final VideoProcessor videoProcessor = new VideoProcessor();
    private boolean paused = false;
    private double speed = 1;
    private boolean runtimeVideoEnabled = true;
    private boolean runtimeAudioEnabled = true;
    private DecoderConfiguration decoderConfiguration = new DecoderConfiguration(new DecoderConfiguration.Builder());
    private final AtomicBoolean transitionScheduled = new AtomicBoolean(false);

    @Nullable
    private volatile MediaPlayImpl mediaPlay;
    private boolean lowOverhead;
    private volatile boolean decoderSuspended;
    private volatile @Nullable Throwable loadError;

    public CompletableFuture<MediaPlay> playAsync(Supplier<Media> mediaSupplier) {
        seekRequestId.incrementAndGet();
        var requestId = loadRequestId.incrementAndGet();
        loadError = null;
        var newFuture = CompletableFuture.supplyAsync(mediaSupplier, McediaExecutors.ioExecutor).thenApply(media -> {
            if (requestId != loadRequestId.get()) {
                throw new CancellationException("stale media load request");
            }

            var play = new MediaPlayImpl(media, audioProcessor, videoProcessor);

            lock.lock();
            try {
                if (requestId != loadRequestId.get()) {
                    throw new CancellationException("stale media load request");
                }
                var preMedia = this.mediaPlay;
                if (preMedia != null) {
                    preMedia.close();
                }
                this.mediaPlay = play;
            } finally {
                lock.unlock();
            }

            play.open(decoderConfiguration);
            play.setSpeed(speed);
            if (paused) play.pause();
            else play.play();
            reconcileTargetState(play);
            return (MediaPlay) play;
        });
        var preFuture = this.loadFuture.getAndSet(newFuture);
        if (preFuture != null && !preFuture.isDone()) {
            preFuture.cancel(true);
        }

        var trackedFuture = newFuture.whenComplete((result, error) -> {
            if (error != null && requestId == loadRequestId.get()) {
                loadError = error;
            }
        });
        return trackedFuture;
    }

    public @Nullable MediaPlay getMedia() {
        return mediaPlay;
    }

    public PlaybackState getPlaybackState() {
        var lf = loadFuture.get();
        if (lf != null && !lf.isDone()) {
            return PlaybackState.LOADING;
        }

        if (loadError != null) {
            return PlaybackState.ERROR;
        }

        var mp = mediaPlay;
        if (mp == null) {
            return PlaybackState.IDLE;
        }

        if (mp.isEnded()) {
            return PlaybackState.ENDED;
        }

        if (mp.isPaused()) {
            return PlaybackState.PAUSED;
        }

        return PlaybackState.PLAYING;
    }

    public @Nullable String getErrorMessage() {
        var error = loadError;
        if (error == null) return null;
        var cursor = error;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        var msg = cursor.getMessage();
        return msg != null && !msg.isBlank() ? msg : cursor.getClass().getSimpleName();
    }

    public CompletableFuture<Void> seekAsync(long time) {
        var requestId = seekRequestId.incrementAndGet();
        var newFuture = seekFuture.updateAndGet(previous -> previous.handle((ignored, throwable) -> null)
                .thenCompose(ignored -> beginSeek(requestId, time)));
        return newFuture;
    }

    private CompletableFuture<Void> beginSeek(long requestId, long time) {
        var currentLoad = loadFuture.get();
        if (currentLoad != null && !currentLoad.isDone()) {
            return currentLoad.handle((ignored, throwable) -> null)
                    .thenRunAsync(() -> {
                        var target = mediaPlay;
                        if (target != null) {
                            seekCurrentMedia(requestId, target, time);
                        }
                    }, McediaExecutors.ioExecutor);
        }

        var target = mediaPlay;
        if (target == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> seekCurrentMedia(requestId, target, time), McediaExecutors.ioExecutor);
    }

    private void seekCurrentMedia(long requestId, MediaPlayImpl target, long time) {
        if (requestId != seekRequestId.get()) {
            return;
        }

        lock.lock();
        try {
            if (requestId != seekRequestId.get() || mediaPlay != target) {
                return;
            }
            target.seek(time);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        loadRequestId.incrementAndGet();
        seekRequestId.incrementAndGet();
        var pendingLoad = loadFuture.getAndSet(null);
        if (pendingLoad != null && !pendingLoad.isDone()) {
            pendingLoad.cancel(true);
        }
        MediaPlayImpl target;
        this.lock.lock();
        try {
            target = mediaPlay;
            mediaPlay = null;
        } finally {
            this.lock.unlock();
        }
        if (target != null) {
            McediaExecutors.ioExecutor.execute(target::close);
        }
        decoderSuspended = false;
        loadError = null;
    }

    @Override
    public void tickVideo() {
        if (!this.lock.tryLock()) {
            return;
        }
        try {
            if (this.mediaPlay != null) {
                this.mediaPlay.tickVideo();
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void tickAudio() {
        var target = this.mediaPlay;
        if (target == null) {
            return;
        }
        target.tickAudio();
    }

    @Override
    public void bindTexture(@Nullable MediaTexture texture) {
        this.videoProcessor.bindTexture(texture);
    }

    @Override
    public void bindAudioSource(@NotNull AudioSource audioSource) {
        this.audioProcessor.bindAudioSources(audioSource);
    }

    @Override
    public void unbindAudioSource(@NotNull AudioSource audioSource) {
        this.audioProcessor.unbindAudioSources(audioSource);
    }

    @Override
    public DecoderConfiguration getDecoderConfiguration() {
        return decoderConfiguration;
    }

    @Override
    public void setDecoderConfiguration(@NotNull DecoderConfiguration decoderConfiguration) {
        this.decoderConfiguration = Objects.requireNonNull(decoderConfiguration, "decoderConfiguration");
        MediaPlayImpl target;
        this.lock.lock();
        try {
            target = mediaPlay;
        } finally {
            this.lock.unlock();
        }
        if (target == null) {
            return;
        }
        McediaExecutors.ioExecutor.execute(() -> {
            this.lock.lock();
            try {
                if (mediaPlay != target) {
                    return;
                }
            } finally {
                this.lock.unlock();
            }
            target.open(this.decoderConfiguration);
            reconcileTargetState(target);
        });
    }

    @Override
    public double getSpeed() {
        return this.speed;
    }

    @Override
    public void setSpeed(double speed) {
        this.speed = speed;
        scheduleTransitionIfNeeded();
    }

    @Override
    public void setLowOverhead(boolean lowOverhead) {
        this.lowOverhead = lowOverhead;
        scheduleTransitionIfNeeded();
    }

    public void setRuntimeVideoEnabled(boolean enabled) {
        this.runtimeVideoEnabled = enabled;
        scheduleTransitionIfNeeded();
    }

    public void setRuntimeAudioEnabled(boolean enabled) {
        this.runtimeAudioEnabled = enabled;
        scheduleTransitionIfNeeded();
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        scheduleTransitionIfNeeded();
    }

    public void suspendDecoder() {
        decoderSuspended = true;
        scheduleTransitionIfNeeded();
    }

    public void resumeDecoder() {
        decoderSuspended = false;
        scheduleTransitionIfNeeded();
    }

    public boolean isDecoderSuspended() {
        return decoderSuspended;
    }

    private void scheduleTransitionIfNeeded() {
        if (!transitionScheduled.compareAndSet(false, true)) {
            return;
        }
        McediaExecutors.ioExecutor.execute(this::runTransitionLoop);
    }

    private void runTransitionLoop() {
        try {
            while (true) {
                MediaPlayImpl target;
                boolean desiredSuspended;
                boolean desiredLowOverhead;
                boolean desiredRuntimeVideoEnabled;
                boolean desiredRuntimeAudioEnabled;
                boolean desiredPaused;
                double desiredSpeed;
                this.lock.lock();
                try {
                    target = mediaPlay;
                    desiredSuspended = decoderSuspended;
                    desiredLowOverhead = lowOverhead;
                    desiredRuntimeVideoEnabled = runtimeVideoEnabled;
                    desiredRuntimeAudioEnabled = runtimeAudioEnabled;
                    desiredPaused = paused;
                    desiredSpeed = speed;
                } finally {
                    this.lock.unlock();
                }

                if (target != null) {
                    reconcileTargetState(
                            target,
                            desiredSuspended,
                            desiredLowOverhead,
                            desiredRuntimeVideoEnabled,
                            desiredRuntimeAudioEnabled,
                            desiredPaused,
                            desiredSpeed);
                }

                this.lock.lock();
                try {
                    boolean changed = target != mediaPlay
                            || desiredSuspended != decoderSuspended
                            || desiredLowOverhead != lowOverhead
                            || desiredRuntimeVideoEnabled != runtimeVideoEnabled
                            || desiredRuntimeAudioEnabled != runtimeAudioEnabled
                            || desiredPaused != paused
                            || Double.compare(desiredSpeed, speed) != 0;
                    if (!changed) {
                        transitionScheduled.set(false);
                        if (target == mediaPlay
                                && desiredSuspended == decoderSuspended
                                && desiredLowOverhead == lowOverhead
                                && desiredRuntimeVideoEnabled == runtimeVideoEnabled
                                && desiredRuntimeAudioEnabled == runtimeAudioEnabled
                                && desiredPaused == paused
                                && Double.compare(desiredSpeed, speed) == 0) {
                            return;
                        }
                        if (!transitionScheduled.compareAndSet(false, true)) {
                            return;
                        }
                    }
                } finally {
                    this.lock.unlock();
                }
            }
        } finally {
            transitionScheduled.set(false);
        }
    }

    private void reconcileTargetState(MediaPlayImpl target) {
        reconcileTargetState(target, decoderSuspended, lowOverhead, runtimeVideoEnabled, runtimeAudioEnabled, paused, speed);
    }

    private void reconcileTargetState(
            MediaPlayImpl target,
            boolean desiredSuspended,
            boolean desiredLowOverhead,
            boolean desiredRuntimeVideoEnabled,
            boolean desiredRuntimeAudioEnabled,
            boolean desiredPaused,
            double desiredSpeed
    ) {
        if (desiredSuspended) {
            if (!target.isDecoderSuspended()) {
                target.suspendDecoder();
            }
            return;
        }
        if (target.isDecoderSuspended()) {
            target.resumeDecoder();
        }
        target.setLowOverhead(desiredLowOverhead);
        target.setRuntimeVideoEnabled(desiredRuntimeVideoEnabled);
        target.setRuntimeAudioEnabled(desiredRuntimeAudioEnabled);
        target.setSpeed(desiredSpeed);
        if (desiredPaused) {
            target.pause();
        } else {
            target.play();
        }
    }

}
