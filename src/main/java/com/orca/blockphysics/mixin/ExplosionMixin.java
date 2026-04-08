package com.orca.blockphysics.mixin;

import com.orca.blockphysics.BlockPhysicsMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Shadow @Final private World world;

    @Shadow public abstract List<BlockPos> getAffectedBlocks();

    @Inject(method = "affectWorld", at = @At("HEAD"), cancellable = true)
    private void onAffectWorld(boolean particles, CallbackInfo ci) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        List<BlockPos> affectedBlocks = getAffectedBlocks();
        Random random = new Random();

        for (BlockPos pos : affectedBlocks) {
            BlockState state = world.getBlockState(pos);

            if (state.isAir()) continue;

            if (BlockPhysicsMod.isExemptBlock(state.getBlock())) {
                continue;
            }

            if (BlockPhysicsMod.isNaturalBlock(state.getBlock())) {
                continue;
            }

            double vx = (random.nextDouble() - 0.5) * 1.5;
            double vy = random.nextDouble() * 1.5 + 0.5;
            double vz = (random.nextDouble() - 0.5) * 1.5;

            FallingBlockEntity fallingBlock = FallingBlockEntity.spawnFromBlock(serverWorld, pos, state);
            if (fallingBlock != null) {
                fallingBlock.setVelocity(vx, vy, vz);
                fallingBlock.timeFalling = 1;
                fallingBlock.dropItem = true;
            }

            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }

        affectedBlocks.clear();
    }
}
