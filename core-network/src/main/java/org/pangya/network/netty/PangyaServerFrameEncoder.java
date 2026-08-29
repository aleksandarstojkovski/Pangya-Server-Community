package org.pangya.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.pangya.protocol.crypto.Cipher;

/** Server → client: MiniLZO + Cipher.ServerEncrypt with salt 0. */
public final class PangyaServerFrameEncoder extends MessageToByteEncoder<byte[]> {

    private final int key;

    public PangyaServerFrameEncoder(int key) {
        this.key = key;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) {
        out.writeBytes(Cipher.serverEncrypt(msg, key, 0));
    }
}
