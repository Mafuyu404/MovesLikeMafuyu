package com.mafuyu404.moveslikemafuyu.network;

public class TagMessage {
    private final String tag;
    private final boolean state;

    public TagMessage(String tag, boolean state) {
        this.tag = tag;
        this.state = state;
    }

    public String tag() {
        return tag;
    }

    public boolean state() {
        return state;
    }
}
