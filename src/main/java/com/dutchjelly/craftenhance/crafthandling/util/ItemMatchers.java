package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import com.dutchjelly.craftenhance.util.StripColors;
import com.saicone.rtag.RtagItem;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class ItemMatchers {

    public enum MatchType {

        MATCH_TYPE(constructIMatcher(ItemMatchers::matchType), "match type"),
        MATCH_META(constructIMatcher(ItemMatchers::matchMeta), "match meta"),
        MATCH_NAME(constructIMatcher(ItemMatchers::matchName), "match name"),
        MATCH_MODELDATA_AND_TYPE(constructIMatcher(ItemMatchers::matchType, ItemMatchers::matchModelData), "match modeldata and type"),
        MATCH_NAME_LORE(constructIMatcher(ItemMatchers::matchNameLore), "match name and lore");
        //        MATCH_ITEMSADDER()
//        MATCH_NAME_AND_TYPE(constructIMatcher(ItemMatchers::matchName, ItemMatchers::matchType), "match name and type");

        @Getter
        private final IMatcher<ItemStack> matcher;

        @Getter
        private final String description;


        MatchType(final IMatcher<ItemStack> matcher, final String description) {
            this.matcher = matcher;
            this.description = description;
        }
    }

    private static boolean backwardsCompatibleMatching = false;
    private static final Map<String, List<Object[]>> NBT_MATCHER_PATHS = new HashMap<>();

    public static void init(final boolean backwardsCompatibleMatching, ConfigurationSection nbtMatchers) {
        ItemMatchers.backwardsCompatibleMatching = backwardsCompatibleMatching;
        NBT_MATCHER_PATHS.clear();
        if (nbtMatchers == null) {
            return;
        }
        for (String key : nbtMatchers.getKeys(false)) {
            final List<Object[]> paths = new ArrayList<>();
            for (String s : nbtMatchers.getStringList(key)) {
                if (s.trim().isEmpty()) {
                    continue;
                }
                final String[] split = s.replace("[.]", "<dot>").split("\\.");
                final Object[] path = new Object[split.length];
                for (int i = 0; i < split.length; i++) {
                    String pathKey = split[i].replace("<dot>", ".");
                    if (pathKey.toLowerCase().startsWith("index=") && pathKey.length() > 6) {
                        pathKey = pathKey.substring(6);
                        try {
                            final int index = Integer.parseInt(pathKey);
                            path[i] = index;
                            continue;
                        } catch (NumberFormatException ignored) { }
                    }
                    path[i] = pathKey;
                }
                paths.add(path);
            }
            if (paths.isEmpty()) {
                Debug.Send("The NBT matcher '" + key + "' is empty");
                continue;
            }
            Debug.Send("The NBT matcher '" + key + "' has been loaded with paths = " + paths);
            NBT_MATCHER_PATHS.put(key, paths);
        }
    }

    public static String getNbtMatcher(final ItemStack item) {
        if (item == null) {
            return null;
        }
        for (Map.Entry<String, List<Object[]>> entry : NBT_MATCHER_PATHS.entrySet()) {
            final RtagItem tag = new RtagItem(item);
            boolean contains = true;
            for (Object[] path : entry.getValue()) {
                if (tag.notHasTag(path)) {
                    contains = false;
                    break;
                }
            }
            if (contains) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean matchItems(final ItemStack a, final ItemStack b) {
        if (a == null || b == null) return a == null && b == null;
        return a.equals(b);
    }

    public static boolean matchModelData(final ItemStack a, final ItemStack b) {
        final ItemMeta am = a.getItemMeta();
        final ItemMeta bm = b.getItemMeta();
        if (am == null) return bm == null || !bm.hasCustomModelData();
        if (bm == null) return am == null || !am.hasCustomModelData();
        return am.hasCustomModelData() == bm.hasCustomModelData() && (!am.hasCustomModelData() || am.getCustomModelData() == bm.getCustomModelData());
    }

    // a = CraftEnhance
    // b = Crafting Table
    public static boolean matchMeta(final ItemStack a, final ItemStack b) {
        if (a == null || b == null) return a == null && b == null;
        final boolean canUseModeldata = Adapter.canUseModeldata();

        if (backwardsCompatibleMatching) {
            return a.getType().equals(b.getType()) && a.getDurability() == b.getDurability() && a.hasItemMeta() == b.hasItemMeta() && (!a.hasItemMeta() || (
                    a.getItemMeta().toString().equals(b.getItemMeta().toString()))
                    && (canUseModeldata && matchModelData(a, b) || !canUseModeldata)
            );
        }

        return a.isSimilar(b) && (!canUseModeldata || matchModelData(a, b));
    }

    public static boolean matchType(final ItemStack a, final ItemStack b) {
        if (a == null || b == null) return a == null && b == null;
        return a.getType().equals(b.getType());
    }

    @SafeVarargs
    public static <T extends ItemStack> IMatcher<T> constructIMatcher(final IMatcher<T>... matchers) {
        return (a, b) -> Arrays.stream(matchers).allMatch(x -> x.match(a, b));
    }

    public static boolean matchCustomModelData(final ItemStack a, final ItemStack b) {
        if (a == null || b == null) return a == null && b == null;

        if (a.hasItemMeta() && b.hasItemMeta()) {
            final ItemMeta itemMetaA = a.getItemMeta();
            final ItemMeta itemMetaB = b.getItemMeta();
            if (itemMetaA != null && itemMetaB != null && itemMetaA.hasCustomModelData() && itemMetaB.hasCustomModelData())
                return itemMetaA.getCustomModelData() == itemMetaB.getCustomModelData();
        }
        return false;
    }

    public static boolean matchTypeData(final ItemStack a, final ItemStack b) {

        if (a == null || b == null) return a == null && b == null;

        if (self().getVersionChecker().olderThan(VersionChecker.ServerVersion.v1_14)) {
            if (a.getData() == null && b.getData() == null)
                return matchType(a, b);
            return a.getData().equals(b.getData());
        } else {
            if (a.hasItemMeta() && b.hasItemMeta()) {
                return matchCustomModelData(a, b) || matchType(a, b);
            }
            return matchType(a, b);
        }
    }

    public static boolean matchName(final ItemStack a, final ItemStack b) {
        if (a.hasItemMeta() && b.hasItemMeta()) {
            return a.getItemMeta().getDisplayName().equals(b.getItemMeta().getDisplayName());
        }
        //neither has item meta, and type has to match
        return a.hasItemMeta() == b.hasItemMeta() && a.getType() == b.getType();
    }

    public static boolean matchNameLore(final ItemStack a, final ItemStack b) {
        if (a.hasItemMeta() && b.hasItemMeta()) {
            final ItemMeta itemMetaA = a.getItemMeta();
            final ItemMeta itemMetaB = b.getItemMeta();

            if (itemMetaA != null && itemMetaB != null) {
                boolean hasSameLore = itemMetaA.getLore() == null || itemMetaA.getLore().equals(itemMetaB.getLore());
                if (!hasSameLore)
                    hasSameLore = StripColors.stripLore(itemMetaA.getLore()).equals(StripColors.stripLore(itemMetaB.getLore()));

                return itemMetaA.getDisplayName().equals(itemMetaB.getDisplayName()) && hasSameLore;
            }
        }
        //neither has item meta, and type has to match
        return a.hasItemMeta() == b.hasItemMeta() && a.getType() == b.getType();
    }

    public static boolean matchNbt(final ItemStack a, final ItemStack b, final String key) {
        return NBT_MATCHER_PATHS.containsKey(key) && matchNbt(a, b, NBT_MATCHER_PATHS.get(key));
    }

    public static boolean matchNbt(final ItemStack a, final ItemStack b, final List<Object[]> paths) {
        final RtagItem tagA = new RtagItem(a);
        final RtagItem tagB = new RtagItem(b);

        for (Object[] path : paths) {
            final Object objectA;
            final Object objectB;
            if ((objectA = tagA.get(path)) == null || (objectB = tagB.get(path)) == null || !Objects.deepEquals(objectA, objectB)) {
                return false;
            }
        }
        return true;
    }
}
