package top.tobyprime.mcedia_platforms.auth;

public final class BilibiliAccountStatus {
    public final boolean isLoggedIn;
    public final boolean isVip;
    public final String username;

    public BilibiliAccountStatus(boolean isLoggedIn, boolean isVip, String username) {
        this.isLoggedIn = isLoggedIn;
        this.isVip = isVip;
        this.username = username;
    }
}
