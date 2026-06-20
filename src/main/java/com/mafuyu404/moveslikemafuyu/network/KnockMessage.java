package com.mafuyu404.moveslikemafuyu.network;

import java.util.ArrayList;
import java.util.List;

public class KnockMessage {
    private final List<Integer> entityIds;

    public KnockMessage(ArrayList<Integer> entityIds) {
        this.entityIds = List.copyOf(entityIds);
    }

    public List<Integer> entityIds() {
        return entityIds;
    }
}
