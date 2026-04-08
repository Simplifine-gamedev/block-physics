package com.orca.blockphysics;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class BlockPhysicsMod implements ModInitializer {
    public static final String MOD_ID = "block-physics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Block Physics mod initialized!");

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld) {
                checkAndTriggerFalling(serverWorld, pos);
            }
        });
    }

    public static void checkAndTriggerFalling(ServerWorld world, BlockPos brokenPos) {
        Set<BlockPos> checked = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();

        for (Direction dir : Direction.values()) {
            toCheck.add(brokenPos.offset(dir));
        }

        while (!toCheck.isEmpty()) {
            BlockPos checkPos = toCheck.poll();
            if (checked.contains(checkPos)) continue;
            checked.add(checkPos);

            BlockState state = world.getBlockState(checkPos);
            if (state.isAir()) continue;

            if (shouldBlockFall(state, world, checkPos)) {
                spawnFallingBlock(world, checkPos, state);
                world.removeBlock(checkPos, false);

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = checkPos.offset(dir);
                    if (!checked.contains(neighbor)) {
                        toCheck.add(neighbor);
                    }
                }
            }
        }
    }

    public static boolean shouldBlockFall(BlockState state, World world, BlockPos pos) {
        Block block = state.getBlock();

        if (isExemptBlock(block)) {
            return false;
        }

        if (isNaturalBlock(block)) {
            return false;
        }

        if (state.getBlock() instanceof FallingBlock) {
            return false;
        }

        return !hasSupport(world, pos, state);
    }

    public static boolean isExemptBlock(Block block) {
        return block == Blocks.STONE ||
               block == Blocks.COBBLESTONE ||
               block == Blocks.MOSSY_COBBLESTONE ||
               block == Blocks.COBBLESTONE_WALL ||
               block == Blocks.COBBLESTONE_STAIRS ||
               block == Blocks.COBBLESTONE_SLAB ||
               block == Blocks.MOSSY_COBBLESTONE_WALL ||
               block == Blocks.MOSSY_COBBLESTONE_STAIRS ||
               block == Blocks.MOSSY_COBBLESTONE_SLAB ||
               block == Blocks.STONE_BRICKS ||
               block == Blocks.STONE_BRICK_STAIRS ||
               block == Blocks.STONE_BRICK_SLAB ||
               block == Blocks.STONE_BRICK_WALL ||
               block == Blocks.DEEPSLATE ||
               block == Blocks.COBBLED_DEEPSLATE ||
               block == Blocks.BEDROCK;
    }

    public static boolean isNaturalBlock(Block block) {
        return block == Blocks.GRASS_BLOCK ||
               block == Blocks.DIRT ||
               block == Blocks.DIRT_PATH ||
               block == Blocks.COARSE_DIRT ||
               block == Blocks.ROOTED_DIRT ||
               block == Blocks.PODZOL ||
               block == Blocks.MYCELIUM ||
               block == Blocks.MUD ||
               block == Blocks.CLAY ||
               block == Blocks.TERRACOTTA ||
               block == Blocks.NETHERRACK ||
               block == Blocks.END_STONE ||
               block == Blocks.SOUL_SAND ||
               block == Blocks.SOUL_SOIL ||
               block == Blocks.SAND ||
               block == Blocks.RED_SAND ||
               block == Blocks.GRAVEL ||
               block == Blocks.WATER ||
               block == Blocks.LAVA ||
               block instanceof FluidBlock ||
               block instanceof PlantBlock ||
               block instanceof SaplingBlock;
    }

    public static boolean isLogBlock(Block block) {
        return block == Blocks.OAK_LOG ||
               block == Blocks.SPRUCE_LOG ||
               block == Blocks.BIRCH_LOG ||
               block == Blocks.JUNGLE_LOG ||
               block == Blocks.ACACIA_LOG ||
               block == Blocks.DARK_OAK_LOG ||
               block == Blocks.MANGROVE_LOG ||
               block == Blocks.CHERRY_LOG ||
               block == Blocks.CRIMSON_STEM ||
               block == Blocks.WARPED_STEM ||
               block == Blocks.OAK_WOOD ||
               block == Blocks.SPRUCE_WOOD ||
               block == Blocks.BIRCH_WOOD ||
               block == Blocks.JUNGLE_WOOD ||
               block == Blocks.ACACIA_WOOD ||
               block == Blocks.DARK_OAK_WOOD ||
               block == Blocks.MANGROVE_WOOD ||
               block == Blocks.CHERRY_WOOD ||
               block == Blocks.STRIPPED_OAK_LOG ||
               block == Blocks.STRIPPED_SPRUCE_LOG ||
               block == Blocks.STRIPPED_BIRCH_LOG ||
               block == Blocks.STRIPPED_JUNGLE_LOG ||
               block == Blocks.STRIPPED_ACACIA_LOG ||
               block == Blocks.STRIPPED_DARK_OAK_LOG ||
               block == Blocks.STRIPPED_MANGROVE_LOG ||
               block == Blocks.STRIPPED_CHERRY_LOG;
    }

    public static boolean isLeavesBlock(Block block) {
        return block instanceof LeavesBlock;
    }

    public static boolean isTreeBlock(Block block) {
        return isLogBlock(block) || isLeavesBlock(block);
    }

    public static boolean hasSupport(World world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if (isTreeBlock(block)) {
            return hasTreeSupport(world, pos, block);
        }

        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);

        if (!belowState.isAir() && !belowState.isLiquid()) {
            return true;
        }

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos side = pos.offset(dir);
            BlockState sideState = world.getBlockState(side);
            if (isExemptBlock(sideState.getBlock()) || isNaturalBlock(sideState.getBlock())) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasTreeSupport(World world, BlockPos pos, Block block) {
        if (isLogBlock(block)) {
            BlockPos below = pos.down();
            BlockState belowState = world.getBlockState(below);

            if (belowState.isAir()) {
                return false;
            }

            Block belowBlock = belowState.getBlock();
            if (isLogBlock(belowBlock)) {
                return hasTreeSupport(world, below, belowBlock);
            }

            if (isNaturalBlock(belowBlock) || isExemptBlock(belowBlock)) {
                return true;
            }

            return false;
        }

        if (isLeavesBlock(block)) {
            return hasAdjacentLog(world, pos, 4, new HashSet<>());
        }

        return true;
    }

    private static boolean hasAdjacentLog(World world, BlockPos pos, int distance, Set<BlockPos> checked) {
        if (distance <= 0) return false;
        if (checked.contains(pos)) return false;
        checked.add(pos);

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            BlockState neighborState = world.getBlockState(neighbor);
            Block neighborBlock = neighborState.getBlock();

            if (isLogBlock(neighborBlock)) {
                if (hasTreeSupport(world, neighbor, neighborBlock)) {
                    return true;
                }
            }

            if (isLeavesBlock(neighborBlock)) {
                if (hasAdjacentLog(world, neighbor, distance - 1, checked)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void spawnFallingBlock(ServerWorld world, BlockPos pos, BlockState state) {
        FallingBlockEntity fallingBlock = FallingBlockEntity.spawnFromBlock(world, pos, state);
        if (fallingBlock != null) {
            fallingBlock.timeFalling = 1;
        }
    }
}
