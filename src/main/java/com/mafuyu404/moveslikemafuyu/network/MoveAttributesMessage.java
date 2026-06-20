package com.mafuyu404.moveslikemafuyu.network;

import net.minecraft.nbt.CompoundTag;

public class MoveAttributesMessage {
    private final CompoundTag tag;

    public MoveAttributesMessage(CompoundTag tag) {
        this.tag = tag;
    }

    public CompoundTag tag() {
        return tag;
    }
}
