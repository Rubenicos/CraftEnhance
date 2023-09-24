package com.dutchjelly.craftenhance.crafthandling.recipes;

import com.dutchjelly.craftenhance.crafthandling.util.IMatcher;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import com.dutchjelly.craftenhance.messaging.Debug;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class WBRecipe extends EnhancedRecipe {

    @Getter @Setter
    private boolean shapeless = false; //false by default

    @Getter
    private final RecipeType type = RecipeType.WORKBENCH;
    private Recipe recipe;

    public WBRecipe(final String perm, final ItemStack result, final ItemStack[] content){
        super(perm, result, content);
    }

    private WBRecipe(final Map<String, Object> args){
        super(args);
        if(args.containsKey("shapeless"))
            shapeless = (Boolean) args.get("shapeless");
    }

    public WBRecipe(){

    }


    public static WBRecipe deserialize(final Map<String,Object> args){

        final WBRecipe recipe = new WBRecipe(args);
        if(args.containsKey("shapeless"))
            recipe.shapeless = (Boolean) args.get("shapeless");

        return recipe;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>(){{
            putAll(WBRecipe.super.serialize());
            put("shapeless", shapeless);
        }};
    }


    @Override
    public Recipe getServerRecipe() {
        if (recipe == null) {
            if (shapeless)
                recipe = ServerRecipeTranslator.translateShapelessEnhancedRecipe(this);
            recipe = ServerRecipeTranslator.translateShapedEnhancedRecipe(this);
        }
        return recipe;
    }


    //The recipe is similar to a server recipe if theyre both shaped and their shapes match, if at least one is shaped and the ingredients match
    //Note that similar doesn't mean that the recipes are always equal. Shaped is always similar to shapeless, but not the other way around.
    @Override
    public boolean isSimilar(final Recipe r) {
        if(r instanceof ShapelessRecipe){
            final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) r);
            final boolean result = WBRecipeComparer.ingredientsMatch(getContentItems(), ingredients, ItemMatchers::matchType);
            return result;
        }

        if(r instanceof ShapedRecipe){
            final ItemStack[] shapedContent = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe)r);
            if(shapeless){
                return WBRecipeComparer.ingredientsMatch(shapedContent, getContentItems(), ItemMatchers::matchType);
            }
            return WBRecipeComparer.shapeMatches(getContentItems(), shapedContent, ItemMatchers::matchType);
        }
        return false;
    }

    //Looks if r is always similar to this (so we know it doesn't have to be loaded in again)
    @Override
    public boolean isAlwaysSimilar(final Recipe r){
        if(!ItemMatchers.matchItems(r.getResult(), getResult().getItem())) //different result means it needs to be loaded in
            return false;

        if(r instanceof ShapelessRecipe){ //shapeless to shaped or shapeless is always similar
            final ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe) r);
            return WBRecipeComparer.ingredientsMatch(getContentItems(), ingredients, ItemMatchers::matchTypeData);
        }

        if(r instanceof ShapedRecipe && !shapeless){ //shaped to shaped (not shapeless) is similar
            final ItemStack[] shapedContent = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe)r);
            return WBRecipeComparer.shapeMatches(getContentItems(), shapedContent, ItemMatchers::matchTypeData);
        }
        return false;
    }

    @Override
    public boolean isSimilar(final EnhancedRecipe r) {
        if(r == null) return false;
        if(!(r instanceof WBRecipe)) return false;

        final WBRecipe wbr = (WBRecipe)r;
        if(wbr.isShapeless() || shapeless){
            return WBRecipeComparer.ingredientsMatch(getContentItems(), wbr.getContentItems(), ItemMatchers::matchType);
        }
        return WBRecipeComparer.shapeMatches(getContentItems(), wbr.getContentItems(), ItemMatchers::matchType);
    }

    @Override
    public boolean matches(final ItemStack[] content) {

        if(isShapeless() && WBRecipeComparer.ingredientsMatch(content, getContent(),  getMatchType().getMatcher())){
            return true;
        }

        if(!isShapeless() && WBRecipeComparer.shapeMatches(content, getContent(), getMatchType().getMatcher())){
            return true;
        }

        return false;
    }

    public boolean makatches(final ItemStack[] content) {

        if (isShapeless() && WBRecipeComparer.ingredientsMatch(content, getContent(),  getMatchType().getMatcher())) {
            return true;
        }

        Debug.Send("Using " + getMatchType().name() + " matcher");
        if (!isShapeless() && shapeMatches(content, getContent(), getMatchType().getMatcher())) {
            return true;
        }

        return false;
    }

    private static ItemStack[] mirror(final ItemStack[] content, final int size){
        if(content == null) return null;
        if(content.length == 0) return content;
        final ItemStack[] mirrored = new ItemStack[content.length];


        for(int i = 0; i < size; i++){

            //Walk through right and left elements of this row and swab them.
            for(int j = 0; j < size/2; j++){
                final int i1 = i * size + (size - j - 1);
                mirrored[i*size+j] = content[i1];
                mirrored[i1] = content[i*size+j];
            }

            //Copy middle item to mirrored.
            if(size%2 != 0)
                mirrored[i*size+(size/2)] = content[i*size+(size/2)];
        }
        return mirrored;
    }

    public static boolean shapeMatches(final ItemStack[] content, final RecipeItem[] stacks, final IMatcher<ItemStack> matcher) {
        final int rowSize = content == null ? 0 : (int)Math.sqrt(content.length);

        if (shapeIterationMatches(content, stacks, matcher, rowSize)) {
            Debug.Send("Shaped matches");
            return true;
        }
        if (shapeIterationMatches(mirror(content, rowSize), stacks, matcher, rowSize)) {
            Debug.Send("Shaped mirror matches");
            return true;
        }
        return false;
    }

    public static boolean shapeIterationMatches(final ItemStack[] itemsOne, final RecipeItem[] itemsTwo, final IMatcher<ItemStack> matcher, final int rowSize) {
        // Find the first element of r and content.
        int indexTwo = -1, indexOne = -1;
        while (++indexTwo < itemsTwo.length && (itemsTwo[indexTwo] == null || itemsTwo[indexTwo].getItem() == null || itemsTwo[indexTwo].getItem().getType() == Material.AIR));
        while (++indexOne < itemsOne.length && (itemsOne[indexOne] == null || itemsOne[indexOne].getType() == Material.AIR));

        // Look if one or both recipes are empty. Return true if both are empty.
        if (indexTwo == itemsTwo.length || indexOne == itemsOne.length) {
            if (indexTwo == itemsTwo.length && indexOne == itemsOne.length) {
                Debug.Send("Both item matrix are empty");
                return true;
            } else {
                Debug.Send("The item matrix " + (indexTwo == itemsTwo.length ? "2" : "1") + " is empty");
                return false;
            }
        }

        if (!matcher.match(itemsTwo[indexTwo], itemsOne[indexOne])) {
            Debug.Send("The items don't match (indexTwo = " + indexTwo + ", indexOne = " + indexOne + ")");
            //Debug.Send("itemsTwo = " + itemsTwo[indexTwo].toString());
            //Debug.Send("itemsOne = " + itemsOne[indexOne].toString());
            return false;
        }

        //Offsets relative to the first item of the recipe.
        int iIndex, twoRowOffset, jIndex, oneRowOffset;
        for(;;) {
            iIndex = twoRowOffset = 0;
            jIndex = oneRowOffset = 0;
            while (++indexTwo < itemsTwo.length) {
                iIndex++;
                if (indexTwo % rowSize == 0) twoRowOffset++;

                if (itemsTwo[indexTwo] != null && itemsTwo[indexTwo].getItem() != null && itemsTwo[indexTwo].getItem().getType() != Material.AIR) break;

            }

            while (++indexOne < itemsOne.length) {
                jIndex++;
                if (indexOne % rowSize == 0) oneRowOffset++;

                if(itemsOne[indexOne] != null && itemsOne[indexOne].getType() != Material.AIR) break;
            }

            if (indexTwo == itemsTwo.length || indexOne == itemsOne.length) {
                Debug.Send("Toki Taku");
                return indexTwo == itemsTwo.length && indexOne == itemsOne.length;
            }
            if (!matcher.match(itemsTwo[indexTwo], itemsOne[indexOne])){
                Debug.Send("The offset relative items don't match (indexTwo = " + indexTwo + ", indexOne = " + indexOne + ")");
                //Debug.Send("itemsTwo = " + itemsTwo[indexTwo].toString());
                //Debug.Send("itemsOne = " + itemsOne[indexOne].toString());
                return false;
            }

            //The offsets have to be the same, otherwise the shape isn't equal.
            if (iIndex != jIndex || twoRowOffset != oneRowOffset) {
                Debug.Send("The offsets have to be the same, otherwise the shape isn't equal");
                return false;
            }
        }
    }

}
