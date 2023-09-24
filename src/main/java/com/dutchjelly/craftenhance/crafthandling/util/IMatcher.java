package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.craftenhance.crafthandling.recipes.RecipeItem;
import org.bukkit.inventory.ItemStack;

public interface IMatcher<T extends ItemStack> {
    boolean match(T a, T b);

    default boolean match(RecipeItem a, T b) {
        if (a.getNbtMatcher() != null) {
            return a.matchNbt(b);
        }
        return match((T) a.getItem(), b);
    }

    default boolean match(T a, RecipeItem b) {
        if (b.getNbtMatcher() != null) {
            return b.matchNbt(a);
        }
        return match(a, (T) b.getItem());
    }
}
