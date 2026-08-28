package org.pangya.network.auth;

import org.pangya.protocol.auth.AuthS2s;

/** Child→Auth replies ({@code unit_auth_server_connect} request writers). */
@FunctionalInterface
public interface AuthOutbound {

    void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info);
}
