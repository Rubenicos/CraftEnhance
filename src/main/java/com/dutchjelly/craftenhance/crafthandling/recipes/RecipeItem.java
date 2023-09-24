package com.dutchjelly.craftenhance.crafthandling.recipes;

import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@RequiredArgsConstructor
public class RecipeItem {

    @Getter
    private final ItemStack item;

    @Getter
    private final String nbtMatcher;

    public static RecipeItem[] of(ItemStack[] array) {
        final RecipeItem[] items = new RecipeItem[array.length];
        for (int i = 0; i < array.length; i++) {
            items[i] = new RecipeItem(array[i]);
        }
        return items;
    }

    public RecipeItem(ItemStack item) {
        this(item, ItemMatchers.getNbtMatcher(item));
    }

    public boolean matchNbt(ItemStack item) {
        return ItemMatchers.matchNbt(item, this.item, nbtMatcher);
    }

    public boolean equals(ItemStack item) {
        if (item == null) {
            return this.item == null;
        }
        if (this.item == null) {
            return false;
        }
        if (nbtMatcher != null) {
            return matchNbt(item);
        }
        return this.item.equals(item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ItemStack) {
            return o.equals(item);
        }
        if (o == null || getClass() != o.getClass()) return false;

        RecipeItem that = (RecipeItem) o;

        return Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return item != null ? item.hashCode() : 0;
    }
}
