package com.mini.rpc.handler;

import com.mini.rpc.common.MiniRpcRequestHolder;
import com.mini.rpc.protocol.MiniRpcProtocol;
import com.mini.rpc.protocol.MsgHeader;
import com.mini.rpc.protocol.MsgType;
import com.mini.rpc.protocol.ProtocolConstants;
import com.mini.rpc.serialization.SerializationTypeEnum;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * date: 2023/12/4
 * description:
 */
@Slf4j
public class HeartSendHandler extends SimpleChannelInboundHandler{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.warn("no match InboundHandler");
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.info("Client send beat-ping to");
            MiniRpcProtocol<Void> protocol = new MiniRpcProtocol();
            MsgHeader header = new MsgHeader();
            header.setMagic(ProtocolConstants.MAGIC);
            header.setVersion(ProtocolConstants.VERSION);
            header.setRequestId(MiniRpcRequestHolder.REQUEST_ID_GEN.incrementAndGet());
            header.setSerialization((byte) SerializationTypeEnum.HESSIAN.getType());
            header.setMsgType((byte) MsgType.HEARTBEAT.getType());
            header.setStatus((byte) 0x1);

            protocol.setHeader(header);
            ctx.channel().writeAndFlush(protocol).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
