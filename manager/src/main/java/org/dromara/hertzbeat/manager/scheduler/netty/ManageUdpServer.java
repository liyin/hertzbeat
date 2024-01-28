package org.dromara.hertzbeat.manager.scheduler.netty;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hertzbeat.common.support.SpringContextHolder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(value = Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "udp",
        name = "enabled", havingValue = "true")
@Slf4j
public class ManageUdpServer implements CommandLineRunner {

    private BootNettyUdpSimpleChannelInboundHandler handler;
    private ManageUdpServerProperties udpServerProperties;

    public ManageUdpServer(BootNettyUdpSimpleChannelInboundHandler handler, ManageUdpServerProperties udpServerProperties){
        this.handler = handler;
        this.udpServerProperties = udpServerProperties;
    }

    public void bind(int port) {
        //表示服务器连接监听线程组，专门接受 accept 新的客户端client 连接
        EventLoopGroup bossLoopGroup = new NioEventLoopGroup();
        try {
            //1，创建netty bootstrap 启动类
            Bootstrap serverBootstrap = new Bootstrap();
            //2、设置boostrap 的eventLoopGroup线程组
            serverBootstrap = serverBootstrap.group(bossLoopGroup);
            //3、设置NIO UDP连接通道
            serverBootstrap = serverBootstrap.channel(NioDatagramChannel.class);
            //4、设置通道参数 SO_BROADCAST广播形式
            serverBootstrap = serverBootstrap.option(ChannelOption.SO_BROADCAST, true);
            // 设置缓冲区大小
            serverBootstrap = serverBootstrap.option(ChannelOption.SO_RCVBUF, 1024 * 1024 * 2);
            // 设置udp接收的最大报文数
            serverBootstrap = serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65535));
            //5、设置处理类 装配流水线
            serverBootstrap = serverBootstrap.handler(handler);
            //6、绑定server，通过调用sync（）方法异步阻塞，直到绑定成功
            ChannelFuture f = serverBootstrap.bind(port).sync();
            log.info(" started and listend on " + f.channel().localAddress());
            //7、监听通道关闭事件，应用程序会一直等待，直到channel关闭
            f.channel().closeFuture().await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//			 System.out.println("netty udp close!");
//			8 关闭EventLoopGroup，
//			 bossLoopGroup.shutdownGracefully();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        new Thread(() -> {
            this.bind(udpServerProperties.getServerPort());
        }, "UDP服务端["+udpServerProperties.getServerPort()+"]线程就绪").start();
    }


        public static Map<String, Object> getPayload(String value) {
            Map<String, Object> map = JSON.parseObject(value, Map.class);
            Map<String, Object> payload = JSON.parseObject(JSON.toJSONString(map.get("payload")), Map.class);
            return payload;
        }

}
