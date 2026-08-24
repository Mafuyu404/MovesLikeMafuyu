package com.mafuyu404.moveslikemafuyu.event;

import com.mafuyu404.moveslikemafuyu.MovesLikeMafuyu;
import com.mafuyu404.moveslikemafuyu.compat.MaidCompat;
import com.mafuyu404.moveslikemafuyu.registry.ModEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

/**
 * 女仆自动闪避的测试模式（服务端），用 {@code /moveslikecommand testmaid on|off} 开关。
 * <p>
 * 开启后骷髅（含流浪者）统一瞄准附近最近的女仆，方便测试女仆对箭矢的自动闪避；
 * 女仆每秒自动获得 2 秒的自动闪避效果。关闭后恢复正常行为。
 */
@Mod.EventBusSubscriber(modid = MovesLikeMafuyu.MODID)
public class MaidTestMode {
    private static final double SKELETON_MAID_SEARCH_RANGE = 24.0;
    private static boolean enabled;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(MinecraftServer server, boolean value) {
        enabled = value;
        MaidAutoDodgeEvent.onTestModeChanged(server, value);
    }

    /**
     * 骷髅的 AI 不会主动给女仆设目标（原版骷髅只锁玩家），只靠
     * LivingChangeTargetEvent 永远等不到触发。ServerTickEvent 每个 tick
     * 触发两次（START 和 END），每次都在所有世界强制骷髅把目标设成最近的
     * 女仆，防止世界 tick 里 AI 清掉/换掉目标。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!enabled) return;
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                if (entity instanceof AbstractSkeleton skeleton) {
                    LivingEntity maid = nearestMaid(skeleton);
                    if (maid != null && skeleton.getTarget() != maid) {
                        skeleton.setTarget(maid);
                    }
                } else if (entity instanceof Mob mob && MaidCompat.isMaid(mob)) {
                    // 自动闪避效果剩余不足 1 秒时补一个 2 秒的（等效每 1 秒续上 2 秒，效果常驻）
                    MobEffectInstance effect = mob.getEffect(ModEffects.AUTO_DODGE.get());
                    if (effect == null || effect.getDuration() < 20) {
                        mob.addEffect(new MobEffectInstance(ModEffects.AUTO_DODGE.get(), 40, 0, false, true, true));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (!enabled) return;
        // 骷髅（含流浪者）改为瞄准最近的女仆；附近没有女仆时保持原目标
        if (event.getEntity() instanceof AbstractSkeleton skeleton) {
            LivingEntity maid = nearestMaid(skeleton);
            if (maid != null) {
                event.setNewTarget(maid);
            }
        }
    }

    private static LivingEntity nearestMaid(AbstractSkeleton skeleton) {
        Level level = skeleton.level();
        AABB box = skeleton.getBoundingBox().inflate(SKELETON_MAID_SEARCH_RANGE);
        List<LivingEntity> maids = level.getEntitiesOfClass(LivingEntity.class, box, MaidCompat::isMaid);
        if (maids.isEmpty()) return null;
        return maids.stream()
                .min(Comparator.comparingDouble(maid -> maid.distanceToSqr(skeleton)))
                .orElse(null);
    }
}
