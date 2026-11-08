package dev.mikaaah.tnkloot.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Stretch the nine center regions once; 1.21.1's atlas nine-slice tiles them. */
public final class CleanLookDraw {
 public static void panel(GuiGraphics g,String name,int x,int y,int w,int h){
  boolean copper=name.equals("panel_copper");int tw=copper?48:200,th=copper?32:20,b=copper?2:3;
  var texture=ResourceLocation.parse("tnkloot:textures/gui/sprites/cleanlook/"+name+".png");
  int[] sx={0,b,tw-b},sy={0,b,th-b},sw={b,tw-2*b,b},sh={b,th-2*b,b};
  int[] dx={0,b,w-b},dy={0,b,h-b},dw={b,w-2*b,b},dh={b,h-2*b,b};
  for(int row=0;row<3;row++)for(int col=0;col<3;col++)
   g.blit(texture,x+dx[col],y+dy[row],dw[col],dh[row],(float)sx[col],(float)sy[row],sw[col],sh[row],tw,th);
 }
 private CleanLookDraw(){}
}
