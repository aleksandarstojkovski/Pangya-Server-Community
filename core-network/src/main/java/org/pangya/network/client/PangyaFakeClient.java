package org.pangya.network.client;

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
import org.pangya.network.netty.PangyaServerFrameDecoder;
import org.pangya.protocol.crypto.Cipher;
import org.pangya.protocol.packet.PacketIo;
import org.pangya.protocol.packet.PacketReader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Fake Pangya TCP client for tests: reads Login's 14-byte hello, then ServerEncrypt frames.
 */
public final class PangyaFakeClient implements AutoCloseable {

    public enum HelloKind {
        LOGIN,
        GAME,
        AUTH,
        RANKING,
        MESSENGER
    }

    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private final BlockingQueue<byte[]> hellos = new ArrayBlockingQueue<>(4);
    private final BlockingQueue<byte[]> plains = new ArrayBlockingQueue<>(64);
    private volatile Channel channel;
    private volatile int key = -1;
    private volatile HelloKind kind = HelloKind.LOGIN;

    public PangyaFakeClient connect(String host, int port) throws InterruptedException {
        return connect(host, port, HelloKind.LOGIN);
    }

    /** @deprecated use {@link #connect(String, int, HelloKind)} */
    @Deprecated
    public PangyaFakeClient connect(String host, int port, boolean expectLoginHello) throws InterruptedException {
        return connect(host, port, expectLoginHello ? HelloKind.LOGIN : HelloKind.AUTH);
    }

    public PangyaFakeClient connect(String host, int port, HelloKind kind) throws InterruptedException {
        this.kind = kind;
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (kind == HelloKind.LOGIN) {
                            ch.pipeline().addLast(new LoginHelloThenFrames());
                        } else {
                            ch.pipeline().addLast(new PangyaServerFrameDecoder());
                            ch.pipeline().addLast(new ServerDecryptHandler());
                        }
                    }
                });
        channel = b.connect(host, port).sync().channel();
        return this;
    }

    public byte[] awaitHello(long timeout, TimeUnit unit) throws InterruptedException {
        byte[] hello = hellos.poll(timeout, unit);
        if (hello == null) {
            throw new IllegalStateException("no hello");
        }
        if (kind == HelloKind.LOGIN) {
            key = hello[6] & 0xff;
        } else {
            byte[] payload = PacketIo.slice(hello, 4, hello.length - 4);
            PacketReader r = new PacketReader(payload);
            int opcode = r.opcode();
            switch (kind) {
                case AUTH -> {
                    key = r.u32() & 0xff;
                }
                case RANKING -> {
                    key = r.u32() & 0xff;
                    if (opcode != 0x1388) {
                        throw new IllegalStateException(
                                "expected ranking hello 0x1388, got 0x" + Integer.toHexString(opcode));
                    }
                }
                case MESSENGER -> {
                    r.u8();
                    r.u8();
                    key = r.u32() & 0xff;
                    if (opcode != 0x2E) {
                        throw new IllegalStateException(
                                "expected messenger hello 0x2E, got 0x" + Integer.toHexString(opcode));
                    }
                }
                default -> {
                    r.u8();
                    r.u8();
                    key = r.u8();
                    if (opcode != 0x3F) {
                        throw new IllegalStateException(
                                "expected game hello 0x3F, got 0x" + Integer.toHexString(opcode));
                    }
                }
            }
        }
        return hello;
    }

    public int key() {
        return key;
    }

    public void sendPlain(byte[] plaintext) {
        if (key < 0) {
            throw new IllegalStateException("hello not received");
        }
        byte[] enc = Cipher.encryptClient(plaintext, key, 0);
        channel.writeAndFlush(channel.alloc().buffer(enc.length).writeBytes(enc));
    }

    public byte[] awaitPlain(long timeout, TimeUnit unit) throws InterruptedException {
        byte[] p = plains.poll(timeout, unit);
        if (p == null) {
            throw new IllegalStateException("no server packet");
        }
        return p;
    }

    public List<byte[]> drainPlain(long waitMs) throws InterruptedException {
        List<byte[]> out = new ArrayList<>();
        long deadline = System.currentTimeMillis() + waitMs;
        while (System.currentTimeMillis() < deadline) {
            byte[] p = plains.poll(50, TimeUnit.MILLISECONDS);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    private final class LoginHelloThenFrames extends ChannelInboundHandlerAdapter {
        private boolean helloDone;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buf = (ByteBuf) msg;
            try {
                if (!helloDone) {
                    if (buf.readableBytes() < 14) {
                        return;
                    }
                    byte[] hello = new byte[14];
                    buf.readBytes(hello);
                    helloDone = true;
                    hellos.offer(hello);
                    ctx.pipeline().addAfter(ctx.name(), "frames", new PangyaServerFrameDecoder());
                    ctx.pipeline().addAfter("frames", "decrypt", new ServerDecryptHandler());
                    ctx.pipeline().remove(this);
                    if (buf.isReadable()) {
                        ctx.fireChannelRead(buf.retain());
                    }
                    return;
                }
            } finally {
                buf.release();
            }
        }
    }

    private final class ServerDecryptHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buf = (ByteBuf) msg;
            byte[] frame = new byte[buf.readableBytes()];
            buf.readBytes(frame);
            buf.release();
            if (key < 0 && kind != HelloKind.LOGIN) {
                hellos.offer(frame);
                return;
            }
            plains.offer(Cipher.decryptServer(frame, key));
        }
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
    }
}
