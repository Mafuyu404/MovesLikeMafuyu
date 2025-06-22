package com.zhenshiz.moveslikemafuyu.event;

import com.zhenshiz.moveslikemafuyu.Config;
import com.zhenshiz.moveslikemafuyu.MovesLikeMafuyu;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = MovesLikeMafuyu.MOD_ID)
public class ClimbEvent {
    public static int COOLDOWN;
    private static long cooldown = COOLDOWN;
    public static boolean Falling = true;
    private static double CATCH_DISTANCE = 0.2;
    private static double FALLING_CATCH_DISTANCE = 0.6;

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.isLocalPlayer() || player.isSpectator()) return;
        Options options = Minecraft.getInstance().options;
        if (cooldown > 0 && cooldown <= COOLDOWN) {
            cooldown--;
        }
        if (!Config.ENABLE_FALLING_RESCUE.get()) {
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

    public static boolean checkWallClimbCondition(Player player) {
        Direction facing = player.getDirection();
        BlockPos checkPos = player.blockPosition().relative(facing);
        BlockPos upperPos = checkPos.above();
        BlockPos belowPos = player.blockPosition().below();
        if (!player.onGround() && isClimbableWall(player.level(), checkPos) && !player.level().getBlockState(belowPos).isSolidRender(player.level(), belowPos) && !isClimbableWall(player.level(), upperPos) && !isClimbableWall(player.level(), player.blockPosition())) {
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
    public static void jumpOnClimbable(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        if (!Config.ENABLE_CLIMB_JUMP.get()) return;
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
}
