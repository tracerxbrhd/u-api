package dev.uapi.client.sidebar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.uapi.UApi;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Client-side inventory helper buttons loaded from config/uapi/u-api/sidebar_buttons.json. */
public final class UApiSidebarButtons {
    public static final int ORIGIN_X = 4;
    public static final int ORIGIN_Y = 4;
    public static final int BUTTON_SIZE = 20;
    public static final int GAP = 3;
    public static final int JEI_TOP_LEFT_RESERVED_HEIGHT = 78;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("uapi").resolve("u-api")
        .resolve("sidebar_buttons.json");
    private static long loadedModified = Long.MIN_VALUE;
    private static List<ButtonDefinition> buttons = List.of();
    private static int columns = 2;

    private UApiSidebarButtons() {}

    public static List<ButtonDefinition> buttons() {
        reloadIfChanged();
        return buttons;
    }

    public static int columns() {
        reloadIfChanged();
        return columns;
    }

    public static int originX() {
        return ORIGIN_X;
    }

    public static int originY() {
        return isJeiLoaded() ? ORIGIN_Y + JEI_TOP_LEFT_RESERVED_HEIGHT : ORIGIN_Y;
    }

    public static int areaWidth() {
        int buttonCount = buttons().size();
        if (buttonCount == 0) return 0;
        int usedColumns = Math.min(columns(), buttonCount);
        return usedColumns * BUTTON_SIZE + (usedColumns - 1) * GAP;
    }

    public static int areaHeight() {
        int buttonCount = buttons().size();
        if (buttonCount == 0) return 0;
        int rows = Math.ceilDiv(buttonCount, columns());
        return rows * BUTTON_SIZE + (rows - 1) * GAP;
    }

    private static boolean isJeiLoaded() {
        return ModList.get().isLoaded("jei");
    }

    private static void reloadIfChanged() {
        try {
            ensureDefaultFile();
            long modified = Files.getLastModifiedTime(FILE).toMillis();
            if (modified == loadedModified) return;
            load();
            loadedModified = modified;
        } catch (RuntimeException | IOException exception) {
            UApi.LOGGER.warn("Failed to load U-API sidebar buttons from {}", FILE, exception);
            buttons = List.of();
            columns = 2;
        }
    }

    private static void ensureDefaultFile() throws IOException {
        if (Files.exists(FILE)) return;
        Files.createDirectories(FILE.getParent());
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(defaultConfig(), writer);
        }
    }

    private static void load() throws IOException {
        try (Reader reader = Files.newBufferedReader(FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("enabled") && !root.get("enabled").getAsBoolean()) {
                buttons = List.of();
                return;
            }

            columns = root.has("columns") ? Math.max(1, Math.min(6, root.get("columns").getAsInt())) : 2;
            List<ButtonDefinition> loaded = new ArrayList<>();
            JsonArray array = root.has("buttons") && root.get("buttons").isJsonArray()
                ? root.getAsJsonArray("buttons") : new JsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                ButtonDefinition definition = readButton(element.getAsJsonObject());
                if (definition != null) loaded.add(definition);
            }
            loaded.sort(Comparator.comparingInt(ButtonDefinition::order).thenComparing(ButtonDefinition::id));
            buttons = List.copyOf(loaded);
        }
    }

    private static ButtonDefinition readButton(JsonObject object) {
        if (object.has("enabled") && !object.get("enabled").getAsBoolean()) return null;
        String id = string(object, "id", "custom:" + System.identityHashCode(object));
        String title = string(object, "title", id);
        int order = object.has("order") ? object.get("order").getAsInt() : 1000;
        ItemStack icon = iconStack(string(object, "item", "minecraft:command_block"));
        List<String> commands = commands(object);
        if (commands.isEmpty()) return null;
        int permissionLevel = permissionLevel(object, commands);
        return new ButtonDefinition(id, order, Component.literal(title), icon, List.copyOf(commands), permissionLevel);
    }

    private static List<String> commands(JsonObject object) {
        List<String> result = new ArrayList<>();
        if (object.has("command")) addCommand(result, object.get("command").getAsString());
        if (object.has("commands") && object.get("commands").isJsonArray()) {
            for (JsonElement element : object.getAsJsonArray("commands")) {
                if (element.isJsonPrimitive()) addCommand(result, element.getAsString());
            }
        }
        return result;
    }

    private static void addCommand(List<String> commands, String command) {
        String normalized = command == null ? "" : command.trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1).trim();
        if (!normalized.isEmpty()) commands.add(normalized);
    }

    private static int permissionLevel(JsonObject object, List<String> commands) {
        if (object.has("permission_level")) return clampPermissionLevel(object.get("permission_level").getAsInt());
        if (object.has("permissionLevel")) return clampPermissionLevel(object.get("permissionLevel").getAsInt());
        return inferPermissionLevel(commands);
    }

    private static int inferPermissionLevel(List<String> commands) {
        for (String command : commands) {
            String root = commandRoot(command);
            if (root.equals("gamemode") || root.equals("weather") || root.equals("time")) return 2;
        }
        return 0;
    }

    private static String commandRoot(String command) {
        String normalized = command == null ? "" : command.trim();
        int space = normalized.indexOf(' ');
        return (space < 0 ? normalized : normalized.substring(0, space)).toLowerCase(Locale.ROOT);
    }

    private static int clampPermissionLevel(int level) {
        return Math.max(0, Math.min(4, level));
    }

    private static ItemStack iconStack(String itemId) {
        ResourceLocation id = parseId(itemId);
        if (id == null) return new ItemStack(Items.BARRIER);
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.BARRIER);
        return new ItemStack(item);
    }

    private static ResourceLocation parseId(String value) {
        try {
            return ResourceLocation.parse(value.toLowerCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private static JsonObject defaultConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        root.addProperty("columns", 2);
        JsonArray array = new JsonArray();
        add(array, "u_api:gamemode_survival", 0, "Survival mode", "minecraft:iron_sword", "gamemode survival", 2);
        add(array, "u_api:gamemode_creative", 1, "Creative mode", "minecraft:command_block", "gamemode creative", 2);
        add(array, "u_api:weather_clear", 2, "Clear weather", "minecraft:sunflower", "weather clear", 2);
        add(array, "u_api:weather_rain", 3, "Rain", "minecraft:water_bucket", "weather rain", 2);
        add(array, "u_api:time_morning", 4, "Set morning", "minecraft:clock", "time set day", 2);
        add(array, "u_api:time_night", 5, "Set night", "minecraft:black_bed", "time set night", 2);
        root.add("buttons", array);
        return root;
    }

    private static void add(JsonArray array, String id, int order, String title, String item, String command,
                            int permissionLevel) {
        JsonObject button = new JsonObject();
        button.addProperty("id", id);
        button.addProperty("order", order);
        button.addProperty("title", title);
        button.addProperty("item", item);
        button.addProperty("command", command);
        button.addProperty("permission_level", permissionLevel);
        button.addProperty("enabled", true);
        array.add(button);
    }

    public static boolean isAllowed(ButtonDefinition button) {
        Minecraft minecraft = Minecraft.getInstance();
        if (button.permissionLevel() <= 0) return true;
        return minecraft.player != null && minecraft.player.hasPermissions(button.permissionLevel());
    }

    public static Component tooltip(ButtonDefinition button) {
        if (button.permissionLevel() <= 0) return button.title();
        return Component.empty().append(button.title()).append("\n")
            .append(Component.literal("Requires permission level " + button.permissionLevel()));
    }

    static void execute(ButtonDefinition button) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null) return;
        if (!isAllowed(button)) {
            minecraft.player.displayClientMessage(Component.literal(
                "You do not have permission to use this helper button."), true);
            return;
        }
        for (String command : button.commands()) {
            minecraft.getConnection().sendCommand(command);
        }
    }

    public record ButtonDefinition(String id, int order, Component title, ItemStack icon, List<String> commands,
                                   int permissionLevel) {}
}
