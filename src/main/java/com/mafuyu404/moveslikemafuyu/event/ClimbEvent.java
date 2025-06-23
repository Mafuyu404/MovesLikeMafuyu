package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class ClimbEvent {
    private static int COOLDOWN;
    private static long cooldown = COOLDOWN;
    public static boolean Falling = true;
    private static double CATCH_DISTANCE = 0.2;
    private static double FALLING_CATCH_DISTANCE = 0.6;

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;
        if (cooldown > 0 && cooldown <= COOLDOWN) {
            cooldown--;
        }
        if (!Config.enable("FallingRescue")) {
            Falling = false;
            return;
        }
        double verticalSpeed = player.getDeltaMovement().y;
        Falling = verticalSpeed < 0 && verticalSpeed > -1 && !player.onGround() && !player.isInWater() && !player.isPassenger();
        if (Falling && player.onClimbable() && options.keyShift.isDown()) {
            if (player.level().getBlockState(player.blockPosition()).is(Blocks.SCAFFOLDING)) return;
            player.setDeltaMovement(0, 0, 0);
        }
    }
    @SubscribeEvent
    public static void jumpOnClimbable(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        if (!Config.enable("ClimbJump")) return;
        Player player = Minecraft.getInstance().player;
        Options options = Minecraft.getInstance().options;
        if (player == null || player.isSpectator()) return;
        if (cooldown <= 0 && event.getKey() == options.keyJump.getKey().getValue()) {
            if (player.isShiftKeyDown() && player.onClimbable()) {
                player.jumpFromGround();
                cooldown = COOLDOWN;
            }
        }
    }

    public static boolean checkWallClimbCondition(Player player) {
        Direction facing = player.getDirection();
        BlockPos checkPos = player.blockPosition().relative(facing);
        BlockPos upperPos = checkPos.above();
        BlockPos belowPos = player.blockPosition().below();
        BlockState state = player.level().getBlockState(checkPos);
        String[] type = state.getBlock().getDescriptionId().split("\\.");
        boolean checkBlock = isClimbableWall(player.level(), checkPos) || Config.CLIMB_BLOCK_WHITELIST.get().contains(type[1] + ":" + type[2]);
        if (!player.onGround() && checkBlock && !player.level().getBlockState(belowPos).isSolidRender(player.level(), belowPos) && !isClimbableWall(player.level(), upperPos) && !isClimbableWall(player.level(), player.blockPosition())) {
            AABB playerBB = player.getBoundingBox();
            double distance = ClimbEvent.Falling ? FALLING_CATCH_DISTANCE : CATCH_DISTANCE;
            AABB wallBB = new AABB(checkPos).inflate(distance);
            return playerBB.intersects(wallBB);
        }
        return false;
    }

    private static boolean isClimbableWall(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        return !collisionShape.isEmpty();
    }

    @SubscribeEvent
    public static void onConfigLoad(PlayerEvent.PlayerLoggedInEvent event) {
        COOLDOWN = Config.ConfigCache.getInt("ClimbJumpCooldown");
    }
}
