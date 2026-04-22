package com.fix.myfix.config;

import com.fix.myfix.MyFix;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MyFix.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HarderBeginningsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue FLINT_TOOLS_ENABLED;
    private static final ForgeConfigSpec.DoubleValue FIBER_DROP_CHANCE;
    private static final ForgeConfigSpec.IntValue CHARCOAL_BURN_SECONDS_PER_LOG;
    private static final ForgeConfigSpec.IntValue CHARCOAL_CHECK_INTERVAL_SECONDS;
    private static final ForgeConfigSpec.DoubleValue CHARCOAL_REQUIRED_SEAL_RATIO;
    private static final ForgeConfigSpec.DoubleValue CHARCOAL_REQUIRED_QUALIFIED_TIME_RATIO;
    private static final ForgeConfigSpec.BooleanValue TORCH_ENABLED;
    private static final ForgeConfigSpec.IntValue TORCH_BURN_SECONDS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CAMPFIRE_FUEL_ENTRIES;
    private static final ForgeConfigSpec.BooleanValue WOOD_WORKBENCH_ENABLED;
    private static final ForgeConfigSpec.BooleanValue DEATH_PENALTY_ENABLED;

    private static volatile CampfireFuelRules campfireFuelRules = CampfireFuelRules.empty();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("flintTools");
        FLINT_TOOLS_ENABLED = builder
                .comment("If true, flint tool related mechanics stay enabled.")
                .define("enabled", true);
        builder.pop();

        builder.push("fiber");
        FIBER_DROP_CHANCE = builder
                .comment("Chance for fiber to drop when breaking grass-type plants with a flint knife. Range: 0.0 to 1.0")
                .defineInRange("dropChance", 0.25D, 0.0D, 1.0D);
        builder.pop();

        builder.push("charcoalPit");
        CHARCOAL_BURN_SECONDS_PER_LOG = builder
                .comment("How many seconds one log needs to finish burning into charcoal.")
                .defineInRange("burnSecondsPerLog", 30, 1, Integer.MAX_VALUE);
        CHARCOAL_CHECK_INTERVAL_SECONDS = builder
                .comment("How often the charcoal pit checks seal quality during burning, in seconds.")
                .defineInRange("checkIntervalSeconds", 3, 1, 3600);
        CHARCOAL_REQUIRED_SEAL_RATIO = builder
                .comment("Seal ratio required for a sample to count as fully sealed. 1.0 means every face must be covered.")
                .defineInRange("requiredSealRatio", 1.0D, 0.0D, 1.0D);
        CHARCOAL_REQUIRED_QUALIFIED_TIME_RATIO = builder
                .comment("How much of the burn duration must pass with a qualified seal sample for charcoal to succeed.")
                .defineInRange("requiredQualifiedTimeRatio", 0.8D, 0.0D, 1.0D);
        builder.pop();

        builder.push("torch");
        TORCH_ENABLED = builder
                .comment("If true, custom torch burnout/extinguish mechanics stay enabled.")
                .define("enabled", true);
        TORCH_BURN_SECONDS = builder
                .comment("How many seconds a torch can stay lit before it burns out.")
                .defineInRange("burnSeconds", 600, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("campfire");
        CAMPFIRE_FUEL_ENTRIES = builder
                .comment(
                        "Campfire fuel list. Format: namespace:item=ticks or #namespace:tag=ticks.",
                        "Examples:",
                        "minecraft:stick=100",
                        "#minecraft:planks=300",
                        "#minecraft:logs=1200",
                        "minecraft:charcoal=1600"
                )
                .defineListAllowEmpty(
                        List.of("fuelEntries"),
                        HarderBeginningsConfig::defaultCampfireFuelEntries,
                        entry -> entry instanceof String
                );
        builder.pop();

        builder.push("woodWorkbench");
        WOOD_WORKBENCH_ENABLED = builder
                .comment("If true, the wood workbench feature stays enabled.")
                .define("enabled", true);
        builder.pop();

        builder.push("deathPenalty");
        DEATH_PENALTY_ENABLED = builder
                .comment("If true, players receive a short aftereffects debuff after respawning.")
                .define("enabled", true);
        builder.pop();

        SPEC = builder.build();
        rebuildCampfireFuelRules();
    }

    private HarderBeginningsConfig() {
    }

    public static double fiberDropChance() {
        return FIBER_DROP_CHANCE.get();
    }

    public static boolean flintToolsEnabled() {
        return FLINT_TOOLS_ENABLED.get();
    }

    public static int charcoalBurnTicksPerLog() {
        return CHARCOAL_BURN_SECONDS_PER_LOG.get() * 20;
    }

    public static int charcoalCheckIntervalTicks() {
        return CHARCOAL_CHECK_INTERVAL_SECONDS.get() * 20;
    }

    public static double charcoalRequiredSealRatio() {
        return CHARCOAL_REQUIRED_SEAL_RATIO.get();
    }

    public static double charcoalRequiredQualifiedTimeRatio() {
        return CHARCOAL_REQUIRED_QUALIFIED_TIME_RATIO.get();
    }

    public static boolean torchEnabled() {
        return TORCH_ENABLED.get();
    }

    public static int torchBurnTicks() {
        return TORCH_BURN_SECONDS.get() * 20;
    }

    public static int getCampfireFuelTicks(ItemStack stack) {
        return campfireFuelRules.getFuelTicks(stack);
    }

    public static boolean woodWorkbenchEnabled() {
        return WOOD_WORKBENCH_ENABLED.get();
    }

    public static boolean deathPenaltyEnabled() {
        return DEATH_PENALTY_ENABLED.get();
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            rebuildCampfireFuelRules();
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            rebuildCampfireFuelRules();
        }
    }

    private static List<String> defaultCampfireFuelEntries() {
        List<String> entries = new ArrayList<>();
        entries.add("minecraft:stick=100");
        entries.add("#minecraft:planks=300");
        entries.add("#minecraft:logs=1200");
        entries.add("minecraft:charcoal=1600");
        entries.add("minecraft:coal=1600");
        entries.add("minecraft:coal_block=16000");
        return entries;
    }

    private static void rebuildCampfireFuelRules() {
        Map<Item, Integer> exactItems = new HashMap<>();
        List<TagFuelEntry> tagEntries = new ArrayList<>();

        for (String rawEntry : CAMPFIRE_FUEL_ENTRIES.get()) {
            ParsedFuelEntry parsedEntry = parseFuelEntry(rawEntry);
            if (parsedEntry == null) {
                continue;
            }

            if (parsedEntry.isTag()) {
                tagEntries.add(new TagFuelEntry(
                        TagKey.create(Registries.ITEM, parsedEntry.id()),
                        parsedEntry.ticks()
                ));
                continue;
            }

            Item item = ForgeRegistries.ITEMS.getValue(parsedEntry.id());
            if (item == null) {
                LOGGER.warn("Ignoring unknown campfire fuel item in config: {}", rawEntry);
                continue;
            }

            exactItems.put(item, parsedEntry.ticks());
        }

        campfireFuelRules = new CampfireFuelRules(exactItems, tagEntries);
    }

    private static ParsedFuelEntry parseFuelEntry(String rawEntry) {
        String trimmed = rawEntry.trim();
        int equalsIndex = trimmed.indexOf('=');
        if (equalsIndex <= 0 || equalsIndex == trimmed.length() - 1) {
            LOGGER.warn("Ignoring invalid campfire fuel config entry: {}", rawEntry);
            return null;
        }

        String keyPart = trimmed.substring(0, equalsIndex).trim();
        String ticksPart = trimmed.substring(equalsIndex + 1).trim();

        int ticks;
        try {
            ticks = Integer.parseInt(ticksPart);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Ignoring campfire fuel config entry with invalid tick value: {}", rawEntry);
            return null;
        }

        if (ticks <= 0) {
            LOGGER.warn("Ignoring campfire fuel config entry with non-positive tick value: {}", rawEntry);
            return null;
        }

        boolean isTag = keyPart.startsWith("#");
        String idString = isTag ? keyPart.substring(1) : keyPart;
        ResourceLocation id = ResourceLocation.tryParse(idString);
        if (id == null) {
            LOGGER.warn("Ignoring campfire fuel config entry with invalid id: {}", rawEntry);
            return null;
        }

        return new ParsedFuelEntry(isTag, id, ticks);
    }

    private record ParsedFuelEntry(boolean isTag, ResourceLocation id, int ticks) {
    }

    private record TagFuelEntry(TagKey<Item> tag, int ticks) {
    }

    private record CampfireFuelRules(Map<Item, Integer> exactItems, List<TagFuelEntry> tagEntries) {
        static CampfireFuelRules empty() {
            return new CampfireFuelRules(Map.of(), List.of());
        }

        int getFuelTicks(ItemStack stack) {
            Integer exactTicks = exactItems.get(stack.getItem());
            if (exactTicks != null) {
                return exactTicks;
            }

            for (TagFuelEntry tagEntry : tagEntries) {
                if (stack.is(tagEntry.tag())) {
                    return tagEntry.ticks();
                }
            }

            return 0;
        }
    }
}
