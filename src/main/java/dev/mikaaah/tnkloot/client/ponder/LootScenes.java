package dev.mikaaah.tnkloot.client.ponder;

import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.loot.IdentifierBlock;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.rarity.RaritySupport;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.machine.MechanicalIdentifierEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Compact, separate demonstrations using real station models and supported depot items. */
public final class LootScenes {
    private static final BlockPos INPUT = new BlockPos(0, 1, 2);
    private static final BlockPos COUNTER = new BlockPos(2, 1, 2);
    private static final BlockPos OUTPUT = new BlockPos(4, 1, 2);
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final BlockPos PORT = new BlockPos(3, 2, 2);
    private LootScenes() {}

    private static ItemStack token() {
        var result = UnidentifiedWeaponItem.create(TnkConfig.Category.WEAPON, 2, "", "ponder_example");
        RaritySupport.force(result, "rare"); return result;
    }
    private static void caption(SceneBuilder scene, String text, BlockPos target, PonderPalette colour) {
        scene.overlay().showText(90).text(text).pointAt(Vec3.atCenterOf(target)).placeNearTarget().colored(colour).attachKeyFrame();
        scene.idle(100);
    }
    private static void click(SceneBuilder scene, BlockPos position, ItemStack item) {
        scene.overlay().showControls(Vec3.atBottomCenterOf(position).add(0, 1, 0).add(0, .2, 0), Pointing.DOWN, 35).rightClick().withItem(item);
    }
    private static void depot(SceneBuilder scene, BlockPos pos, ItemStack item) {
        scene.world().modifyBlockEntity(pos, DepotBlockEntity.class, block -> block.setHeldItem(item.copy()));
    }
    private static void workshop(SceneBuilder scene, SceneBuildingUtil util) {
        scene.configureBasePlate(0, 0, 5); scene.scaleSceneView(1.05f); scene.showBasePlate(); scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN); scene.idle(20);
        scene.world().createEntity(world -> {
            var clerk = TnkLoot.IDENTIFIER_ENTITY.get().create(world);
            if (clerk != null) IdentifierBlock.position(clerk, COUNTER, TnkLoot.IDENTIFIER_BLOCK.get().defaultBlockState().setValue(IdentifierBlock.FACING, Direction.NORTH));
            return clerk;
        });
    }
    public static void loot(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("loot_basics", "Sealed loot and character levels"); workshop(scene, util);
        depot(scene, INPUT, token());
        caption(scene, "Adventure chests and hostile mobs can award sealed equipment. Ordinary materials remain ordinary loot.", INPUT, PonderPalette.WHITE);
        caption(scene, "The source level selects an equipment tier. Your TNK Progression milestone limits the roll; rarity is sealed when the drop is generated.", INPUT, PonderPalette.BLUE);
        caption(scene, "Weapons, ranged gear, armour, tools and accessories have separate pools. Dimension equipment retains its origin.", INPUT, PonderPalette.WHITE);
        click(scene, COUNTER, token());
        caption(scene, "Boss crates use that boss's reward pool. Trophies, keys, ingredients and advancement rewards keep their original items.", COUNTER, PonderPalette.BLUE);
        depot(scene, OUTPUT, new ItemStack(Items.IRON_SWORD));
        caption(scene, "Use the Identifier's Preview to inspect a drop. The Journal's Collection tracks your discoveries.", OUTPUT, PonderPalette.GREEN);
        scene.markAsFinished();
    }
    public static void identify(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("identifying", "Identify a batch at the counter"); workshop(scene, util);
        depot(scene, INPUT, token()); click(scene, COUNTER, token());
        caption(scene, "Right-click the Identifier. Place up to seven sealed drops into its input slots.", COUNTER, PonderPalette.WHITE);
        caption(scene, "Preview lists eligible equipment and its chance. Looking at the pool does not consume or reroll your drop.", INPUT, PonderPalette.BLUE);
        click(scene, COUNTER, new ItemStack(Items.EXPERIENCE_BOTTLE));
        caption(scene, "Read the batch price in vanilla XP points. Stored salvage credit reduces the amount you pay.", COUNTER, PonderPalette.GREEN);
        depot(scene, INPUT, ItemStack.EMPTY); depot(scene, OUTPUT, new ItemStack(Items.IRON_SWORD));
        caption(scene, "Confirm once to reveal the batch. The sealed tier, rarity and boss pool carry through to the result and its attributes.", OUTPUT, PonderPalette.GREEN);
        caption(scene, "Collect your equipment from the same slots. Closing or changing tabs returns any remaining items safely.", COUNTER, PonderPalette.WHITE);
        scene.markAsFinished();
    }
    public static void salvage(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("salvaging", "Review unwanted equipment before salvaging"); workshop(scene, util);
        depot(scene, INPUT, new ItemStack(Items.IRON_SWORD)); click(scene, COUNTER, new ItemStack(Items.IRON_SWORD));
        caption(scene, "Open Salvage from the Identifier. Use its seven input slots for unwanted identified equipment.", COUNTER, PonderPalette.WHITE);
        caption(scene, "Check the XP fee and the credit you will receive. Your inputs remain intact during review.", INPUT, PonderPalette.BLUE);
        click(scene, COUNTER, new ItemStack(Items.EXPERIENCE_BOTTLE));
        caption(scene, "Press Review, then confirm Salvage. The final action destroys the reviewed equipment and charges the displayed fee.", COUNTER, PonderPalette.RED);
        depot(scene, INPUT, ItemStack.EMPTY);
        caption(scene, "Salvage grants stored identification credit. It pays part of a future identification; it does not grant XP or character levels.", COUNTER, PonderPalette.GREEN);
        caption(scene, "Changed inputs require a new review. Close the screen before confirming to take your equipment back.", COUNTER, PonderPalette.WHITE);
        scene.markAsFinished();
    }
    public static void mechanical(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("mechanical_identifying", "Automate identification with Liquid XP");
        scene.configureBasePlate(0, 0, 5); scene.scaleSceneView(.95f); scene.showBasePlate(); scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 2, 2, 2, 2), Direction.DOWN); scene.idle(20);
        caption(scene, "The Mechanical Identifier uses Liquid XP to reveal equipment. The Identifier itself needs no rotational power.", MACHINE, PonderPalette.WHITE);
        scene.world().showSection(util.select().fromTo(2, 3, 2, 2, 4, 2), Direction.DOWN);
        scene.world().modifyBlockEntity(MACHINE, MechanicalIdentifierEntity.class, block -> block.inventory().setStackInSlot(0, token()));
        caption(scene, "Feed sealed drops from above or an ordinary side. The hopper points straight into the seven input slots.", new BlockPos(2, 3, 2), PonderPalette.BLUE);
        scene.rotateCameraY(-35); scene.idle(20);
        scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 2, 2), Direction.WEST);
        caption(scene, "Supply supported Liquid XP at the right-side port. The right side follows the machine's facing; the tank holds 4,000 mB by default.", PORT, PonderPalette.GREEN);
        caption(scene, "Your fluid network must pump XP into this port. Each item costs its normal identification fee converted to fluid and takes 40 ticks by default.", MACHINE, PonderPalette.WHITE);
        scene.rotateCameraY(35); scene.idle(20);
        scene.world().modifyBlockEntity(MACHINE, MechanicalIdentifierEntity.class, block -> { block.inventory().setStackInSlot(0, ItemStack.EMPTY); block.inventory().setStackInSlot(7, new ItemStack(Items.IRON_SWORD)); });
        depot(scene, new BlockPos(1, 1, 2), new ItemStack(Items.IRON_SWORD));
        caption(scene, "Extract revealed equipment underneath. This hopper faces the output depot; sealed rarity, tier and boss rewards are preserved.", new BlockPos(1, 1, 2), PonderPalette.GREEN);
        click(scene, MACHINE, ItemStack.EMPTY);
        caption(scene, "Right-click to inspect the machine. Missing XP or full outputs pause processing while keeping your items safe.", MACHINE, PonderPalette.WHITE);
        scene.markAsFinished();
    }
}
