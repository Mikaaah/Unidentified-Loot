package dev.mikaaah.tnkloot.features;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.loot.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

/** Synchronized vanilla chest menus. Display items never enter a player's inventory. */
public final class FeatureMenus extends ChestMenu {
 public record Option(String id,int weight,String source){}
 private final ServerPlayer owner;private final SimpleContainer display;
 private final Map<Integer,Runnable> actions=new HashMap<>();
 private final Map<Integer,ItemStack> staged=new LinkedHashMap<>();
 private String view="hub";private int page,filter;private ItemStack preview=ItemStack.EMPTY;
 FeatureMenus(int id,Inventory inv,ServerPlayer owner,SimpleContainer display,String view){super(MenuType.GENERIC_9x6,id,inv,display,6);this.owner=owner;this.display=display;this.view=view;refresh();dev.mikaaah.tnkloot.dimension.NativeDisplays.decorate(this,owner,0,54);}
 public static void open(ServerPlayer p,String view){if(view.equals("collection")||view.equals("picker")){dev.mikaaah.tnkloot.workshop.CatalogMenu.open(p,view.equals("collection"),null);return;}if(view.equals("salvage")){dev.mikaaah.tnkloot.workshop.SalvageMenu.open(p,null);return;}p.openMenu(new dev.mikaaah.tnkloot.menu.StationMenuProvider((id,inv,who)->new FeatureMenus(id,inv,p,new SimpleContainer(54),view),Component.literal("TNK Loot · Loot Journal")));}
 public static void commands(RegisterCommandsEvent e){
  var root=Commands.literal("tnkloot");
  for(String view:List.of("journal","collection","salvage","preview"))root.then(Commands.literal(view).executes(c->{if(view.equals("journal"))dev.mikaaah.tnkloot.workshop.LootJournal.open(c.getSource().getPlayerOrException());else open(c.getSource().getPlayerOrException(),view.equals("preview")?"picker":view);return 1;}));
  root.then(Commands.literal("features").then(Commands.literal("reload").requires(s->s.hasPermission(2)).executes(c->{boolean ok=FeatureConfig.reload()&&dev.mikaaah.tnkloot.workshop.ExperienceCosts.reload();c.getSource().sendSuccess(()->Component.literal(ok?"TNK Loot features reloaded":"Invalid features.json; last working settings retained"),false);return ok?1:0;})));
  e.getDispatcher().register(root);
 }
 static ItemStack icon(Item item,String name,String... lore){var s=new ItemStack(item);s.set(DataComponents.CUSTOM_NAME,Component.literal(name));s.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(t->(Component)Component.literal(t)).toList()));return s;}
 private void button(int slot,Item item,String name,Runnable action,String... lore){display.setItem(slot,icon(item,name,lore));if(action!=null)actions.put(slot,action);}
 private void go(String target){view=target;page=0;filter=0;staged.clear();refresh();}
 private void listing(int size,java.util.function.IntConsumer render){int pages=Math.max(1,(size+35)/36);page=Math.min(page,pages-1);for(int i=page*36;i<Math.min(size,(page+1)*36);i++)render.accept(i);
  button(45,Items.ARROW,"Previous",()->{page=Math.max(0,page-1);refresh();});button(49,Items.PAPER,"Page "+(page+1)+" / "+pages,null);button(53,Items.ARROW,"Next",()->{page=Math.min(pages-1,page+1);refresh();});}
 public static List<Option> options(ItemStack token){return dev.mikaaah.tnkloot.dimension.DimensionLoot.options(token);}
 private static boolean available(String id){var key=ResourceLocation.tryParse(id);return key!=null&&BuiltInRegistries.ITEM.containsKey(key)&&BuiltInRegistries.ITEM.get(key)!=Items.AIR;}
 public static Map<String,String> catalogue(ServerPlayer p){
  var entries=new TreeMap<String,String>();
  for(var w:TnkConfig.get().weapons())if(WeaponSelector.available(w)&&!BossRewards.exclusive(w.id()))entries.merge(w.id(),"Levels "+dev.mikaaah.tnkloot.compat.EquipmentGroups.levels(w.minTier(),w.maxTier())+" · Tiers "+w.minTier()+"–"+w.maxTier(),(a,b)->a.equals(b)?a:a+", "+b);
  for(var pool:BossRewards.active())for(var w:pool.items())if(available(w.item()))entries.merge(w.item(),"Boss: "+pool.name(),(a,b)->a+" · "+b);
  for(String id:PlayerLedger.read(p).getCompound("discoveries").getAllKeys())if(available(id))entries.putIfAbsent(id,"Previously discovered");return entries;
 }
 private void refresh(){
  display.clearContent();actions.clear();button(36,Items.OAK_DOOR,"Back to journal",()->{owner.closeContainer();dev.mikaaah.tnkloot.workshop.LootJournal.open(owner);});
  if(view.equals("hub")){
   button(11,Items.BOOK,"Discovery collection",()->go("collection"),"Track identified equipment and boss trophies");button(15,Items.ENDER_EYE,"Reward pool preview",()->go("picker"),"Choose an unidentified item from your inventory");
   button(29,Items.ANVIL,"Salvage equipment",()->dev.mikaaah.tnkloot.workshop.SalvageMenu.open(owner,null),"Destroy unwanted identified gear for salvage credit","Confirm before any items are consumed");
   button(33,Items.AMETHYST_SHARD,"Salvage fragments: "+PlayerLedger.credit(owner),null,"Stored per player and kept through death","1 credit pays 1 unit of identification currency","Applied automatically at the Item Identifier");return;
  }
  if(view.equals("collection")){
   if(!FeatureConfig.get().collection()){disabled("Collection disabled");return;}
   var found=PlayerLedger.read(owner).getCompound("discoveries");var all=catalogue(owner);var entries=all.entrySet().stream().filter(e->filter==0||filter==1&&found.contains(e.getKey())||filter==2&&!found.contains(e.getKey())||filter==3&&e.getValue().contains("Boss:")).toList();
   listing(entries.size(),i->{var e=entries.get(i);boolean known=found.contains(e.getKey());var info=found.getCompound(e.getKey());
    button(i%36,known?BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getKey())):Items.BLACK_STAINED_GLASS_PANE,known?BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getKey())).getDescription().getString():"Undiscovered equipment",null,e.getValue(),known?"Identified "+info.getInt("count")+" times":"Identify this equipment to discover it",known?"Best tier "+info.getInt("best_tier")+" · "+List.of("Common","Uncommon","Rare","Epic","Legendary","Mythic").get(Math.min(5,info.getInt("rarity"))):"???");});
   button(40,Items.COMPASS,"Filter: "+List.of("All","Discovered","Undiscovered","Boss rewards").get(filter),()->{filter=(filter+1)%4;page=0;refresh();});button(44,Items.BOOK,all.keySet().stream().filter(found::contains).count()+" / "+all.size()+" discovered",null);return;
  }
  if(view.equals("picker")){
   if(!FeatureConfig.get().previews()){disabled("Previews disabled");return;}
   var slots=new ArrayList<Integer>();for(int i=0;i<owner.getInventory().getContainerSize();i++)if(owner.getInventory().getItem(i).getItem() instanceof UnidentifiedWeaponItem)slots.add(i);
   listing(slots.size(),i->{var token=owner.getInventory().getItem(slots.get(i));var shown=token.copy();display.setItem(i%36,shown);actions.put(i%36,()->{preview=token.copy();dev.mikaaah.tnkloot.dimension.DimensionLoot.bindOrigin(preview,owner.level());view="preview";page=0;refresh();});});if(slots.isEmpty())button(13,Items.BARRIER,"No unidentified items",null,"Carry an unidentified item, then reopen this screen");return;
  }
  if(view.equals("preview")){
   if(!FeatureConfig.get().previews()){disabled("Previews disabled");return;}
   var list=options(preview);long total=list.stream().mapToLong(Option::weight).sum();var tag=UnidentifiedWeaponItem.data(preview);
   listing(list.size(),i->{var o=list.get(i);Item item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(o.id()));button(i%36,item,item.getDescription().getString(),null,String.format(Locale.ROOT,"Chance: %.3f%%",100.0*o.weight()/Math.max(1,total)),o.source(),"Rarity stays fixed; affixes roll on identification");});
   button(40,Items.ENDER_EYE,preview.getHoverName().getString(),null,"Tier "+UnidentifiedWeaponItem.tier(preview),tag.contains("tnk_rarity")?"Rarity: "+tag.getString("tnk_rarity_name"):"Creative template · rarity not rolled",list.size()+" possible equipment results");return;
  }
  if(view.equals("salvage")){
   if(!FeatureConfig.get().salvage()){disabled("Salvaging disabled");return;}
   int index=0;for(var e:staged.entrySet()){final int inventorySlot=e.getKey();var s=e.getValue().copy();s.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Salvage: "+PlayerLedger.salvageValue(s)+" fragments"),Component.literal("Click to remove from selection"))));display.setItem(index,s);actions.put(index,()->{staged.remove(inventorySlot);refresh();});index++;}
   int total=staged.values().stream().mapToInt(PlayerLedger::salvageValue).sum();
   button(22,Items.HOPPER,"Select up to seven items",null,"Click or shift-click identified gear in your inventory","Items stay in your inventory until you confirm","Unidentified items cannot be salvaged");
   button(40,Items.LAVA_BUCKET,"CONFIRM · destroy "+staged.size()+" items for "+total+" fragments",this::salvage,"This consumes the selected equipment permanently");
   button(44,Items.AMETHYST_SHARD,"Balance: "+PlayerLedger.credit(owner),null);button(48,Items.BARRIER,"Clear selection",()->{staged.clear();refresh();});
  }
 }
 private void disabled(String text){button(13,Items.BARRIER,text,null,"Controlled by config/tnkloot/features.json");}
 void select(int inventorySlot){var stack=owner.getInventory().getItem(inventorySlot);if(PlayerLedger.salvageValue(stack)<=0)return;if(staged.containsKey(inventorySlot)){staged.remove(inventorySlot);}else if(staged.size()<7)staged.put(inventorySlot,stack.copyWithCount(1));refresh();}
 int stagedCount(){return staged.size();}
 void salvage(){
  if(!FeatureConfig.get().salvage()||staged.isEmpty())return;
  for(var e:staged.entrySet()){var actual=owner.getInventory().getItem(e.getKey());if(actual.isEmpty()||!ItemStack.isSameItemSameComponents(actual,e.getValue())||PlayerLedger.salvageValue(actual)<=0){staged.clear();refresh();throw new IllegalArgumentException("Selected equipment changed; select it again");}}
  int total=staged.values().stream().mapToInt(PlayerLedger::salvageValue).sum();PlayerLedger.addCredit(owner,total);
  for(int slot:staged.keySet())owner.getInventory().getItem(slot).shrink(1);
  staged.clear();owner.getInventory().setChanged();owner.sendSystemMessage(Component.literal("Salvaged equipment for "+total+" fragments · balance "+PlayerLedger.credit(owner)));refresh();
 }
 @Override public void broadcastChanges(){dev.mikaaah.tnkloot.dimension.NativeDisplays.decorate(this,owner,preview.isEmpty()?0:UnidentifiedWeaponItem.tier(preview),54);super.broadcastChanges();}
 @Override public boolean stillValid(Player p){return p==owner&&p.isAlive();}
 @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
 @Override public void clicked(int slot,int button,ClickType type,Player p){
  if(!stillValid(p))return;try{
   if(type!=ClickType.PICKUP&&type!=ClickType.QUICK_MOVE)return;
   if(slot>=54&&slot<this.slots.size()&&view.equals("salvage")){select(this.slots.get(slot).getContainerSlot());return;}
   if(slot>=0&&slot<54&&type==ClickType.PICKUP){var action=actions.get(slot);if(action!=null)action.run();}
  }catch(RuntimeException ex){owner.sendSystemMessage(Component.literal("TNK Loot: "+ex.getMessage()));}
 }
}
