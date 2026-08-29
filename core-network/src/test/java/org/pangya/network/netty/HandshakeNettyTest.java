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
import org.pangya.protocol.crypto.Cipher;
import org.pangya.protocol.packet.PacketIo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandshakeNettyTest {

    @Test
    void loginClientReceivesHardcodedKeyFrame() throws Exception {
        ArrayBlockingQueue<byte[]> fromClient = new ArrayBlockingQueue<>(1);
        SessionManager sessions = new SessionManager(new IpDdosFilter());
        try (PangyaNettyServer server = new PangyaNettyServer(ServerKind.LOGIN, sessions, (s, p) -> fromClient.offer(p))) {
            server.bind(0);
            int port = server.localPort();

            EventLoopGroup group = new NioEventLoopGroup(1);
            ArrayBlockingQueue<byte[]> hello = new ArrayBlockingQueue<>(1);
            try {
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
                                        byte[] data = new byte[buf.readableBytes()];
                                        buf.readBytes(data);
                                        buf.release();
                                        hello.offer(data);
                                    }
                                });
                            }
                        });
                Channel ch = b.connect("127.0.0.1", port).sync().channel();
                byte[] frame = hello.poll(5, TimeUnit.SECONDS);
                assertNotNull(frame);
                assertEquals(14, frame.length);
                assertEquals(0x00, frame[0] & 0xff);
                assertEquals(0x0B, frame[1] & 0xff);
                int key = frame[6] & 0xff;
                assertTrue(key >= 0 && key < 16);
                assertArrayEquals(PacketIo.loginHello(key), frame);

                byte[] payload = new byte[] {0x01, 0x00, 't', 'e', 's', 't'};
                byte[] enc = Cipher.encryptClient(payload, key, 0);
                ch.writeAndFlush(ch.alloc().buffer(enc.length).writeBytes(enc)).sync();
                byte[] received = fromClient.poll(5, TimeUnit.SECONDS);
                assertArrayEquals(payload, received);
                ch.close().sync();
            } finally {
                group.shutdownGracefully();
            }
        }
    }
}
