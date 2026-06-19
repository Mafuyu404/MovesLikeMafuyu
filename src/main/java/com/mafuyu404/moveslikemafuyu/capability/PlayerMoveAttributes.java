package com.mafuyu404.moveslikemafuyu.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.EnumMap;
import java.util.Map;

public class PlayerMoveAttributes implements INBTSerializable<CompoundTag> {
    private final EnumMap<MoveAttribute, Double> overrides = new EnumMap<>(MoveAttribute.class);

    public Double getOverride(MoveAttribute attribute) {
        return overrides.get(attribute);
    }

    public void set(MoveAttribute attribute, double value) {
        overrides.put(attribute, attribute.isInteger() ? (double) Math.round(value) : value);
    }

    public void clear(MoveAttribute attribute) {
        overrides.remove(attribute);
    }

    public double getDouble(MoveAttribute attribute) {
        Double override = overrides.get(attribute);
        return override != null ? override : attribute.defaultValue();
    }

    public int getInt(MoveAttribute attribute) {
        return (int) Math.round(getDouble(attribute));
    }

    public void copyFrom(PlayerMoveAttributes other) {
        overrides.clear();
        overrides.putAll(other.overrides);
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<MoveAttribute, Double> entry : overrides.entrySet()) {
            tag.putDouble(entry.getKey().key(), entry.getValue());
        }
        return tag;
    }

    public void read(CompoundTag tag) {
        overrides.clear();
        for (MoveAttribute attribute : MoveAttribute.values()) {
            if (tag.contains(attribute.key())) {
                set(attribute, tag.getDouble(attribute.key()));
            }
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return write();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        read(nbt);
    }
}
