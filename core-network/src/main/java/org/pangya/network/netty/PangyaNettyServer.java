package org.pangya.network.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import org.pangya.network.ddos.IpDdosFilter;
import org.pangya.network.session.Session;
import org.pangya.network.session.SessionManager;
import org.pangya.protocol.packet.PacketIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public final class PangyaNettyServer implements AutoCloseable {

    public static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("pangyaSession");

    private static final Logger log = LoggerFactory.getLogger(PangyaNettyServer.class);

    private final ServerKind kind;
    private final SessionManager sessions;
    private final PangyaClientDecryptHandler.PacketSink sink;
    private final EventLoopGroup boss;
    private final EventLoopGroup worker;
    private Channel channel;

    public PangyaNettyServer(ServerKind kind, SessionManager sessions, PangyaClientDecryptHandler.PacketSink sink) {
        this.kind = kind;
        this.sessions = sessions;
        this.sink = sink;
        this.boss = new NioEventLoopGroup(1);
        this.worker = new NioEventLoopGroup();
    }

    public ChannelFuture bind(int port) {
        ServerBootstrap b = new ServerBootstrap();
        b.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        String ip = ((InetSocketAddress) ch.remoteAddress()).getAddress().getHostAddress();
                        IpDdosFilter ddos = sessions.ddos();
                        if (ddos.isBlocked(ip)) {
                            ch.close();
                            return;
                        }
                        ddos.onConnect(ip);
                        Session session = sessions.create(ch);
                        ch.attr(SESSION_KEY).set(session);
                        ch.closeFuture().addListener(f -> sessions.remove(session));
                        writeHello(ch, session);
                        ch.pipeline().addLast(new PangyaClientFrameDecoder());
                        ch.pipeline().addLast(new PangyaServerFrameEncoder(session.key()));
                        ch.pipeline().addLast(new PangyaClientDecryptHandler(session, sink));
                    }
                });
        ChannelFuture future = b.bind(port);
        future.syncUninterruptibly();
        channel = future.channel();
        log.info("pangya netty {} bound {}", kind, channel.localAddress());
        return future;
    }

    public int localPort() {
        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    private void writeHello(SocketChannel ch, Session session) {
        byte[] hello = switch (kind) {
            case LOGIN -> PacketIo.loginHello(session.key());
            case GAME -> PacketIo.gameHello(session.key(), session.ip());
            case AUTH -> PacketIo.makeRaw(PacketIo.concat(
                    PacketIo.opcode(0x00),
                    intLe(session.key()),
                    intLe(8888)
            ));
        };
        ch.writeAndFlush(ch.alloc().buffer(hello.length).writeBytes(hello));
    }

    private static byte[] intLe(int v) {
        return new byte[] {
                (byte) v, (byte) (v >>> 8), (byte) (v >>> 16), (byte) (v >>> 24)
        };
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close().syncUninterruptibly();
        }
        worker.shutdownGracefully();
        boss.shutdownGracefully();
    }
}
