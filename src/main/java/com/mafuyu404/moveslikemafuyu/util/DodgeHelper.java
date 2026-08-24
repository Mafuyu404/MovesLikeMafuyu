package com.mafuyu404.moveslikemafuyu.util;

import com.mafuyu404.moveslikemafuyu.Config;
import com.mafuyu404.moveslikemafuyu.mixin.AbstractArrowAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 侧中立的自动闪避核心逻辑：投射物预测、候选方向生成、安全评估与水平闪避执行。
 * <p>
 * 玩家路径（{@link com.mafuyu404.moveslikemafuyu.event.AutoDodgeEvent}，客户端）和
 * 女仆路径（服务端）共用这里的方法，避免两处重复且飘移。方法只依赖实体通用能力，
 * 不引用任何客户端专用类，因此可以安全地在服务端使用。
 */
public final class DodgeHelper {
    private static final int PREDICT_TICKS = 10;
    private static final int DODGE_PATH_CHECK_STEPS = 4;
    private static final int DODGE_PATH_PREDICT_TICKS = 4;
    private static final double DODGE_LEAD_TICKS = 5.5;
    private static final double MIN_PROJECTILE_SPEED_SQR = 0.0025;
    private static final double DODGE_TARGET_SPEED = 0.475;
    private static final double HIT_MARGIN = 0.08;
    private static final double PRONE_HEIGHT = 0.62;

    private DodgeHelper() {
    }

    public static List<Projectile> getProjectilesInReach(LivingEntity entity) {
        return getProjectilesInReach(entity, null);
    }

    public static List<Projectile> getProjectilesInReach(LivingEntity entity, Projectile extraProjectile) {
        // 女仆等非玩家实体没有 forge:block_reach 属性，直接 getAttributeValue 会抛异常，
        // 先查属性实例，没有就用固定探测距离。
        AttributeInstance reachAttribute = entity.getAttribute(ForgeMod.BLOCK_REACH.get());
        double reach = reachAttribute == null ? 3.0 : Math.max(3.0, reachAttribute.getValue());
        double maxLeadDistance = reach + 12.0;
        AABB searchBox = entity.getBoundingBox().inflate(maxLeadDistance);
        Vec3 entityCenter = entity.getBoundingBox().getCenter();
        ArrayList<Projectile> projectiles = new ArrayList<>(entity.level().getEntitiesOfClass(Projectile.class, searchBox, projectile ->
                isProjectileInReach(entity, projectile, entityCenter, reach)
        ));
        if (extraProjectile != null
                && isProjectileInReach(entity, extraProjectile, entityCenter, reach)
                && !projectiles.contains(extraProjectile)) {
            projectiles.add(extraProjectile);
        }
        return projectiles;
    }

    private static boolean isProjectileInReach(LivingEntity target, Projectile projectile,
                                               Vec3 targetCenter, double reach) {
        return projectile.isAlive()
                && projectile.getOwner() != target
                && !isEmbeddedArrow(projectile)
                && projectile.getDeltaMovement().lengthSqr() >= MIN_PROJECTILE_SPEED_SQR
                && projectile.position().distanceTo(targetCenter)
                <= reach + projectile.getDeltaMovement().length() * DODGE_LEAD_TICKS
                && (!Config.AUTO_DODGE_ONLY_DAMAGING_PROJECTILES.get()
                || assessProjectileDamage(projectile, target).canDamage());
    }

    private static boolean isEmbeddedArrow(Projectile projectile) {
        return projectile instanceof AbstractArrowAccessor accessor
                && accessor.movesLikeMafuyu$isInGround();
    }

    /**
     * Determines whether a projectile can damage this target without invoking hurt or posting Forge events.
     * Unknown mod projectiles remain threats because their impact behavior cannot be inspected generically.
     */
    public static ProjectileDamageAssessment assessProjectileDamage(Projectile projectile, LivingEntity target) {
        if (projectile instanceof FishingHook) {
            return nonDamaging("fishing_hook_only_hooks_entities");
        }
        if (projectile instanceof ThrownEgg) {
            return nonDamaging("egg_deals_zero_damage");
        }
        if (projectile instanceof ThrownExperienceBottle) {
            return nonDamaging("experience_bottle_only_spawns_experience");
        }
        if (projectile instanceof ThrownEnderpearl) {
            return nonDamaging("ender_pearl_deals_zero_impact_damage");
        }
        if (projectile instanceof Snowball snowball) {
            if (!(target instanceof Blaze)) {
                return nonDamaging("snowball_only_damages_blazes");
            }
            return assessDamageSource(target,
                    snowball.damageSources().thrown(snowball, snowball.getOwner()),
                    "snowball_damages_blaze");
        }
        if (projectile instanceof ThrownPotion potion) {
            return assessPotionDamage(potion, target);
        }
        if (projectile instanceof FireworkRocketEntity firework) {
            ItemStack stack = firework.getItem();
            CompoundTag fireworks = stack.getTagElement("Fireworks");
            ListTag explosions = fireworks == null ? null : fireworks.getList("Explosions", Tag.TAG_COMPOUND);
            if (explosions == null || explosions.isEmpty()) {
                return nonDamaging("firework_has_no_explosions");
            }
            return assessDamageSource(target,
                    firework.damageSources().fireworks(firework, firework.getOwner()),
                    "exploding_firework");
        }
        if (projectile instanceof AbstractArrow arrow) {
            if (isEmbeddedArrow(arrow)) {
                return nonDamaging("arrow_is_in_ground");
            }
            double baseImpactDamage = arrow.getDeltaMovement().length() * arrow.getBaseDamage();
            if (baseImpactDamage <= 0.0 && !arrow.isCritArrow()) {
                return nonDamaging("arrow_has_zero_impact_damage");
            }
            Entity owner = arrow.getOwner();
            DamageSource source = arrow instanceof ThrownTrident
                    ? arrow.damageSources().trident(arrow, owner == null ? arrow : owner)
                    : arrow.damageSources().arrow(arrow, owner == null ? arrow : owner);
            return assessDamageSource(target, source, "damaging_arrow");
        }
        if (projectile instanceof LlamaSpit spit) {
            if (!(spit.getOwner() instanceof LivingEntity owner)) {
                return nonDamaging("llama_spit_has_no_living_owner");
            }
            return assessDamageSource(target,
                    spit.damageSources().mobProjectile(spit, owner),
                    "llama_spit");
        }
        if (projectile instanceof ShulkerBullet bullet) {
            LivingEntity owner = bullet.getOwner() instanceof LivingEntity living ? living : null;
            return assessDamageSource(target,
                    bullet.damageSources().mobProjectile(bullet, owner),
                    "shulker_bullet");
        }
        if (projectile instanceof DragonFireball) {
            if (target.isInvertedHealAndHarm()) {
                return nonDamaging("dragon_breath_harm_effect_heals_undead");
            }
            return assessDamageSource(target, target.damageSources().magic(), "dragon_breath_harm_effect");
        }
        if (projectile instanceof WitherSkull witherSkull) {
            Entity owner = witherSkull.getOwner();
            DamageSource source = owner instanceof LivingEntity
                    ? witherSkull.damageSources().witherSkull(witherSkull, owner)
                    : witherSkull.damageSources().magic();
            return assessDamageSource(target, source, "wither_skull");
        }
        if (projectile instanceof Fireball fireball) {
            return assessDamageSource(target,
                    fireball.damageSources().fireball(fireball, fireball.getOwner()),
                    "fireball");
        }
        if (projectile instanceof AbstractHurtingProjectile) {
            return damaging("unknown_hurting_projectile_assumed_damaging");
        }
        return damaging("unknown_mod_projectile_assumed_damaging");
    }

    private static ProjectileDamageAssessment assessPotionDamage(ThrownPotion potion, LivingEntity target) {
        ItemStack stack = potion.getItem();
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        if (effects.isEmpty()) {
            if (PotionUtils.getPotion(stack) == Potions.WATER && target.isSensitiveToWater()) {
                return assessDamageSource(target,
                        potion.damageSources().indirectMagic(potion, potion.getOwner()),
                        "water_potion_damages_water_sensitive_target");
            }
            return nonDamaging("potion_has_no_damaging_effect");
        }
        if (!target.isAffectedByPotions()) {
            return nonDamaging("target_is_not_affected_by_potions");
        }

        for (MobEffectInstance effectInstance : effects) {
            MobEffect effect = effectInstance.getEffect();
            if ((effect == MobEffects.HARM && !target.isInvertedHealAndHarm())
                    || (effect == MobEffects.HEAL && target.isInvertedHealAndHarm())) {
                return assessDamageSource(target,
                        potion.damageSources().indirectMagic(potion, potion.getOwner()),
                        "instant_potion_damage");
            }
            if (effect == MobEffects.POISON && target.getHealth() > 1.0F
                    && target.getMobType() != MobType.UNDEAD) {
                return assessDamageSource(target, target.damageSources().magic(), "poison_potion");
            }
            if (effect == MobEffects.WITHER) {
                return assessDamageSource(target, target.damageSources().wither(), "wither_potion");
            }
            if (!"minecraft".equals(BuiltInRegistries.MOB_EFFECT.getKey(effect).getNamespace())
                    && effect.getCategory() == MobEffectCategory.HARMFUL) {
                return damaging("unknown_harmful_mod_potion_effect_assumed_damaging");
            }
        }
        return nonDamaging("potion_effects_do_not_deal_damage");
    }

    private static ProjectileDamageAssessment assessDamageSource(LivingEntity target, DamageSource source,
                                                                  String damagingReason) {
        if (target.isInvulnerableTo(source)) {
            return nonDamaging("target_is_invulnerable_to_damage_source");
        }
        if (source.is(DamageTypeTags.IS_FIRE) && target.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return nonDamaging("target_has_fire_resistance");
        }
        if (target instanceof Player victim && source.getEntity() instanceof Player attacker
                && !victim.canHarmPlayer(attacker)) {
            return nonDamaging("player_friendly_fire_is_disabled");
        }
        if (target.isDamageSourceBlocked(source)) {
            return nonDamaging("target_is_currently_blocking_damage_source");
        }
        return damaging(damagingReason);
    }

    private static ProjectileDamageAssessment damaging(String reason) {
        return new ProjectileDamageAssessment(true, reason);
    }

    private static ProjectileDamageAssessment nonDamaging(String reason) {
        return new ProjectileDamageAssessment(false, reason);
    }

    public static List<Candidate> buildDodgeCandidates(LivingEntity entity, List<Threat> threats) {
        ArrayList<Vec3> directions = new ArrayList<>();
        for (Threat threat : threats) {
            Vec3 preferred = preferredDodgeDirection(entity, threat);
            addDirection(directions, preferred);
            addDirection(directions, preferred.scale(-1));
        }
        Vec3 look = new Vec3(entity.getLookAngle().x, 0, entity.getLookAngle().z);
        if (look.lengthSqr() > 1.0E-6) {
            look = look.normalize();
            addDirection(directions, new Vec3(-look.z, 0, look.x));
            addDirection(directions, new Vec3(look.z, 0, -look.x));
            addDirection(directions, look);
            addDirection(directions, look.scale(-1));
        }
        Vec3 motion = new Vec3(entity.getDeltaMovement().x, 0, entity.getDeltaMovement().z);
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

    public static Optional<Threat> predictHit(Projectile projectile, AABB targetBox) {
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
                double segmentDistance = position.distanceTo(hit.get());
                double hitDistance = traveled + segmentDistance;
                double segmentFactor = segmentDistance / Math.max(motion.length(), 1.0E-6);
                return Optional.of(new Threat(
                        projectile,
                        hit.get(),
                        hitDistance / Math.max(totalLength, 1.0E-6),
                        tick + segmentFactor,
                        hitDistance
                ));
            }
            traveled += position.distanceTo(next);
            position = next;
            motion = nextProjectileMotion(projectile, motion);
        }
        return Optional.empty();
    }

    public static List<PredictionStep> tracePredictionPath(Projectile projectile, AABB targetBox) {
        Vec3 motion = projectile.getDeltaMovement();
        if (motion.lengthSqr() < MIN_PROJECTILE_SPEED_SQR) return List.of();
        AABB expandedTarget = targetBox.inflate(
                projectile.getBbWidth() * 0.5 + HIT_MARGIN,
                HIT_MARGIN,
                projectile.getBbWidth() * 0.5 + HIT_MARGIN
        );
        List<PredictionStep> steps = new ArrayList<>(PREDICT_TICKS);
        Vec3 position = projectile.position();
        for (int tick = 0; tick < PREDICT_TICKS; tick++) {
            Vec3 next = position.add(motion);
            Vec3 motionAfter = nextProjectileMotion(projectile, motion);
            steps.add(new PredictionStep(
                    tick, position, next, motion, motionAfter, expandedTarget.clip(position, next)
            ));
            position = next;
            motion = motionAfter;
        }
        return steps;
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

    public static Vec3 preferredDodgeDirection(LivingEntity entity, Threat threat) {
        Vec3 velocity = threat.projectile.getDeltaMovement();
        Vec3 forward = new Vec3(velocity.x, 0, velocity.z);
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(entity.getLookAngle().x, 0, entity.getLookAngle().z);
        }
        if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0, 0, 1);
        Vec3 perpendicular = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 entityCenter = entity.getBoundingBox().getCenter();
        Vec3 projectileToEntity = entityCenter.subtract(threat.projectile.position());
        double side = projectileToEntity.dot(perpendicular);
        return side >= 0 ? perpendicular : perpendicular.scale(-1);
    }

    public static boolean canHorizontalDodge(LivingEntity entity, Candidate candidate, List<Projectile> projectiles) {
        AABB targetBox = targetBox(entity, candidate);
        return entity.level().noCollision(entity, targetBox)
                && hasLandingSupport(entity, targetBox)
                && isSafeDuringDodge(entity, candidate, projectiles);
    }

    public static boolean hasLandingSupport(LivingEntity entity, AABB targetBox) {
        Level level = entity.level();
        if (!level.noCollision(entity, targetBox.move(0, -0.12, 0))) return true;
        BlockPos belowCenter = BlockPos.containing(targetBox.getCenter().x, targetBox.minY - 0.2, targetBox.getCenter().z);
        return !level.getBlockState(belowCenter).getCollisionShape(level, belowCenter).isEmpty();
    }

    public static boolean isSafeFromProjectiles(AABB targetBox, List<Projectile> projectiles) {
        return projectiles.stream().noneMatch(projectile -> predictHit(projectile, targetBox).isPresent());
    }

    public static int dodgeScore(LivingEntity entity, Candidate candidate, List<Projectile> projectiles) {
        AABB targetBox = targetBox(entity, candidate);
        int unsafeProjectiles = 0;
        for (Projectile projectile : projectiles) {
            if (predictHit(projectile, targetBox).isPresent()) unsafeProjectiles++;
        }
        int score = -unsafeProjectiles * 100;
        if (entity.level().noCollision(entity, targetBox)) score += 10;
        if (hasLandingSupport(entity, targetBox)) score += 10;
        if (isSafeDuringDodge(entity, candidate, projectiles)) score += 40;
        score += projectileClearanceScore(targetBox, projectiles);
        score += (int) (candidate.distance * 2);
        return score;
    }

    public static AABB targetBox(LivingEntity entity, Candidate candidate) {
        return activeDodgeBox(entity).move(candidate.direction.normalize().scale(candidate.distance));
    }

    public static AABB activeDodgeBox(LivingEntity entity) {
        if (entity.getTags().contains("auto_craw")) {
            return proneBox(entity);
        }
        return entity.getBoundingBox();
    }

    public static AABB proneBox(LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        return new AABB(box.minX, box.minY, box.minZ, box.maxX, box.minY + PRONE_HEIGHT, box.maxZ);
    }

    public static int projectileClearanceScore(AABB targetBox, List<Projectile> projectiles) {
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

    public static boolean isSafeDuringDodge(LivingEntity entity, Candidate candidate, List<Projectile> projectiles) {
        return isSafeDuringDodge(entity, activeDodgeBox(entity), candidate, projectiles);
    }

    public static List<DodgePathStep> traceDodgePath(LivingEntity entity, Candidate candidate,
                                                     List<Projectile> projectiles) {
        AABB baseBox = activeDodgeBox(entity);
        Vec3 horizontal = candidate.direction.normalize();
        Vec3 simulatedMotion = entity.getDeltaMovement().add(horizontal.scale(dodgeBoost(entity, candidate)));
        double speed = new Vec3(simulatedMotion.x, 0, simulatedMotion.z).length();
        List<DodgePathStep> steps = new ArrayList<>();
        if (speed < 1.0E-6) {
            List<Integer> unsafe = new ArrayList<>();
            for (Projectile projectile : projectiles) {
                if (predictHit(projectile, baseBox).isPresent()) unsafe.add(projectile.getId());
            }
            steps.add(new DodgePathStep(0, 0.0, 0.0, baseBox,
                    entity.level().noCollision(entity, baseBox), List.copyOf(unsafe)));
            return steps;
        }

        for (int step = 1; step <= DODGE_PATH_CHECK_STEPS; step++) {
            double distance = candidate.distance * step / DODGE_PATH_CHECK_STEPS;
            double arrivalTick = Math.max(1, distance / speed);
            AABB stepBox = baseBox.move(horizontal.scale(distance));
            List<Integer> unsafe = new ArrayList<>();
            for (Projectile projectile : projectiles) {
                if (predictHitNearTick(projectile, stepBox, arrivalTick).isPresent()) {
                    unsafe.add(projectile.getId());
                }
            }
            steps.add(new DodgePathStep(
                    step, distance, arrivalTick, stepBox,
                    entity.level().noCollision(entity, stepBox), List.copyOf(unsafe)
            ));
        }
        return steps;
    }

    public static boolean isSafeDuringDodge(LivingEntity entity, AABB baseBox, Candidate candidate, List<Projectile> projectiles) {
        Vec3 horizontal = candidate.direction.normalize();
        Vec3 simulatedMotion = entity.getDeltaMovement().add(horizontal.scale(dodgeBoost(entity, candidate)));
        Vec3 horizontalMotion = new Vec3(simulatedMotion.x, 0, simulatedMotion.z);
        double speed = horizontalMotion.length();
        if (speed < 1.0E-6) return isSafeFromProjectiles(baseBox, projectiles);

        for (int step = 1; step <= DODGE_PATH_CHECK_STEPS; step++) {
            double distance = candidate.distance * step / DODGE_PATH_CHECK_STEPS;
            double arrivalTick = Math.max(1, distance / speed);
            AABB stepBox = baseBox.move(horizontal.scale(distance));
            if (!entity.level().noCollision(entity, stepBox)) return false;
            if (!isSafeAtDodgeStep(stepBox, projectiles, arrivalTick)) return false;
        }
        return true;
    }

    public static boolean isSafeAtDodgeStep(AABB targetBox, List<Projectile> projectiles, double arrivalTick) {
        for (Projectile projectile : projectiles) {
            if (predictHitNearTick(projectile, targetBox, arrivalTick).isPresent()) return false;
        }
        return true;
    }

    public static Optional<Vec3> predictHitNearTick(Projectile projectile, AABB targetBox, double arrivalTick) {
        Vec3 velocity = projectile.getDeltaMovement();
        if (velocity.lengthSqr() < MIN_PROJECTILE_SPEED_SQR) return Optional.empty();
        AABB expandedTarget = targetBox.inflate(projectile.getBbWidth() * 0.5 + HIT_MARGIN, HIT_MARGIN, projectile.getBbWidth() * 0.5 + HIT_MARGIN);
        Vec3 position = projectile.position();
        Vec3 motion = velocity;
        int minTick = Math.max(0, (int) Math.floor(arrivalTick) - 1);
        int maxTick = Math.min(PREDICT_TICKS, minTick + DODGE_PATH_PREDICT_TICKS);
        for (int tick = 0; tick < maxTick; tick++) {
            Vec3 next = position.add(motion);
            if (tick >= minTick) {
                Optional<Vec3> hit = expandedTarget.clip(position, next);
                if (hit.isPresent()) return hit;
            }
            position = next;
            motion = nextProjectileMotion(projectile, motion);
        }
        return Optional.empty();
    }

    public static void dodge(LivingEntity entity, Candidate candidate) {
        Vec3 horizontal = candidate.direction.normalize();
        Vec3 motion = entity.getDeltaMovement();
        double boost = dodgeBoost(entity, candidate);
        entity.setDeltaMovement(motion.add(horizontal.x * boost, 0, horizontal.z * boost));
    }

    public static double dodgeBoost(LivingEntity entity, Candidate candidate) {
        Vec3 horizontal = candidate.direction.normalize();
        Vec3 motion = entity.getDeltaMovement();
        double currentSpeed = motion.x * horizontal.x + motion.z * horizontal.z;
        double targetSpeed = DODGE_TARGET_SPEED * Math.min(1.25, Math.max(0.8, candidate.distance));
        double boost = Math.max(0, targetSpeed - currentSpeed);
        return Math.min(boost, DODGE_TARGET_SPEED * 1.5);
    }

    public static void horizontalDodge(LivingEntity entity, Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6) horizontal = new Vec3(0, 0, 1);
        horizontal = horizontal.normalize();
        Vec3 motion = entity.getDeltaMovement();
        double currentSpeed = motion.x * horizontal.x + motion.z * horizontal.z;
        double boost = Math.max(0, DODGE_TARGET_SPEED - currentSpeed);
        entity.setDeltaMovement(motion.add(horizontal.x * boost, 0, horizontal.z * boost));
    }

    public record Threat(Projectile projectile, Vec3 hit, double timeFactor,
                         double hitTick, double hitDistance) {
    }

    public record PredictionStep(int tick, Vec3 start, Vec3 end, Vec3 motionBefore,
                                 Vec3 motionAfter, Optional<Vec3> intersection) {
    }

    public record DodgePathStep(int step, double distance, double arrivalTick, AABB box,
                                boolean collisionFree, List<Integer> unsafeProjectileIds) {
    }

    public record ProjectileDamageAssessment(boolean canDamage, String reason) {
    }

    public record Candidate(Vec3 direction, double distance) {
    }
}
