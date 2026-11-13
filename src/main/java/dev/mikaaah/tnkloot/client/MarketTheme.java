package dev.mikaaah.tnkloot.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
public final class MarketTheme {
 public static final int WIDTH=312,HEIGHT=222,INK=0xFFF4EBE5,MUTED=0xFFA9C3C8,CREAM=0xFFF4EBE5,GOLD=0xFFC09473,PURPLE=0xFF729AA1,RED=0xFFCF8D7C,GREEN=0xFFCCE3AD;
 @FunctionalInterface public interface Paint{void fill(int x1,int y1,int x2,int y2,int color);}
 private static void rect(Paint p,int x,int y,int w,int h,int c){p.fill(x,y,x+w,y+h,c);}
 public static final int LIGHT=0xFFF4EBE5,SOFT=0xFFA9C3C8,BROWN=0xFF446077;
 public static void center(GuiGraphics g,Font font,String text,int x,int y,int color){g.drawString(font,text,x-font.width(text)/2,y,color,false);}
 private static int shade(int color,int delta){int out=color&0xFF000000;for(int shift=0;shift<=16;shift+=8)out|=Math.clamp(((color>>shift)&255)+delta,0,255)<<shift;return out;}
 public static void panel(Paint p,int x,int y,int w,int h,int inside){
  rect(p,x,y,w,h,0xFF20293F);rect(p,x+1,y+1,w-2,h-2,0xFF729AA1);
  for(int row=2;row<h-2;row++)rect(p,x+2,y+row,w-4,1,shade(inside,5-10*row/Math.max(1,h-1)));
  rect(p,x+2,y+1,w-4,1,0xFFA9C3C8);rect(p,x+1,y+2,1,h-4,0xFF86A6AE);
  rect(p,x+2,y+h-3,w-4,1,shade(inside,-24));rect(p,x+w-3,y+3,1,h-5,shade(inside,-18));
 }
 public static void panel(GuiGraphics g,int x,int y,int w,int h,int colour){CleanLookDraw.panel(g,colour==0xFF905C49||colour==0xFF7A4841?"panel_copper":"button",x,y,w,h);}
 public static void nativeSlot(GuiGraphics g,int x,int y){g.blit(net.minecraft.resources.ResourceLocation.parse("tnkloot:textures/gui/cleanlook/slot.png"),x-1,y-1,0f,0f,18,18,18,18);}
 public static void slot(Paint p,int x,int y,int tint){
  if(tint!=0xFFB88B68)rect(p,x-1,y-1,18,18,tint==0xFF4D2B32?0xFFA77257:0xFF729AA1);
  rect(p,x,y,16,16,shade(tint,-14));
  for(int row=1;row<15;row++)rect(p,x+1,y+row,14,1,shade(tint,2-row/4));
  rect(p,x+1,y+15,14,1,shade(tint,10));
 }
 private static void texture(GuiGraphics g,String name,int x,int y,int w,int h){g.blit(net.minecraft.resources.ResourceLocation.parse("tnkloot:textures/gui/"+name+".png"),x-8,y-16,0f,0f,w+16,h+28,w+16,h+28);}
 public static void counter(GuiGraphics g,int x,int y,boolean salvage){texture(g,salvage?"salvage_counter":"identifier_counter",x,y,264,222);}
 public static void preview(GuiGraphics g,int x,int y){texture(g,"reward_preview",x,y,312,222);}
 public static void book(GuiGraphics g,int x,int y){texture(g,"collection",x,y,312,222);}
 public static void vault(GuiGraphics g,int x,int y,boolean items){texture(g,items?"tier_vault_items":"tier_vault",x,y,304,250);}
 public static void heading(GuiGraphics g,Font font,String text,int center,int y,int color){var label=Component.literal(text).withStyle(style->style.withFont(net.minecraft.resources.ResourceLocation.parse("tnkloot:menu")));g.drawString(font,label,center-font.width(label)/2+1,y,color,false);}
 public static String fit(Font font,String s,int width){return font.width(s)<=width?s:font.plainSubstrByWidth(s,Math.max(0,width-font.width("…")))+"…";}
 public static Button button(int x,int y,int w,int h,String label,int accent,Button.OnPress press,String tooltip){var b=new MarketButton(x,y,w,h,label,accent,press);if(tooltip!=null)b.setTooltip(Tooltip.create(Component.literal(tooltip)));return b;}
 private static final class MarketButton extends Button {
  private final int accent;
  MarketButton(int x,int y,int w,int h,String label,int accent,OnPress press){super(x,y,w,h,Component.literal(label),press,DEFAULT_NARRATION);this.accent=accent;}
  @Override protected void renderWidget(GuiGraphics g,int mx,int my,float tick){
   int x=getX(),y=getY();CleanLookDraw.panel(g,!active?"button_disabled":isHoveredOrFocused()?"button_hover":"button",x,y,width,height);
   var font=net.minecraft.client.Minecraft.getInstance().font;
   center(g,font,fit(font,getMessage().getString(),width-6),x+width/2,y+(height-8)/2,active?LIGHT:0xFF91ABB6);
  }
 }
}
