package org.pangya.auth;

import org.pangya.db.LoginRepository;
import org.pangya.network.session.Session;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C# {@code AuthServer.requestAuthenticPlayer}.
 */
public final class AuthHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

    private final LoginRepository repo;

    public AuthHandler(LoginRepository repo) {
        this.repo = repo;
    }

    public void onPacket(Session session, byte[] plaintext) {
        if (plaintext.length < 2) {
            return;
        }
        PacketReader reader = new PacketReader(plaintext);
        int opcode = reader.opcode();
        if (opcode == AuthS2s.REGISTER) {
            authenticate(session, reader);
            return;
        }
        if (!session.authorized()) {
            log.warn("unauthorized auth opcode 0x{} oid={}", Integer.toHexString(opcode), session.oid());
            session.disconnect();
            return;
        }
        log.debug("auth opcode 0x{} from server uid={}", Integer.toHexString(opcode), session.player().uid);
    }

    private void authenticate(Session session, PacketReader reader) {
        AuthS2s.RegisterRequest req = AuthS2s.readRegister(reader);
        var stored = repo.authServerKey(req.uid());
        if (stored.isEmpty() || !stored.get().valid() || !stored.get().key().equals(req.key())) {
            log.warn("auth key mismatch serverUid={} key={}", req.uid(), req.key());
            session.disconnect();
            return;
        }
        repo.invalidateAuthServerKey(req.uid());
        session.player().uid = req.uid();
        session.player().id = req.name();
        session.player().tipo = req.tipo();
        session.setAuthorized(true);
        session.send(AuthS2s.registerAck(session.oid()));
        log.info("child registered tipo={} uid={} oid={}", req.tipo(), req.uid(), session.oid());
    }
}
