package com.mini.rpc.consumer;

import com.mini.rpc.codec.MiniRpcDecoder;
import com.mini.rpc.codec.MiniRpcEncoder;
import com.mini.rpc.common.MiniRpcRequest;
import com.mini.rpc.common.RpcServiceHelper;
import com.mini.rpc.common.ServiceMeta;
import com.mini.rpc.handler.HeartSendHandler;
import com.mini.rpc.handler.RpcResponseHandler;
import com.mini.rpc.protocol.MiniRpcProtocol;
import com.mini.rpc.provider.registry.RegistryService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RpcConsumer{
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    private Map<ServiceMeta, Channel> channelMap = new ConcurrentHashMap<>();

    private Map<ServiceMeta, Boolean> connectingChannel = new ConcurrentHashMap<>();

    private Lock lock = new ReentrantLock();

    private static volatile RpcConsumer instance;

    public static RpcConsumer getInstance() {
        if (instance == null) {
            synchronized (RpcConsumer.class) {
                if (instance == null) {
                    instance = new RpcConsumer();
                }
            }
        }
        return instance;
    }

    private RpcConsumer() {
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(4);
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>(){
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS))
                                .addLast(new MiniRpcEncoder())
                                .addLast(new MiniRpcDecoder())
                                .addLast(new RpcResponseHandler())
                                .addLast(new HeartSendHandler());
                    }
                });
    }


    public void sendRequest(MiniRpcProtocol<MiniRpcRequest> protocol, RegistryService registryService) throws Exception {
        MiniRpcRequest request = protocol.getBody();
        Object[] params = request.getParams();
        String serviceKey = RpcServiceHelper.buildServiceKey(request.getClassName(), request.getServiceVersion());

        int invokerHashCode = params.length > 0 ? params[0].hashCode() : serviceKey.hashCode();
        ServiceMeta serviceMetadata = registryService.discovery(serviceKey, invokerHashCode);

        if (serviceMetadata != null) {

            Boolean isInitializing = connectingChannel.get(serviceMetadata);
            if (isInitializing != null && isInitializing) {
                log.info("connecting serviceMetadata:{} ", serviceMetadata);
                lock.lock();
                try {
                    while (connectingChannel.get(serviceMetadata)) {
                        // 等待初始化完成
                    }
                } finally {
                    lock.unlock();
                }
            }

            Channel channel = channelMap.get(serviceMetadata);
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(protocol);
                log.info("from channelMap serviceMetadata:{} ", serviceMetadata);
                return;
            }

            connectingChannel.put(serviceMetadata, true);

            ChannelFuture future = bootstrap.connect(serviceMetadata.getServiceAddr(), serviceMetadata.getServicePort());

            future.addListener((ChannelFutureListener) arg0 -> {
                if (future.isSuccess()) {
                    channelMap.put(serviceMetadata, future.channel());
                    connectingChannel.put(serviceMetadata, false);
                    future.channel().writeAndFlush(protocol);

                    log.info("connect rpc server {} on port {} success.", serviceMetadata.getServiceAddr(), serviceMetadata.getServicePort());
                } else {
                    log.error("connect rpc server {} on port {} failed.", serviceMetadata.getServiceAddr(), serviceMetadata.getServicePort());
                    future.cause().printStackTrace();
                }
            });
            //future.channel().writeAndFlush(protocol);
        }
    }


    public void stop() {
        eventLoopGroup.shutdownGracefully();
    }
}
