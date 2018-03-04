package web.pkusz.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import web.pkusz.serialize.Entry;
import web.pkusz.serialize.SerializeUtil;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by nick on 2017/12/3.
 */
public class RequestHandler extends ChannelInboundHandlerAdapter {
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        String v = in.toString(CharsetUtil.UTF_8);

        System.out.println(v);

        List<Entry> en = SerializeUtil.parseCharArray(v.toCharArray());
        String respond = RequestProcessor.process(en);
        try {
            if (respond != null && respond.length() != 0) {
                ctx.write(respond.getBytes("UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //log
        System.out.println("Server Channel occur exception.");
        cause.printStackTrace();
        ctx.close();
    }
}
