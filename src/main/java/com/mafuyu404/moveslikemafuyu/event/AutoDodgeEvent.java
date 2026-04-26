package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID, value = Dist.CLIENT)
public class AutoDodgeEvent {
    private static final int PREDICT_TICKS = 10;
    private static final int EMERGENCY_COOLDOWN_TICKS = 6;
    private static final int TEMPORARY_CRAW_TICKS = 8;
    private static final int PROJECTILE_JOIN_SCAN_TICKS = 3;
    private static final int DODGE_PATH_CHECK_TICKS = 4;
    private static final double DODGE_LEAD_TICKS = 5.5;
    private static final double MIN_PROJECTILE_SPEED_SQR = 0.0025;
    private static final double DODGE_TARGET_SPEED = 0.475;
    private static final double HIT_MARGIN = 0.08;
    private static final double PRONE_HEIGHT = 0.62;
    private static int cooldown;
    private static int projectileJoinScanTicks;
    private static Projectile joinedProjectile;

    @SubscribeEvent
    public static void onClientTick(TickEvent.PlayerTickEvent event) {
        if (!Config.enable("AutoDodge")) return;
        Player player = event.player;
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
        return isSafeFromProjectiles(standingBox, getProjectilesInReach(player));
    }

    private static void tryAutoDodge(Player player, boolean allowEmergency, Projectile extraProjectile) {
        if (!player.isLocalPlayer() || player.isSpectator() || player.isInWater() || !player.onGround()) return;
        if (player.getTags().contains("slide")) return;
        if (player.getTags().contains("craw") && !CrawEvent.isAutoCraw(player)) return;

        List<Projectile> projectiles = getProjectilesInReach(player, extraProjectile);
        if (projectiles.isEmpty()) return;

        AABB playerBox = activeDodgeBox(player);
        List<Threat> threats = projectiles.stream()
                .map(projectile -> predictHit(projectile, playerBox))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingDouble(threat -> threat.timeFactor))
                .toList();
        if (threats.isEmpty()) return;

        List<Candidate> candidates = buildDodgeCandidates(player, threats);
        Optional<Candidate> horizontalDodge = candidates.stream()
                .filter(candidate -> canHorizontalDodge(player, candidate, projectiles))
                .max(Comparator.comparingInt(candidate -> dodgeScore(player, candidate, projectiles)));

        if (horizontalDodge.isPresent()) {
            dodge(player, horizontalDodge.get());
            return;
        }

        if (!allowEmergency || cooldown > 0) return;
        Candidate bestCandidate = candidates.stream()
                .max(Comparator.comparingInt(candidate -> dodgeScore(player, candidate, projectiles)))
                .orElse(new Candidate(preferredDodgeDirection(player, threats.get(0)), 0.5));
        Vec3 bestDirection = bestCandidate.direction;
        if (CrawEvent.isAutoCraw(player)) {
            horizontalDodge(player, bestDirection);
            cooldown = EMERGENCY_COOLDOWN_TICKS;
            return;
        }
        boolean upperBodyHit = threats.get(0).hit.y > playerBox.minY + playerBox.getYsize() * 0.55;
        if (upperBodyHit) {
            handleUpperBodyThreat(player, projectiles, bestDirection);
        } else {
            handleLowerBodyThreat(player, projectiles, bestDirection);
        }
    }

    private static List<Projectile> getProjectilesInReach(Player player) {
        return getProjectilesInReach(player, null);
    }

    private static List<Projectile> getProjectilesInReach(Player player, Projectile extraProjectile) {
        double reach = Math.max(3.0, player.getAttributeValue(ForgeMod.BLOCK_REACH.get()));
        double maxLeadDistance = reach + 12.0;
        AABB searchBox = player.getBoundingBox().inflate(maxLeadDistance);
        Vec3 playerCenter = player.getBoundingBox().getCenter();
        ArrayList<Projectile> projectiles = new ArrayList<>(player.level().getEntitiesOfClass(Projectile.class, searchBox, projectile ->
                projectile.isAlive()
                        && projectile.getOwner() != player
                        && projectile.getDeltaMovement().lengthSqr() >= MIN_PROJECTILE_SPEED_SQR
                        && projectile.position().distanceTo(playerCenter) <= reach + projectile.getDeltaMovement().length() * DODGE_LEAD_TICKS
        ));
        if (extraProjectile != null
                && extraProjectile.isAlive()
                && extraProjectile.getOwner() != player
                && extraProjectile.getDeltaMovement().lengthSqr() >= MIN_PROJECTILE_SPEED_SQR
                && extraProjectile.position().distanceTo(playerCenter) <= reach + extraProjectile.getDeltaMovement().length() * DODGE_LEAD_TICKS
                && !projectiles.contains(extraProjectile)) {
            projectiles.add(extraProjectile);
        }
        return projectiles;
    }

    private static List<Candidate> buildDodgeCandidates(Player player, List<Threat> threats) {
        ArrayList<Vec3> directions = new ArrayList<>();
        for (Threat threat : threats) {
            Vec3 preferred = preferredDodgeDirection(player, threat);
            addDirection(directions, preferred);
            addDirection(directions, preferred.scale(-1));
        }
        Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        if (look.lengthSqr() > 1.0E-6) {
            look = look.normalize();
            addDirection(directions, new Vec3(-look.z, 0, look.x));
            addDirection(directions, new Vec3(look.z, 0, -look.x));
            addDirection(directions, look);
            addDirection(directions, look.scale(-1));
        }
        Vec3 motion = new Vec3(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
        if (motion.lengthSqr() > 1.0E-6) {
            addDirection(directions, motion);
            addDirection(directions, motion.scale(-1));
        }
        ArrayList<Candidate> candidates = new ArrayList<>();
        double[] distances = {0.4, 0.575, 0.75};
        for (Vec3 direction : directions) {
            for (double distance : distances) {
                candidates.add(new Candidate(direction, distance));
            }
        }
        return candidates;
    }

    private static void addDirection(List<Vec3> candidates, Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6) return;
        horizontal = horizontal.normalize();
        for (Vec3 candidate : candidates) {
            if (candidate.dot(horizontal) > 0.96) return;
        }
        candidates.add(horizontal);
    }

    private static Optional<Threat> predictHit(Projectile projectile, AABB targetBox) {
        Vec3 velocity = projectile.getDeltaMovement();
        if (velocity.lengthSqr() < MIN_PROJECTILE_SPEED_SQR) return Optional.empty();
        AABB expandedTarget = targetBox.inflate(projectile.getBbWidth() * 0.5 + HIT_MARGIN, HIT_MARGIN, projectile.getBbWidth() * 0.5 + HIT_MARGIN);
        Vec3 start = projectile.position();
        Vec3 position = start;
        Vec3 motion = velocity;
        double traveled = 0;
        double totalLength = predictedPathLength(projectile, velocity);
        for (int tick = 0; tick < PREDICT_TICKS; tick++) {
            Vec3 next = position.add(motion);
            Optional<Vec3> hit = expandedTarget.clip(position, next);
            if (hit.isPresent()) {
                double hitDistance = traveled + position.distanceTo(hit.get());
                return Optional.of(new Threat(projectile, hit.get(), hitDistance / Math.max(totalLength, 1.0E-6)));
            }
            traveled += position.distanceTo(next);
            position = next;
            motion = nextProjectileMotion(projectile, motion);
        }
        return Optional.empty();
    }

    private static double predictedPathLength(Projectile projectile, Vec3 initialVelocity) {
        Vec3 motion = initialVelocity;
        double length = 0;
        for (int tick = 0; tick < PREDICT_TICKS; tick++) {
            length += motion.length();
            motion = nextProjectileMotion(projectile, motion);
        }
        return length;
    }

    private static Vec3 nextProjectileMotion(Projectile projectile, Vec3 motion) {
        double drag = projectile.isInWater() ? 0.6 : 0.99;
        double gravity = projectile.isNoGravity() ? 0 : projectileGravity(projectile);
        return motion.scale(drag).add(0, -gravity, 0);
    }

    private static double projectileGravity(Projectile projectile) {
        if (projectile instanceof AbstractArrow) return 0.05;
        return 0.03;
    }

    private static Vec3 preferredDodgeDirection(Player player, Threat threat) {
        Vec3 velocity = threat.projectile.getDeltaMovement();
        Vec3 forward = new Vec3(velocity.x, 0, velocity.z);
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        }
        if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0, 0, 1);
        Vec3 perpendicular = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 playerCenter = player.getBoundingBox().getCenter();
        Vec3 projectileToPlayer = playerCenter.subtract(threat.projectile.position());
        double side = projectileToPlayer.dot(perpendicular);
        return side >= 0 ? perpendicular : perpendicular.scale(-1);
    }

    private static boolean canHorizontalDodge(Player player, Candidate candidate, List<Projectile> projectiles) {
        AABB targetBox = targetBox(player, candidate);
        return player.level().noCollision(player, targetBox)
                && hasLandingSupport(player, targetBox)
                && isSafeDuringDodge(player, candidate, projectiles);
    }

    private static boolean hasLandingSupport(Player player, AABB targetBox) {
        Level level = player.level();
        if (!level.noCollision(player, targetBox.move(0, -0.12, 0))) return true;
        BlockPos belowCenter = BlockPos.containing(targetBox.getCenter().x, targetBox.minY - 0.2, targetBox.getCenter().z);
        return !level.getBlockState(belowCenter).getCollisionShape(level, belowCenter).isEmpty();
    }

    private static boolean isSafeFromProjectiles(AABB targetBox, List<Projectile> projectiles) {
        return projectiles.stream().noneMatch(projectile -> predictHit(projectile, targetBox).isPresent());
    }

    private static int dodgeScore(Player player, Candidate candidate, List<Projectile> projectiles) {
        AABB targetBox = targetBox(player, candidate);
        int unsafeProjectiles = 0;
        for (Projectile projectile : projectiles) {
            if (predictHit(projectile, targetBox).isPresent()) unsafeProjectiles++;
        }
        int score = -unsafeProjectiles * 100;
        if (player.level().noCollision(player, targetBox)) score += 10;
        if (hasLandingSupport(player, targetBox)) score += 10;
        if (isSafeDuringDodge(player, candidate, projectiles)) score += 40;
        score += projectileClearanceScore(targetBox, projectiles);
        score += (int) (candidate.distance * 2);
        return score;
    }

    private static AABB targetBox(Player player, Candidate candidate) {
        return activeDodgeBox(player).move(candidate.direction.normalize().scale(candidate.distance));
    }

    private static AABB activeDodgeBox(Player player) {
        if (CrawEvent.isAutoCraw(player)) {
            return proneBox(player);
        }
        return player.getBoundingBox();
    }

    private static AABB proneBox(Player player) {
        AABB box = player.getBoundingBox();
        return new AABB(box.minX, box.minY, box.minZ, box.maxX, box.minY + PRONE_HEIGHT, box.maxZ);
    }

    private static int projectileClearanceScore(AABB targetBox, List<Projectile> projectiles) {
        Vec3 center = targetBox.getCenter();
        double clearance = 0;
        for (Projectile projectile : projectiles) {
            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.lengthSqr() < MIN_PROJECTILE_SPEED_SQR) continue;
            Vec3 toCenter = center.subtract(projectile.position());
            double progress = Math.max(0, Math.min(PREDICT_TICKS, toCenter.dot(velocity) / velocity.lengthSqr()));
            Vec3 nearest = projectile.position().add(velocity.scale(progress));
            clearance += Math.min(4.0, nearest.distanceTo(center));
        }
        return (int) (clearance * 5);
    }

    private static boolean isSafeDuringDodge(Player player, Candidate candidate, List<Projectile> projectiles) {
        return isSafeDuringDodge(player, activeDodgeBox(player), candidate, projectiles);
    }

    private static boolean isSafeDuringDodge(Player player, AABB baseBox, Candidate candidate, List<Projectile> projectiles) {
        Vec3 horizontal = candidate.direction.normalize();
        Vec3 simulatedMotion = player.getDeltaMovement().add(horizontal.scale(dodgeBoost(player, candidate)));
        Vec3 horizontalMotion = new Vec3(simulatedMotion.x, 0, simulatedMotion.z);
        double speed = horizontalMotion.length();
        if (speed < 1.0E-6) return isSafeFromProjectiles(baseBox, projectiles);

        for (int tick = 1; tick <= DODGE_PATH_CHECK_TICKS; tick++) {
            double distance = Math.min(candidate.distance, speed * tick);
            AABB stepBox = baseBox.move(horizontal.scale(distance));
            if (!player.level().noCollision(player, stepBox)) return false;
            if (!isSafeFromProjectiles(stepBox, projectiles)) return false;
        }
        return true;
    }

    private static void dodge(Player player, Candidate candidate) {
        Vec3 horizontal = candidate.direction.normalize();
        Vec3 motion = player.getDeltaMovement();
        double boost = dodgeBoost(player, candidate);
        player.setDeltaMovement(motion.add(horizontal.x * boost, 0, horizontal.z * boost));
    }

    private static double dodgeBoost(Player player, Candidate candidate) {
        Vec3 horizontal = candidate.direction.normalize();
        Vec3 motion = player.getDeltaMovement();
        double currentSpeed = motion.x * horizontal.x + motion.z * horizontal.z;
        double targetSpeed = DODGE_TARGET_SPEED * Math.min(1.25, Math.max(0.8, candidate.distance));
        double boost = Math.max(0, targetSpeed - currentSpeed);
        return Math.min(boost, DODGE_TARGET_SPEED * 1.5);
    }

    private static void handleUpperBodyThreat(Player player, List<Projectile> projectiles, Vec3 bestDirection) {
        if (isSafeFromProjectiles(proneBox(player), projectiles)) {
            CrawEvent.startTemporaryCraw(player, TEMPORARY_CRAW_TICKS);
        } else {
            CrawEvent.startTemporaryCraw(player, TEMPORARY_CRAW_TICKS);
            horizontalDodge(player, bestDirection);
        }
        cooldown = EMERGENCY_COOLDOWN_TICKS;
    }

    private static void handleLowerBodyThreat(Player player, List<Projectile> projectiles, Vec3 bestDirection) {
        AABB jumpBox = player.getBoundingBox().move(0, 0.8, 0);
        if (isSafeFromProjectiles(jumpBox, projectiles)) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.42, 0));
        } else {
            CrawEvent.startLeap(player, bestDirection);
        }
        cooldown = EMERGENCY_COOLDOWN_TICKS;
    }

    private static void horizontalDodge(Player player, Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6) horizontal = new Vec3(0, 0, 1);
        horizontal = horizontal.normalize();
        Vec3 motion = player.getDeltaMovement();
        double currentSpeed = motion.x * horizontal.x + motion.z * horizontal.z;
        double boost = Math.max(0, DODGE_TARGET_SPEED - currentSpeed);
        player.setDeltaMovement(motion.add(horizontal.x * boost, 0, horizontal.z * boost));
    }

    private record Threat(Projectile projectile, Vec3 hit, double timeFactor) {
    }

    private record Candidate(Vec3 direction, double distance) {
    }
}
