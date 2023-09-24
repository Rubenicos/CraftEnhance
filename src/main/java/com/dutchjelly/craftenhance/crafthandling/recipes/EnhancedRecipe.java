package com.dutchjelly.craftenhance.crafthandling.recipes;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.files.FileManager;
import com.dutchjelly.craftenhance.gui.interfaces.GuiPlacable;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EnhancedRecipe extends GuiPlacable implements ConfigurationSerializable, ServerLoadable {

    @Getter @Setter
    private int id;

    @Getter @Setter
    private String key;

    @Getter @Setter
    private RecipeItem result;

    @Getter @Setter
    private RecipeItem[] content;

    @Getter @Setter
    private ItemMatchers.MatchType matchType = ItemMatchers.MatchType.MATCH_META;

    @Getter @Setter
    private String permissions;

    @Getter @Setter
    private boolean hidden;

    @Getter
    @Setter
    private String onCraftCommand;

    @Getter
    private RecipeType type;
    @Getter
    private Map<String,Object> deserialize;
    @Getter
    private Map<String,Object> serialize;

    public EnhancedRecipe() {
        this(null, new RecipeItem(null), new RecipeItem[0]);
    }

    public EnhancedRecipe(final String perm, final ItemStack result, final ItemStack[] content) {
        this(perm, new RecipeItem(result), RecipeItem.of(content));
    }

    public EnhancedRecipe(final String perm, final RecipeItem result, final RecipeItem[] content) {
        this.permissions = perm;
        this.result = result;
        this.content = content;
    }

    protected EnhancedRecipe(final Map<String,Object> args) {
        super(args);
        final FileManager fm = CraftEnhance.self().getFm();

        final List<String> recipeKeys;
        result = new RecipeItem(fm.getItem((String)args.get("result")));
        permissions = (String)args.get("permission");
        if (args.containsKey("matchtype")) {
            matchType = ItemMatchers.MatchType.valueOf((String)args.get("matchtype"));
        } else if (args.containsKey("matchmeta")) {
            matchType = (Boolean) args.get("matchmeta") ?
                    ItemMatchers.MatchType.MATCH_META :
                    ItemMatchers.MatchType.MATCH_TYPE;
        }

        if (args.containsKey("oncraftcommand")) {
            onCraftCommand = (String)args.get("oncraftcommand");
        }

        if (args.containsKey("hidden")) {
            hidden = (Boolean) args.get("hidden");
        }


        recipeKeys = (List<String>)args.get("recipe");
        setContent(new RecipeItem[recipeKeys.size()]);
        for (int i = 0; i < content.length; i++) {
            content[i] = new RecipeItem(fm.getItem(recipeKeys.get(i)));
        }
        this.deserialize = args;
    }

    public ItemStack[] getContentItems() {
        final ItemStack[] items = new ItemStack[content.length];
        for (int i = 0; i < content.length; i++) {
            items[i] = content[i].getItem();
        }
        return items;
    }

    @Override
    public Map<String, Object> serialize() {
        final FileManager fm = CraftEnhance.getPlugin(CraftEnhance.class).getFm();
        return new HashMap<String, Object>(){{
            putAll(EnhancedRecipe.super.serialize());
            put("permission", permissions);
            put("matchtype", matchType.name());
            put("hidden", hidden);
            put("oncraftcommand", onCraftCommand);
            put("result", fm.getItemKey(result.getItem()));
            put("recipe", Arrays.stream(content).map(x -> fm.getItemKey(x.getItem())).toArray(String[]::new));
            if (serialize != null && !serialize.isEmpty())
                putAll(serialize);
        }};
    }

    public String validate() {

        if (result.getItem() == null) {
            return "recipe cannot have null result";
        } else if (!Adapter.canUseModeldata() && matchType == ItemMatchers.MatchType.MATCH_MODELDATA_AND_TYPE) {
            return "recipe is using modeldata match while the server doesn't support it";
        } if (content.length == 0 || !Arrays.stream(content).anyMatch(x -> x != null)) {
            return "recipe content cannot be empty";
        }
        return null;
    }

    @Override
    public String toString() {
        return "EnhancedRecipe{" +
                "key='" + key + '\'' +
                ", result=" + (this.result.getItem() == null ? "null" : result.getItem())  +
                '}';
    }

    @Override
    public ItemStack getDisplayItem() {
        return getResult().getItem();
    }

    public void save() {
        if (validate() == null) {
            CraftEnhance.self().getFm().saveRecipe(this);
        }
    }

    public void load(){
        RecipeLoader.getInstance().loadRecipe(this);
    }

    public abstract boolean matches(ItemStack[] content);
}