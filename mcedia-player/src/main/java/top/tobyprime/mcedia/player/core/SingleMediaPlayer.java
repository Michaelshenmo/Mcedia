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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class SingleMediaPlayer implements MediaPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleMediaPlayer.class);
    final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<@Nullable CompletableFuture<MediaPlay>> loadFuture = new AtomicReference<>(null);
    private final AtomicLong loadRequestId = new AtomicLong(0);
    private final AtomicLong seekRequestId = new AtomicLong(0);
    private final AtomicReference<CompletableFuture<Void>> seekFuture = new AtomicReference<>(CompletableFuture.completedFuture(null));
    private final AudioProcessor audioProcessor = new AudioProcessor();
    private final VideoProcessor videoProcessor = new VideoProcessor();
    private boolean paused = false;
    private double speed = 1;
    private DecoderConfiguration decoderConfiguration = new DecoderConfiguration(new DecoderConfiguration.Builder());

    @Nullable
    private MediaPlayImpl mediaPlay;
    private boolean lowOverhead;
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
            play.setLowOverhead(lowOverhead);
            play.setSpeed(speed);
            if (paused) play.pause();
            else play.play();
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
        MediaPlayImpl target;
        lock.lock();
        try {
            target = mediaPlay;
        } finally {
            lock.unlock();
        }
        if (target == null) {
            return CompletableFuture.completedFuture(null);
        }

        var requestId = seekRequestId.incrementAndGet();
        var newFuture = seekFuture.updateAndGet(previous -> previous.handle((ignored, throwable) -> null)
                .thenRunAsync(() -> seekCurrentMedia(requestId, target, time), McediaExecutors.ioExecutor));
        return newFuture;
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
        seekRequestId.incrementAndGet();
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
        if (!this.lock.tryLock()) {
            return;
        }
        try {
            if (this.mediaPlay != null) {
                this.mediaPlay.tickAudio();
            }
        } finally {
            this.lock.unlock();
        }
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
        this.lock.lock();
        try {
            if (mediaPlay != null) {
                mediaPlay.open(decoderConfiguration);
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public double getSpeed() {
        return this.speed;
    }

    @Override
    public void setSpeed(double speed) {
        this.speed = speed;
        this.lock.lock();
        try {
            if (mediaPlay != null) {
                mediaPlay.setSpeed(speed);
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void setLowOverhead(boolean lowOverhead) {
        this.lowOverhead = lowOverhead;
        if (!this.lock.tryLock()) return;
        try {
            if (mediaPlay != null) {
                mediaPlay.setLowOverhead(lowOverhead);
            }
        } finally {
            this.lock.unlock();
        }
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
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
                if (mediaPlay != target || this.paused != paused) {
                    return;
                }
                if (paused) target.pause();
                else target.play();
            } finally {
                this.lock.unlock();
            }
        });
    }
}
