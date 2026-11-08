package dev.mikaaah.tnkloot.client;
import dev.mikaaah.tnkloot.workshop.CatalogMenu;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
public final class CollectionScreen extends AbstractContainerScreen<CatalogMenu> {
 private Button filter,previous,next;private final java.util.List<Button> categories=new java.util.ArrayList<>();
 public CollectionScreen(CatalogMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=312;imageHeight=222;inventoryLabelY=titleLabelY=10000;}
 private Button button(int x,int y,int w,int h,String label,int id,String help){return addRenderableWidget(MarketTheme.button(leftPos+x,topPos+y,w,h,label,MarketTheme.RED,b->send(id),help));}
 private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
 @Override protected void init(){super.init();topPos=StationHeader.top(height);categories.clear();for(int i=0;i<TnkConfig.Category.values().length;i++){var c=TnkConfig.Category.values()[i];categories.add(button(23+i%3*38,62+i/3*25,31,22,"",100+i,c.name().toLowerCase(java.util.Locale.ROOT)));}button(99,112,31,22,"All",99,"All equipment groups");filter=button(23,145,107,19,"All",3,"Cycle All, Found, Unknown and Boss equipment");button(23,195,107,17,"‹ Journal",91,"Return to the Patchouli guide");previous=button(173,192,27,19,"‹",0,"Previous page");next=button(269,192,27,19,"›",1,"Next page");update();}
 private void update(){if(filter==null)return;var h=CatalogMenu.header(menu);filter.setMessage(Component.literal(new String[]{"All equipment","Discovered","Undiscovered","Boss rewards"}[Math.clamp(h.getInt("filter"),0,3)]));for(int i=0;i<categories.size();i++)categories.get(i).active=h.getInt("category")!=i;previous.active=h.getInt("page")>0;next.active=h.getInt("page")+1<h.getInt("pages");}
 @Override protected void containerTick(){super.containerTick();update();}
 @Override protected void renderBg(GuiGraphics g,float t,int mx,int my){MarketTheme.book(g,leftPos,topPos);for(int i=0;i<36;i++){var s=menu.getSlot(i).getItem();if(!s.isEmpty()){int c=UnidentifiedWeaponItem.data(s).getInt("catalog_color");var slot=menu.getSlot(i);g.fill(leftPos+slot.x+1,topPos+slot.y+1,leftPos+slot.x+15,topPos+slot.y+15,0x55000000|(c&0xFFFFFF));}}}
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){var h=CatalogMenu.header(menu);StationHeader.draw(g,font,"Collection",17,127);MarketTheme.center(g,font,"Equipment groups",80,49,MarketTheme.MUTED);for(int i=0;i<TnkConfig.Category.values().length;i++)g.renderItem(new ItemStack(icon(TnkConfig.Category.values()[i])),31+i%3*38,65+i/3*25);MarketTheme.center(g,font,"Discovered",80,166,MarketTheme.MUTED);MarketTheme.center(g,font,h.getInt("known")+" / "+h.getInt("total"),80,178,MarketTheme.INK);MarketTheme.center(g,font,h.getBoolean("disabled")?"Collection disabled":"Available equipment",233,25,MarketTheme.INK);MarketTheme.center(g,font,(h.getInt("page")+1)+" / "+Math.max(1,h.getInt("pages")),234,198,MarketTheme.INK);}
 private static Item icon(TnkConfig.Category c){return switch(c){case WEAPON->Items.IRON_SWORD;case BOW->Items.BOW;case HELMET->Items.IRON_HELMET;case CHESTPLATE->Items.IRON_CHESTPLATE;case LEGGINGS->Items.IRON_LEGGINGS;case BOOTS->Items.IRON_BOOTS;case TOOL->Items.IRON_PICKAXE;case ACCESSORY->Items.AMETHYST_SHARD;};}
 @Override public void render(GuiGraphics g,int mx,int my,float t){super.render(g,mx,my,t);renderTooltip(g,mx,my);}
}
