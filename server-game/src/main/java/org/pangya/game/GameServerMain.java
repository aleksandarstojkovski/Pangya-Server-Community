package org.pangya.game;

import org.pangya.network.AppConfig;

public final class GameServerMain {

    public static void main(String[] args) {
        GameRuntime.runBlocking(AppConfig.load("application.yml"));
    }
}
