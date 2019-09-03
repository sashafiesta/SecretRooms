package com.wynprice.secretrooms.server.blocks;

import com.wynprice.secretrooms.client.model.providers.SecretQuadProvider;
import com.wynprice.secretrooms.server.tileentity.SecretTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IEnviromentBlockReader;

import javax.annotation.Nullable;
import java.util.Optional;

public class SecretBaseBlock extends Block {

    public static final BooleanProperty SOLID = BooleanProperty.create("solid");

    private final SecretQuadProvider provider;

    public SecretBaseBlock(Properties properties) {
        this(properties, null);
    }

    public SecretBaseBlock(Properties properties, SecretQuadProvider provider) {
        super(properties.variableOpacity());
        this.provider = provider;
        this.setDefaultState(this.getStateContainer().getBaseState().with(SOLID, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(SOLID);
    }

    //Entity#createRunningParticles
    @Override
    public boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity) {
        Optional<BlockState> mirrorState = getMirrorState(world, pos);
        if(mirrorState.isPresent()) {
            BlockState blockstate = mirrorState.get();
            if (blockstate.getRenderType() != BlockRenderType.INVISIBLE) {
                Vec3d vec3d = entity.getMotion();
                world.addParticle(new BlockParticleData(ParticleTypes.BLOCK, blockstate),
                        entity.posX + (world.rand.nextFloat() - 0.5D) * entity.getWidth(),
                        entity.posY + 0.1D,
                        entity.posZ + (world.rand.nextFloat() - 0.5D) * entity.getWidth(),

                        vec3d.x * -4.0D, 1.5D, vec3d.z * -4.0D);
            }
            return true;
        }
        return false;
    }

    //ParticleManager#addBlockHitEffects
    @Override
    public boolean addHitEffects(BlockState state, World world, RayTraceResult target, ParticleManager manager) {
        if(target instanceof BlockRayTraceResult) {
            BlockPos pos = ((BlockRayTraceResult) target).getPos();
            Optional<BlockState> mirrorState = getMirrorState(world, pos);
            if(mirrorState.isPresent()) {
                BlockState blockstate = mirrorState.get();
                Direction side = ((BlockRayTraceResult) target).getFace();
                if (blockstate.getRenderType() != BlockRenderType.INVISIBLE) {
                    int x = pos.getX();
                    int y = pos.getY();
                    int z = pos.getZ();
                    AxisAlignedBB axisalignedbb = blockstate.getShape(world, pos).getBoundingBox();
                    double xPos = x + world.rand.nextDouble() * (axisalignedbb.maxX - axisalignedbb.minX - 0.2F) + 0.1F + axisalignedbb.minX;
                    double yPos = y + world.rand.nextDouble() * (axisalignedbb.maxY - axisalignedbb.minY - 0.2F) + 0.1F + axisalignedbb.minY;
                    double zPos = z + world.rand.nextDouble() * (axisalignedbb.maxZ - axisalignedbb.minZ - 0.2F) + 0.1F + axisalignedbb.minZ;

                    switch (side) {
                        case UP: yPos = y + axisalignedbb.maxY + 0.1F; break;
                        case DOWN: yPos = y + axisalignedbb.minY - 0.1F; break;
                        case NORTH: zPos = z + axisalignedbb.minZ - 0.1F; break;
                        case SOUTH: zPos = z + axisalignedbb.maxZ + 0.1F; break;
                        case WEST: xPos = x + axisalignedbb.minX - 0.1F; break;
                        case EAST: xPos = x + axisalignedbb.maxX + 0.1F; break;
                    }

                    Minecraft.getInstance().particles.addEffect(
                            new DiggingParticle(world, xPos, yPos, zPos, 0.0D, 0.0D, 0.0D, blockstate)
                                    .setBlockPos(pos)
                                    .multiplyVelocity(0.2F)
                                    .multipleParticleScaleBy(0.6F)
                    );
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addDestroyEffects(BlockState stateIn, World world, BlockPos pos, ParticleManager manager) {
        Optional<BlockState> mirrorState = getMirrorState(world, pos);
        if (mirrorState.isPresent()) {
            if(mirrorState.get().isAir(world, pos)) {
                return false;
            }
            BlockState state = mirrorState.get();
            VoxelShape voxelshape = state.getShape(world, pos);
            voxelshape.forEachBox((x1, y1, z1, x2, y2, z2) -> {
                double xDelta = Math.min(1.0D, x2 - x1);
                double yDelta = Math.min(1.0D, y2 - y1);
                double zDelta = Math.min(1.0D, z2 - z1);
                int xAmount = Math.max(2, MathHelper.ceil( xDelta / 0.25D));
                int yAmount = Math.max(2, MathHelper.ceil( yDelta / 0.25D));
                int zAmount = Math.max(2, MathHelper.ceil( zDelta / 0.25D));

                for(int x = 0; x < xAmount; ++x) {
                    for(int y = 0; y < yAmount; ++y) {
                        for(int z = 0; z < zAmount; ++z) {
                            double dx = (x + 0.5D) / xAmount;
                            double dy = (y + 0.5D) / yAmount;
                            double dz = (z + 0.5D) / zAmount;
                            double xPos = dx * xDelta + x1;
                            double yPos = dy * yDelta + y1;
                            double zPos = dz * zDelta + z1;
                            Minecraft.getInstance().particles.addEffect(
                                    new DiggingParticle(world,
                                            pos.getX() + xPos,pos.getY() + yPos, pos.getZ() + zPos,
                                            dx - 0.5D, dy - 0.5D,dz - 0.5D, state)
                                            .setBlockPos(pos)
                            );
                        }
                    }
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean addLandingEffects(BlockState state1, ServerWorld worldserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        Optional<BlockState> mirrorState = getMirrorState(worldserver, pos);
        if(mirrorState.isPresent()) {
            worldserver.spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, mirrorState.get()), entity.posX, entity.posY, entity.posZ, numberOfParticles, 0.0D, 0.0D, 0.0D, 0.15F);
            return true;
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return getValue(worldIn, pos, (mirror, reader, pos1) -> mirror.getShape(reader, pos1, context), super.getShape(state, worldIn, pos, context));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return getValue(worldIn, pos, (mirror, reader, pos1) -> mirror.getShape(reader, pos1, context), super.getCollisionShape(state, worldIn, pos, context));
    }

    @Override
    public VoxelShape getRenderShape(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return getValue(worldIn, pos, BlockState::getRenderShape, super.getRenderShape(state, worldIn, pos));
    }

    @Override
    public VoxelShape getRaytraceShape(BlockState state, IBlockReader world, BlockPos pos) {
        return getValue(world, pos, BlockState::getRaytraceShape, super.getRaytraceShape(state, world, pos));
    }

    @Nullable
    @Override
    public RayTraceResult getRayTraceResult(BlockState state, World world, BlockPos pos, Vec3d start, Vec3d end, RayTraceResult original) {
        return getValue(world, pos, (mirror, reader, pos1) -> mirror.getBlock().getRayTraceResult(mirror, world, pos, start, end, original), super.getRayTraceResult(state, world, pos, start, end, original));
    }

    @Override
    public float getBlockHardness(BlockState state, IBlockReader world, BlockPos pos) {
        return getValue(world, pos, BlockState::getBlockHardness, super.getBlockHardness(state, world, pos));
    }

    @Override
    public int getLightValue(BlockState state, IEnviromentBlockReader world, BlockPos pos) {
        return getValue(world, pos, BlockState::getLightValue, super.getLightValue(state, world, pos));
    }

    @Override
    public int getOpacity(BlockState state, IBlockReader world, BlockPos pos) {
        return getValue(world, pos, BlockState::getOpacity, super.getOpacity(state, world, pos));
    }

    @Override
    public float func_220080_a(BlockState state, IBlockReader world, BlockPos pos) {
        return getValue(world, pos, BlockState::func_215703_d, super.func_220080_a(state, world, pos));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader world, BlockPos pos) {
        return getValue(world, pos, BlockState::propagatesSkylightDown, super.propagatesSkylightDown(state, world, pos));
    }

    @Override
    public boolean isNormalCube(BlockState state, IBlockReader world, BlockPos pos) {
        return getValue(world, pos, BlockState::isNormalCube, super.isNormalCube(state, world, pos));
    }

    @Override
    public boolean isSolid(BlockState state) {
        return state.get(SOLID);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new SecretTileEntity();
    }

    @Nullable
    public SecretQuadProvider getProvider(IBlockReader world, BlockPos pos, BlockState state) {
        return this.provider;

    public static <T, W extends IBlockReader> T getValue(W reader, BlockPos pos, StateFunction<T, W> function, T defaultValue) {
        return getMirrorState(reader, pos).map(mirror -> function.getValue(mirror, reader, pos)).orElse(defaultValue);
    }

    public static Optional<BlockState> getMirrorState(IBlockReader world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return world.getBlockState(pos).getBlock() instanceof SecretBaseBlock && te instanceof SecretTileEntity ?
                Optional.of(((SecretTileEntity) te).getData().getBlockState()) : Optional.empty();
    }

    private interface StateFunction<T, W extends IBlockReader> {
        T getValue(BlockState mirror, W reader, BlockPos pos);
    }

}