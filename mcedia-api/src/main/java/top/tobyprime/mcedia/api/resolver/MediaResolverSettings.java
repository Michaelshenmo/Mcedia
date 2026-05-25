package top.tobyprime.mcedia.api.resolver;

public final class MediaResolverSettings {
    private static volatile int resolutionLimit;

    private MediaResolverSettings() {
    }

    public static void setResolutionLimit(int maxHeight) {
        resolutionLimit = Math.max(0, maxHeight);
    }

    public static int getResolutionLimit() {
        return resolutionLimit;
    }
}
