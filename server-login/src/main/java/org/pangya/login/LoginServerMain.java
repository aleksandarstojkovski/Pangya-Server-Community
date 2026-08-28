package org.pangya.login;

import org.pangya.network.AppConfig;

public final class LoginServerMain {

    public static void main(String[] args) {
        LoginRuntime.runBlocking(AppConfig.load("application.yml"));
    }
}
