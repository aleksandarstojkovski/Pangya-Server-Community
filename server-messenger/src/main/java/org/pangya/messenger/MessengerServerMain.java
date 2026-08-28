package org.pangya.messenger;

import org.pangya.network.AppConfig;

public final class MessengerServerMain {

    public static void main(String[] args) {
        MessengerRuntime.runBlocking(AppConfig.load("application.yml"));
    }
}
