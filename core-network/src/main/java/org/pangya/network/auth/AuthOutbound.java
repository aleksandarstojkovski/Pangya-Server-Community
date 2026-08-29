package org.pangya.network.auth;

import org.pangya.protocol.auth.AuthS2s;

/** Child→Auth requests/replies ({@code unit_auth_server_connect}). */
public interface AuthOutbound {

    /** C# {@code m_unit_connect.isLive()}. */
    default boolean isLive() {
        return false;
    }

    /** C# {@code getInfoPlayerOnline} — Child→Auth {@code 0x04}. */
    default void requestInfoPlayerOnline(int gameServerUid, long playerUid) {}

    /** C# {@code sendInfoPlayerOnline} — Child→Auth {@code 0x05}. */
    void sendInfoPlayerOnline(int reqServerUid, AuthS2s.AuthServerPlayerInfo info);

    /** C# {@code sendConfirmDisconnectPlayer} — Child→Auth {@code 0x03}. */
    default void sendConfirmDisconnectPlayer(long serverUid, long playerUid) {}
}
