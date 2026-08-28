package org.pangya.network.auth;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.pangya.network.AppConfig;
import org.pangya.network.netty.PangyaServerFrameDecoder;
import org.pangya.protocol.auth.AuthS2s;
import org.pangya.protocol.crypto.Cipher;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * C# {@code unit_auth_server_connect}: TCP to Auth, first packet raw, then Cipher.
 * Reconnect uses exponential backoff on a virtual thread (never on the Netty event loop).
 */
public final class AuthServerConnector implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AuthServerConnector.class);

    public interface AuthKeyIssuer {
        String newKey(int serverUid);
    }

    private final AppConfig config;
    private final AuthKeyIssuer keys;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger assignedOid = new AtomicInteger(-1);
    private final CountDownLatch registered = new CountDownLatch(1);
    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private volatile Channel channel;
    private Thread loop;

    public AuthServerConnector(AppConfig config, AuthKeyIssuer keys) {
        this.config = config;
        this.keys = keys;
    }

    public void start() {
        loop = Thread.ofVirtual().name("auth-connector-" + config.serverName()).start(this::reconnectLoop);
    }

    public boolean awaitRegistered(long timeout, TimeUnit unit) throws InterruptedException {
        return registered.await(timeout, unit);
    }

    public int oid() {
        return assignedOid.get();
    }

    private void reconnectLoop() {
        long delayMs = 1000;
        while (running.get()) {
            try {
                connectOnce();
                delayMs = 1000;
            } catch (Exception e) {
                if (running.get()) {
                    log.warn("auth connector reconnect in {}ms: {}", delayMs, e.toString());
                    sleep(delayMs);
                    delayMs = Math.min(delayMs * 2, 30_000);
                }
            }
        }
    }

    private void connectOnce() throws InterruptedException {
        Bootstrap b = new Bootstrap();
        CountDownLatch closed = new CountDownLatch(1);
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new PangyaServerFrameDecoder());
                        ch.pipeline().addLast(new Handler(closed));
                    }
                });
        Channel ch = b.connect(config.authHost(), config.authPort()).sync().channel();
        this.channel = ch;
        log.info("auth connector connected to {}:{}", config.authHost(), config.authPort());
        closed.await();
        if (running.get()) {
            throw new IllegalStateException("auth connection closed");
        }
    }

    private final class Handler extends ChannelInboundHandlerAdapter {
        private final CountDownLatch closed;
        private boolean first = true;
        private int key;

        Handler(CountDownLatch closed) {
            this.closed = closed;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buf = (ByteBuf) msg;
            byte[] frame = new byte[buf.readableBytes()];
            buf.readBytes(frame);
            buf.release();
            if (first) {
                first = false;
                byte[] payload = PacketIo.slice(frame, 4, frame.length - 4);
                PacketReader r = new PacketReader(payload);
                r.opcode();
                key = r.u32() & 0xff;
                int authUid = r.u32();
                log.info("auth first key={} authUid={}", key, authUid);
                String dbKey = keys.newKey(config.uid());
                byte[] plain = AuthS2s.register(
                        config.tipo(),
                        config.uid(),
                        config.serverName(),
                        dbKey,
                        config.clientVersion(),
                        config.packetVersion());
                byte[] enc = Cipher.encryptClient(plain, key, 0);
                ctx.writeAndFlush(ctx.alloc().buffer(enc.length).writeBytes(enc));
                return;
            }
            byte[] plain = Cipher.decryptServer(frame, key);
            PacketReader r = new PacketReader(plain);
            int opcode = r.opcode();
            if (opcode == AuthS2s.REGISTER_ACK) {
                int oid = r.u32();
                assignedOid.set(oid);
                registered.countDown();
                log.info("registered with auth oid={}", oid);
            } else {
                log.debug("auth s2s opcode=0x{}", Integer.toHexString(opcode));
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            closed.countDown();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.warn("auth connector error: {}", cause.toString());
            ctx.close();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
        if (loop != null) {
            loop.interrupt();
        }
    }
}
