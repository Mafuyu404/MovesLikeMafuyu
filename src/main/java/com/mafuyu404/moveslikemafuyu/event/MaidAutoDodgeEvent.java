package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.MaidCompat;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import com.mafuyu404.moveslikemafuyu.util.DodgeHelper;
import com.mafuyu404.moveslikemafuyu.util.PoseHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative automatic dodge for Touhou Little Maid entities.
 * Moving projectiles are tracked as they enter a server level, so each tick only
 * searches for affected maids around active projectiles instead of scanning every entity.
 */
@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class MaidAutoDodgeEvent {
    public static final String DUCK_TAG = "moveslikemafuyu_maid_dodge_duck";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int HORIZONTAL_COOLDOWN = 2;
    private static final int EMERGENCY_COOLDOWN = 6;
    private static final int DUCK_TICKS = 8;
    private static final int HORIZONTAL_HOLD_TICKS = 3;
    private static final double JUMP_BOOST = 0.42;
    private static final float DUCK_HEIGHT = 0.6F;
    private static final double HELD_HORIZONTAL_SPEED = 0.45;
    private static final double PROJECTILE_MAID_SEARCH_RANGE = 16.0;
    private static final double MIN_PROJECTILE_SPEED_SQR = 0.0025;
    private static final int RESULT_WINDOW_TICKS = 20;
    private static final int MAID_DODGE_PATH_CHECK_STEPS = 4;
    private static final double MAID_PROJECTILE_VERTICAL_PADDING_FACTOR = 0.5;

    private static final Set<Projectile> trackedProjectiles =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<UUID, Integer> cooldowns = new HashMap<>();
    private static final Map<UUID, DuckState> ducking = new HashMap<>();
    private static final Map<UUID, HorizontalDodgeState> horizontalDodges = new HashMap<>();
    private static final Map<UUID, MaidTickBoundaryState> maidTickStarts = new HashMap<>();
    private static final Map<UUID, Set<UUID>> predictedThreats = new HashMap<>();
    private static final Map<Long, DodgeAttempt> activeAttempts = new LinkedHashMap<>();
    private static final List<DamageObservation> pendingDamageObservations = new ArrayList<>();
    private static int diagnosticCooldown;
    private static long nextAttemptId = 1;
    private static long debugSequence;

    private MaidAutoDodgeEvent() {
    }

    static void onTestModeChanged(MinecraftServer server, boolean enabled) {
        activeAttempts.clear();
        pendingDamageObservations.clear();
        predictedThreats.clear();
        maidTickStarts.clear();
        nextAttemptId = 1;
        debugSequence = 0;
        LOGGER.info("[MaidAutoDodge/Detail] Debug logging {}; serverTick={} trackedProjectiles={} activeDucks={} activeHorizontalDodges={}",
                enabled ? "ENABLED" : "DISABLED",
                server.getTickCount(), trackedProjectiles.size(), ducking.size(), horizontalDodges.size());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof LivingEntity living
                && MaidCompat.isMaid(living)
                && living.getTags().contains(DUCK_TAG)) {
            clearDuckState(living);
        }
        if (!(event.getEntity() instanceof Projectile projectile)) return;
        trackedProjectiles.add(projectile);
        if (MaidTestMode.isEnabled()) {
            ServerLevel level = (ServerLevel) event.getLevel();
            debug(level, "PROJECTILE_TRACKED",
                    "projectile=%s owner=%s pos=%s velocity=%s speed=%.4f noGravity=%s box=%s",
                    entityRef(projectile), entityRef(projectile.getOwner()), vector(projectile.position()),
                    vector(projectile.getDeltaMovement()), projectile.getDeltaMovement().length(),
                    projectile.isNoGravity(), box(projectile.getBoundingBox()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide()) return;
        Projectile projectile = event.getProjectile();
        HitResult result = event.getRayTraceResult();
        markProjectileImpact(projectile, result);
        if (MaidTestMode.isEnabled() && projectile.level() instanceof ServerLevel level) {
            String target = result instanceof EntityHitResult entityHit
                    ? entityRef(entityHit.getEntity()) : "none";
            debug(level, "PROJECTILE_IMPACT",
                    "projectile=%s owner=%s resultType=%s location=%s target=%s canceled=%s "
                            + "projectilePos=%s projectilePreviousPos=%s projectileVelocity=%s "
                            + "projectileBox=%s projectileAge=%d predictedMaids=%s",
                    entityRef(projectile), entityRef(projectile.getOwner()), result.getType(),
                    vector(result.getLocation()), target, event.isCanceled(), vector(projectile.position()),
                    previousPosition(projectile), vector(projectile.getDeltaMovement()),
                    box(projectile.getBoundingBox()), projectile.tickCount,
                    predictedThreats.getOrDefault(projectile.getUUID(), Set.of()));
        }
        if (!(result instanceof EntityHitResult hitResult)
                || !MaidCompat.isMaid(hitResult.getEntity())) return;

        LivingEntity maid = (LivingEntity) hitResult.getEntity();
        UUID maidId = maid.getUUID();
        boolean predicted = predictedThreats.getOrDefault(projectile.getUUID(), Set.of()).contains(maidId);
        if (MaidTestMode.isEnabled() && projectile.level() instanceof ServerLevel level) {
            AABB maidBox = maid.getBoundingBox();
            AABB expandedMaidBox = maidBox.inflate(projectile.getBbWidth() * 0.5);
            debug(level, "MAID_PROJECTILE_IMPACT",
                    "attempt=%s maid=%s predictedThreat=%s impactLocation=%s centerDistance=%.4f "
                            + "maidBoxContainsImpact=%s expandedMaidBoxContainsImpact=%s "
                            + "impactSegmentClip=%s maidState={%s} maidSnapshot={%s} projectile={%s}",
                    latestAttemptId(maid), entityRef(maid), predicted, vector(result.getLocation()),
                    projectile.position().distanceTo(maidBox.getCenter()),
                    maidBox.contains(result.getLocation()), expandedMaidBox.contains(result.getLocation()),
                    expandedMaidBox.clip(projectile.position(), result.getLocation())
                            .map(MaidAutoDodgeEvent::vector).orElse("none"),
                    maidDodgeState(maid), EntitySnapshot.capture(maid),
                    projectileObservation(projectile, maid));
        }
        predictedThreats.remove(projectile.getUUID());
        if (MaidTestMode.isEnabled()) {
            LOGGER.info("[MaidAutoDodge/Server] Projectile {} hit maid {}; predicted threat: {}",
                    projectile.getId(), hitResult.getEntity().getId(), predicted);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingAttack(LivingAttackEvent event) {
        logDamageStage("ATTACK", event.getEntity(), event.getSource(), event.getAmount(), event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingHurt(LivingHurtEvent event) {
        logDamageStage("HURT", event.getEntity(), event.getSource(), event.getAmount(), event.isCanceled());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        logDamageStage("DAMAGE", event.getEntity(), event.getSource(), event.getAmount(), false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!MaidTestMode.isEnabled() || event.getEntity().level().isClientSide()
                || !MaidCompat.isMaid(event.getEntity())) return;
        LivingEntity maid = event.getEntity();
        DodgeAttempt attempt = findAttempt(maid, event.getSource());
        if (attempt != null) attempt.died = !event.isCanceled();
        ServerLevel level = (ServerLevel) maid.level();
        debug(level, "DEATH",
                "attempt=%s maid=%s canceled=%s source=%s health=%.3f absorption=%.3f pos=%s",
                attemptId(attempt), entityRef(maid), event.isCanceled(), damageSource(event.getSource()),
                maid.getHealth(), maid.getAbsorptionAmount(), vector(maid.position()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (diagnosticCooldown > 0) diagnosticCooldown--;

        if (MaidTestMode.isEnabled()) {
            LOGGER.info("[MaidAutoDodge/Detail][seq={}][tick={}][TICK_SUMMARY] trackedProjectiles={} cooldowns={} "
                            + "ducks={} horizontalDodges={} attempts={} trackedProjectileIds={} "
                            + "cooldownState={} duckState={} horizontalState={}",
                    ++debugSequence, event.getServer().getTickCount(), trackedProjectiles.size(),
                    cooldowns.size(), ducking.size(),
                    horizontalDodges.size(), activeAttempts.size(),
                    entityIds(new ArrayList<>(trackedProjectiles)), cooldowns,
                    duckStateSummary(), horizontalStateSummary());
        }

        if (!Config.enable("AutoDodge") || !MaidCompat.isLoaded()) {
            clearDucking();
            trackedProjectiles.clear();
            cooldowns.clear();
            horizontalDodges.clear();
            predictedThreats.clear();
            maidTickStarts.clear();
            maintainDodgeAttempts();
            return;
        }

        Map<UUID, LivingEntity> nearbyMaids = collectNearbyMaids();
        for (LivingEntity maid : nearbyMaids.values()) {
            tryAutoDodge(maid);
        }

        maintainDucking();
        maintainHorizontalDodges();
        cooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
        maintainDodgeAttempts();
    }

    private static Map<UUID, LivingEntity> collectNearbyMaids() {
        Map<UUID, LivingEntity> maids = new HashMap<>();
        Iterator<Projectile> iterator = trackedProjectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            String removalReason = projectileRemovalReason(projectile);
            if (removalReason != null) {
                if (MaidTestMode.isEnabled() && projectile.level() instanceof ServerLevel level) {
                    debug(level, "PROJECTILE_UNTRACKED",
                            "projectile=%s reason=%s pos=%s velocity=%s predictedMaids=%s",
                            entityRef(projectile), removalReason, vector(projectile.position()),
                            vector(projectile.getDeltaMovement()),
                            predictedThreats.getOrDefault(projectile.getUUID(), Set.of()));
                }
                predictedThreats.remove(projectile.getUUID());
                iterator.remove();
                continue;
            }

            ServerLevel level = (ServerLevel) projectile.level();
            AABB searchBox = projectile.getBoundingBox().inflate(PROJECTILE_MAID_SEARCH_RANGE);
            List<LivingEntity> foundMaids = level.getEntitiesOfClass(
                    LivingEntity.class,
                    searchBox,
                    maid -> MaidCompat.isMaid(maid) && maid.hasEffect(ModEffects.AUTO_DODGE.get())
            );
            foundMaids.forEach(maid -> maids.put(maid.getUUID(), maid));
            if (MaidTestMode.isEnabled()) {
                debug(level, "PROJECTILE_SCAN",
                        "projectile=%s owner=%s age=%d alive=%s pos=%s previousPos=%s velocity=%s "
                                + "speed=%.4f noGravity=%s projectileBox=%s searchBox=%s eligibleMaids=%s",
                        entityRef(projectile), entityRef(projectile.getOwner()), projectile.tickCount,
                        projectile.isAlive(), vector(projectile.position()), previousPosition(projectile),
                        vector(projectile.getDeltaMovement()), projectile.getDeltaMovement().length(),
                        projectile.isNoGravity(), box(projectile.getBoundingBox()), box(searchBox),
                        entityIds(foundMaids));
            }
        }
        return maids;
    }

    private static void tryAutoDodge(LivingEntity maid) {
        UUID maidId = maid.getUUID();
        ServerLevel level = (ServerLevel) maid.level();
        boolean alreadyDucking = ducking.containsKey(maidId);
        String skipReason = evaluationSkipReason(maid);
        if (MaidTestMode.isEnabled()) {
            debug(level, "EVALUATION_START",
                    "maid=%s pos=%s velocity=%s pose=%s box=%s health=%.3f absorption=%.3f onGround=%s "
                            + "cooldown=%d ducking=%s horizontalHold=%s skipReason=%s dodgeState={%s} "
                            + "nearbyTrackedProjectiles=%s",
                    entityRef(maid), vector(maid.position()), vector(maid.getDeltaMovement()), maid.getPose(),
                    box(maid.getBoundingBox()), maid.getHealth(), maid.getAbsorptionAmount(), maid.onGround(),
                    cooldowns.getOrDefault(maidId, 0), ducking.containsKey(maidId),
                    horizontalDodges.containsKey(maidId), skipReason == null ? "none" : skipReason,
                    maidDodgeState(maid), nearbyProjectileObservations(maid));
        }
        if (skipReason != null) {
            debug(level, "EVALUATION_SKIPPED",
                    "maid=%s reason=%s dodgeState={%s} nearbyTrackedProjectiles=%s",
                    entityRef(maid), skipReason, maidDodgeState(maid),
                    nearbyProjectileObservations(maid));
            return;
        }

        List<Projectile> projectiles = DodgeHelper.getProjectilesInReach(maid);
        if (projectiles.isEmpty()) {
            debug(level, "EVALUATION_SKIPPED",
                    "maid=%s reason=no_projectiles_in_reach trackedProjectiles=%d damageFilterEnabled=%s "
                            + "nearbyTrackedProjectiles=%s",
                    entityRef(maid), trackedProjectiles.size(),
                    Config.AUTO_DODGE_ONLY_DAMAGING_PROJECTILES.get(), nearbyProjectileObservations(maid));
            return;
        }

        AABB maidBox = maid.getBoundingBox();
        List<DodgeHelper.Threat> threats = new ArrayList<>();
        for (Projectile projectile : projectiles) {
            Optional<DodgeHelper.Threat> prediction = predictMaidHit(projectile, maidBox);
            logPrediction(maid, projectile, maidBox, prediction);
            prediction.ifPresent(threats::add);
        }
        threats.sort(Comparator.comparingDouble(DodgeHelper.Threat::hitTick)
                .thenComparingDouble(DodgeHelper.Threat::timeFactor));
        if (threats.isEmpty()) {
            logDiagnostic("Saw {} projectile(s) near maid {} but predicted no hit",
                    projectiles.size(), maid.getId());
            debug(level, "EVALUATION_RESULT",
                    "maid=%s result=NO_PREDICTED_HIT checkedProjectiles=%s",
                    entityRef(maid), projectileIds(projectiles));
            return;
        }
        if (MaidTestMode.isEnabled()) {
            for (DodgeHelper.Threat threat : threats) {
                predictedThreats
                        .computeIfAbsent(threat.projectile().getUUID(), ignored -> new java.util.HashSet<>())
                        .add(maidId);
            }
        }

        List<DodgeHelper.Candidate> candidates = DodgeHelper.buildDodgeCandidates(maid, threats);
        if (MaidTestMode.isEnabled()) {
            debug(level, "THREAT_ORDER",
                    "maid=%s threats=%s candidateCount=%d",
                    entityRef(maid), threatSummary(threats), candidates.size());
            for (int i = 0; i < candidates.size(); i++) {
                logCandidate(maid, i, candidates.get(i), projectiles);
            }
        }
        Optional<DodgeHelper.Candidate> horizontal = candidates.stream()
                .filter(candidate -> canMaidHorizontalDodge(maid, candidate, projectiles))
                .max(Comparator.comparingInt(candidate -> maidDodgeScore(maid, candidate, projectiles)));
        if (horizontal.isPresent()) {
            DodgeHelper.Candidate selected = horizontal.get();
            debug(level, "DECISION",
                    "maid=%s selected=HORIZONTAL direction=%s distance=%.3f score=%d "
                            + "reason=safe_horizontal_candidate activeDuck=%s cooldown=%d",
                    entityRef(maid), vector(selected.direction()), selected.distance(),
                    maidDodgeScore(maid, selected, projectiles), alreadyDucking,
                    cooldowns.getOrDefault(maidId, 0));
            applyHorizontalDodge(maid, selected, DodgeAction.HORIZONTAL, HORIZONTAL_COOLDOWN, threats);
            return;
        }

        Optional<DodgeHelper.Candidate> traversable = candidates.stream()
                .filter(candidate -> {
                    AABB targetBox = DodgeHelper.targetBox(maid, candidate);
                    return maid.level().noCollision(maid, targetBox)
                            && DodgeHelper.hasLandingSupport(maid, targetBox);
                })
                .max(Comparator.comparingInt(candidate -> maidDodgeScore(maid, candidate, projectiles)));
        DodgeHelper.Candidate best = traversable.orElseGet(() -> candidates.stream()
                .max(Comparator.comparingInt(candidate -> maidDodgeScore(maid, candidate, projectiles)))
                .orElse(new DodgeHelper.Candidate(
                        DodgeHelper.preferredDodgeDirection(maid, threats.get(0)), 0.5
                )));

        if (alreadyDucking) {
            DodgeHelper.Candidate correction = candidates.stream()
                    .filter(candidate -> {
                        AABB targetBox = DodgeHelper.targetBox(maid, candidate);
                        return maid.level().noCollision(maid, targetBox)
                                && DodgeHelper.hasLandingSupport(maid, targetBox);
                    })
                    .max(Comparator.comparingInt(candidate -> maidDuckHorizontalScore(
                            maid, candidate, projectiles, threats.get(0).projectile())))
                    .orElse(best);
            AABB correctionBox = DodgeHelper.targetBox(maid, correction);
            boolean immediateMove = maid.level().noCollision(maid, correctionBox)
                    && DodgeHelper.hasLandingSupport(maid, correctionBox);
            debug(level, "DECISION",
                    "maid=%s selected=DUCK_AND_HORIZONTAL direction=%s distance=%.3f score=%d "
                            + "immediateMove=%s cooldown=%d reason=active_duck_box_still_unsafe",
                    entityRef(maid), vector(correction.direction()), correction.distance(),
                    maidDuckHorizontalScore(maid, correction, projectiles, threats.get(0).projectile()),
                    immediateMove, cooldowns.getOrDefault(maidId, 0));
            applyDuckAndHorizontalDodge(maid, correction, immediateMove, threats);
            return;
        }

        int cooldown = cooldowns.getOrDefault(maidId, 0);
        if (cooldown > 0) {
            debug(level, "DECISION",
                    "maid=%s selected=NONE reason=emergency_cooldown cooldown=%d "
                            + "currentBoxStillUnsafe=true threats=%s",
                    entityRef(maid), cooldown, threatSummary(threats));
            return;
        }

        boolean upperBodyHit = threats.get(0).hit().y > maidBox.minY + maidBox.getYsize() * 0.55;
        boolean duckSafe = isMaidSafeFromProjectiles(duckBox(maid), projectiles);
        boolean imminentDuckSafe = predictMaidHit(
                threats.get(0).projectile(), duckBox(maid)).isEmpty();
        boolean jumpSafe = maid.onGround()
                && isMaidSafeFromProjectiles(maidBox.move(0, 0.8, 0), projectiles);
        debug(level, "FALLBACK_ANALYSIS",
                "maid=%s hitRegion=%s firstHit=%s upperThreshold=%.3f duckSafeAll=%s "
                        + "imminentDuckSafe=%s imminentProjectile=%s duckBox=%s onGround=%s "
                        + "jumpSafeAll=%s jumpBox=%s bestEffortTraversableCandidate=%s",
                entityRef(maid), upperBodyHit ? "UPPER" : "LOWER", vector(threats.get(0).hit()),
                maidBox.minY + maidBox.getYsize() * 0.55, duckSafe, imminentDuckSafe,
                entityRef(threats.get(0).projectile()), box(duckBox(maid)), maid.onGround(),
                jumpSafe, box(maidBox.move(0, 0.8, 0)), traversable.isPresent());
        if (upperBodyHit && duckSafe) {
            debug(level, "DECISION", "maid=%s selected=DUCK reason=upper_hit_and_duck_box_safe",
                    entityRef(maid));
            applyDuck(maid, threats);
        } else if (upperBodyHit) {
            DodgeHelper.Candidate combined = candidates.stream()
                    .filter(candidate -> {
                        AABB targetBox = DodgeHelper.targetBox(maid, candidate);
                        return maid.level().noCollision(maid, targetBox)
                                && DodgeHelper.hasLandingSupport(maid, targetBox);
                    })
                    .max(Comparator.comparingInt(candidate -> maidDuckHorizontalScore(
                            maid, candidate, projectiles, threats.get(0).projectile())))
                    .orElse(best);
            boolean immediateMove = maid.level().noCollision(maid, DodgeHelper.targetBox(maid, combined))
                    && DodgeHelper.hasLandingSupport(maid, DodgeHelper.targetBox(maid, combined));
            debug(level, "DECISION",
                    "maid=%s selected=DUCK_AND_HORIZONTAL direction=%s distance=%.3f score=%d "
                            + "immediateMove=%s imminentProjectile=%s "
                            + "reason=no_fully_safe_option_prioritize_imminent_upper_threat",
                    entityRef(maid), vector(combined.direction()), combined.distance(),
                    maidDuckHorizontalScore(maid, combined, projectiles, threats.get(0).projectile()),
                    immediateMove, entityRef(threats.get(0).projectile()));
            applyDuckAndHorizontalDodge(maid, combined, immediateMove, threats);
        } else if (!upperBodyHit && jumpSafe) {
            debug(level, "DECISION", "maid=%s selected=JUMP reason=lower_hit_and_jump_box_safe",
                    entityRef(maid));
            applyJump(maid, threats);
        } else if (traversable.isPresent()) {
            debug(level, "DECISION",
                    "maid=%s selected=EMERGENCY_HORIZONTAL direction=%s distance=%.3f score=%d "
                            + "reason=vertical_option_unsafe",
                    entityRef(maid), vector(best.direction()), best.distance(),
                    maidDodgeScore(maid, best, projectiles));
            applyHorizontalDodge(maid, best, DodgeAction.EMERGENCY_HORIZONTAL,
                    EMERGENCY_COOLDOWN, threats);
        } else {
            logDiagnostic("Found a threat to maid {} but no traversable dodge path", maid.getId());
            debug(level, "DECISION",
                    "maid=%s selected=NONE reason=no_traversable_path hitRegion=%s duckSafe=%s jumpSafe=%s threats=%s",
                    entityRef(maid), upperBodyHit ? "UPPER" : "LOWER", duckSafe, jumpSafe,
                    threatSummary(threats));
        }
    }

    private static void applyHorizontalDodge(LivingEntity maid, DodgeHelper.Candidate candidate,
                                             DodgeAction action, int cooldown,
                                             List<DodgeHelper.Threat> threats) {
        EntitySnapshot before = EntitySnapshot.capture(maid);
        Vec3 direction = new Vec3(candidate.direction().x, 0, candidate.direction().z).normalize();
        Vec3 targetPosition = maid.position().add(direction.scale(candidate.distance()));
        maid.move(MoverType.SELF, direction.scale(candidate.distance()));
        if (action == DodgeAction.HORIZONTAL) {
            DodgeHelper.dodge(maid, candidate);
        } else {
            DodgeHelper.horizontalDodge(maid, direction);
        }
        maid.hurtMarked = true;
        startHorizontalDodge(maid, direction, targetPosition);
        setCooldown(maid, cooldown);
        beginAttempt(maid, action, threats, before,
                String.format(Locale.ROOT,
                        "direction=%s distance=%.3f targetPosition=%s scoreContext=horizontal",
                        vector(direction), candidate.distance(), vector(targetPosition)));
        logApplied(maid, action);
    }

    private static void applyJump(LivingEntity maid, List<DodgeHelper.Threat> threats) {
        EntitySnapshot before = EntitySnapshot.capture(maid);
        maid.setDeltaMovement(maid.getDeltaMovement().add(0, JUMP_BOOST, 0));
        maid.hurtMarked = true;
        setCooldown(maid, EMERGENCY_COOLDOWN);
        beginAttempt(maid, DodgeAction.JUMP, threats, before,
                String.format(Locale.ROOT, "verticalBoost=%.3f", JUMP_BOOST));
        logApplied(maid, DodgeAction.JUMP);
    }

    private static void applyDuck(LivingEntity maid, List<DodgeHelper.Threat> threats) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        EntitySnapshot before = EntitySnapshot.capture(maid);
        ducking.put(maid.getUUID(), new DuckState(
                level, maid.getId(), DUCK_TICKS, level.getGameTime()
        ));
        maid.addTag(DUCK_TAG);
        PoseHelper.forcePose(maid, Pose.SWIMMING);
        syncEntityData(maid);
        maid.hurtMarked = true;
        setCooldown(maid, EMERGENCY_COOLDOWN);
        beginAttempt(maid, DodgeAction.DUCK, threats, before,
                String.format(Locale.ROOT, "durationTicks=%d duckHeight=%.3f", DUCK_TICKS, DUCK_HEIGHT));
        logApplied(maid, DodgeAction.DUCK);
    }

    private static void applyDuckAndHorizontalDodge(LivingEntity maid,
                                                    DodgeHelper.Candidate candidate,
                                                    boolean immediateMove,
                                                    List<DodgeHelper.Threat> threats) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        EntitySnapshot before = EntitySnapshot.capture(maid);
        Vec3 direction = new Vec3(candidate.direction().x, 0, candidate.direction().z).normalize();
        AABB plannedDuckBox = duckBox(maid).move(direction.scale(candidate.distance()));
        Vec3 targetPosition = maid.position().add(direction.scale(candidate.distance()));

        ducking.put(maid.getUUID(), new DuckState(
                level, maid.getId(), DUCK_TICKS, level.getGameTime()
        ));
        maid.addTag(DUCK_TAG);
        PoseHelper.forcePose(maid, Pose.SWIMMING);
        if (immediateMove) maid.move(MoverType.SELF, direction.scale(candidate.distance()));
        DodgeHelper.horizontalDodge(maid, direction);
        startHorizontalDodge(maid, direction, targetPosition);
        syncEntityData(maid);
        maid.hurtMarked = true;
        setCooldown(maid, EMERGENCY_COOLDOWN);
        beginAttempt(maid, DodgeAction.DUCK_AND_HORIZONTAL, threats, before,
                String.format(Locale.ROOT,
                        "durationTicks=%d direction=%s distance=%.3f immediateMove=%s "
                                + "targetPosition=%s plannedDuckBox=%s",
                        DUCK_TICKS, vector(direction), candidate.distance(), immediateMove,
                        vector(targetPosition), box(plannedDuckBox)));
        logApplied(maid, DodgeAction.DUCK_AND_HORIZONTAL);
    }

    private static void setCooldown(LivingEntity maid, int ticks) {
        cooldowns.merge(maid.getUUID(), ticks, Math::max);
    }

    private static void startHorizontalDodge(LivingEntity maid, Vec3 rawDirection,
                                             Vec3 targetPosition) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        Vec3 direction = new Vec3(rawDirection.x, 0, rawDirection.z).normalize();
        horizontalDodges.put(maid.getUUID(), new HorizontalDodgeState(
                level, maid.getId(), direction, targetPosition, HORIZONTAL_HOLD_TICKS
        ));
    }

    private static void maintainHorizontalDodges() {
        Iterator<Map.Entry<UUID, HorizontalDodgeState>> iterator = horizontalDodges.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HorizontalDodgeState> entry = iterator.next();
            HorizontalDodgeState state = entry.getValue();
            Entity entity = state.level().getEntity(state.entityId());
            if (!(entity instanceof LivingEntity maid)
                    || !MaidCompat.isMaid(maid)
                    || !maid.hasEffect(ModEffects.AUTO_DODGE.get())
                    || state.ticks() <= 0) {
                debug(state.level(), "HORIZONTAL_HOLD_END",
                        "maidEntityId=%d remainingTicks=%d reason=%s",
                        state.entityId(), state.ticks(), horizontalEndReason(entity, state));
                iterator.remove();
                continue;
            }

            Vec3 motion = maid.getDeltaMovement();
            Vec3 toTarget = new Vec3(
                    state.targetPosition().x - maid.getX(), 0,
                    state.targetPosition().z - maid.getZ()
            );
            double remainingDistance = toTarget.length();
            Vec3 desiredMotion = remainingDistance <= 0.02
                    ? Vec3.ZERO
                    : toTarget.scale(Math.min(HELD_HORIZONTAL_SPEED, remainingDistance) / remainingDistance);
            boolean corrected = Math.abs(motion.x - desiredMotion.x) > 1.0E-6
                    || Math.abs(motion.z - desiredMotion.z) > 1.0E-6;
            if (corrected) {
                maid.setDeltaMovement(desiredMotion.x, motion.y, desiredMotion.z);
                maid.hurtMarked = true;
            }
            debug(state.level(), "HORIZONTAL_HOLD_TICK",
                    "attempt=%s maid=%s remainingTicks=%d direction=%s targetPosition=%s "
                            + "remainingDistance=%.4f targetSpeed=%.4f corrected=%s "
                            + "velocityBefore=%s velocityAfter=%s",
                    latestAttemptId(maid), entityRef(maid), state.ticks(), vector(state.direction()),
                    vector(state.targetPosition()), remainingDistance,
                    new Vec3(desiredMotion.x, 0, desiredMotion.z).length(), corrected,
                    vector(motion), vector(maid.getDeltaMovement()));
            entry.setValue(new HorizontalDodgeState(
                    state.level(), state.entityId(), state.direction(), state.targetPosition(), state.ticks() - 1
            ));
        }
    }

    private static void maintainDucking() {
        Iterator<Map.Entry<UUID, DuckState>> iterator = ducking.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DuckState> entry = iterator.next();
            DuckState state = entry.getValue();
            Entity entity = state.level().getEntity(state.entityId());
            if (!(entity instanceof LivingEntity maid)
                    || !MaidCompat.isMaid(maid)
                    || !maid.hasEffect(ModEffects.AUTO_DODGE.get())) {
                debug(state.level(), "DUCK_END",
                        "maidEntityId=%d remainingTicks=%d reason=%s",
                        state.entityId(), state.ticks(), duckEndReason(entity));
                iterator.remove();
                if (entity instanceof LivingEntity living) clearDuckState(living);
                continue;
            }

            PoseHelper.forcePose(maid, Pose.SWIMMING);
            syncEntityData(maid);
            boolean createdThisTick = state.startedTick() == state.level().getGameTime();
            int remaining = createdThisTick ? state.ticks() : state.ticks() - 1;
            debug(state.level(), "DUCK_TICK",
                    "attempt=%s maid=%s remainingBefore=%d remainingAfter=%d createdThisTick=%s "
                            + "pose=%s box=%s pos=%s velocity=%s",
                    latestAttemptId(maid), entityRef(maid), state.ticks(), remaining, createdThisTick,
                    maid.getPose(), box(maid.getBoundingBox()), vector(maid.position()),
                    vector(maid.getDeltaMovement()));
            if (remaining <= 0) {
                List<Projectile> projectiles = DodgeHelper.getProjectilesInReach(maid);
                AABB standingBox = standingBox(maid);
                boolean collisionFree = maid.level().noCollision(maid, standingBox);
                List<Integer> unsafeProjectiles = unsafeMaidProjectileIds(standingBox, projectiles);
                if (!collisionFree || !unsafeProjectiles.isEmpty()) {
                    entry.setValue(new DuckState(
                            state.level(), state.entityId(), 1, state.startedTick()
                    ));
                    debug(state.level(), "DUCK_EXTENDED",
                            "attempt=%s maid=%s reason=standing_box_unsafe collisionFree=%s "
                                    + "unsafeProjectileIds=%s standingBox=%s currentBox=%s",
                            latestAttemptId(maid), entityRef(maid), collisionFree, unsafeProjectiles,
                            box(standingBox), box(maid.getBoundingBox()));
                } else {
                    iterator.remove();
                    debug(state.level(), "DUCK_END",
                            "attempt=%s maid=%s reason=duration_elapsed_and_standing_safe "
                                    + "standingBox=%s checkedProjectiles=%s finalPose=%s finalBox=%s",
                            latestAttemptId(maid), entityRef(maid), box(standingBox),
                            projectileIds(projectiles), maid.getPose(), box(maid.getBoundingBox()));
                    clearDuckState(maid);
                }
            } else {
                entry.setValue(new DuckState(
                        state.level(), state.entityId(), remaining, state.startedTick()
                ));
            }
        }
    }

    public static void logMaidTickStart(LivingEntity maid) {
        if (!MaidTestMode.isEnabled() || !(maid.level() instanceof ServerLevel level)) return;

        String nearbyProjectiles = nearbyProjectileObservations(maid);
        if (!maid.hasEffect(ModEffects.AUTO_DODGE.get())
                && !maid.getTags().contains(DUCK_TAG)
                && !ducking.containsKey(maid.getUUID())
                && !horizontalDodges.containsKey(maid.getUUID())
                && "[]".equals(nearbyProjectiles)) return;

        MaidTickBoundaryState state = new MaidTickBoundaryState(
                level.getGameTime(), EntitySnapshot.capture(maid), maidDodgeState(maid)
        );
        maidTickStarts.put(maid.getUUID(), state);
        debug(level, "MAID_TICK_HEAD",
                "attempt=%s maid=%s entityAge=%d snapshot={%s} dodgeState={%s} "
                        + "nearbyTrackedProjectiles=%s",
                latestAttemptId(maid), entityRef(maid), maid.tickCount, state.snapshot(),
                state.dodgeState(), nearbyProjectiles);
    }

    public static void keepDuckPoseAfterMaidTick(LivingEntity maid) {
        if (!(maid.level() instanceof ServerLevel level)) return;

        MaidTickBoundaryState start = maidTickStarts.remove(maid.getUUID());
        EntitySnapshot beforeCorrection = MaidTestMode.isEnabled() ? EntitySnapshot.capture(maid) : null;
        boolean correctionApplied = maid.getTags().contains(DUCK_TAG);
        if (correctionApplied) PoseHelper.forcePose(maid, Pose.SWIMMING);
        if (!MaidTestMode.isEnabled()) return;

        String nearbyProjectiles = nearbyProjectileObservations(maid);
        if (start == null && !correctionApplied && "[]".equals(nearbyProjectiles)) return;
        EntitySnapshot afterCorrection = EntitySnapshot.capture(maid);
        boolean poseChangedDuringTick = start != null
                && start.snapshot().pose() != beforeCorrection.pose();
        boolean boxChangedDuringTick = start != null
                && !start.snapshot().boundingBox().equals(beforeCorrection.boundingBox());
        debug(level, "MAID_TICK_TAIL",
                "attempt=%s maid=%s entityAge=%d startGameTick=%s start={%s} "
                        + "beforeCorrection={%s} afterCorrection={%s} positionDeltaDuringTick=%s "
                        + "velocityDeltaDuringTick=%s poseChangedDuringTick=%s boxChangedDuringTick=%s "
                        + "correctionApplied=%s correctionPoseDelta=%s correctionHeightDelta=%.4f "
                        + "dodgeStateBefore={%s} dodgeStateAfter={%s} nearbyTrackedProjectiles=%s",
                latestAttemptId(maid), entityRef(maid), maid.tickCount,
                start == null ? "none" : Long.toString(start.gameTick()),
                start == null ? "none" : start.snapshot(), beforeCorrection, afterCorrection,
                start == null ? "none" : vector(beforeCorrection.position().subtract(start.snapshot().position())),
                start == null ? "none" : vector(beforeCorrection.velocity().subtract(start.snapshot().velocity())),
                poseChangedDuringTick, boxChangedDuringTick, correctionApplied,
                beforeCorrection.pose() + "->" + afterCorrection.pose(),
                afterCorrection.boundingBox().getYsize() - beforeCorrection.boundingBox().getYsize(),
                start == null ? "none" : start.dodgeState(), maidDodgeState(maid), nearbyProjectiles);
    }

    private static void clearDucking() {
        List<DuckState> states = List.copyOf(ducking.values());
        ducking.clear();
        for (DuckState state : states) {
            Entity entity = state.level().getEntity(state.entityId());
            if (entity instanceof LivingEntity living) clearDuckState(living);
        }
    }

    private static void clearDuckState(LivingEntity maid) {
        Pose beforePose = maid.getPose();
        AABB beforeBox = maid.getBoundingBox();
        maid.removeTag(DUCK_TAG);
        PoseHelper.clearForcedPose(maid);
        syncEntityData(maid);
        if (maid.level() instanceof ServerLevel level) {
            debug(level, "DUCK_CLEARED",
                    "attempt=%s maid=%s poseBefore=%s poseAfter=%s boxBefore=%s boxAfter=%s",
                    latestAttemptId(maid), entityRef(maid), beforePose, maid.getPose(),
                    box(beforeBox), box(maid.getBoundingBox()));
        }
    }

    private static void syncEntityData(LivingEntity maid) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        List<SynchedEntityData.DataValue<?>> values = maid.getEntityData().packDirty();
        if (values == null || values.isEmpty()) {
            values = maid.getEntityData().getNonDefaultValues();
        }
        if (values == null || values.isEmpty()) {
            debug(level, "ENTITY_DATA_SYNC",
                    "maid=%s result=SKIPPED reason=no_values pose=%s box=%s",
                    entityRef(maid), maid.getPose(), box(maid.getBoundingBox()));
            return;
        }
        level.getChunkSource().broadcast(maid, new ClientboundSetEntityDataPacket(maid.getId(), values));
        debug(level, "ENTITY_DATA_SYNC",
                "maid=%s result=SENT valueCount=%d pose=%s box=%s",
                entityRef(maid), values.size(), maid.getPose(), box(maid.getBoundingBox()));
    }

    private static String projectileRemovalReason(Projectile projectile) {
        if (!projectile.isAlive()) return "not_alive";
        if (projectile.isRemoved()) return "removed:" + projectile.getRemovalReason();
        if (!(projectile.level() instanceof ServerLevel)) return "not_server_level";
        if (projectile.getDeltaMovement().lengthSqr() < MIN_PROJECTILE_SPEED_SQR) {
            return String.format(Locale.ROOT, "speed_below_threshold:%.6f<%.6f",
                    projectile.getDeltaMovement().lengthSqr(), MIN_PROJECTILE_SPEED_SQR);
        }
        return null;
    }

    private static String evaluationSkipReason(LivingEntity maid) {
        List<String> reasons = new ArrayList<>();
        if (maid.isInWater()) reasons.add("in_water");
        if (maid.getTags().contains("slide")) reasons.add("slide_tag");
        if (maid.getTags().contains("roll")) reasons.add("roll_tag");
        return reasons.isEmpty() ? null : String.join(",", reasons);
    }

    private static Optional<DodgeHelper.Threat> predictMaidHit(Projectile projectile,
                                                               AABB targetBox) {
        return DodgeHelper.predictHit(projectile, maidCollisionPredictionBox(projectile, targetBox));
    }

    private static Optional<Vec3> predictMaidHitNearTick(Projectile projectile, AABB targetBox,
                                                         double arrivalTick) {
        return DodgeHelper.predictHitNearTick(
                projectile, maidCollisionPredictionBox(projectile, targetBox), arrivalTick
        );
    }

    private static AABB maidCollisionPredictionBox(Projectile projectile, AABB targetBox) {
        double verticalPadding = projectile.getBbHeight() * MAID_PROJECTILE_VERTICAL_PADDING_FACTOR;
        return targetBox.inflate(0, verticalPadding, 0);
    }

    private static boolean isMaidSafeFromProjectiles(AABB targetBox,
                                                      List<Projectile> projectiles) {
        for (Projectile projectile : projectiles) {
            if (predictMaidHit(projectile, targetBox).isPresent()) return false;
        }
        return true;
    }

    private static boolean canMaidHorizontalDodge(LivingEntity maid,
                                                  DodgeHelper.Candidate candidate,
                                                  List<Projectile> projectiles) {
        AABB targetBox = DodgeHelper.targetBox(maid, candidate);
        return maid.level().noCollision(maid, targetBox)
                && DodgeHelper.hasLandingSupport(maid, targetBox)
                && isMaidSafeDuringDodge(maid, candidate, projectiles)
                && isMaidSafeFromProjectiles(targetBox, projectiles);
    }

    private static boolean isMaidSafeDuringDodge(LivingEntity maid,
                                                 DodgeHelper.Candidate candidate,
                                                 List<Projectile> projectiles) {
        for (DodgeHelper.DodgePathStep step : traceMaidDodgePath(maid, candidate, projectiles)) {
            if (!step.collisionFree() || !step.unsafeProjectileIds().isEmpty()) return false;
        }
        return true;
    }

    private static List<DodgeHelper.DodgePathStep> traceMaidDodgePath(
            LivingEntity maid, DodgeHelper.Candidate candidate, List<Projectile> projectiles) {
        AABB baseBox = DodgeHelper.activeDodgeBox(maid);
        Vec3 horizontal = candidate.direction().normalize();
        Vec3 simulatedMotion = maid.getDeltaMovement()
                .add(horizontal.scale(DodgeHelper.dodgeBoost(maid, candidate)));
        double speed = new Vec3(simulatedMotion.x, 0, simulatedMotion.z).length();
        List<DodgeHelper.DodgePathStep> steps = new ArrayList<>();
        if (speed < 1.0E-6) {
            List<Integer> unsafe = unsafeMaidProjectileIds(baseBox, projectiles);
            steps.add(new DodgeHelper.DodgePathStep(
                    0, 0.0, 0.0, baseBox, maid.level().noCollision(maid, baseBox), unsafe
            ));
            return steps;
        }

        for (int step = 1; step <= MAID_DODGE_PATH_CHECK_STEPS; step++) {
            double distance = candidate.distance() * step / MAID_DODGE_PATH_CHECK_STEPS;
            double arrivalTick = Math.max(1, distance / speed);
            AABB stepBox = baseBox.move(horizontal.scale(distance));
            List<Integer> unsafe = new ArrayList<>();
            for (Projectile projectile : projectiles) {
                if (predictMaidHitNearTick(projectile, stepBox, arrivalTick).isPresent()) {
                    unsafe.add(projectile.getId());
                }
            }
            steps.add(new DodgeHelper.DodgePathStep(
                    step, distance, arrivalTick, stepBox,
                    maid.level().noCollision(maid, stepBox), List.copyOf(unsafe)
            ));
        }
        return steps;
    }

    private static int maidDodgeScore(LivingEntity maid, DodgeHelper.Candidate candidate,
                                      List<Projectile> projectiles) {
        AABB targetBox = DodgeHelper.targetBox(maid, candidate);
        int score = -unsafeMaidProjectileIds(targetBox, projectiles).size() * 100;
        if (maid.level().noCollision(maid, targetBox)) score += 10;
        if (DodgeHelper.hasLandingSupport(maid, targetBox)) score += 10;
        if (isMaidSafeDuringDodge(maid, candidate, projectiles)) score += 40;
        score += DodgeHelper.projectileClearanceScore(targetBox, projectiles);
        score += (int) (candidate.distance() * 2);
        return score;
    }

    private static int maidDuckHorizontalScore(LivingEntity maid,
                                               DodgeHelper.Candidate candidate,
                                               List<Projectile> projectiles,
                                               Projectile imminentProjectile) {
        Vec3 displacement = candidate.direction().normalize().scale(candidate.distance());
        AABB targetDuckBox = duckBox(maid).move(displacement);
        int unsafeCount = unsafeMaidProjectileIds(targetDuckBox, projectiles).size();
        int score = maidDodgeScore(maid, candidate, projectiles) - unsafeCount * 200;
        if (predictMaidHit(imminentProjectile, targetDuckBox).isEmpty()) score += 1000;
        score += DodgeHelper.projectileClearanceScore(targetDuckBox, projectiles);
        return score;
    }

    private static List<Integer> unsafeMaidProjectileIds(AABB targetBox,
                                                         List<Projectile> projectiles) {
        List<Integer> unsafe = new ArrayList<>();
        for (Projectile projectile : projectiles) {
            if (predictMaidHit(projectile, targetBox).isPresent()) {
                unsafe.add(projectile.getId());
            }
        }
        return List.copyOf(unsafe);
    }

    private static void logPrediction(LivingEntity maid, Projectile projectile, AABB targetBox,
                                      Optional<DodgeHelper.Threat> prediction) {
        if (!MaidTestMode.isEnabled() || !(maid.level() instanceof ServerLevel level)) return;
        double centerDistance = projectile.position().distanceTo(targetBox.getCenter());
        AABB collisionPredictionBox = maidCollisionPredictionBox(projectile, targetBox);
        for (DodgeHelper.PredictionStep step : DodgeHelper.tracePredictionPath(
                projectile, collisionPredictionBox)) {
            debug(level, "PREDICTION_PATH_STEP",
                    "maid=%s projectile=%s step=%d start=%s end=%s motionBefore=%s motionAfter=%s "
                            + "segmentLength=%.4f intersectsExpandedTarget=%s intersection=%s",
                    entityRef(maid), entityRef(projectile), step.tick(), vector(step.start()),
                    vector(step.end()), vector(step.motionBefore()), vector(step.motionAfter()),
                    step.start().distanceTo(step.end()), step.intersection().isPresent(),
                    step.intersection().map(MaidAutoDodgeEvent::vector).orElse("none"));
        }
        if (prediction.isPresent()) {
            DodgeHelper.Threat threat = prediction.get();
            debug(level, "PREDICTION",
                    "maid=%s projectile=%s owner=%s result=HIT hitTick=%.3f hitDistance=%.3f "
                            + "pathFactor=%.6f hitPoint=%s projectilePos=%s projectileVelocity=%s speed=%.4f "
                            + "centerDistance=%.3f targetBox=%s collisionPredictionBox=%s projectileBox=%s",
                    entityRef(maid), entityRef(projectile), entityRef(projectile.getOwner()),
                    threat.hitTick(), threat.hitDistance(), threat.timeFactor(), vector(threat.hit()),
                    vector(projectile.position()), vector(projectile.getDeltaMovement()),
                    projectile.getDeltaMovement().length(), centerDistance, box(targetBox),
                    box(collisionPredictionBox),
                    box(projectile.getBoundingBox()));
        } else {
            debug(level, "PREDICTION",
                    "maid=%s projectile=%s owner=%s result=MISS projectilePos=%s projectileVelocity=%s "
                            + "speed=%.4f centerDistance=%.3f targetBox=%s collisionPredictionBox=%s "
                            + "projectileBox=%s",
                    entityRef(maid), entityRef(projectile), entityRef(projectile.getOwner()),
                    vector(projectile.position()), vector(projectile.getDeltaMovement()),
                    projectile.getDeltaMovement().length(), centerDistance, box(targetBox),
                    box(collisionPredictionBox),
                    box(projectile.getBoundingBox()));
        }
    }

    private static void logCandidate(LivingEntity maid, int index, DodgeHelper.Candidate candidate,
                                     List<Projectile> projectiles) {
        ServerLevel level = (ServerLevel) maid.level();
        AABB targetBox = DodgeHelper.targetBox(maid, candidate);
        boolean collisionFree = maid.level().noCollision(maid, targetBox);
        boolean landingSupport = DodgeHelper.hasLandingSupport(maid, targetBox);
        boolean pathSafe = isMaidSafeDuringDodge(maid, candidate, projectiles);
        boolean finalBoxSafe = isMaidSafeFromProjectiles(targetBox, projectiles);
        List<Integer> unsafeProjectiles = unsafeMaidProjectileIds(targetBox, projectiles);
        int clearanceScore = DodgeHelper.projectileClearanceScore(targetBox, projectiles);
        int score = maidDodgeScore(maid, candidate, projectiles);
        double boost = DodgeHelper.dodgeBoost(maid, candidate);
        List<String> rejectionReasons = new ArrayList<>();
        if (!collisionFree) rejectionReasons.add("block_collision");
        if (!landingSupport) rejectionReasons.add("no_landing_support");
        if (!pathSafe) rejectionReasons.add("unsafe_during_travel");
        if (!finalBoxSafe) rejectionReasons.add("unsafe_final_box:" + unsafeProjectiles);
        boolean primaryAccepted = collisionFree && landingSupport && pathSafe && finalBoxSafe;
        boolean bestEffortTraversable = collisionFree && landingSupport;
        debug(level, "CANDIDATE",
                "maid=%s index=%d direction=%s distance=%.3f targetBox=%s collisionFree=%s "
                        + "landingSupport=%s pathSafe=%s finalBoxSafe=%s unsafeProjectileIds=%s "
                        + "clearanceScore=%d boost=%.4f totalScore=%d accepted=%s "
                        + "bestEffortTraversable=%s rejectionReasons=%s",
                entityRef(maid), index, vector(candidate.direction()), candidate.distance(), box(targetBox),
                collisionFree, landingSupport, pathSafe, finalBoxSafe, unsafeProjectiles,
                clearanceScore, boost, score, primaryAccepted, bestEffortTraversable, rejectionReasons);
        for (DodgeHelper.DodgePathStep step : traceMaidDodgePath(maid, candidate, projectiles)) {
            debug(level, "CANDIDATE_PATH_STEP",
                    "maid=%s candidateIndex=%d step=%d distance=%.4f arrivalTick=%.4f box=%s "
                            + "collisionFree=%s unsafeProjectileIds=%s safe=%s",
                    entityRef(maid), index, step.step(), step.distance(), step.arrivalTick(),
                    box(step.box()), step.collisionFree(), step.unsafeProjectileIds(),
                    step.collisionFree() && step.unsafeProjectileIds().isEmpty());
        }
    }

    private static void beginAttempt(LivingEntity maid, DodgeAction action,
                                     List<DodgeHelper.Threat> threats, EntitySnapshot before,
                                     String actionContext) {
        if (!MaidTestMode.isEnabled() || !(maid.level() instanceof ServerLevel level)) return;
        List<Projectile> projectiles = new ArrayList<>();
        Set<UUID> ids = new java.util.HashSet<>();
        for (DodgeHelper.Threat threat : threats) {
            if (ids.add(threat.projectile().getUUID())) projectiles.add(threat.projectile());
        }
        long id = nextAttemptId++;
        DodgeAttempt attempt = new DodgeAttempt(
                id, level, maid.getId(), maid.getUUID(), action, level.getGameTime(),
                before, projectiles, ids, actionContext
        );
        activeAttempts.put(id, attempt);
        EntitySnapshot after = EntitySnapshot.capture(maid);
        debug(level, "ACTION_APPLIED",
                "attempt=%d action=%s maid=%s context={%s} threats=%s "
                        + "before={%s} after={%s} displacement=%s velocityDelta=%s cooldown=%d "
                        + "dodgeState={%s} postActionPredictions=%s",
                id, action, entityRef(maid), actionContext, threatSummary(threats), before,
                after, vector(after.position().subtract(before.position())),
                vector(after.velocity().subtract(before.velocity())),
                cooldowns.getOrDefault(maid.getUUID(), 0), maidDodgeState(maid),
                projectilePredictions(maid, projectiles));
    }

    private static void markProjectileImpact(Projectile projectile, HitResult result) {
        if (!MaidTestMode.isEnabled()) return;
        Entity hitEntity = result instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        if (hitEntity != null && MaidCompat.isMaid(hitEntity)) {
            DodgeAttempt directMatch = null;
            DodgeAttempt fallback = null;
            for (DodgeAttempt attempt : activeAttempts.values()) {
                if (!attempt.maidId.equals(hitEntity.getUUID())) continue;
                if (fallback == null || attempt.id > fallback.id) fallback = attempt;
                if (attempt.threatIds.contains(projectile.getUUID())
                        && (directMatch == null || attempt.id > directMatch.id)) {
                    directMatch = attempt;
                }
            }
            DodgeAttempt attributed = directMatch != null ? directMatch : fallback;
            if (attributed != null) {
                attributed.impactCount++;
                attributed.directHit = true;
                if (directMatch == null) attributed.untrackedImpactCount++;
            }
            return;
        }

        for (DodgeAttempt attempt : activeAttempts.values()) {
            if (!attempt.threatIds.contains(projectile.getUUID())) continue;
            attempt.impactCount++;
        }
    }

    private static void logDamageStage(String stage, LivingEntity maid, DamageSource source,
                                       float amount, boolean canceled) {
        if (!MaidTestMode.isEnabled() || maid.level().isClientSide() || !MaidCompat.isMaid(maid)) return;
        DodgeAttempt attempt = findAttempt(maid, source);
        if (attempt != null) {
            attempt.lastDamageSource = damageSource(source);
            if ("ATTACK".equals(stage)) {
                attempt.attackEvents++;
                if (!canceled) attempt.attackAmount += amount;
            } else if ("HURT".equals(stage)) {
                attempt.hurtEvents++;
                if (!canceled) attempt.hurtAmount += amount;
            } else if ("DAMAGE".equals(stage)) {
                attempt.damageEvents++;
                attempt.damageAmount += amount;
            }
        }
        ServerLevel level = (ServerLevel) maid.level();
        if ("DAMAGE".equals(stage)) {
            pendingDamageObservations.add(new DamageObservation(
                    level, maid.getId(), maid.getUUID(), attemptId(attempt), level.getGameTime(),
                    maid.getHealth(), maid.getAbsorptionAmount(), amount, damageSource(source)
            ));
        }
        Entity direct = source.getDirectEntity();
        boolean directThreatMatch = attempt != null && direct instanceof Projectile
                && attempt.threatIds.contains(direct.getUUID());
        debug(level, "DAMAGE_" + stage,
                "attempt=%s maid=%s amount=%.3f canceled=%s healthAtEvent=%.3f maxHealth=%.3f "
                        + "absorptionAtEvent=%.3f invulnerableToSource=%s directThreatMatch=%s source=%s "
                        + "pos=%s velocity=%s pose=%s box=%s",
                attemptId(attempt), entityRef(maid), amount, canceled, maid.getHealth(), maid.getMaxHealth(),
                maid.getAbsorptionAmount(), maid.isInvulnerableTo(source), directThreatMatch,
                damageSource(source), vector(maid.position()), vector(maid.getDeltaMovement()),
                maid.getPose(), box(maid.getBoundingBox()));
    }

    private static DodgeAttempt findAttempt(LivingEntity maid, DamageSource source) {
        UUID directId = source.getDirectEntity() instanceof Projectile projectile
                ? projectile.getUUID() : null;
        DodgeAttempt directMatch = null;
        DodgeAttempt fallback = null;
        for (DodgeAttempt attempt : activeAttempts.values()) {
            if (!attempt.maidId.equals(maid.getUUID())) continue;
            if (fallback == null || attempt.id > fallback.id) fallback = attempt;
            if (directId != null && attempt.threatIds.contains(directId)
                    && (directMatch == null || attempt.id > directMatch.id)) {
                directMatch = attempt;
            }
        }
        return directMatch != null ? directMatch : fallback;
    }

    private static void maintainDodgeAttempts() {
        if (!MaidTestMode.isEnabled()) {
            activeAttempts.clear();
            pendingDamageObservations.clear();
            return;
        }
        maintainDamageObservations();
        Iterator<DodgeAttempt> iterator = activeAttempts.values().iterator();
        while (iterator.hasNext()) {
            DodgeAttempt attempt = iterator.next();
            Entity entity = attempt.level.getEntity(attempt.entityId);
            long age = attempt.level.getGameTime() - attempt.startTick;
            if (!(entity instanceof LivingEntity maid) || !MaidCompat.isMaid(maid)) {
                debug(attempt.level, "ATTEMPT_RESULT",
                        "attempt=%d action=%s result=ENTITY_MISSING ageTicks=%d start={%s} "
                                + "context={%s} threats=%s damageEvents=%d finalDamage=%.3f",
                        attempt.id, attempt.action, age, attempt.start, attempt.actionContext,
                        threatStates(attempt.projectiles), attempt.damageEvents, attempt.damageAmount);
                iterator.remove();
                continue;
            }

            EntitySnapshot current = EntitySnapshot.capture(maid);
            debug(attempt.level, "ATTEMPT_STATE",
                    "attempt=%d action=%s ageTicks=%d maid=%s current={%s} displacement=%s "
                            + "threats=%s impacts=%d untrackedImpacts=%d directHit=%s "
                            + "attackEvents=%d hurtEvents=%d "
                            + "damageEvents=%d finalDamage=%.3f dodgeState={%s} currentPredictions=%s",
                    attempt.id, attempt.action, age, entityRef(maid), current,
                    vector(current.position().subtract(attempt.start.position())),
                    threatStates(attempt.projectiles), attempt.impactCount,
                    attempt.untrackedImpactCount, attempt.directHit,
                    attempt.attackEvents, attempt.hurtEvents, attempt.damageEvents, attempt.damageAmount,
                    maidDodgeState(maid), projectilePredictions(maid, attempt.projectiles));

            if (age >= RESULT_WINDOW_TICKS || attempt.died || !maid.isAlive()) {
                String result;
                if (attempt.died || !maid.isAlive()) {
                    result = "DEAD";
                } else if (attempt.damageEvents > 0 || attempt.damageAmount > 0.0F
                        || current.health() < attempt.start.health()) {
                    result = "DAMAGED";
                } else if (attempt.directHit) {
                    result = "IMPACTED_NO_DAMAGE";
                } else {
                    result = "SUCCESS_NO_DAMAGE";
                }
                debug(attempt.level, "ATTEMPT_RESULT",
                        "attempt=%d action=%s result=%s ageTicks=%d maid=%s context={%s} "
                                + "start={%s} final={%s} displacement=%s healthDelta=%.3f absorptionDelta=%.3f "
                                + "impacts=%d untrackedImpacts=%d directHit=%s attackEvents=%d "
                                + "attackAmount=%.3f hurtEvents=%d hurtAmount=%.3f damageEvents=%d "
                                + "finalDamage=%.3f lastDamageSource=%s threats=%s",
                        attempt.id, attempt.action, result, age, entityRef(maid), attempt.actionContext,
                        attempt.start, current, vector(current.position().subtract(attempt.start.position())),
                        current.health() - attempt.start.health(),
                        current.absorption() - attempt.start.absorption(), attempt.impactCount,
                        attempt.untrackedImpactCount, attempt.directHit,
                        attempt.attackEvents, attempt.attackAmount,
                        attempt.hurtEvents, attempt.hurtAmount, attempt.damageEvents, attempt.damageAmount,
                        attempt.lastDamageSource, threatStates(attempt.projectiles));
                iterator.remove();
            }
        }
    }

    private static void maintainDamageObservations() {
        Iterator<DamageObservation> iterator = pendingDamageObservations.iterator();
        while (iterator.hasNext()) {
            DamageObservation observation = iterator.next();
            Entity entity = observation.level().getEntity(observation.entityId());
            if (entity instanceof LivingEntity maid && maid.getUUID().equals(observation.maidId())) {
                debug(observation.level(), "DAMAGE_RESULT",
                        "attempt=%s maid=%s eventTick=%d reportedFinalDamage=%.3f source=%s "
                                + "healthBefore=%.3f healthAfter=%.3f healthDelta=%.3f "
                                + "absorptionBefore=%.3f absorptionAfter=%.3f absorptionDelta=%.3f "
                                + "alive=%s invulnerableTime=%d pos=%s velocity=%s pose=%s box=%s",
                        observation.attemptId(), entityRef(maid), observation.eventTick(),
                        observation.reportedDamage(), observation.source(), observation.healthBefore(),
                        maid.getHealth(), maid.getHealth() - observation.healthBefore(),
                        observation.absorptionBefore(), maid.getAbsorptionAmount(),
                        maid.getAbsorptionAmount() - observation.absorptionBefore(), maid.isAlive(),
                        maid.invulnerableTime, vector(maid.position()), vector(maid.getDeltaMovement()),
                        maid.getPose(), box(maid.getBoundingBox()));
            } else {
                debug(observation.level(), "DAMAGE_RESULT",
                        "attempt=%s maidEntityId=%d eventTick=%d reportedFinalDamage=%.3f "
                                + "source=%s result=ENTITY_MISSING",
                        observation.attemptId(), observation.entityId(), observation.eventTick(),
                        observation.reportedDamage(), observation.source());
            }
            iterator.remove();
        }
    }

    private static String horizontalEndReason(Entity entity, HorizontalDodgeState state) {
        if (entity == null) return "entity_missing";
        if (!(entity instanceof LivingEntity maid)) return "not_living_entity";
        if (!MaidCompat.isMaid(maid)) return "not_maid";
        if (!maid.hasEffect(ModEffects.AUTO_DODGE.get())) return "effect_missing";
        if (state.ticks() <= 0) return "duration_elapsed";
        return "unknown";
    }

    private static String duckEndReason(Entity entity) {
        if (entity == null) return "entity_missing";
        if (!(entity instanceof LivingEntity maid)) return "not_living_entity";
        if (!MaidCompat.isMaid(maid)) return "not_maid";
        if (!maid.hasEffect(ModEffects.AUTO_DODGE.get())) return "effect_missing";
        return "unknown";
    }

    private static String attemptId(DodgeAttempt attempt) {
        return attempt == null ? "none" : Long.toString(attempt.id);
    }

    private static String latestAttemptId(LivingEntity maid) {
        DodgeAttempt latest = null;
        for (DodgeAttempt attempt : activeAttempts.values()) {
            if (attempt.maidId.equals(maid.getUUID()) && (latest == null || attempt.id > latest.id)) {
                latest = attempt;
            }
        }
        return attemptId(latest);
    }

    private static String entityRef(Entity entity) {
        if (entity == null) return "none";
        String name = entity.getName().getString().replace('"', '\'');
        return String.format(Locale.ROOT, "%s{id=%d,uuid=%s,type=%s,name='%s'}",
                entity.getClass().getSimpleName(), entity.getId(), entity.getUUID(),
                entity.getType().getDescriptionId(), name);
    }

    private static String damageSource(DamageSource source) {
        Vec3 sourcePos = source.getSourcePosition();
        return String.format(Locale.ROOT, "{msgId=%s,direct=%s,causing=%s,position=%s}",
                source.getMsgId(), entityRef(source.getDirectEntity()), entityRef(source.getEntity()),
                sourcePos == null ? "none" : vector(sourcePos));
    }

    private static String entityIds(List<? extends Entity> entities) {
        List<String> ids = new ArrayList<>();
        for (Entity entity : entities) ids.add(entity.getId() + "/" + entity.getUUID());
        return ids.toString();
    }

    private static String projectileIds(List<Projectile> projectiles) {
        return entityIds(projectiles);
    }

    private static String threatSummary(List<DodgeHelper.Threat> threats) {
        List<String> details = new ArrayList<>();
        for (DodgeHelper.Threat threat : threats) {
            details.add(String.format(Locale.ROOT,
                    "{projectile=%d/%s,hitTick=%.3f,hitDistance=%.3f,pathFactor=%.6f,hit=%s}",
                    threat.projectile().getId(), threat.projectile().getUUID(), threat.hitTick(),
                    threat.hitDistance(), threat.timeFactor(), vector(threat.hit())));
        }
        return details.toString();
    }

    private static String threatStates(List<Projectile> projectiles) {
        List<String> details = new ArrayList<>();
        for (Projectile projectile : projectiles) {
            details.add(String.format(Locale.ROOT,
                    "{id=%d,uuid=%s,alive=%s,removed=%s,removalReason=%s,pos=%s,velocity=%s}",
                    projectile.getId(), projectile.getUUID(), projectile.isAlive(), projectile.isRemoved(),
                    projectile.getRemovalReason(), vector(projectile.position()),
                    vector(projectile.getDeltaMovement())));
        }
        return details.toString();
    }

    private static String maidDodgeState(LivingEntity maid) {
        UUID maidId = maid.getUUID();
        DuckState duckState = ducking.get(maidId);
        HorizontalDodgeState horizontalState = horizontalDodges.get(maidId);
        MobEffectInstance effect = maid.getEffect(ModEffects.AUTO_DODGE.get());
        String effectState = effect == null ? "absent" : String.format(Locale.ROOT,
                "present(duration=%d,amplifier=%d,ambient=%s,visible=%s)",
                effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible());
        String horizontal = horizontalState == null ? "absent" : String.format(Locale.ROOT,
                "present(ticks=%d,direction=%s)", horizontalState.ticks(),
                vector(horizontalState.direction()));
        return String.format(Locale.ROOT,
                "effect=%s,cooldown=%d,duckTag=%s,duckMap=%s,duckTicks=%s,"
                        + "horizontal=%s,latestAttempt=%s,pose=%s,boxHeight=%.4f,hurtMarked=%s,"
                        + "invulnerableTime=%d",
                effectState, cooldowns.getOrDefault(maidId, 0), maid.getTags().contains(DUCK_TAG),
                duckState != null, duckState == null ? "none" : Integer.toString(duckState.ticks()),
                horizontal, latestAttemptId(maid), maid.getPose(), maid.getBoundingBox().getYsize(),
                maid.hurtMarked, maid.invulnerableTime);
    }

    private static String duckStateSummary() {
        List<String> details = new ArrayList<>();
        for (Map.Entry<UUID, DuckState> entry : ducking.entrySet()) {
            DuckState state = entry.getValue();
            details.add(String.format(Locale.ROOT,
                    "{%s:entity=%d,ticks=%d,startedTick=%d,dimension=%s}",
                    entry.getKey(), state.entityId(), state.ticks(), state.startedTick(),
                    state.level().dimension().location()));
        }
        return details.toString();
    }

    private static String horizontalStateSummary() {
        List<String> details = new ArrayList<>();
        for (Map.Entry<UUID, HorizontalDodgeState> entry : horizontalDodges.entrySet()) {
            HorizontalDodgeState state = entry.getValue();
            details.add(String.format(Locale.ROOT,
                    "{%s:entity=%d,ticks=%d,direction=%s,target=%s,dimension=%s}",
                    entry.getKey(), state.entityId(), state.ticks(), vector(state.direction()),
                    vector(state.targetPosition()), state.level().dimension().location()));
        }
        return details.toString();
    }

    private static String nearbyProjectileObservations(LivingEntity maid) {
        List<String> details = new ArrayList<>();
        Vec3 maidCenter = maid.getBoundingBox().getCenter();
        double rangeSqr = PROJECTILE_MAID_SEARCH_RANGE * PROJECTILE_MAID_SEARCH_RANGE;
        for (Projectile projectile : trackedProjectiles) {
            if (projectile.level() != maid.level()
                    || projectile.position().distanceToSqr(maidCenter) > rangeSqr) continue;
            details.add(projectileObservation(projectile, maid));
        }
        return details.toString();
    }

    private static String projectilePredictions(LivingEntity maid, List<Projectile> projectiles) {
        List<String> details = new ArrayList<>();
        for (Projectile projectile : projectiles) {
            details.add(projectileObservation(projectile, maid));
        }
        return details.toString();
    }

    private static String projectileObservation(Projectile projectile, LivingEntity maid) {
        AABB maidBox = maid.getBoundingBox();
        DodgeHelper.ProjectileDamageAssessment damage = DodgeHelper.assessProjectileDamage(projectile, maid);
        Vec3 projectilePos = projectile.position();
        Vec3 previousPos = new Vec3(projectile.xo, projectile.yo, projectile.zo);
        Vec3 relative = maidBox.getCenter().subtract(projectilePos);
        double closingSpeed = relative.lengthSqr() < 1.0E-9 ? 0.0
                : projectile.getDeltaMovement().dot(relative.normalize());
        AABB actualCollisionBox = maidBox.inflate(projectile.getBbWidth() * 0.5);
        Optional<Vec3> actualSegmentClip = actualCollisionBox.clip(previousPos, projectilePos);
        return String.format(Locale.ROOT,
                "{projectile=%d/%s,class=%s,owner=%s,age=%d,alive=%s,removed=%s,"
                        + "damageFilterEnabled=%s,canDamageTarget=%s,damageReason=%s,"
                        + "pos=%s,previousPos=%s,velocity=%s,speed=%.4f,relativeToMaid=%s,"
                        + "centerDistance=%.4f,closingSpeed=%.4f,box=%s,actualSegmentClip=%s,"
                        + "currentBoxPrediction=%s,duckBoxPrediction=%s}",
                projectile.getId(), projectile.getUUID(), projectile.getClass().getSimpleName(),
                entityRef(projectile.getOwner()), projectile.tickCount, projectile.isAlive(),
                projectile.isRemoved(), Config.AUTO_DODGE_ONLY_DAMAGING_PROJECTILES.get(),
                damage.canDamage(), damage.reason(), vector(projectilePos), vector(previousPos),
                vector(projectile.getDeltaMovement()), projectile.getDeltaMovement().length(),
                vector(relative), relative.length(), closingSpeed, box(projectile.getBoundingBox()),
                actualSegmentClip.map(MaidAutoDodgeEvent::vector).orElse("none"),
                predictionSummary(predictMaidHit(projectile, maidBox)),
                predictionSummary(predictMaidHit(projectile, duckBox(maid))));
    }

    private static String predictionSummary(Optional<DodgeHelper.Threat> prediction) {
        if (prediction.isEmpty()) return "MISS";
        DodgeHelper.Threat threat = prediction.get();
        return String.format(Locale.ROOT,
                "HIT(tick=%.3f,distance=%.3f,pathFactor=%.6f,point=%s)",
                threat.hitTick(), threat.hitDistance(), threat.timeFactor(), vector(threat.hit()));
    }

    private static String previousPosition(Entity entity) {
        return vector(new Vec3(entity.xo, entity.yo, entity.zo));
    }

    private static String vector(Vec3 value) {
        return String.format(Locale.ROOT, "(%.4f,%.4f,%.4f)", value.x, value.y, value.z);
    }

    private static String box(AABB value) {
        return String.format(Locale.ROOT, "[(%.4f,%.4f,%.4f)->(%.4f,%.4f,%.4f)]",
                value.minX, value.minY, value.minZ, value.maxX, value.maxY, value.maxZ);
    }

    private static void debug(ServerLevel level, String event, String format, Object... arguments) {
        if (!MaidTestMode.isEnabled()) return;
        LOGGER.info("[MaidAutoDodge/Detail][seq={}][{}][tick={}][{}] {}",
                ++debugSequence, level.dimension().location(), level.getGameTime(), event,
                String.format(Locale.ROOT, format, arguments));
    }

    private static void logApplied(LivingEntity maid, DodgeAction action) {
        if (!MaidTestMode.isEnabled()) return;
        LOGGER.info("[MaidAutoDodge/Server] Applied {} to maid {}; velocity is {}",
                action, maid.getId(), maid.getDeltaMovement());
    }

    private static void logDiagnostic(String message, Object... arguments) {
        if (!MaidTestMode.isEnabled() || diagnosticCooldown > 0) return;
        LOGGER.info("[MaidAutoDodge/Server] " + message, arguments);
        diagnosticCooldown = 20;
    }

    private static AABB duckBox(LivingEntity entity) {
        EntityDimensions dimensions = entity.getDimensions(Pose.SWIMMING);
        return EntityDimensions.fixed(dimensions.width, DUCK_HEIGHT).makeBoundingBox(entity.position());
    }

    private static AABB standingBox(LivingEntity entity) {
        EntityDimensions dimensions = entity.getDimensions(Pose.STANDING);
        return EntityDimensions.fixed(dimensions.width, dimensions.height).makeBoundingBox(entity.position());
    }

    private enum DodgeAction {
        HORIZONTAL,
        EMERGENCY_HORIZONTAL,
        JUMP,
        DUCK,
        DUCK_AND_HORIZONTAL
    }

    private record EntitySnapshot(Vec3 position, Vec3 velocity, AABB boundingBox, Pose pose,
                                  float health, float absorption, boolean onGround,
                                  boolean alive, float fallDistance) {
        private static EntitySnapshot capture(LivingEntity entity) {
            return new EntitySnapshot(
                    entity.position(), entity.getDeltaMovement(), entity.getBoundingBox(), entity.getPose(),
                    entity.getHealth(), entity.getAbsorptionAmount(), entity.onGround(), entity.isAlive(),
                    entity.fallDistance
            );
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT,
                    "pos=%s,velocity=%s,box=%s,pose=%s,health=%.3f,absorption=%.3f,"
                            + "onGround=%s,alive=%s,fallDistance=%.3f",
                    vector(position), vector(velocity), box(boundingBox), pose, health, absorption,
                    onGround, alive, fallDistance);
        }
    }

    private record DamageObservation(ServerLevel level, int entityId, UUID maidId,
                                     String attemptId, long eventTick, float healthBefore,
                                     float absorptionBefore, float reportedDamage, String source) {
    }

    private record MaidTickBoundaryState(long gameTick, EntitySnapshot snapshot, String dodgeState) {
    }

    private static final class DodgeAttempt {
        private final long id;
        private final ServerLevel level;
        private final int entityId;
        private final UUID maidId;
        private final DodgeAction action;
        private final long startTick;
        private final EntitySnapshot start;
        private final List<Projectile> projectiles;
        private final Set<UUID> threatIds;
        private final String actionContext;
        private int impactCount;
        private int untrackedImpactCount;
        private boolean directHit;
        private int attackEvents;
        private float attackAmount;
        private int hurtEvents;
        private float hurtAmount;
        private int damageEvents;
        private float damageAmount;
        private boolean died;
        private String lastDamageSource = "none";

        private DodgeAttempt(long id, ServerLevel level, int entityId, UUID maidId,
                             DodgeAction action, long startTick, EntitySnapshot start,
                             List<Projectile> projectiles, Set<UUID> threatIds,
                             String actionContext) {
            this.id = id;
            this.level = level;
            this.entityId = entityId;
            this.maidId = maidId;
            this.action = action;
            this.startTick = startTick;
            this.start = start;
            this.projectiles = List.copyOf(projectiles);
            this.threatIds = Set.copyOf(threatIds);
            this.actionContext = actionContext;
        }
    }

    private record DuckState(ServerLevel level, int entityId, int ticks, long startedTick) {
    }

    private record HorizontalDodgeState(ServerLevel level, int entityId, Vec3 direction,
                                        Vec3 targetPosition, int ticks) {
    }
}
