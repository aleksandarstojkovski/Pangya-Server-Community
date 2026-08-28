package org.pangya.network.netty;

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
import org.junit.jupiter.api.Test;
import org.pangya.network.ddos.IpDdosFilter;
import org.pangya.network.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionLoadIT {

    @Test
    void loginHellosHoldThousandsOfSessions() throws Exception {
        int target = Integer.parseInt(System.getProperty("pangya.load.sessions", "3000"));
        IpDdosFilter.Config ddos = new IpDdosFilter.Config();
        ddos.enabled = false;
        SessionManager sessions = new SessionManager(new IpDdosFilter(ddos));
        try (PangyaNettyServer server = new PangyaNettyServer(ServerKind.LOGIN, sessions, (s, p) -> {})) {
            server.bind(0);
            int port = server.localPort();
            EventLoopGroup group = new NioEventLoopGroup();
            CountDownLatch hellos = new CountDownLatch(target);
            AtomicInteger seen = new AtomicInteger();
            List<Channel> channels = new ArrayList<>(target);
            try {
                for (int i = 0; i < target; i++) {
                    Bootstrap b = new Bootstrap();
                    b.group(group)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) {
                                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                            ByteBuf buf = (ByteBuf) msg;
                                            buf.release();
                                            seen.incrementAndGet();
                                            hellos.countDown();
                                        }
                                    });
                                }
                            });
                    channels.add(b.connect("127.0.0.1", port).sync().channel());
                }
                assertTrue(hellos.await(60, TimeUnit.SECONDS), "hellos=" + seen.get() + " target=" + target);
                assertTrue(sessions.size() >= target, "sessions=" + sessions.size());
            } finally {
                for (Channel ch : channels) {
                    ch.close();
                }
                group.shutdownGracefully();
            }
        }
    }
}
