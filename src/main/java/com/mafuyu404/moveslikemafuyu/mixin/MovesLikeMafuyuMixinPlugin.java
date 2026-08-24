package com.mafuyu404.moveslikemafuyu.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class MovesLikeMafuyuMixinPlugin implements IMixinConfigPlugin {
    private static final String MAID_MOD_ID = "touhou_little_maid";
    private static final Set<String> MAID_MIXINS = Set.of(
            "MaidDodgeDimensionsMixin",
            "MaidDodgeRendererMixin",
            "MaidDodgeTickMixin",
            "MoveControlMixin"
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simpleMixinName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        return !MAID_MIXINS.contains(simpleMixinName) || isMaidModLoaded();
    }

    private static boolean isMaidModLoaded() {
        LoadingModList modList = FMLLoader.getLoadingModList();
        return modList != null && modList.getModFileById(MAID_MOD_ID) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
    }
}
