package top.tobyprime.mcedia.player.config;

import java.util.List;
import java.util.Properties;

public class Configs {
    public static boolean PHYSICS = true;
    public static boolean SHOW_LOAD_INFO = true;
    /** ACTIVE 驻留态的最大 decoder 数量 */
    public static int ACTIVE_DECODER_LIMIT = 1;
    /** THROTTLED 驻留态的最大 decoder 数量（独立于 ACTIVE 名额，超过后进入 SUSPENDED） */
    public static int THROTTLED_DECODER_LIMIT = 5;
    /** low overhead 模式下的上传帧率限制 */
    public static int LOW_OVERHEAD_UPLOAD_FPS = 5;
    public static List<String> ARMOR_STAND_PLAYER_NAME_PATTERNS = List.of("mcedia", "mcdia");
    /** 预设分辨率级别的标准高度值 */
    public static final int RES_320P = 320;
    public static final int RES_720P = 720;
    public static final int RES_1080P = 1080;
    public static final int RES_2K = 1440;
    public static final int RES_4K = 2160;
    public static final int RES_8K = 4320;

    /** 最大分辨率高度，0 表示无限制。需配合 RES_* 常量使用。 */
    public static int MAX_RESOLUTION_HEIGHT = 0;

    // 音量
    public static Float VOLUME_FACTOR = 1f;

    // 弹幕设置
    public static boolean DANMAKU_VISIBLE = true;
    public static Float DANMAKU_DURATION = 4.f;
    public static int DANMAKU_TRACKS = 12;
    public static Float DANMAKU_OPACITY = 0.5f;

    // 缓冲与解码配置
    public static int DECODER_MAX_AUDIO_FRAMES = 512;
    public static int DECODER_MAX_VIDEO_FRAMES = 120;
    public static int DECODER_LOW_OVERHEAD_VIDEO_FRAMES = 20;

    public static boolean ALLOW_DIRECT_LINK = false;
    public static boolean ALLOW_YHDM = false;

    public static void fromProperties(Properties props) {
        Configs.ACTIVE_DECODER_LIMIT = Integer.parseInt(props.getProperty("ACTIVE_DECODER_LIMIT", props.getProperty("MAX_NON_LOW_OVERHEAD_PLAYER_COUNT", String.valueOf(Configs.ACTIVE_DECODER_LIMIT))));
        Configs.THROTTLED_DECODER_LIMIT = Integer.parseInt(props.getProperty("THROTTLED_DECODER_LIMIT", String.valueOf(Configs.THROTTLED_DECODER_LIMIT)));
        Configs.LOW_OVERHEAD_UPLOAD_FPS = Integer.parseInt(props.getProperty("LOW_OVERHEAD_UPLOAD_FPS", String.valueOf(Configs.LOW_OVERHEAD_UPLOAD_FPS)));
        Configs.SHOW_LOAD_INFO = Boolean.parseBoolean(props.getProperty("SHOW_LOAD_INFO", String.valueOf(Configs.SHOW_LOAD_INFO)));
        Configs.VOLUME_FACTOR = Float.parseFloat(props.getProperty("VOLUME_FACTOR", String.valueOf(Configs.VOLUME_FACTOR)));
        Configs.ARMOR_STAND_PLAYER_NAME_PATTERNS = List.of(props.getProperty("ARMOR_STAND_PLAYER_NAME_PATTERNS", String.join(";", ARMOR_STAND_PLAYER_NAME_PATTERNS)).split(";"));
        Configs.MAX_RESOLUTION_HEIGHT = Integer.parseInt(props.getProperty("MAX_RESOLUTION_HEIGHT", String.valueOf(Configs.MAX_RESOLUTION_HEIGHT)));
        Configs.PHYSICS = Boolean.parseBoolean(props.getProperty("PHYSICS", String.valueOf(Configs.PHYSICS)));

        Configs.DANMAKU_VISIBLE = Boolean.parseBoolean(props.getProperty("DANMAKU_VISIBLE", String.valueOf(Configs.DANMAKU_VISIBLE)));
        Configs.DANMAKU_DURATION = Float.parseFloat(props.getProperty("DANMAKU_DURATION", String.valueOf(Configs.DANMAKU_DURATION)));
        Configs.DANMAKU_TRACKS = Integer.parseInt(props.getProperty("DANMAKU_TRACKS", String.valueOf(Configs.DANMAKU_TRACKS)));
        Configs.DANMAKU_OPACITY = Float.parseFloat(props.getProperty("DANMAKU_OPACITY", String.valueOf(Configs.DANMAKU_OPACITY)));

        Configs.ALLOW_DIRECT_LINK = Boolean.parseBoolean(props.getProperty("ALLOW_DIRECT_LINK", String.valueOf(Configs.ALLOW_DIRECT_LINK)));
        Configs.ALLOW_YHDM = Boolean.parseBoolean(props.getProperty("ALLOW_YHDM", String.valueOf(Configs.ALLOW_YHDM)));
    }

    public static void writeToProperties(Properties props) {
        props.setProperty("ACTIVE_DECODER_LIMIT", String.valueOf(Configs.ACTIVE_DECODER_LIMIT));
        props.setProperty("THROTTLED_DECODER_LIMIT", String.valueOf(Configs.THROTTLED_DECODER_LIMIT));
        props.setProperty("LOW_OVERHEAD_UPLOAD_FPS", String.valueOf(Configs.LOW_OVERHEAD_UPLOAD_FPS));
        props.setProperty("SHOW_LOAD_INFO", String.valueOf(Configs.SHOW_LOAD_INFO));
        props.setProperty("VOLUME_FACTOR", String.valueOf(Configs.VOLUME_FACTOR));
        props.setProperty("ARMOR_STAND_PLAYER_NAME_PATTERNS", String.join(";", Configs.ARMOR_STAND_PLAYER_NAME_PATTERNS));
        props.setProperty("MAX_RESOLUTION_HEIGHT", String.valueOf(Configs.MAX_RESOLUTION_HEIGHT));
        props.setProperty("PHYSICS", String.valueOf(Configs.PHYSICS));

        props.setProperty("DANMAKU_VISIBLE", String.valueOf(Configs.DANMAKU_VISIBLE));
        props.setProperty("DANMAKU_DURATION", String.valueOf(Configs.DANMAKU_DURATION));
        props.setProperty("DANMAKU_TRACKS", String.valueOf(Configs.DANMAKU_TRACKS));
        props.setProperty("DANMAKU_OPACITY", String.valueOf(Configs.DANMAKU_OPACITY));

        props.setProperty("ALLOW_DIRECT_LINK", String.valueOf(Configs.ALLOW_DIRECT_LINK));
        props.setProperty("ALLOW_YHDM", String.valueOf(Configs.ALLOW_YHDM));
    }
}
