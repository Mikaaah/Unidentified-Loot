package dev.mikaaah.tnkloot.client;
import dev.mikaaah.tnkloot.workshop.CatalogMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class PreviewScreen extends AbstractContainerScreen<CatalogMenu> {
 private Button back,previous,next;
 public PreviewScreen(CatalogMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=312;imageHeight=222;inventoryLabelY=titleLabelY=10000;}
 private Button button(int x,int y,int w,int h,String label,int id,String help){
  Button widget=y==StationHeader.NAV_Y&&h==StationHeader.NAV_HEIGHT
    ?new SalvageButton(leftPos+x,topPos+y,w,h,label,label.equals("Preview"),false,b->send(id))
    :MarketTheme.button(leftPos+x,topPos+y,w,h,label,MarketTheme.PURPLE,b->send(id),help);
  widget.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(help)));
  return addRenderableWidget(widget);
 }
 private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
 @Override protected void init(){super.init();topPos=StationHeader.top(height);button(16,StationHeader.NAV_Y,66,StationHeader.NAV_HEIGHT,"Identify",90,"Return to identification");button(85,StationHeader.NAV_Y,66,StationHeader.NAV_HEIGHT,"Salvage",94,"Open the salvage anvil");button(154,StationHeader.NAV_Y,66,StationHeader.NAV_HEIGHT,"Preview",2,"Choose a different unidentified item");button(223,StationHeader.NAV_Y,73,StationHeader.NAV_HEIGHT,"Journal",91,"Open the Loot Journal and collection");back=button(22,197,86,17,"Choose item",2,"Choose another drop from your inventory");previous=button(177,197,27,17,"‹",0,"Previous page");next=button(264,197,27,17,"›",1,"Next page");update();}
 private void update(){if(back==null)return;var h=CatalogMenu.header(menu);back.active=!h.getBoolean("picker");previous.active=h.getInt("page")>0;next.active=h.getInt("page")+1<h.getInt("pages");}
 @Override protected void containerTick(){super.containerTick();update();}
 @Override protected void renderBg(GuiGraphics g,float t,int mx,int my){MarketTheme.preview(g,leftPos,topPos);for(int i=0;i<36;i++){var s=menu.getSlot(i).getItem();int color=dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem.data(s).getInt("tnk_rarity_color");if(!s.isEmpty()&&color!=0){var slot=menu.getSlot(i);g.fill(leftPos+slot.x+1,topPos+slot.y+1,leftPos+slot.x+15,topPos+slot.y+15,0x40000000|(color&0xFFFFFF));}}}
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){var h=CatalogMenu.header(menu);StationHeader.draw(g,font,"Reward Preview",49,214);String caption=h.getBoolean("disabled")?"Reward previews are disabled":h.getBoolean("picker")?"Choose an unidentified item from your inventory":h.getString("selection");MarketTheme.center(g,font,MarketTheme.fit(font,caption,276),156,49,MarketTheme.LIGHT);if(h.getInt("size")==0)MarketTheme.center(g,font,h.getBoolean("picker")?"Carry an unidentified drop to inspect it":"No eligible rewards in this pool",156,122,MarketTheme.SOFT);MarketTheme.center(g,font,(h.getInt("page")+1)+" / "+Math.max(1,h.getInt("pages")),234,202,MarketTheme.LIGHT);}
 @Override public void render(GuiGraphics g,int mx,int my,float t){super.render(g,mx,my,t);renderTooltip(g,mx,my);}
}
