package top.tobyprime.mcedia.api.decoder;

import org.jetbrains.annotations.NotNull;
import top.tobyprime.mcedia.api.config.DecoderConfiguration;
import top.tobyprime.mcedia.api.media.MediaPlayInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public final class DecoderProviders {
    private DecoderProviders() {
    }

    public static @NotNull DecoderFactory find(@NotNull MediaPlayInfo playInfo, @NotNull DecoderConfiguration configuration) {
        return find(playInfo, configuration, ServiceLoader.load(DecoderProvider.class));
    }

    static @NotNull DecoderFactory find(@NotNull MediaPlayInfo playInfo, @NotNull DecoderConfiguration configuration, @NotNull Iterable<DecoderProvider> providers) {
        Objects.requireNonNull(playInfo, "playInfo");
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(providers, "providers");

        List<DecoderProvider> matchedProviders = new ArrayList<>();
        for (var provider : providers) {
            if (provider.supports(playInfo, configuration)) {
                matchedProviders.add(provider);
            }
        }

        if (matchedProviders.isEmpty()) {
            throw new IllegalStateException("No decoder provider found for media: " + playInfo.getUrl());
        }

        DecoderProvider selected = null;
        int selectedPriority = Integer.MIN_VALUE;
        List<String> selectedConflicts = new ArrayList<>();
        for (var provider : matchedProviders) {
            int priority = provider.getPriority();
            if (selected == null || priority > selectedPriority) {
                selected = provider;
                selectedPriority = priority;
                selectedConflicts.clear();
                selectedConflicts.add(provider.getClass().getName());
                continue;
            }
            if (priority == selectedPriority) {
                selectedConflicts.add(provider.getClass().getName());
            }
        }

        if (selectedConflicts.size() > 1) {
            throw new IllegalStateException("Ambiguous decoder providers at priority " + selectedPriority + ": " + String.join(", ", selectedConflicts));
        }

        var factory = Objects.requireNonNull(selected, "selected provider is null").getFactory();
        return Objects.requireNonNull(factory, "decoder factory is null");
    }
}
