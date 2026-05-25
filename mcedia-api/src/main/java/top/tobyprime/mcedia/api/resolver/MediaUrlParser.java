package top.tobyprime.mcedia.api.resolver;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.media.Media;

import java.util.Optional;

/**
 * 非标准播放字符串解析器。
 * 将用户友好的输入（如 https://www.bilibili.com/video/BV1xx、BV1xx 等）
 * 自动解析为 Media 对象。
 * <p>
 * 通过 {@link MediaResolvers#registerParser(MediaUrlParser, int)} 注册，
 * 并按照 priority（值越小优先级越高）排序。
 */
@FunctionalInterface
public interface MediaUrlParser {
    @NotNull Optional<Media> tryParse(@NotNull String input);
}
