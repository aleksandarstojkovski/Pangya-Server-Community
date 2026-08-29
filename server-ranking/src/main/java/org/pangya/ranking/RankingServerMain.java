package org.pangya.ranking;

import org.pangya.network.AppConfig;

public final class RankingServerMain {

    public static void main(String[] args) {
        RankingRuntime.runBlocking(AppConfig.load("application.yml"));
    }
}
