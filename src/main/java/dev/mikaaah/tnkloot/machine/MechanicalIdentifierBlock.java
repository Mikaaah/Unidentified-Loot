package dev.mikaaah.tnkloot.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/** A separate station: never inherits the NPC-spawning IdentifierBlock. */
public final class MechanicalIdentifierBlock extends BaseEntityBlock {
 public static final MapCodec<MechanicalIdentifierBlock> CODEC=simpleCodec(MechanicalIdentifierBlock::new);
 public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
 private static final java.util.Map<Direction,net.minecraft.world.phys.shapes.VoxelShape> SHAPES=shapes();
 private static java.util.Map<Direction,net.minecraft.world.phys.shapes.VoxelShape> shapes(){
  var north=net.minecraft.world.phys.shapes.Shapes.or(Block.box(0,0,0,16,4,16),Block.box(1,4,2,14,14,14),Block.box(2,14,3,13,16,13),Block.box(12,3,3,16,13,13),Block.box(3,4,.5,12,5,3));
  var result=new java.util.EnumMap<Direction,net.minecraft.world.phys.shapes.VoxelShape>(Direction.class);result.put(Direction.NORTH,north);
  for(var direction:new Direction[]{Direction.EAST,Direction.SOUTH,Direction.WEST}){var boxes=new java.util.ArrayList<net.minecraft.world.phys.shapes.VoxelShape>();north.forAllBoxes((x1,y1,z1,x2,y2,z2)->boxes.add(net.minecraft.world.phys.shapes.Shapes.box(1-z2,y1,x1,1-z1,y2,x2)));north=net.minecraft.world.phys.shapes.Shapes.or(net.minecraft.world.phys.shapes.Shapes.empty(),boxes.toArray(net.minecraft.world.phys.shapes.VoxelShape[]::new));result.put(direction,north);}
  return result;
 }
 @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return SHAPES.get(state.getValue(FACING));}
 public MechanicalIdentifierBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection().getOpposite());}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MechanicalIdentifierEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,MechanicalIdentifier.ENTITY.get(),MechanicalIdentifierEntity::tick);}
 private void open(Level level,BlockPos pos,Player player){if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof MechanicalIdentifierEntity machine)server.openMenu(machine,buf->buf.writeBlockPos(pos));}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(!level.isClientSide)open(level,pos,player);return InteractionResult.sidedSuccess(level.isClientSide);}
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
  if(level.isClientSide)return ItemInteractionResult.SUCCESS;
  if(level.getBlockEntity(pos) instanceof MechanicalIdentifierEntity machine&&hit.getDirection()==state.getValue(FACING).getClockWise()&&FluidUtil.interactWithFluidHandler(player,hand,machine.fluidInput()))return ItemInteractionResult.SUCCESS;
  open(level,pos,player);return ItemInteractionResult.SUCCESS;
 }
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){
  if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MechanicalIdentifierEntity machine){
   if(!level.isClientSide)for(int i=0;i<14;i++){var drop=machine.inventory().getStackInSlot(i).copy();machine.inventory().setStackInSlot(i,ItemStack.EMPTY);Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,drop);}
   level.invalidateCapabilities(pos);
  }
  super.onRemove(state,level,pos,next,moved);
 }
}
