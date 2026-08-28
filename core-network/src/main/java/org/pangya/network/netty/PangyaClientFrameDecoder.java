package org.pangya.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/** Client → server frames: 5-byte header, length field at offset 1 (LE) = frameLen - 4. */
public final class PangyaClientFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) {
            return;
        }
        int reader = in.readerIndex();
        int lenField = in.getUnsignedShortLE(reader + 1);
        int frameLen = lenField + 4;
        if (frameLen < 5 || in.readableBytes() < frameLen) {
            return;
        }
        out.add(in.readRetainedSlice(frameLen));
    }
}
