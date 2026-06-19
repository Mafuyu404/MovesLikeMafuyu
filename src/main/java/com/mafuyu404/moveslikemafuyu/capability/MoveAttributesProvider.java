package com.mafuyu404.moveslikemafuyu.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class MoveAttributesProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerMoveAttributes attributes = new PlayerMoveAttributes();
    private final LazyOptional<PlayerMoveAttributes> optional = LazyOptional.of(() -> attributes);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ModCapabilities.PLAYER_MOVE_ATTRIBUTES ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return attributes.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        attributes.deserializeNBT(nbt);
    }
}
