package org.pangya.ranking;

import org.pangya.network.AppConfig;
import org.pangya.network.PangyaProcess;

public final class RankingServerMain {

    public static void main(String[] args) {
        PangyaProcess.runBlocking(AppConfig.load("application.yml"));
    }
}
