package web.pkusz.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * Created by nick on 2017/12/3.
 */
/**
 ServerManager类是CM的服务器类，在这个类中启动对客户端的服务器，使CM可以接收来自客户端的指令。
 在实现上，ServerManager内部使用了Netty通信框架作为服务器处理外部通信请求。
 客户端请求的格式为JSON，JSON的第一项表示该请求的类型。
 在处理请求时，由RequestProcessor类对请求进行解析和分发，不同的请求类型使用不同的策略对象来处理。
 不同请求处理对象的共有基类是RequestProcessStrategy，当需要扩展新的请求类型时，需要继承该类来实现请求处理逻辑。
 */
public class ServerManager implements Runnable {
    public static int DEFAULT_PORT = 10060;

    int port;

    public ServerManager(int port) {
        this.port = port;
    }

    public ServerManager() {
        this.port = DEFAULT_PORT;
    }

    public void start() throws Exception {
        final RequestHandler reqh = new RequestHandler();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress(port)).
                    childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(reqh);
                        }
                    });
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public static void main(String[] args) throws Exception {
        ServerManager serv = new ServerManager();
        serv.start();
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            //log
            System.out.println("Netty server stops running because of some exceptions happening.");
            System.out.println(e.toString());
        }
    }
}
