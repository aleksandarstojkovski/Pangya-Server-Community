package org.pangya.messenger;

import org.pangya.network.AppConfig;
import org.pangya.network.PangyaProcess;

public final class MessengerServerMain {

    public static void main(String[] args) {
        PangyaProcess.runBlocking(AppConfig.load("application.yml"));
    }
}
