package com.dutchjelly.craftenhance.gui.util;

import com.dutchjelly.craftenhance.exceptions.ConfigError;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static menulibrary.dependencies.rbglib.TextTranslator.toSpigotFormat;

public class GuiUtil {

    public static Inventory CopyInventory(ItemStack[] invContent, String title, InventoryHolder holder){
        if(invContent == null) return null;
        List<ItemStack> copiedItems = Arrays.stream(invContent).map(x -> x == null ? null : x.clone()).collect(Collectors.toList());
        if(copiedItems.size() != invContent.length)
            throw new IllegalStateException("Failed to copy inventory items.");
        Inventory copy = Bukkit.createInventory(holder, invContent.length, title);
        for(int i = 0; i < copiedItems.size(); i++)
            copy.setItem(i, copiedItems.get(i));
        return copy;
    }

    public static Inventory FillInventory(Inventory inv, List<Integer> fillSpots, List<ItemStack> items){
        if(inv == null)
            throw new ConfigError("Cannot fill null inventory");

        if(items.size() > fillSpots.size())
            throw new ConfigError("Too few slots to fill.");

        for(int i = 0; i < items.size(); i++){
            if(fillSpots.get(i) >= inv.getSize())
                throw new ConfigError("Fill spot is outside inventory.");
            inv.setItem(fillSpots.get(i), items.get(i));
        }
        return inv;
    }
    @NotNull
    public static ItemStack setTextItem(@NotNull ItemStack itemStack, String displayName , List<String> lore){
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(setcolorName( displayName));
            meta.setLore(setcolorLore( lore));
        }
        itemStack.setItemMeta(meta);
        return itemStack;
    }
    public static String setcolorName(String name){
            if ( name == null) return null;

        return toSpigotFormat(name);
    }

    public static List<String> setcolorLore(List<String> lore){
        List<String> lores = new ArrayList<>();
        for ( String text :lore){
           if ( text == null) {
               lores.add(null);
               continue;
           }
            lores.add( toSpigotFormat( text));
        }
        return lores;
    }
    public static ItemStack ReplaceAllPlaceHolders(ItemStack item, Map<String,String> placeholders){
        if(item == null) return null;
        placeholders.forEach((key, value) -> ReplacePlaceHolder(item, key, value));
        return item;
    }

    public static ItemStack ReplacePlaceHolder(ItemStack item, String placeHolder, String value){
        if(item == null) return null;
        if(value == null) return null;

        ItemMeta meta = item.getItemMeta();
        if(meta.getDisplayName().contains(placeHolder)){
            meta.setDisplayName(meta.getDisplayName().replace(placeHolder, value));
            item.setItemMeta(meta);
        }


        List<String> lore = meta.getLore();
        if(lore == null)
            return item;

        lore = lore.stream().map(x -> (x == null ? null : x.replace(placeHolder, value))).collect(Collectors.toList());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    //Finds the destination for item in inv, in format <slot, amount>. Sets non-fitting amount in slot -1.
    public static Map<Integer, Integer> findDestination(ItemStack item, Inventory inv, int amount, boolean preferEmpty, List<Integer> whitelist){
        if(item == null)
            return new HashMap<>();
        if(inv == null)
            throw new RuntimeException("cannot try to fit item into null inventory");

        if(amount == -1)
            amount = item.getAmount();

        ItemStack[] storage = inv.getStorageContents();
        Map<Integer,Integer> destination = new HashMap<>();
        int remainingItemQuantity = amount;

        //Fill empty slots first if @preferEmpty is true.
        if(preferEmpty){
            for(int i = 0; i < storage.length; i++){
                if(whitelist != null && !whitelist.contains(i)) continue;
                if(storage[i] == null){
                    destination.put(i, Math.min(remainingItemQuantity, item.getMaxStackSize()));
                    remainingItemQuantity -= destination.get(i);
                }
                if(remainingItemQuantity == 0)
                    return destination;
            }
        }

        //Fill slots from left to right if there's any room for @item.
        for(int i = 0; i < storage.length; i++){
            if(whitelist != null && !whitelist.contains(i)) continue;
            if(storage[i] == null && !destination.containsKey(i)){
                destination.put(i, Math.min(remainingItemQuantity, item.getMaxStackSize()));
                remainingItemQuantity -= destination.get(i);
            }
            else if(storage[i].getAmount() < storage[i].getMaxStackSize() && storage[i].isSimilar(item)){
                int room = Math.min(remainingItemQuantity, storage[i].getMaxStackSize()-storage[i].getAmount());
                destination.put(i, room);
                remainingItemQuantity -= room;
            }

            if(remainingItemQuantity == 0)
                return destination;
        }

        //Look if anything couldn't be filled. Give this slot index -1.
        if(remainingItemQuantity > 0)
            destination.put(-1, remainingItemQuantity);

        return destination;
    }

    public static <T> void swap(T[] list, int a, int b){
        T t = list[a];
        list[a] = list[b];
        list[b] = t;
    }

    public static boolean isNull(ItemStack item){
        return item == null || item.getType().equals(Material.AIR);
    }

    public static boolean changeCategoryName(String msg, Player player){
        if (msg.equals("") || msg.equals("cancel") || msg.equals("quit") || msg.equals("exit"))
            return false;
        if (!msg.isEmpty()) {
            String[] split = msg.split(" ");
            if (split.length > 1){
                CategoryData categoryData = self().getCategoryDataCache().getRecipeCategorys().get(split[0]);
                if (categoryData == null){
                    Messenger.Message("Your category name not exist", player);
                    return true;
                } else {
                    CategoryData newCategoryData = self().getCategoryDataCache().of(split[0],categoryData.getRecipeCategoryItem(),split[1]);
                    newCategoryData.setEnhancedRecipes(categoryData.getEnhancedRecipes());
                    self().getCategoryDataCache().getRecipeCategorys().put(split[0],newCategoryData);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean changeCategory(String msg, Player player){
        if (msg.equals("") || msg.equals("cancel") || msg.equals("quit") || msg.equals("exit"))
            return false;
        if (!msg.isEmpty()) {
            String[] split = msg.split(" ");
            if (split.length > 1){
                CategoryData categoryData = self().getCategoryDataCache().getRecipeCategorys().get(split[0]);
                if (categoryData == null){
                    Messenger.Message("Your category name not exist", player);
                    return true;
                } else {
                    Material material = null;
                    if (split.length >= 3)
                        material = Material.getMaterial(split[2].toUpperCase());
                    CategoryData newCategoryData = self().getCategoryDataCache().of(split[1],material != null ? new ItemStack( material) :categoryData.getRecipeCategoryItem(),null);
                    self().getCategoryDataCache().getRecipeCategorys().remove(split[0]);
                    newCategoryData.setEnhancedRecipes(categoryData.getEnhancedRecipes());
                    self().getCategoryDataCache().getRecipeCategorys().put(split[1],newCategoryData);
                    return false;
                }
            }
        }
        return true;
    }
    public static boolean newCategory(String msg, Player player) {
        if (msg.equals("") || msg.equals("cancel") || msg.equals("quit") || msg.equals("exit"))
            return false;
        if (!msg.isEmpty()) {
            String[] split = msg.split(" ");
            if (split.length > 1) {
                Material material = Material.getMaterial(split[1].toUpperCase());
                if (material == null) {
                    Messenger.Message("Please input valid item name. Your input " + split[1],   player);
                    return true;
                }
                if (self().getCategoryDataCache().addCategory(split[0], new ItemStack(material),null)) {
                    Messenger.Message("Your category name alredy exist",  player);
                    return true;
                } else return false;
            }
        }
        return true;

    }
    public static boolean changeOrCreateCategory(String msg,Player player){
        if (msg.equals("") || msg.equalsIgnoreCase("q") || msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("quit") || msg.equalsIgnoreCase("exit"))
            return false;
        if (!msg.isEmpty()) {
            String[] split = msg.split(" ");
            if (split.length > 1) {
                Material material = null;
                CategoryData categoryData = self().getCategoryDataCache().getRecipeCategorys().get(split[0]);
                if (split.length >= 3)
                    material = Material.getMaterial(split[2].toUpperCase());
                if (categoryData == null) {
                    if (material == null)
                        material = Material.getMaterial(split[1].toUpperCase());
                    if (material == null) {
                        Messenger.Message("Please input valid item name. Your input " + msg, player);
                        return true;
                    }
                    self().getCategoryDataCache().addCategory(split[0], new ItemStack(material),null);
                } else {
                    CategoryData newCategoryData =  self().getCategoryDataCache().of(split[1],material != null ? new ItemStack(material) : categoryData.getRecipeCategoryItem(), null);
                    self().getCategoryDataCache().getRecipeCategorys().remove(split[0]);
                    newCategoryData.setEnhancedRecipes(categoryData.getEnhancedRecipes());
                    self().getCategoryDataCache().getRecipeCategorys().put(split[1], newCategoryData);
                }
                return false;
            }
        }
        return true;
    }
    public static boolean seachCategory(String msg){
        if (msg.equals(""))
            return false;
        return !msg.equals("cancel") && !msg.equals("quit") && !msg.equals("exit");
    }
}
