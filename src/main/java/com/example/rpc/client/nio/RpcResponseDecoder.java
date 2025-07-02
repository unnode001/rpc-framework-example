package com.example.rpc.client.nio;

import java.util.List;

import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.serialization.Serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * RPC 响应解码器
 */
public class RpcResponseDecoder extends ByteToMessageDecoder {

    private final Serializer serializer;

    public RpcResponseDecoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return; // 长度字段还没有完整接收
        }

        // 读取数据长度
        in.markReaderIndex();
        int dataLength = in.readInt();

        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex(); // 数据还没有完整接收，重置读取位置
            return;
        }

        // 读取数据
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        // 反序列化
        RpcResponse response = serializer.deserialize(data, RpcResponse.class);
        out.add(response);
    }
}
