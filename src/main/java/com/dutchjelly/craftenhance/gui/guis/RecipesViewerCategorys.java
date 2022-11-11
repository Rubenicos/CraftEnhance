package com.dutchjelly.craftenhance.gui.guis;

import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.files.MenuSettingsCache;
import com.dutchjelly.craftenhance.gui.templates.MenuTemplate;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.gui.util.FormatListContents;
import com.dutchjelly.craftenhance.gui.util.GuiUtil;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.brokenarrow.menu.library.MenuButton;
import org.brokenarrow.menu.library.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map.Entry;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static menulibrary.dependencies.rbglib.TextTranslator.toSpigotFormat;

public class RecipesViewerCategorys extends MenuHolder {
	private final MenuSettingsCache menuSettingsCache  = self().getMenuSettingsCache();
	private final MenuTemplate menuTemplate;

	public RecipesViewerCategorys(String grupSeachFor) {
		super(FormatListContents.getCategorys(self().getCategoryDataCache().getRecipeCategorys().values(), grupSeachFor));
		this.menuTemplate = menuSettingsCache.getTemplates().get("RecipesCategorys");
		setFillSpace(this.menuTemplate.getFillSlots());
		setTitle(this.menuTemplate.getMenuTitel());
		setMenuSize(54);
	}

	@Override
	public MenuButton getFillButtonAt(Object object) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory inventory, ClickType clickType, ItemStack itemStack, Object o) {
				if (o instanceof CategoryData)
					new RecipesViewerCopy((CategoryData) o,"",player).menuOpen(player);
			}

			@Override
			public ItemStack getItem(Object object) {
				if (object instanceof CategoryData) {
					ItemStack itemStack = ((CategoryData) object).getRecipeCategoryItem();
					ItemMeta meta = itemStack.getItemMeta();
					if (meta != null){
						String displayName = ((CategoryData) object).getDisplayName();
						if (displayName == null || displayName.equals(""))
							displayName = ((CategoryData) object).getRecipeCategory();
						meta.setDisplayName(toSpigotFormat(displayName));
					}
					itemStack.setItemMeta(meta);
					return itemStack;
				}
				return null;
			}

			@Override
			public ItemStack getItem() {
				return null;
			}
		};
	}
	@Override
	public MenuButton getButtonAt(int slot) {
		if (this.menuTemplate == null) return null;
		for (Entry<List<Integer>, com.dutchjelly.craftenhance.gui.templates.MenuButton> menuTemplate : this.menuTemplate.getMenuButtons().entrySet()){
			if (menuTemplate.getKey().contains(slot)){
				return registerButtons(menuTemplate.getValue());
			}
		}
		return null;
	}


	private MenuButton registerButtons(com.dutchjelly.craftenhance.gui.templates.MenuButton value) {
		return new MenuButton() {
			@Override
			public void onClickInsideMenu(Player player, Inventory menu, ClickType click, ItemStack clickedItem, Object object) {
				if (run(value, menu, player, click))
					updateButtons();
			}

			@Override
			public ItemStack getItem() {
				return value.getItemStack();
			}
		};
	}
	public boolean run(com.dutchjelly.craftenhance.gui.templates.MenuButton value, Inventory menu, Player player, ClickType click) {
		if (value.getButtonType() == ButtonType.PrvPage){
			previousPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.NxtPage){
			nextPage();
			return true;
		}
		if (value.getButtonType() == ButtonType.Search) {
			if (click == ClickType.RIGHT) {
				Messenger.Message("Search for categorys.", getViewer());
				self().getGuiManager().waitForChatInput(this, getViewer(), msg-> {
					if (GuiUtil.seachCategory(msg)){
						new RecipesViewerCategorys( msg).menuOpen(getViewer());
						return false;
					}
					return true;
				});
			}
			else new RecipesViewerCategorys("").menuOpen(player);
		}
		if (value.getButtonType() == ButtonType.changeCategoryName){
			Messenger.Message("Please input your category name and new display name. Like this 'category name' without '.", getViewer());
			self().getGuiManager().waitForChatInput(new RecipesViewerCategorys(""), getViewer(), msg-> {
				if(!GuiUtil.changeCategoryName(msg,player)){
					new RecipesViewerCategorys("").menuOpen(player);
					return false;
				}
				return true;
			});
		}
		if (value.getButtonType() == ButtonType.newCategory){
			Messenger.Message("Please input your category name and item type you want. Like this 'category crafting_table' without '.", getViewer());
			self().getGuiManager().waitForChatInput(new RecipesViewerCategorys(""), getViewer(), msg-> {
				if (!GuiUtil.newCategory(msg, player)) {
					new RecipesViewerCategorys("").menuOpen(player);
					return false;
				}
				return true;
			});
		}
		if (value.getButtonType() == ButtonType.changeCategory){
			Messenger.Message("Change category name and you can also change item (if not set it will use the old one). Like this 'category new_category_name crafting_table' without '.", getViewer());
			self().getGuiManager().waitForChatInput(new RecipesViewerCategorys(""), getViewer(), msg-> {
				if (GuiUtil.changeCategory(msg, player)) {
					new RecipesViewerCategorys("").menuOpen(player);
					return false;
				}
				return true;
			});
			return true;
		}
		return false;
	}
}
