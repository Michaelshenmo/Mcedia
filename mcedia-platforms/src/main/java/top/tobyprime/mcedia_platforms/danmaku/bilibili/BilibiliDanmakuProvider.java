package top.tobyprime.mcedia_platforms.danmaku.bilibili;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tobyprime.mcedia.api.danmaku.DanmakuDocument;
import top.tobyprime.mcedia.api.danmaku.DanmakuItem;
import top.tobyprime.mcedia.api.danmaku.DanmakuType;
import top.tobyprime.mcedia.api.media.MediaInfo;
import top.tobyprime.mcedia_platforms.danmaku.DanmakuProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class BilibiliDanmakuProvider implements DanmakuProvider {
    public static final String KEY_BVID = "bilibili.bvid";
    public static final String KEY_CID = "bilibili.cid";
    public static final String KEY_PAGE = "bilibili.page";

    private static final Logger LOGGER = LoggerFactory.getLogger(BilibiliDanmakuProvider.class);
    private static final HttpClient HTTP = HttpClient.newBuilder().build();
    private static final Pattern ITEM_PATTERN = Pattern.compile("<d p=\"([^\"]*)\">([^<]*)</d>");

    @Override
    public @NotNull CompletableFuture<DanmakuDocument> load(@NotNull MediaInfo mediaInfo) {
        var cidValue = mediaInfo.getExtraMetadata().get(KEY_CID);
        if (cidValue == null || cidValue.isBlank()) {
            return CompletableFuture.completedFuture(DanmakuDocument.EMPTY);
        }

        long cid;
        try {
            cid = Long.parseLong(cidValue);
        } catch (NumberFormatException e) {
            LOGGER.warn("Ignore invalid bilibili cid '{}'", cidValue);
            return CompletableFuture.completedFuture(DanmakuDocument.EMPTY);
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.bilibili.com/x/v1/dm/list.so?oid=" + cid))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept-Encoding", "deflate")
                .GET()
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> parseResponseBody(response.statusCode(), response.body(), cid));
    }

    static DanmakuDocument parseResponseBody(int statusCode, byte[] body, long cid) {
        if (statusCode != 200 || body == null || body.length == 0) {
            return DanmakuDocument.EMPTY;
        }
        try {
            return parseXml(inflate(body));
        } catch (Exception e) {
            LOGGER.warn("Failed to decode bilibili danmaku for cid={}", cid, e);
            return DanmakuDocument.EMPTY;
        }
    }

    static DanmakuDocument parseXml(String xml) {
        if (xml == null || xml.isBlank()) {
            return DanmakuDocument.EMPTY;
        }
        var matcher = ITEM_PATTERN.matcher(xml);
        var items = new ArrayList<DanmakuItem>();
        while (matcher.find()) {
            var parts = matcher.group(1).split(",");
            if (parts.length < 4) {
                continue;
            }
            var type = parseType(parts[1]);
            if (type == null) {
                continue;
            }
            try {
                long timeUs = Math.round(Double.parseDouble(parts[0]) * 1_000_000d);
                int rgb = Integer.parseInt(parts[3]);
                items.add(new DanmakuItem(timeUs, decodeXmlEntities(matcher.group(2)), 0xFF000000 | rgb, type));
            } catch (RuntimeException ignored) {
            }
        }
        return items.isEmpty() ? DanmakuDocument.EMPTY : new DanmakuDocument(items);
    }

    private static String inflate(byte[] body) throws IOException {
        var inflater = new Inflater(true);
        try (InputStream input = new InflaterInputStream(new ByteArrayInputStream(body), inflater)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            inflater.end();
        }
    }

    private static DanmakuType parseType(String modeValue) {
        int mode;
        try {
            mode = Integer.parseInt(modeValue);
        } catch (NumberFormatException e) {
            return null;
        }
        if (mode >= 1 && mode <= 3) {
            return DanmakuType.SCROLLING;
        }
        if (mode == 4) {
            return DanmakuType.BOTTOM;
        }
        if (mode == 5) {
            return DanmakuType.TOP;
        }
        return null;
    }

    private static String decodeXmlEntities(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}
