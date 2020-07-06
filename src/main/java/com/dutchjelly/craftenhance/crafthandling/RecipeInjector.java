package com.dutchjelly.craftenhance.crafthandling;


import java.util.Arrays;
import java.util.List;


import com.dutchjelly.craftenhance.IEnhancedRecipe;

import com.dutchjelly.craftenhance.api.CraftEnhanceAPI;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import com.dutchjelly.craftenhance.messaging.Debug;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;

import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

public class RecipeInjector implements Listener{
	
	private JavaPlugin plugin;
	private RecipeLoader loader;
	
	public RecipeInjector(JavaPlugin plugin){
	    this.plugin = plugin;
		loader = RecipeLoader.getInstance();
	}



    @EventHandler
    public void handleCrafting(PrepareItemCraftEvent e){

	    if(e.getRecipe() == null || e.getRecipe().getResult() == null || !plugin.getConfig().getBoolean("enable-recipes")) return;

	    if(!(e.getInventory() instanceof CraftingInventory)) return;

	    CraftingInventory inv = e.getInventory();

	    Recipe serverRecipe = e.getRecipe();

        Debug.Send("The server wants to inject " + serverRecipe.getResult().toString() + " ceh will check or modify this.");

        List<RecipeGroup> possibleRecipeGroups = loader.findGroupsByResult(serverRecipe.getResult());

        if(possibleRecipeGroups == null || possibleRecipeGroups.size() == 0) return;

        for(RecipeGroup group : possibleRecipeGroups){
            for(IEnhancedRecipe eRecipe : group.getEnhancedRecipes()){
                if(!(eRecipe instanceof WBRecipe)) return;

                WBRecipe wbRecipe = (WBRecipe)eRecipe;

                Debug.Send("Checking if enhanced recipe for " + wbRecipe.getResult().toString() + " matches");

                if(wbRecipe.isShapeless()){
                    if(WBRecipeComparer.ingredientsMatch(inv.getMatrix(), wbRecipe.getContent(), wbRecipe.isMatchMeta() ? ItemMatchers::matchMeta : ItemMatchers::matchType)){
                        if(!e.getViewers().stream().allMatch(x -> entityCanCraft(x, wbRecipe)))
                            continue;
                        if(CraftEnhanceAPI.fireEvent(wbRecipe, (Player)e.getViewers().get(0), inv, group))
                            continue;
                        inv.setResult(wbRecipe.getResult());
                        return;
                    }
                }else{
                    if(WBRecipeComparer.shapeMatches(inv.getMatrix(), wbRecipe.getContent(), wbRecipe.isMatchMeta() ? ItemMatchers::matchMeta : ItemMatchers::matchType)){
                        if(!e.getViewers().stream().allMatch(x -> entityCanCraft(x, wbRecipe)))
                            continue;
                        if(CraftEnhanceAPI.fireEvent(wbRecipe, (Player)e.getViewers().get(0), inv, group))
                            continue;
                        inv.setResult(wbRecipe.getResult());
                        return;
                    }else{
                        Debug.Send("shape doesn't match");
                    }
                }
            }
            for(Recipe sRecipe : group.getServerRecipes()){
                if(sRecipe instanceof ShapedRecipe){
                    ItemStack[] content = ServerRecipeTranslator.translateShapedRecipe((ShapedRecipe)sRecipe);
                    if(WBRecipeComparer.shapeMatches(content, inv.getMatrix(), ItemMatchers::matchType)){
                        inv.setResult(sRecipe.getResult());
                        return;
                    }
                }else if(sRecipe instanceof ShapelessRecipe){
                    ItemStack[] ingredients = ServerRecipeTranslator.translateShapelessRecipe((ShapelessRecipe)sRecipe);
                    if(WBRecipeComparer.ingredientsMatch(ingredients, inv.getMatrix(), ItemMatchers::matchType)){
                        inv.setResult(sRecipe.getResult());
                        return;
                    }
                }else continue;
            }
        }
        inv.setResult(null); //We found similar custom recipes, but none matched exactly. So set result to null.
    }
	

    private boolean entityCanCraft(HumanEntity entity, IEnhancedRecipe recipe){
	    return recipe.getPermissions() == null || recipe.getPermissions() == ""
                || entity.hasPermission(recipe.getPermissions());
    }
}
