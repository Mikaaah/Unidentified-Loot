package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import dev.mikaaah.tnkloot.menu.TnkMenuNetwork;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TnkMenuScreen extends AbstractContainerScreen<TnkMenu> {
   private static final int TEXT = MarketTheme.INK;
   private static final int MUTED = MarketTheme.MUTED;
   private static final int GOLD = MarketTheme.GOLD;
   private Button confirm;
   private Button toggle;
   private Button remove;
   private Button apply;
   private Button equal;
   private Button previous;
   private Button next;
   private Button filter;
   private Button rarity;
   private EditBox chance;
   private final List<AbstractWidget> vaultWidgets = new ArrayList<>();
   private final List<TnkMenuScreen.NavigationCard> groupCards = new ArrayList<>();
   private final List<TnkMenuScreen.NavigationCard> tierCards = new ArrayList<>();
   private Button back;
   private Button tierPrevious;
   private Button tierNext;
   private String selectedKey = "";

   public TnkMenuScreen(TnkMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = menu.admin() ? 304 : 264;
      this.imageHeight = menu.admin() ? 250 : 222;
      this.inventoryLabelY = 10000;
      this.titleLabelY = 10000;
   }

   protected void init() {
      super.init();
      this.topPos = StationHeader.top(this.height);
      int x = this.leftPos;
      int y = this.topPos;
      if (((TnkMenu)this.menu).admin()) {
         this.vaultWidgets.clear();
         this.groupCards.clear();
         this.tierCards.clear();
         this.filter = this.addButton("Groups", x + 196, y + 24, 100, 18, 31, "Choose a different item group");
         this.previous = this.addButton("‹", x + 16, y + 128, 20, 18, 10, "Previous vault page");
         this.next = this.addButton("›", x + 158, y + 128, 20, 18, 11, "Next vault page");
         this.toggle = this.addButton("Disable", x + 196, y + 74, 100, 18, 20, "Exclude this item from rolls in this tier; keep its entry");
         this.remove = this.addButton("Remove", x + 196, y + 96, 100, 18, 21, "Remove this item from this tier only");
         this.chance = new EditBox(this.font, x + 197, y + 132, 55, 18, Component.literal(RaritySupport.clientHint("Chance percentage")));
         this.chance.setMaxLength(6);
         this.chance.setBordered(false);
         this.chance.setFilter(s -> s.matches("[0-9]{0,3}(\\.[0-9]{0,2})?"));
         this.chance.setTextColor(MarketTheme.INK);
         this.chance
            .setTooltip(
               Tooltip.create(
                  Component.literal(
                     RaritySupport.clientHint("0–100%. Other chances adjust to keep 100%. 0 disables this item; 100 disables the other items in its category.")
                  )
               )
            );
         this.addRenderableWidget(this.chance);
         this.apply = (Button)this.addRenderableWidget(
            MarketTheme.button(x+256,y+132,40,18,"Set %",MarketTheme.PURPLE,b -> this.setChance(),"Apply chance")
         );
         this.equal = this.addButton("Equal chances", x + 196, y + 157, 100, 18, 22, "Reset this group’s enabled items to equal chances in this tier");
         this.rarity = this.addButton("Rarity rolls", x + 196, y + 181, 100, 18, 3, "Show this tier's rarity chances and cost multipliers");
         this.addButton("+ New tier", x + 196, y + 205, 100, 18, 6, "Create a new progression tier with a separate pool in each item group");
         this.vaultWidgets.addAll(List.of(this.filter, this.previous, this.next, this.toggle, this.remove, this.chance, this.apply, this.equal, this.rarity));

         for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget) {
               AbstractWidget widget = (AbstractWidget)child;
               if (!this.vaultWidgets.contains(widget)) {
                  this.vaultWidgets.add(widget);
               }
            }
         }

         this.back = this.addButton("‹ Groups", x + 16, y + 24, 68, 18, 30, "Return to the previous chooser");

         for (TnkConfig.Category category : TnkConfig.Category.values()) {
            int n = category.ordinal();
            TnkMenuScreen.NavigationCard card = (TnkMenuScreen.NavigationCard)this.addRenderableWidget(
               new TnkMenuScreen.NavigationCard(
                  x + 18 + n % 2 * 140, y + 45 + n / 2 * 46, 128, 40, b -> this.send(100 + n), groupIcon(category), groupName(category), "Choose tier"
               )
            );
            this.groupCards.add(card);
         }

         for (int i = 0; i < 12; i++) {
            int n = i;
            this.tierCards
               .add(
                  (TnkMenuScreen.NavigationCard)this.addRenderableWidget(
                     new TnkMenuScreen.NavigationCard(
                        x + 16 + i % 3 * 94,
                        y + 47 + i / 3 * 42,
                        84,
                        36,
                        b -> this.send(1000 + ((TnkMenu)this.menu).tierPage() * 12 + n),
                        ItemStack.EMPTY,
                        "",
                        ""
                     )
                  )
               );
         }

         this.tierPrevious = this.addButton("‹", x + 16, y + 225, 20, 18, 32, "Previous tier page");
         this.tierNext = this.addButton("›", x + 268, y + 225, 20, 18, 33, "Next tier page");
         this.selectedKey = "";
      } else {
         this.confirm = (Button)this.addRenderableWidget(new TnkMenuScreen.CounterButton(x + 214, y + 51, 34, 30, b -> this.send(0)));
      }

      this.updateControls();
   }

   private void send(int action) {
      if (this.minecraft != null && this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((TnkMenu)this.menu).containerId, action);
      }
   }

   private Button addButton(String label, int x, int y, int width, int height, int action, String tooltip) {
      Button button = (Button)this.addRenderableWidget(
         MarketTheme.button(x,y,width,height,RaritySupport.clientHint(label),MarketTheme.PURPLE,b -> this.send(action),tooltip)
      );
      button.setTooltip(Tooltip.create(Component.literal(RaritySupport.clientHint(tooltip))));
      return button;
   }

   private void setChance() {
      CompoundTag entry = ((TnkMenu)this.menu).selectedMetadata();
      if (!entry.isEmpty()) {
         try {
            int basis = new BigDecimal(this.chance.getValue()).movePointRight(2).intValueExact();
            if (basis < 0 || basis > 10000) {
               return;
            }

            PacketDistributor.sendToServer(
               new TnkMenuNetwork.Chance(((TnkMenu)this.menu).containerId, ((TnkMenu)this.menu).header().getInt("rank"), entry.getString("item"), basis),
               new CustomPacketPayload[0]
            );
         } catch (ArithmeticException | NumberFormatException var3) {
         }
      }
   }

   protected void containerTick() {
      super.containerTick();
      this.updateControls();
   }

   private void updateControls() {
      if (this.confirm != null) {
         this.confirm.active = !((TnkMenu)this.menu).busy() && ((TnkMenu)this.menu).affordable() && ((TnkMenu)this.menu).inputCount() > 0;
         this.confirm
            .setTooltip(
               Tooltip.create(
                  Component.translatable(
                     "menu.tnkloot.confirm",
                     new Object[]{
                        ((TnkMenu)this.menu).inputCount(),
                        ((TnkMenu)this.menu).quotedCost(),
                        Component.translatable(((TnkMenu)this.menu).currency().getDescriptionId())
                     }
                  )
               )
            );
      }

      if (this.chance != null) {
         int view = ((TnkMenu)this.menu).adminView();
         boolean vault = view == 2;
         boolean tiers = view == 1;

         for (AbstractWidget widget : this.vaultWidgets) {
            widget.visible = vault;
         }

         for (TnkMenuScreen.NavigationCard card : this.groupCards) {
            card.visible = view == 0;
         }

         List<TnkConfig.Tier> tierList = ((TnkMenu)this.menu).navigationTiers();

         for (int i = 0; i < this.tierCards.size(); i++) {
            int n = ((TnkMenu)this.menu).tierPage() * 12 + i;
            TnkMenuScreen.NavigationCard card = this.tierCards.get(i);
            card.visible = tiers && n < tierList.size();
            if (n < tierList.size()) {
               TnkConfig.Tier tier = tierList.get(n);
               card.setMessage(Component.literal(RaritySupport.clientHint(tier.name())));
               card.detail = "Tier " + tier.rank() + " · " + ((TnkMenu)this.menu).navigationPoolCount(tier.rank()) + " items";
               card.setTooltip(
                  Tooltip.create(
                     Component.literal(
                        RaritySupport.clientHint(
                           groupName(((TnkMenu)this.menu).filterCategory()) + " / " + tier.name() + "\nSource levels: " + dev.mikaaah.tnkloot.compat.EquipmentGroups.levels(tier.rank()) + "\nRarity and attributes follow the sealed drop"
                        )
                     )
                  )
               );
            }
         }

         this.back.visible = view != 0;
         this.back.setWidth(vault ? 46 : 68);
         this.back.setMessage(Component.literal(RaritySupport.clientHint(vault ? "‹ Tiers" : "‹ Groups")));
         this.tierPrevious.visible = this.tierNext.visible = tiers;
         this.tierPrevious.active = ((TnkMenu)this.menu).tierPage() > 0;
         this.tierNext.active = (((TnkMenu)this.menu).tierPage() + 1) * 12 < tierList.size();
         if (!vault) {
            this.chance.setFocused(false);
         } else {
            CompoundTag entry = ((TnkMenu)this.menu).selectedMetadata();
            boolean selected = !entry.isEmpty();
            String key = ((TnkMenu)this.menu).header().getInt("rank")
               + ":"
               + entry.getString("item")
               + ":"
               + entry.getString("chance")
               + ":"
               + entry.getBoolean("enabled");
            if (!key.equals(this.selectedKey)) {
               this.chance.setValue(selected ? entry.getString("chance") : "");
               this.selectedKey = key;
            }

            this.toggle.active = this.remove.active = this.chance.active = selected;
            this.toggle.setMessage(Component.literal(RaritySupport.clientHint(entry.getBoolean("enabled") ? "Disable" : "Enable")));
            this.apply.active = selected && this.validChance();
            this.previous.active = ((TnkMenu)this.menu).page() > 0;
            this.next.active = ((TnkMenu)this.menu).page() + 1 < ((TnkMenu)this.menu).pageCount();
            this.filter.setMessage(Component.literal(RaritySupport.clientHint(groupName(((TnkMenu)this.menu).filterCategory()))));
            String group = ((TnkMenu)this.menu).header().getString("rarity");
            this.rarity.setMessage(Component.literal(RaritySupport.clientHint(this.fit("Affixes: " + group.substring(group.indexOf(58) + 1), 94))));
            this.equal.active = ((TnkMenu)this.menu).totalEntries() > 0;
         }
      }
   }

   private boolean validChance() {
      try {
         BigDecimal n = new BigDecimal(this.chance.getValue());
         return n.signum() >= 0 && n.compareTo(BigDecimal.valueOf(100L)) <= 0;
      } catch (NumberFormatException var2) {
         return false;
      }
   }

   private static String pretty(String value) {
      String s = value.toLowerCase(Locale.ROOT);
      return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
   }

   private static void panel(GuiGraphics g, int x, int y, int w, int h, int fill) {
      MarketTheme.panel(g,x,y,w,h,fill);
   }

   private static void slotBackground(GuiGraphics g, int x, int y, boolean purple) {
      MarketTheme.slot(g::fill,x,y,purple?0xFF4D2B32:0xFFB88B68);
   }

   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      if(menu.admin())MarketTheme.vault(g,x,y,menu.adminView()==2);
      if (((TnkMenu)this.menu).admin()) {
         if (((TnkMenu)this.menu).adminView() != 2) {
            return;
         }


         panel(g, x + 190, y + 47, 110, 131, 0xFF446077);
         MarketTheme.panel(g,x+195,y+130,59,22,0xFF20293F);



         int selected = ((TnkMenu)this.menu).selectedVaultSlot();
         if (selected >= 0) {
            Slot slotx = ((TnkMenu)this.menu).getSlot(selected);
            int sx = x + slotx.x;
            int sy = y + slotx.y;
            g.renderOutline(sx - 2, sy - 2, 20, 20, MarketTheme.GOLD);
         }
      } else {
         MarketTheme.counter(g,x,y,false);
         if(menu.busy())g.renderOutline(x+23+Math.clamp(menu.rollingIndex(),0,6)*24,y+55,20,20,MarketTheme.GOLD);
      }
   }

   private static void ribbon(GuiGraphics g, int x, int y, int width) {
      for (int row = 0; row < 27; row++) {
         int notch = row < 13 ? row / 3 : (26 - row) / 3;
         g.fill(x + notch, y + row, x + width - notch, y + row + 1, -13229762);
      }

      g.fill(x + 4, y + 2, x + width - 4, y + 23, -10078854);
      g.fill(x + 16, y, x + width - 16, y + 25, -6528595);
      g.fill(x + 18, y + 2, x + width - 18, y + 22, -9881729);
      g.fill(x + 20, y + 3, x + width - 20, y + 5, -8238694);
      g.fill(x + 16, y + 23, x + width - 16, y + 26, -12574387);
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      if (((TnkMenu)this.menu).admin()) {
         if (((TnkMenu)this.menu).adminView() != 2) {
            boolean groups = ((TnkMenu)this.menu).adminView() == 0;
            StationHeader.draw(g,this.font,groups ? "Choose item group" : groupName(((TnkMenu)this.menu).filterCategory()) + " · Choose tier",8,288);
            g.drawString(this.font, groups ? "Each group has its own tier vaults" : "Click a tier to open its vault", groups ? 16 : 93, 28, MarketTheme.MUTED, false);
            if (!groups) {
               g.drawCenteredString(
                  this.font,
                  ((TnkMenu)this.menu).tierPage() + 1 + " / " + Math.max(1, (((TnkMenu)this.menu).navigationTiers().size() + 12 - 1) / 12),
                  152,
                  231,
                  MarketTheme.MUTED
               );
            }

            return;
         }

         StationHeader.draw(g,this.font,"Tier Vault",8,288);
         g.drawString(this.font, "Saved automatically", 190, 14, MarketTheme.MUTED, false);
         CompoundTag h = ((TnkMenu)this.menu).header();
         g.drawCenteredString(this.font, this.fit(h.getInt("rank") + " · " + h.getString("name"), 126), 128, 29, MarketTheme.INK);
         g.drawCenteredString(
            this.font,
            ((TnkMenu)this.menu).page() + 1 + " / " + ((TnkMenu)this.menu).pageCount() + " · " + ((TnkMenu)this.menu).totalEntries() + " items",
            97,
            134,
            MarketTheme.MUTED
         );
         g.drawString(this.font, "Inventory", 16, 154, MarketTheme.MUTED, false);
         g.drawString(this.font, "Shift-click to add", 90, 154, MarketTheme.MUTED, false);
         CompoundTag entry = ((TnkMenu)this.menu).selectedMetadata();
         int selected = ((TnkMenu)this.menu).selectedVaultSlot();
         if (!entry.isEmpty() && selected >= 0) {
            ItemStack stack = ((TnkMenu)this.menu).getSlot(selected).getItem();
            g.drawString(this.font, this.fit(stack.getHoverName().getString(), 98), 196, 53, MarketTheme.INK, false);
            g.drawString(
               this.font,
               this.fit(entry.getString("chance") + "% · " + pretty(entry.getString("category")), 98),
               196,
               64,
               entry.getBoolean("enabled") ? MarketTheme.GOLD : -2263177,
               false
            );
         } else {
            g.drawString(this.font, "Select an item", 196, 56, MarketTheme.MUTED, false);
         }

         g.drawString(this.font, "Chance (%)", 196, 120, MarketTheme.MUTED, false);
         g.drawWordWrap(this.font, Component.literal(RaritySupport.clientHint("Edits affect this tier only.")), 196, 231, 100, MarketTheme.MUTED);
      } else {
         StationHeader.draw(g,this.font,Component.translatable("menu.tnkloot.identifier"),41,182);
         boolean results = !((TnkMenu)this.menu).busy()
            && ((TnkMenu)this.menu).slots.subList(0, 7).stream().anyMatch(s -> s.hasItem() && !(s.getItem().getItem() instanceof UnidentifiedWeaponItem));
         g.drawCenteredString(
            this.font,
            Component.translatable(((TnkMenu)this.menu).busy() ? "menu.tnkloot.rolling" : (results ? "menu.tnkloot.collect" : "menu.tnkloot.insert")),
            108,
            28,
            MarketTheme.INK
         );
         String total = ((TnkMenu)this.menu).inputCount() <= 0 && !((TnkMenu)this.menu).busy()
            ? "Up to seven items per visit"
            : Component.translatable(
                  "menu.tnkloot.total",
                  new Object[]{((TnkMenu)this.menu).quotedCost(), Component.translatable(((TnkMenu)this.menu).currency().getDescriptionId())}
               )
               .getString();
         g.drawCenteredString(
            this.font,
            this.fit(total, 232),
            this.imageWidth / 2,
            110,
            ((TnkMenu)this.menu).inputCount() != 0 && !((TnkMenu)this.menu).busy() && !((TnkMenu)this.menu).affordable() ? -1145210 : MarketTheme.GOLD
         );
         g.drawString(this.font, Component.translatable("container.inventory"), 25, 121, MarketTheme.MUTED, false);
         if (((TnkMenu)this.menu).currency() != Items.AIR) {
            g.renderItem(new ItemStack(((TnkMenu)this.menu).currency()), 216, 140);
         }

         g.drawCenteredString(this.font, "Cost", 223, 163, MarketTheme.MUTED);
         g.drawCenteredString(this.font, Integer.toString(((TnkMenu)this.menu).quotedCost()), 223, 177, MarketTheme.GOLD);
         g.drawCenteredString(this.font, ((TnkMenu)this.menu).busy() ? "Rolling" : "Confirm", 231, 87, MarketTheme.MUTED);
      }
   }

   private String fit(String value, int width) {
      return this.font.width(value) <= width ? value : this.font.plainSubstrByWidth(value, Math.max(0, width - this.font.width("…"))) + "…";
   }

   public boolean keyPressed(int key, int scanCode, int modifiers) {
      if (this.chance == null || !this.chance.visible || !this.chance.isFocused() || key == 256) {
         return super.keyPressed(key, scanCode, modifiers);
      } else if (key != 257 && key != 335) {
         return this.chance.keyPressed(key, scanCode, modifiers) || key != 256;
      } else {
         this.setChance();
         return true;
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      if (((TnkMenu)this.menu).admin() && ((TnkMenu)this.menu).adminView() == 2) {
         for (int i = 0; i < 36; i++) {
            Slot slot = ((TnkMenu)this.menu).getSlot(i);
            CompoundTag tag = TnkMenu.metadata(slot.getItem());
            if (!tag.isEmpty() && !tag.getBoolean("enabled")) {
               int x = this.leftPos + slot.x;
               int y = this.topPos + slot.y;
               g.fill(x, y, x + 16, y + 16, -2009591528);

               for (int n = 2; n < 14; n++) {
                  g.fill(x + n, y + 15 - n, x + n + 2, y + 17 - n, -1545104);
               }
            }
         }
      }

      if (!((TnkMenu)this.menu).busy()) {
         this.renderTooltip(g, mouseX, mouseY);
      }
   }

   private static String groupName(TnkConfig.Category c) {
      if (c == null) {
         return "Items";
      } else {
         return switch (c) {
            case WEAPON -> "Weapons";
            case BOW -> "Bows";
            case HELMET -> "Helmets";
            case CHESTPLATE -> "Chestplates";
            case LEGGINGS -> "Leggings";
            case BOOTS -> "Boots";
            case TOOL -> "Tools";
            case ACCESSORY -> "Accessories";
         };
      }
   }

   private static ItemStack groupIcon(TnkConfig.Category c) {
      return new ItemStack(switch (c) {
         case WEAPON -> Items.IRON_SWORD;
         case BOW -> Items.BOW;
         case HELMET -> Items.IRON_HELMET;
         case CHESTPLATE -> Items.IRON_CHESTPLATE;
         case LEGGINGS -> Items.IRON_LEGGINGS;
         case BOOTS -> Items.IRON_BOOTS;
         case TOOL -> Items.IRON_PICKAXE;
         case ACCESSORY -> Items.HEART_OF_THE_SEA;
      });
   }

   private static final class CounterButton extends Button {
      CounterButton(int x, int y, int w, int h, OnPress press) {
         super(x, y, w, h, Component.literal("Confirm"), press, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         TnkMenuScreen.panel(g, x, y, w, h, this.active ? (this.isHoveredOrFocused() ? -3428503 : -7179714) : -12304327);
         g.fill(x + 4, y + 4, x + w - 4, y + 5, this.active ? -1979766 : -10067886);
         int color = this.active ? -3380 : -8225424;

         for (int i = 0; i < 5; i++) {
            g.fill(x + 7 + i, y + 15 + i, x + 10 + i, y + 18 + i, color);
         }

         for (int i = 0; i < 11; i++) {
            g.fill(x + 12 + i, y + 19 - i, x + 15 + i, y + 22 - i, color);
         }
      }
   }

   private final class NavigationCard extends Button {
      private final ItemStack icon;
      private String detail;

      NavigationCard(int x, int y, int width, int height, OnPress press, ItemStack icon, String title, String detail) {
         super(x, y, width, height, Component.literal(title), press, DEFAULT_NARRATION);
         this.icon = icon;
         this.detail = detail;
      }

      protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
         int x = this.getX();
         int y = this.getY();
         int width = this.getWidth();
         TnkMenuScreen.panel(g, x, y, width, this.getHeight(), this.isHoveredOrFocused() ? 0xFF54768B : 0xFF446077);
         if (this.isHoveredOrFocused()) {
            g.renderOutline(x, y, width, this.getHeight(), MarketTheme.GOLD);
         }

         int textX = x + (this.icon.isEmpty() ? 6 : 32);
         int available = width - (textX - x) - 6;
         if (!this.icon.isEmpty()) {
            g.renderItem(this.icon, x + 9, y + 11);
         }

         g.drawString(TnkMenuScreen.this.font, TnkMenuScreen.this.fit(this.getMessage().getString(), available), textX, y + 8, MarketTheme.INK, false);
         g.drawString(TnkMenuScreen.this.font, TnkMenuScreen.this.fit(this.detail, available), textX, y + 22, MarketTheme.MUTED, false);
      }

      public void updateWidgetNarration(NarrationElementOutput output) {
         this.defaultButtonNarrationText(output);
         output.add(NarratedElementType.HINT, Component.literal(this.detail));
      }
   }
}
