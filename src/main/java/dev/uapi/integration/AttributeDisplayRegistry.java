package dev.uapi.integration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client-safe extension point for assigning modded attributes to character-screen categories.
 * Addons may register rules without linking their UI implementation to SOUL ASCENSION internals.
 */
public final class AttributeDisplayRegistry {
    public record Category(ResourceLocation id, Component title, int order) {}
    public record Rule(ResourceLocation categoryId, Boolean visible) {}

    private static final Map<ResourceLocation, Category> CATEGORIES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Rule> RULES = new LinkedHashMap<>();

    private AttributeDisplayRegistry() {}

    public static synchronized void registerCategory(ResourceLocation id, Component title, int order) {
        CATEGORIES.put(id, new Category(id, title, order));
    }

    /** A null visible value leaves visibility under the user's client configuration. */
    public static synchronized void registerAttribute(ResourceLocation attributeId, ResourceLocation categoryId,
                                                      Boolean visible) {
        RULES.put(attributeId, new Rule(categoryId, visible));
    }

    public static synchronized Optional<Rule> rule(ResourceLocation attributeId) {
        return Optional.ofNullable(RULES.get(attributeId));
    }

    public static synchronized Optional<Category> category(ResourceLocation id) {
        return Optional.ofNullable(CATEGORIES.get(id));
    }

    public static synchronized List<Category> categories() {
        List<Category> result = new ArrayList<>(CATEGORIES.values());
        result.sort(Comparator.comparingInt(Category::order).thenComparing(value -> value.id().toString()));
        return List.copyOf(result);
    }
}
