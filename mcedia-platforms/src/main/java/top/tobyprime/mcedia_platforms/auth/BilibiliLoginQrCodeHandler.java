package top.tobyprime.mcedia_platforms.auth;

@FunctionalInterface
public interface BilibiliLoginQrCodeHandler {
    void onDisplayQrCode(String qrCodeUrl);
}
