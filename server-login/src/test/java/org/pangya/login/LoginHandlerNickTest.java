package org.pangya.login;

import org.junit.jupiter.api.Test;
import org.pangya.protocol.login.LoginPackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginHandlerNickTest {

    @Test
    void nickCheckMatchesJpFlags() {
        assertEquals(LoginPackets.NICK_OK, LoginHandler.checkNick("NewNick", "newuser", 0, false));
        assertEquals(LoginPackets.NICK_SAME_AS_ID, LoginHandler.checkNick("newuser", "newuser", 0, false));
        assertEquals(LoginPackets.NICK_IN_USE, LoginHandler.checkNick("Taken", "newuser", 0, true));
        assertEquals(LoginPackets.NICK_INCORRECT, LoginHandler.checkNick("ab", "newuser", 0, false));
        assertEquals(LoginPackets.NICK_EMPTY, LoginHandler.checkNick("has space", "newuser", 0, false));
        assertEquals(LoginPackets.NICK_BAD_WORD, LoginHandler.checkNick("xxGMyy", "newuser", 0, false));
        assertEquals(LoginPackets.NICK_OK, LoginHandler.checkNick("xxGMyy", "newuser", 4, false));
        assertEquals(LoginPackets.NICK_INCORRECT, LoginHandler.checkNick("nick!", "newuser", 0, false));
    }
}
