package dev.mikaaah.tnkloot.boss;
import com.google.gson.*;
import dev.mikaaah.tnkloot.compat.BossRewards;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.menu.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;

/** Vanilla synchronized chest screens; every display stack is server-owned and never transferable. */
public final class BossEditor extends ChestMenu {
    private final ServerPlayer owner;
    private final SimpleContainer display;
    private final Map<Integer,Runnable> actions=new HashMap<>();
    private String view="home",group="",pool="",selected="";
    private int page;
    private int pages=1;
    BossEditor(int id,Inventory inv,ServerPlayer owner,SimpleContainer display){
        super(MenuType.GENERIC_9x6,id,inv,display,6);this.owner=owner;this.display=display;refresh();dev.mikaaah.tnkloot.dimension.NativeDisplays.decorate(this,owner,0,54);
    }
    public static void open(ServerPlayer player){
        if(!player.hasPermissions(2))return;
        player.openMenu(new dev.mikaaah.tnkloot.menu.StationMenuProvider((id,inv,p)->new BossEditor(id,inv,player,new SimpleContainer(54)),Component.literal("TNK Loot · Boss & Equipment Editor")));
    }
    private void navigate(String target){view=target;page=0;selected="";refresh();}
    private static ItemStack icon(Item item,String name,String... lore){
        ItemStack stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));
        stack.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(Component::literal).map(c->(Component)c).toList()));return stack;
    }
    private void put(int slot,ItemStack stack,Runnable action){display.setItem(slot,stack);if(action!=null)actions.put(slot,action);}
    private void button(int slot,Item item,String text,Runnable action,String... lore){put(slot,icon(item,text,lore),action);}
    private void listing(int size,java.util.function.IntConsumer show){
        pages=Math.max(1,(size+35)/36);page=Math.min(page,pages-1);
        for(int i=page*36;i<Math.min(size,(page+1)*36);i++)show.accept(i);
        button(45,Items.ARROW,"Previous page",()->{page=Math.max(0,page-1);refresh();});
        button(49,Items.PAPER,"Page "+(page+1)+" / "+pages,null);
        button(53,Items.ARROW,"Next page",()->{page=Math.min(pages-1,page+1);refresh();});
    }
    private void equipment(){
        owner.openMenu(new dev.mikaaah.tnkloot.menu.StationMenuProvider((id,inv,p)->new TnkMenu(TnkMenus.ADMIN.get(),id,inv,true),Component.literal("TNK Loot · Equipment Tiers")),buf->buf.writeUtf(TnkConfig.tierSnapshot()));
    }
    private static List<JsonObject> entries(JsonObject p){return p.getAsJsonArray("items").asList().stream().map(JsonElement::getAsJsonObject).toList();}
    private JsonObject entry(JsonObject p){return entries(p).stream().filter(e->e.get("item").getAsString().equals(selected)).findFirst().orElseThrow();}
    private void edit(java.util.function.Consumer<JsonObject> change){BossStore.edit(pool,change);refresh();}
    private void refresh(){
        display.clearContent();actions.clear();
        if(view.equals("home")){
            button(20,Items.DIAMOND_SWORD,"Equipment groups & tiers",this::equipment,"Weapons, armour, tools and accessories");
            button(24,Items.NETHER_STAR,"Boss groups",()->navigate("groups"),"One sealed reward per boss","Edit pools, add bosses and import equipment");return;
        }
        button(36,Items.OAK_DOOR,"Back",()->navigate(switch(view){case "groups"->"home";case "bosses","add"->"groups";case "entry"->"pool";default->"bosses";}));
        if(view.equals("groups")){
            var groups=BossRewards.all().stream().map(BossRewards.Pool::mod).distinct().sorted().toList();
            listing(groups.size(),i->button(i%36,Items.CHEST,groups.get(i),()->{group=groups.get(i);navigate("bosses");},BossRewards.all().stream().filter(p->p.mod().equals(groups.get(i))).count()+" bosses"));
            button(40,Items.SPAWNER,"+ Add boss manually",()->navigate("add"),"Choose a registered mob","Starts disabled with an empty pool");return;
        }
        if(view.equals("add")){
            var entities=BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(k->(BuiltInRegistries.ENTITY_TYPE.get(k).getCategory()!=MobCategory.MISC || SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(k))!=null)).sorted(Comparator.comparing(ResourceLocation::toString)).toList();
            listing(entities.size(),i->{var id=entities.get(i);var egg=SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(id));button(i%36,egg==null?Items.SPAWNER:egg,id.toString(),()->{pool=BossStore.add(id.toString());group=id.getNamespace();navigate("pool");},"Click to create this boss's reward pool");});return;
        }
        if(view.equals("bosses")){
            var bosses=BossRewards.all().stream().filter(p->p.mod().equals(group)).toList();
            listing(bosses.size(),i->{var p=bosses.get(i);button(i%36,Items.NETHER_STAR,p.name(),()->{pool=p.id();navigate("pool");},p.entity(),"Tier "+p.tier()+" · "+(p.enabled()?"Enabled":"Disabled"),p.items().size()+" enabled rewards");});
            button(40,Items.SPAWNER,"+ Add boss manually",()->navigate("add"));return;
        }
        JsonObject p=BossStore.pool(pool);
        if(view.equals("pool")){
            var list=entries(p);long sum=list.stream().filter(BossEditor::enabled).mapToLong(e->e.get("weight").getAsLong()).sum();
            listing(list.size(),i->{var e=list.get(i);var id=e.get("item").getAsString();var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
                button(i%36,item==Items.AIR?Items.BARRIER:item,id,()->{selected=id;view="entry";refresh();},enabled(e)?String.format(Locale.ROOT,"%.2f%%",100.0*e.get("weight").getAsLong()/Math.max(1,sum)):"Disabled","Category: "+e.get("category").getAsString(),"Click to edit");});
            button(37,Items.LIME_DYE,p.get("enabled").getAsBoolean()?"Enabled · click to disable":"Disabled · click to enable",()->edit(q->q.addProperty("enabled",!q.get("enabled").getAsBoolean())));
            button(38,Items.REDSTONE,"Tier -",()->edit(q->q.addProperty("tier",Math.max(0,q.get("tier").getAsInt()-1))));
            button(39,Items.DIAMOND,"Tier "+p.get("tier").getAsInt()+" +",()->edit(q->q.addProperty("tier",Math.min(TnkConfig.get().tiers().stream().mapToInt(TnkConfig.Tier::rank).max().orElse(7),q.get("tier").getAsInt()+1))));
            button(40,Items.EMERALD,"Equal chances",()->edit(q->entries(q).forEach(e->e.addProperty("weight",1))),"Reset enabled rewards to equal probability");
            button(41,Items.HOPPER,"Import equipment",null,"Shift-click an item from your inventory","Or click here while holding a stack","Copies the item type without consuming it");
            button(42,Items.REDSTONE,"Bonus chance -10%",()->chance(-.1));
            button(43,Items.EMERALD,"Bonus chance "+Math.round(p.get("chance").getAsDouble()*100)+"% +10%",()->chance(.1));
            button(44,Items.PAPER,"Native equipment replacement",null,"Matching equipment drops become ONE boss crate","Other materials remain; bonus chance applies","when no configured equipment was dropped");return;
        }
        if(view.equals("entry")){
            JsonObject e=entry(p);String id=e.get("item").getAsString();
            button(13,BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)),id,null,"Weight "+e.get("weight").getAsInt());
            button(19,Items.LEVER,enabled(e)?"Disable reward":"Enable reward",()->edit(q->entry(q).addProperty("enabled",!enabled(entry(q)))));
            button(21,Items.BARRIER,"Remove reward",()->{BossStore.edit(pool,q->q.getAsJsonArray("items").remove(entry(q)));navigate("pool");});
            button(23,Items.REDSTONE,"Weight -1",()->weight(-1));button(24,Items.EMERALD,"Weight +1",()->weight(1));button(25,Items.EMERALD_BLOCK,"Weight +10",()->weight(10));
            button(28,Items.PAPER,"Category: "+e.get("category").getAsString(),()->edit(q->{var w=entry(q);var cat=TnkConfig.Category.parse(w.get("category").getAsString());w.addProperty("category",TnkConfig.Category.values()[(cat.ordinal()+1)%TnkConfig.Category.values().length].name().toLowerCase(Locale.ROOT));}),"Click to cycle equipment category");
            for(int i=0;i<5;i++){final int percent=new int[]{5,10,25,50,100}[i];button(30+i,Items.AMETHYST_SHARD,"Set "+percent+"%",()->edit(q->setPercent(q,entry(q),percent)),"Other enabled chances adjust proportionally");}
        }
    }
    private static boolean enabled(JsonObject e){return !e.has("enabled")||e.get("enabled").getAsBoolean();}
    private void weight(int amount){edit(q->{var e=entry(q);e.addProperty("weight",Math.max(1,Math.min(1000000,e.get("weight").getAsInt()+amount)));});}
    private void chance(double amount){edit(q->q.addProperty("chance",Math.max(0,Math.min(1,Math.round((q.get("chance").getAsDouble()+amount)*10)/10.0))));}
    public static void setPercent(JsonObject p,JsonObject target,int percent){
        var others=entries(p).stream().filter(e->e!=target&&enabled(e)).toList();
        target.addProperty("enabled",true);
        if(percent==100||others.isEmpty()){target.addProperty("weight",10000);others.forEach(e->e.addProperty("enabled",false));return;}
        long total=others.stream().mapToLong(e->e.get("weight").getAsLong()).sum();int remaining=10000-percent*100;
        int[] weights=new int[others.size()];double[] remainders=new double[others.size()];int assigned=0;
        for(int i=0;i<weights.length;i++){double exact=remaining*others.get(i).get("weight").getAsDouble()/total;weights[i]=(int)Math.floor(exact);remainders[i]=exact-weights[i];assigned+=weights[i];}
        while(assigned<remaining){int best=0;for(int i=1;i<weights.length;i++)if(remainders[i]>remainders[best])best=i;weights[best]++;remainders[best]=-1;assigned++;}
        for(int i=0;i<weights.length;i++){others.get(i).addProperty("weight",Math.max(1,weights[i]));others.get(i).addProperty("enabled",weights[i]>0);}
        target.addProperty("weight",percent*100);
    }
    @Override public void broadcastChanges(){int tier=pool.isBlank()?0:BossStore.pool(pool).get("tier").getAsInt();dev.mikaaah.tnkloot.dimension.NativeDisplays.decorate(this,owner,tier,54);super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&p.hasPermissions(2);}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!stillValid(player))return;
        try{
            if(slot>=54&&slot<this.slots.size()&&type==ClickType.QUICK_MOVE&&view.equals("pool")) {BossStore.importItem(pool,this.slots.get(slot).getItem());refresh();return;}
            if(slot==41&&view.equals("pool")&&!getCarried().isEmpty()){BossStore.importItem(pool,getCarried());refresh();return;}
            if(slot>=0&&slot<54){if(type==ClickType.PICKUP&&actions.containsKey(slot))actions.get(slot).run();return;}
            if(slot<0||type==ClickType.PICKUP_ALL||type==ClickType.QUICK_CRAFT)return;
            super.clicked(slot,button,type,player);
        }catch(RuntimeException ex){owner.sendSystemMessage(Component.literal("Boss editor: "+ex.getMessage()));}
    }
}
