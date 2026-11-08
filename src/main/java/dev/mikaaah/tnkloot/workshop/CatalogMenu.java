package dev.mikaaah.tnkloot.workshop;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.dimension.*;
import dev.mikaaah.tnkloot.features.*;
import dev.mikaaah.tnkloot.loot.*;
import dev.mikaaah.tnkloot.menu.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import java.util.*;

/** Read-only synchronized catalogue. Display stacks never become inventory items. */
public final class CatalogMenu extends AbstractContainerMenu {
 private final Player owner;private final SimpleContainer display=new SimpleContainer(37);
 private final boolean book;private BlockPos station;private final net.minecraft.world.phys.Vec3 opening;
 private int page,filter,category=-1;private ItemStack token=ItemStack.EMPTY;
 private List<ItemStack> choices=List.of();
 public CatalogMenu(int id,Inventory inv,boolean book){super(book?TnkMenus.COLLECTION.get():TnkMenus.PREVIEW.get(),id);this.book=book;owner=inv.player;opening=owner.position();
  for(int i=0;i<37;i++){int x=i==36?-10000:book?174+(i%6)*20:32+(i%9)*29;int y=i==36?-10000:book?47+(i/6)*23:78+(i/9)*27;
   addSlot(new Slot(display,i,x,y){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
  }refresh();
 }
 public static void open(ServerPlayer p,boolean book,BlockPos station){p.openMenu(new dev.mikaaah.tnkloot.menu.StationMenuProvider((id,inv,who)->{var m=new CatalogMenu(id,inv,book);m.station=station==null?null:station.immutable();return m;},Component.literal(book?"Loot Collection":"Identifier · Reward Preview")));}
 public static CompoundTag header(CatalogMenu menu){return UnidentifiedWeaponItem.data(menu.getSlot(36).getItem());}
 private static ItemStack icon(Item item,String name,String... lore){var s=new ItemStack(item);s.set(DataComponents.CUSTOM_NAME,Component.literal(name));s.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(t->(Component)Component.literal(t)).toList()));return s;}
 private void refresh(){if(!(owner instanceof ServerPlayer p))return;display.clearContent();var h=new CompoundTag();h.putBoolean("book",book);h.putBoolean("picker",token.isEmpty());h.putInt("filter",filter);h.putInt("category",category);int size=0;
  if(book){
   var found=PlayerLedger.read(p).getCompound("discoveries");var all=FeatureMenus.catalogue(p);h.putInt("known",(int)all.keySet().stream().filter(found::contains).count());h.putInt("total",all.size());
   var entries=FeatureConfig.get().collection()?all.entrySet().stream().filter(e->{boolean known=found.contains(e.getKey());var item=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(e.getKey()));var c=dev.mikaaah.tnkloot.injection.FinalLoot.category(new ItemStack(item));return (category<0||c==TnkConfig.Category.values()[category])&&(filter==0||filter==1&&known||filter==2&&!known||filter==3&&e.getValue().contains("Boss:"));}).toList():List.<Map.Entry<String,String>>of();
   size=entries.size();page=Math.clamp(page,0,Math.max(0,(size-1)/36));
   for(int i=page*36;i<Math.min(size,(page+1)*36);i++){var e=entries.get(i);boolean known=found.contains(e.getKey());var info=found.getCompound(e.getKey());Item item=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(e.getKey()));
    var s=icon(known?item:Items.GRAY_STAINED_GLASS_PANE,known?item.getDescription().getString():"Undiscovered equipment",e.getValue(),known?(info.getInt("count")==0?"Discovered from acquired equipment":"Identified "+info.getInt("count")+" times"):"Identify this equipment to discover it",known?"Best tier "+info.getInt("best_tier"):"???");var tag=UnidentifiedWeaponItem.data(s);tag.putBoolean("catalog_known",known);tag.putInt("catalog_color",known?new int[]{0xDDD6BE,0x69B54C,0xE981BA,0xAA72CE,0x69C9DC,0xEFA24A}[Math.clamp(info.getInt("rarity"),0,5)]:0xAAA18B);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));display.setItem(i%36,NativeDisplays.prepare(s,p,info.getInt("best_tier"),i));
   }
  }else if(token.isEmpty()){
   var list=new ArrayList<ItemStack>();if(FeatureConfig.get().previews())for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.getItem() instanceof UnidentifiedWeaponItem)list.add(s.copy());}choices=List.copyOf(list);size=list.size();page=Math.clamp(page,0,Math.max(0,(size-1)/36));for(int i=page*36;i<Math.min(size,(page+1)*36);i++)display.setItem(i%36,list.get(i));
  }else{
   var entries=FeatureConfig.get().previews()?FeatureMenus.options(token):List.<FeatureMenus.Option>of();size=entries.size();long total=entries.stream().mapToLong(FeatureMenus.Option::weight).sum();page=Math.clamp(page,0,Math.max(0,(size-1)/36));
   for(int i=page*36;i<Math.min(size,(page+1)*36);i++){var e=entries.get(i);var item=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(e.id()));var s=icon(item,item.getDescription().getString(),String.format(Locale.ROOT,"Equipment chance: %.3f%%",100.0*e.weight()/Math.max(1,total)),e.source(),"Rarity is fixed · affixes roll on identification");RaritySupportBridge.copy(s,token);display.setItem(i%36,NativeDisplays.prepare(s,p,UnidentifiedWeaponItem.tier(token),i));}
   h.putString("selection",token.getHoverName().getString());h.putInt("tier",UnidentifiedWeaponItem.tier(token));h.putString("origin",DimensionLoot.origin(token));
  }
  h.putBoolean("disabled",book?!FeatureConfig.get().collection():!FeatureConfig.get().previews());h.putInt("size",size);h.putInt("page",page);h.putInt("pages",Math.max(1,(size+35)/36));var header=new ItemStack(Items.PAPER);header.set(DataComponents.CUSTOM_DATA,CustomData.of(h));display.setItem(36,header);
 }
 private static final class RaritySupportBridge {static void copy(ItemStack shown,ItemStack source){dev.mikaaah.tnkloot.rarity.RaritySupport.base(shown,source);}}
 @Override public boolean clickMenuButton(Player who,int id){if(!(who instanceof ServerPlayer p)||!stillValid(p))return false;
  if(id==90){TnkMenuProvider.openIdentifier(p,station);return true;}if(id==91){p.closeContainer();LootJournal.open(p);return true;}if(id==94){SalvageMenu.open(p,station);return true;}
  if(id==0)page=Math.max(0,page-1);else if(id==1)page++;else if(id==2){token=ItemStack.EMPTY;page=0;}else if(id==3&&book){filter=(filter+1)%4;page=0;}else if(id>=100&&id<100+TnkConfig.Category.values().length&&book){category=category==id-100?-1:id-100;page=0;}else if(id==99&&book){category=-1;page=0;}else return false;refresh();return true;
 }
 @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p)||book||!token.isEmpty()||slot<0||slot>=36||type!=ClickType.PICKUP)return;int i=page*36+slot;if(i>=choices.size())return;token=choices.get(i).copy();DimensionLoot.bindOrigin(token,p.level());page=0;refresh();}
 @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&(book||p.distanceToSqr(opening)<=64&&(station==null||p.level().getBlockState(station).getBlock() instanceof IdentifierBlock));}
 @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
