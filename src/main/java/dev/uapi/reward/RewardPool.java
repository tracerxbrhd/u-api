package dev.uapi.reward;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.uapi.difficulty.DifficultyRank;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public record RewardPool(int rolls, List<Entry> entries) {
    public record Entry(Identifier type, int weight, DifficultyRank minimumRank, JsonObject data) {}

    public static RewardPool parse(JsonObject json) {
        int rolls = Math.max(1, GsonHelper.getAsInt(json, "rolls", 1));
        List<Entry> entries = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "entries")) {
            JsonObject entry = element.getAsJsonObject();
            entries.add(new Entry(Identifier.parse(GsonHelper.getAsString(entry, "type")),
                Math.max(1, GsonHelper.getAsInt(entry, "weight", 1)),
                DifficultyRank.byName(GsonHelper.getAsString(entry, "minimum_rank", "E")), entry.deepCopy()));
        }
        return new RewardPool(rolls, List.copyOf(entries));
    }
}
