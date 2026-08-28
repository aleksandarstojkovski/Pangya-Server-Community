package org.pangya.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/** Server → client frames: length field at offset 1 (LE) = frameLen - 3. */
public final class PangyaServerFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }
        int reader = in.readerIndex();
        int lenField = in.getUnsignedShortLE(reader + 1);
        int frameLen = lenField + 3;
        if (frameLen < 4 || in.readableBytes() < frameLen) {
            return;
        }
        out.add(in.readRetainedSlice(frameLen));
    }
}
