package com.mafuyu404.moveslikemafuyu.network;

public class CrawlPacket {
    private final boolean start;

    public CrawlPacket(boolean start) {
        this.start = start;
    }

    public boolean start() {
        return start;
    }
}
