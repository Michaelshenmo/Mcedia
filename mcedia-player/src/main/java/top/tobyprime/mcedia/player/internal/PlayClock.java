package top.tobyprime.mcedia.player.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayClock {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayClock.class);
    boolean paused = true;
    private long startTime = 0;
    private long basePlayTime = 0;
    private double speed = 1;

    public void setSpeed(double speed) {
        basePlayTime = getPlaytime();
        startTime =  System.currentTimeMillis() * 1_000;
        this.speed = speed;
    }

    public void reset() {
        paused = true;
        basePlayTime = 0;
    }

    public void pause() {
        basePlayTime = getPlaytime();
        paused = true;
    }

    public void play() {
        startTime = System.currentTimeMillis() * 1_000;
        paused = false;
    }

    public long getPlaytime() {
        if (paused) {
            return basePlayTime;
        }
        return (long) (basePlayTime + (System.currentTimeMillis() * 1_000 - startTime) * speed);
    }

    public void seek(long time) {
        basePlayTime = time;
        startTime = System.currentTimeMillis() * 1_000;
    }
}
