package org.pangya.auth;

import org.pangya.network.AppConfig;

public final class AuthServerMain {

    public static void main(String[] args) {
        AuthRuntime.runBlocking(AppConfig.load("application.yml"));
    }
}
