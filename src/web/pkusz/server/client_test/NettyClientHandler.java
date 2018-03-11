package web.pkusz.server.client_test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import web.pkusz.serialize.Entry;
import web.pkusz.serialize.SerializeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 2018/3/8.
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        List<Entry> es = new ArrayList<>();
        Entry e0 = new Entry("type", "1");
        Entry e1 = new Entry("大力哥", "呵呵");
        es.add(e0);
        es.add(e1);
        ctx.writeAndFlush(Unpooled.copiedBuffer(SerializeUtil.getStringFromEntries(es), CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        System.out.println("Client received: " + in.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
