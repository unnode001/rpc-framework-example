package com.example.rpc.server.nio;

import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.serialization.Serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * RPC 响应编码器
 */
public class RpcResponseEncoder extends MessageToByteEncoder<RpcResponse> {

    private final Serializer serializer;

    public RpcResponseEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcResponse response, ByteBuf out) {
        // 序列化响应
        byte[] data = serializer.serialize(response);

        // 写入数据长度和数据
        out.writeInt(data.length);
        out.writeBytes(data);
    }
}
