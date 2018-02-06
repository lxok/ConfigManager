package web.pkusz.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.io.UnsupportedEncodingException;

/**
 * Created by nick on 2017/11/26.
 */
public class NettyServerHandler extends ChannelHandlerAdapter {

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        //发送信息
        ctx.writeAndFlush(getSendByteBuf("服务器-->客户端 你好"));
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //收到消息
        ByteBuf buf = (ByteBuf) msg;// 获取客户端传来的Msg
        String recieved = getMessage(buf);
        System.out.println("------收到信息------"+recieved );
    }

    /*
     * 从ByteBuf中获取信息 使用UTF-8编码返回
     */
    private String getMessage(ByteBuf buf) {
        return null;
    }

    /*
     * 将Sting转化为UTF-8编码的字节
     */
    private ByteBuf getSendByteBuf(String message) throws UnsupportedEncodingException {
        byte[] req = message.getBytes("UTF-8");
        ByteBuf pingMessage = Unpooled.buffer();
        pingMessage.writeBytes(req);
        return pingMessage;
    }
}
