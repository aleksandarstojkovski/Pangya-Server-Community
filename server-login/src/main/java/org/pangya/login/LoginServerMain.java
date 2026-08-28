package org.pangya.login;

import org.pangya.network.AppConfig;
import org.pangya.network.PangyaProcess;

public final class LoginServerMain {

    public static void main(String[] args) {
        PangyaProcess.runBlocking(AppConfig.load("application.yml"));
    }
}
