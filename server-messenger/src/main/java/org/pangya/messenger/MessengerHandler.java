package org.pangya.messenger;

import org.pangya.db.LoginRepository;
import org.pangya.network.session.PlayerContext;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.messenger.MessengerPackets;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GB {@code MessengerServer.requestLogin} + friend-list status packet.
 * Auth confirm is skipped when {@code auth.enabled=false}.
 */
public final class MessengerHandler {

    private static final Logger log = LoggerFactory.getLogger(MessengerHandler.class);

    private final LoginRepository repo;
    private final SessionManager sessions;

    public MessengerHandler(LoginRepository repo, SessionManager sessions) {
        this.repo = repo;
        this.sessions = sessions;
    }

    public void onPacket(Session session, byte[] plaintext) {
        if (plaintext.length < 2) {
            return;
        }
        PacketReader reader = new PacketReader(plaintext);
        int opcode = reader.opcode();
        switch (opcode) {
            case MessengerPackets.CLIENT_CONNECT -> requestLogin(session, reader);
            case MessengerPackets.CLIENT_REQ_USERINFO -> friendList(session);
            default -> log.debug("unhandled messenger opcode 0x{}", Integer.toHexString(opcode));
        }
    }

    private void requestLogin(Session session, PacketReader reader) {
        try {
            MessengerPackets.Login data = MessengerPackets.readLogin(reader);
            if (data.uid() == 0 || data.nickname() == null || data.nickname().isEmpty()) {
                session.send(MessengerPackets.loginFail());
                session.disconnect();
                return;
            }
            var info = repo.playerInfo(data.uid() & 0xffff_ffffL).orElse(null);
            if (info == null || !data.nickname().equals(info.nickname())) {
                session.send(MessengerPackets.loginFail());
                session.disconnect();
                return;
            }
            if (info.idState() != 0) {
                session.send(MessengerPackets.loginFail());
                session.disconnect();
                return;
            }
            PlayerContext pi = session.player();
            pi.uid = info.uid();
            pi.id = info.id();
            pi.nickname = info.nickname();
            sessions.disconnectOthersWithUid(pi.uid, session);
            session.setAuthorized(true);
            session.send(MessengerPackets.loginOk((int) pi.uid));
            log.info("messenger login nick={} uid={}", pi.nickname, pi.uid);
        } catch (RuntimeException e) {
            log.warn("messenger login failed: {}", e.toString());
            session.send(MessengerPackets.loginFail());
            session.disconnect();
        }
    }

    private void friendList(Session session) {
        if (!session.authorized()) {
            return;
        }
        PlayerContext pi = session.player();
        session.send(MessengerPackets.friendStatus(
                (int) pi.uid, MessengerPackets.STATE_ONLINE, MessengerPackets.emptyChannelPlayerInfo()));
    }
}
