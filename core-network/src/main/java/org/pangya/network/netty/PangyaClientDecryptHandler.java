package org.pangya.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.pangya.network.session.Session;
import org.pangya.protocol.crypto.Cipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decrypts a complete client frame on the Netty event loop, then dispatches
 * the plaintext to a virtual thread for blocking domain work.
 */
public final class PangyaClientDecryptHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(PangyaClientDecryptHandler.class);

    private final Session session;
    private final PacketSink sink;

    public interface PacketSink {
        void onPacket(Session session, byte[] plaintext);
    }

    public PangyaClientDecryptHandler(Session session, PacketSink sink) {
        this.session = session;
        this.sink = sink;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] frame = new byte[msg.readableBytes()];
        msg.readBytes(frame);
        byte[] plain = Cipher.decryptClient(frame, session.key());
        Thread.startVirtualThread(() -> {
            try {
                sink.onPacket(session, plain);
            } catch (RuntimeException e) {
                log.error("session {} handler failed", session.oid(), e);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("session {} error: {}", session.oid(), cause.toString());
        ctx.close();
    }
}
