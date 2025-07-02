package com.example.rpc.client.nio;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.serialization.Serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * RPC 请求编码器
 */
public class RpcRequestEncoder extends MessageToByteEncoder<RpcRequest> {

    private final Serializer serializer;

    public RpcRequestEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcRequest request, ByteBuf out) {
        // 序列化请求
        byte[] data = serializer.serialize(request);

        // 写入数据长度和数据
        out.writeInt(data.length);
        out.writeBytes(data);
    }
}
