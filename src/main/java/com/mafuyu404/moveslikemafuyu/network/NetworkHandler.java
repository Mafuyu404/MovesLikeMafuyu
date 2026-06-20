package com.mafuyu404.moveslikemafuyu.network;

public class NetworkHandler {
    public static final LocalChannel CHANNEL = new LocalChannel();

    public static void register() {
    }

    public static class LocalChannel {
        public void sendToServer(Object message) {
            // TODO: Port client-to-server packets to NeoForge custom payload networking.
        }
    }
}
