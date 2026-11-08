package dev.mikaaah.tnkloot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Supplied CleanLook widget with a warm copper selected state. */
final class SalvageButton extends Button {
 private boolean selected;private final boolean icon;
 SalvageButton(int x,int y,int w,int h,String label,boolean selected,boolean icon,OnPress press){
  super(x,y,w,h,Component.literal(label),press,DEFAULT_NARRATION);this.selected=selected;this.icon=icon;
 }
 SalvageButton selected(boolean value){selected=value;return this;}
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
  if(icon){
   var texture=ResourceLocation.parse("tnkloot:textures/gui/salvage/"+(isHoveredOrFocused()&&active?"check_hover":"check")+".png");
   if(!active)g.setColor(.55f,.55f,.55f,1);
   g.pose().pushPose();g.pose().translate(getX(),getY(),0);g.pose().scale(width/16F,height/16F,1);
   try{g.blit(texture,0,0,0F,0F,16,16,16,16);}finally{g.pose().popPose();g.setColor(1,1,1,1);}
  }else{
   String state=selected?"panel_copper":!active?"button_disabled":isHoveredOrFocused()?"button_hover":"button";
   CleanLookDraw.panel(g,state,getX(),getY(),width,height);
   var f=Minecraft.getInstance().font;
   g.drawString(f,getMessage(),getX()+(width-f.width(getMessage()))/2,getY()+(height-f.lineHeight)/2,0xFFF4EBE5,false);
  }
 }
}
