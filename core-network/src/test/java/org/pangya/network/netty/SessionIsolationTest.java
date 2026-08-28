package org.pangya.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
import org.pangya.protocol.crypto.Cipher;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionIsolationTest {

    @Test
    void throwingHandlerDoesNotKillServer() throws Exception {
        AtomicInteger seen = new AtomicInteger();
        SessionManager sessions = new SessionManager(new IpDdosFilter());
        try (PangyaNettyServer server = new PangyaNettyServer(ServerKind.LOGIN, sessions, (s, p) -> {
            seen.incrementAndGet();
            throw new RuntimeException("boom");
        })) {
            server.bind(0);
            int port = server.localPort();
            connectAndSend(port);
            connectAndSend(port);
            assertEquals(2, seen.get());
            assertTrue(server.localPort() > 0);
        }
    }

    private static void connectAndSend(int port) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup(1);
        ArrayBlockingQueue<byte[]> hello = new ArrayBlockingQueue<>(1);
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            ByteBuf buf = (ByteBuf) msg;
                            byte[] data = new byte[buf.readableBytes()];
                            buf.readBytes(data);
                            buf.release();
                            hello.offer(data);
                        }
                    });
                }
            });
            var ch = b.connect("127.0.0.1", port).sync().channel();
            byte[] frame = hello.poll(5, TimeUnit.SECONDS);
            assertNotNull(frame);
            int key = frame[6] & 0xff;
            byte[] enc = Cipher.encryptClient(new byte[] {0x01, 0x00}, key, 0);
            ch.writeAndFlush(ch.alloc().buffer(enc.length).writeBytes(enc)).sync();
            Thread.sleep(200);
            ch.close().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
