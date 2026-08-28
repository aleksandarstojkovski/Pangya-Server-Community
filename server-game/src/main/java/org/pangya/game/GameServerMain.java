package org.pangya.game;

import org.pangya.network.AppConfig;
import org.pangya.network.PangyaProcess;

public final class GameServerMain {

    public static void main(String[] args) {
        PangyaProcess.runBlocking(AppConfig.load("application.yml"));
    }
}
