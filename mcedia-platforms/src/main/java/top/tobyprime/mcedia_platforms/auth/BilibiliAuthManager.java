package top.tobyprime.mcedia_platforms.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class BilibiliAuthManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BilibiliAuthManager.class);
    private static final BilibiliAuthManager INSTANCE = new BilibiliAuthManager();

    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final Gson gson = new Gson();
    private volatile BilibiliAccountStatus accountStatus = new BilibiliAccountStatus(false, false, "游客");
    private final List<BilibiliAccountStatusUpdateEventHandler> handlers = new ArrayList<>();

    private BilibiliAuthManager() {
    }

    public static BilibiliAuthManager getInstance() {
        return INSTANCE;
    }

    public BilibiliAccountStatus getAccountStatus() {
        return accountStatus;
    }

    private void setAccountStatus(BilibiliAccountStatus accountStatus) {
        this.accountStatus = accountStatus;
        this.handlers.forEach(handler -> handler.OnAccountStatusUpdated(accountStatus));
    }

    public void addStatusUpdateHandler(BilibiliAccountStatusUpdateEventHandler handler) {
        handlers.add(handler);
    }

    public void logout() {
        setAccountStatus(new BilibiliAccountStatus(false, false, "游客"));
        BilibiliCookie.saveCookies("");
    }

    public CompletableFuture<String> checkAndUpdateLoginStatusAsync() {
        var cookie = BilibiliCookie.getCookie();
        if (cookie == null || cookie.isEmpty()) {
            setAccountStatus(new BilibiliAccountStatus(false, false, ""));
            return CompletableFuture.completedFuture("无本地登录数据");
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.bilibili.com/x/web-interface/nav"))
                .header("User-Agent", "Mozilla/5.0")
                .header("Cookie", cookie)
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .handle((body, err) -> {
                    if (err != null) {
                        setAccountStatus(new BilibiliAccountStatus(false, false, ""));
                        return "检查Bilibili Cookie时发生网络错误";
                    }

                    try {
                        var json = gson.fromJson(body, JsonObject.class);
                        if (json.get("code").getAsInt() == 0 && json.getAsJsonObject("data").get("isLogin").getAsBoolean()) {
                            var data = json.getAsJsonObject("data");
                            var uname = data.get("uname").getAsString();
                            var vipStatus = data.get("vipStatus").getAsInt();
                            setAccountStatus(new BilibiliAccountStatus(true, vipStatus == 1, vipStatus == 1 ? uname + " [大会员]" : uname));
                            return "登录成功";
                        }
                        throw new IllegalStateException("API返回未登录状态或错误码: " + json.get("code").getAsInt());
                    } catch (Exception e) {
                        setAccountStatus(new BilibiliAccountStatus(false, false, ""));
                        BilibiliCookie.saveCookies("");
                        return "登录已失效";
                    }
                });
    }

    public CompletableFuture<String> loginAsync(BilibiliLoginQrCodeHandler qrCode) {
        LOGGER.info("loginAsync: 开始B站登录流程");
        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://passport.bilibili.com/x/passport-login/web/qrcode/generate?source=main-fe-header"))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        LOGGER.info("loginAsync: 发送QR码生成请求至 passport.bilibili.com");
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    LOGGER.info("loginAsync: QR码生成请求完成, statusCode={}", response.statusCode());
                    return response.body();
                })
                .thenCompose(body -> {
                    LOGGER.info("loginAsync: 解析QR码生成响应");
                    var json = gson.fromJson(body, JsonObject.class);
                    var code = json.get("code").getAsInt();
                    LOGGER.info("loginAsync: QR码生成API返回 code={}", code);
                    if (code != 0) {
                        throw new IllegalStateException("生成二维码失败, code=" + code);
                    }

                    var data = json.getAsJsonObject("data");
                    var qrContentUrl = data.get("url").getAsString();
                    var qrcodeKey = data.get("qrcode_key").getAsString();
                    LOGGER.info("loginAsync: QR码URL已获取, qrcodeKey={}", qrcodeKey);

                    var encodedUrl = URLEncoder.encode(qrContentUrl, StandardCharsets.UTF_8);
                    var qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + encodedUrl;

                    LOGGER.info("loginAsync: 调用onDisplayQrCode回调");
                    qrCode.onDisplayQrCode(qrImageUrl);
                    LOGGER.info("loginAsync: onDisplayQrCode回调完成, 开始轮询扫码状态");
                    return waitForScanQrCodeAsync(qrcodeKey);
                })
                .thenCompose(v -> {
                    LOGGER.info("loginAsync: 扫码成功，检查登录状态");
                    return checkAndUpdateLoginStatusAsync();
                })
                .exceptionally(err -> {
                    LOGGER.error("loginAsync: 登录流程异常", err);
                    return "登录失败: " + err.getMessage();
                });
    }

    private CompletableFuture<Void> waitForScanQrCodeAsync(String qrcodeKey) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        CompletableFuture<Void> future = new CompletableFuture<>();
        long startTime = System.currentTimeMillis();
        AtomicReference<ScheduledFuture<?>> holder = new AtomicReference<>();

        Runnable task = () -> {
            if (future.isDone()) {
                var scheduledFuture = holder.get();
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
                scheduler.shutdownNow();
                return;
            }

            try {
                if (System.currentTimeMillis() - startTime > 180_000) {
                    throw new IllegalStateException("登录超时");
                }

                var request = HttpRequest.newBuilder()
                        .uri(URI.create("https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrcodeKey + "&source=main-fe-header"))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();

                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                var json = gson.fromJson(response.body(), JsonObject.class);
                var data = json.getAsJsonObject("data");
                var code = data.get("code").getAsInt();

                if (code == 0) {
                    var cookieHeaders = response.headers().allValues("Set-Cookie");
                    if (cookieHeaders.isEmpty()) {
                        throw new IllegalStateException("登录成功，但未能获取到Cookie");
                    }

                    var fullCookie = cookieHeaders.stream().map(h -> h.split(";", 2)[0]).collect(Collectors.joining("; "));
                    BilibiliCookie.saveCookies(fullCookie);
                    future.complete(null);
                    return;
                }

                if (code == 86038) {
                    throw new IllegalStateException("二维码已过期，请重新登录");
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };

        holder.set(scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS));
        future.whenComplete((v, ex) -> {
            var scheduledFuture = holder.get();
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
            scheduler.shutdownNow();
        });
        return future;
    }
}
