package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import com.mafuyu404.moveslikemafuyu.util.DodgeHelper;
import com.mafuyu404.moveslikemafuyu.util.PoseHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class AutoDodgeEvent {
    private static final int EMERGENCY_COOLDOWN_TICKS = 6;
    private static final int TEMPORARY_CRAW_TICKS = 8;
    private static final int PROJECTILE_JOIN_SCAN_TICKS = 3;
    private static int cooldown;
    private static int projectileJoinScanTicks;
    private static Projectile joinedProjectile;

    @SubscribeEvent
    public static void onClientTick(TickEvent.PlayerTickEvent event) {
        if (!Config.enable("AutoDodge")) return;
        Player player = event.player;
        if (!hasAutoDodgeEffect(player)) return;
        if (event.phase == TickEvent.Phase.END && cooldown > 0) cooldown--;
        Projectile extraProjectile = projectileJoinScanTicks > 0 && joinedProjectile != null && joinedProjectile.isAlive() ? joinedProjectile : null;
        tryAutoDodge(player, true, extraProjectile);
        if (event.phase == TickEvent.Phase.END && projectileJoinScanTicks > 0) projectileJoinScanTicks--;
    }

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (!Config.enable("AutoDodge") || !(event.getEntity() instanceof Projectile projectile)) return;
        Player player = Minecraft.getInstance().player;
        if (player == null || player.level() != event.getLevel()) return;
        if (!hasAutoDodgeEffect(player)) return;
        joinedProjectile = projectile;
        projectileJoinScanTicks = PROJECTILE_JOIN_SCAN_TICKS;
        tryAutoDodge(player, false, projectile);
    }

    public static boolean canStandSafely(Player player) {
        if (!Config.enable("AutoDodge")) return true;
        AABB standingBox = new AABB(
                player.getX() - player.getBbWidth() * 0.5,
                player.getY(),
                player.getZ() - player.getBbWidth() * 0.5,
                player.getX() + player.getBbWidth() * 0.5,
                player.getY() + 1.8,
                player.getZ() + player.getBbWidth() * 0.5
        );
        return DodgeHelper.isSafeFromProjectiles(standingBox, DodgeHelper.getProjectilesInReach(player));
    }

    private static void tryAutoDodge(Player player, boolean allowEmergency, Projectile extraProjectile) {
        if (!player.isLocalPlayer() || player.isSpectator() || player.isInWater()) return;
        if (RollEvent.isRolling(player)) return;
        if (player.getTags().contains("slide")) return;
        if (player.getTags().contains("craw") && !CrawEvent.isAutoCraw(player)) return;

        List<Projectile> projectiles = DodgeHelper.getProjectilesInReach(player, extraProjectile);
        if (projectiles.isEmpty()) return;

        AABB playerBox = DodgeHelper.activeDodgeBox(player);
        List<DodgeHelper.Threat> threats = projectiles.stream()
                .map(projectile -> DodgeHelper.predictHit(projectile, playerBox))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingDouble(threat -> threat.timeFactor()))
                .toList();
        if (threats.isEmpty()) return;

        // 空中：趴下、起跳这些地面动作不适用，只用翻滚类闪避。
        if (!player.onGround()) {
            if (!allowEmergency || cooldown > 0) return;
            if (CrawEvent.isAutoCraw(player)) CrawEvent.cancelCraw(player);
            airRollDodge(player, DodgeHelper.preferredDodgeDirection(player, threats.get(0)));
            cooldown = EMERGENCY_COOLDOWN_TICKS;
            return;
        }

        List<DodgeHelper.Candidate> candidates = DodgeHelper.buildDodgeCandidates(player, threats);
        Optional<DodgeHelper.Candidate> horizontalDodge = candidates.stream()
                .filter(candidate -> DodgeHelper.canHorizontalDodge(player, candidate, projectiles))
                .max(Comparator.comparingInt(candidate -> DodgeHelper.dodgeScore(player, candidate, projectiles)));

        if (horizontalDodge.isPresent()) {
            DodgeHelper.Candidate best = horizontalDodge.get();
            if (CrawEvent.isAutoCraw(player)) {
                dodgeFromCraw(player, best.direction());
            } else {
                DodgeHelper.dodge(player, best);
            }
            return;
        }

        if (!allowEmergency || cooldown > 0) return;
        DodgeHelper.Candidate bestCandidate = candidates.stream()
                .max(Comparator.comparingInt(candidate -> DodgeHelper.dodgeScore(player, candidate, projectiles)))
                .orElse(new DodgeHelper.Candidate(DodgeHelper.preferredDodgeDirection(player, threats.get(0)), 0.5));
        Vec3 bestDirection = bestCandidate.direction();
        if (CrawEvent.isAutoCraw(player)) {
            dodgeFromCraw(player, bestDirection);
            cooldown = EMERGENCY_COOLDOWN_TICKS;
            return;
        }
        boolean upperBodyHit = threats.get(0).hit().y > playerBox.minY + playerBox.getYsize() * 0.55;
        if (upperBodyHit) {
            handleUpperBodyThreat(player, projectiles, bestDirection);
        } else {
            handleLowerBodyThreat(player, projectiles, bestDirection);
        }
    }

    private static boolean hasAutoDodgeEffect(Player player) {
        return player != null && player.hasEffect(ModEffects.AUTO_DODGE.get());
    }

    private static void handleUpperBodyThreat(Player player, List<Projectile> projectiles, Vec3 bestDirection) {
        if (DodgeHelper.isSafeFromProjectiles(DodgeHelper.proneBox(player), projectiles)) {
            CrawEvent.startTemporaryCraw(player, TEMPORARY_CRAW_TICKS);
        } else {
            CrawEvent.startTemporaryCraw(player, TEMPORARY_CRAW_TICKS);
            DodgeHelper.horizontalDodge(player, bestDirection);
        }
        cooldown = EMERGENCY_COOLDOWN_TICKS;
    }

    private static void handleLowerBodyThreat(Player player, List<Projectile> projectiles, Vec3 bestDirection) {
        AABB jumpBox = player.getBoundingBox().move(0, 0.8, 0);
        if (DodgeHelper.isSafeFromProjectiles(jumpBox, projectiles)) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.42, 0));
        } else {
            // 飞扑改为空中翻滚：向最佳方向翻滚，并仿照地面闪避补一份水平位移。
            airRollDodge(player, bestDirection);
        }
        cooldown = EMERGENCY_COOLDOWN_TICKS;
    }

    /**
     * 趴下状态下的侧面闪避：直接翻滚躲开（正因为原地不安全才需要翻滚）。
     * 翻滚期间暂不强制趴下姿态，让玩家保持站立翻滚；翻滚结束后由
     * CrawEvent 的 auto_craw 逻辑按当时是否安全决定继续趴着还是起身。
     */
    private static void dodgeFromCraw(Player player, Vec3 direction) {
        if (RollEvent.startRoll(player, direction)) {
            PoseHelper.clearForcedPose(player);
            return;
        }
        DodgeHelper.horizontalDodge(player, direction);
    }

    /**
     * 空中翻滚：翻滚动画 + 地面闪避同款水平位移。
     * 空中且未在跳起窗口内时，翻滚本身不产生位移（rollMovementEnabled 为 false），
     * 所以在这里补一份水平位移，保证空中翻滚确实移动。
     */
    private static void airRollDodge(Player player, Vec3 direction) {
        RollEvent.startAirRoll(player, direction);
        DodgeHelper.horizontalDodge(player, direction);
    }
}
