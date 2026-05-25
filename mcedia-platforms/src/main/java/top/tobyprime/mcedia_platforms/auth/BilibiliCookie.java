package top.tobyprime.mcedia_platforms.auth;

import org.jetbrains.annotations.Nullable;

import java.util.Properties;

public final class BilibiliCookie {
    @Nullable
    private static String COOKIES;

    private BilibiliCookie() {
    }

    public static @Nullable String getCookie() {
        return COOKIES;
    }

    public static void saveCookies(String cookies) {
        COOKIES = cookies;
    }

    public static void fromProperties(Properties props) {
        COOKIES = props.getProperty("BILIBILI_COOKIES", COOKIES);
    }

    public static void writeToProperties(Properties props) {
        if (COOKIES != null) {
            props.setProperty("BILIBILI_COOKIES", COOKIES);
        }
    }
}
