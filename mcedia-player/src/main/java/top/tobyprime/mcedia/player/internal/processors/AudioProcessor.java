package top.tobyprime.mcedia.player.internal.processors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.tobyprime.mcedia.api.audio.AudioFrame;
import top.tobyprime.mcedia.api.audio.AudioSource;
import top.tobyprime.mcedia.api.stream.FrameStream;
import top.tobyprime.mcedia.player.runtime.McediaExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

public class AudioProcessor {
    private final ReentrantLock lock = new ReentrantLock();
    private final List<@NotNull AudioSource> audioSources = new ArrayList<>();
    @Nullable private volatile FrameStream<AudioFrame> stream;
    private volatile int audioSourceCount = 0;
    private volatile long playtime = -1;
    private volatile boolean ended = false;
    boolean paused = false;

    private double speed = 1.0;
    private final Executor closeExecutor;

    public AudioProcessor() {
        this(McediaExecutors.ioExecutor);
    }

    AudioProcessor(Executor closeExecutor) {
        this.closeExecutor = closeExecutor;
    }

    public void bindStream(FrameStream<AudioFrame> stream) {
        this.stream = stream;
        lock.lock();
        try {
            for (var source : audioSources) {
                source.clear();
            }
            playtime = -1;
            ended = false;
            audioSourceCount = this.audioSources.size();
        } finally {
            lock.unlock();
        }
    }

    public void seek(long time) {
        lock.lock();
        try {
            for (var source : audioSources) {
                source.clear();
            }
            playtime = time;
            ended = false;
        } finally {
            lock.unlock();
        }
    }

    public void bindAudioSources(@NotNull AudioSource source) {
        lock.lock();
        try {
            source.setSpeed(speed);
            if (this.paused) {
                source.pause();
            } else {
                source.play();
            }
            this.audioSources.add(source);
            audioSourceCount = this.audioSources.size();
            playtime = -1;
            ended = false;
        } finally {
            lock.unlock();
        }
    }

    public void unbindAudioSources(AudioSource source) {
        lock.lock();
        try {
            this.audioSources.remove(source);
            audioSourceCount = this.audioSources.size();
            if (this.audioSources.isEmpty()) {
                playtime = -1;
                ended = false;
            }
        } finally {
            lock.unlock();
        }
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        lock.lock();
        try {
            for (var source : audioSources) {
                source.setSpeed(speed);
            }
        } finally {
            lock.unlock();
        }
    }

    public void pause() {
        paused = true;
        lock.lock();
        try {
            for (var source : audioSources) {
                source.pause();
            }
        } finally {
            lock.unlock();
        }
    }

    public void play() {
        lock.lock();
        paused = false;
        try {
            for (var source : audioSources) {
                source.play();
            }
        } finally {
            lock.unlock();
        }
    }


    public void tick(long time) {
        lock.lock();

        try {
            audioSourceCount = audioSources.size();
            // 更新声源
            for (var source : audioSources) {
                source.tick();

                if (paused) {
                    source.pause();
                } else {
                    source.play();
                }
            }

            if (paused) {
                ended = false;
                playtime = stream == null || audioSources.isEmpty() ? -1 : audioSources.getFirst().getPlaytime();
                return;
            }

            if (stream == null || audioSources.isEmpty()) {
                // 如果没有音频流或没有声源，仍然消费音频帧，以确保
                while (stream != null) {
                    var nextFrame = stream.poll(x -> x.getTime() < time);

                    if (nextFrame == null) {
                        break;
                    }
                    closeExecutor.execute(nextFrame::close);
                }
            } else {
                // 上传音频数据，直到任意一声源不需要音频帧
                while (audioSources.stream().allMatch(AudioSource::getRequiredFrame)) {
                    var frame = stream.poll();
                    if (frame == null) {
                        break;
                    }
                    try {
                        for (AudioSource source : audioSources) {
                            source.upload(frame);
                        }
                    } finally {
                        closeExecutor.execute(frame::close);
                    }
                }
            }

            // 更新播放时间
            ended = stream != null && !audioSources.isEmpty() && audioSources.getFirst().isEnded();
            playtime = stream == null || audioSources.isEmpty() ? -1 : audioSources.getFirst().getPlaytime();
        } finally {
            lock.unlock();
        }
    }

    public long getPlaytime() {
        var currentPlaytime = playtime;
        if (stream == null || audioSourceCount == 0) {
            return -1;
        }
        return currentPlaytime;
    }

    public boolean isEnded() {
        return ended;
    }
}
