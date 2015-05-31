package mmo.client.connection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import mmo.client.message.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConnection {
    private final String host;
    private final int port;
    private final Map<MessageListener, Boolean> listeners = new
            ConcurrentHashMap<>();

    private final ObjectWriter messageWriter;
    private final ObjectReader messageReader;

    private boolean firstMessageDiscarded = false;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;

        ObjectMapper mapper = new ObjectMapper().setDefaultTyping(
                new ObjectMapper.DefaultTypeResolverBuilder(
                        ObjectMapper.DefaultTyping
                                .OBJECT_AND_NON_CONCRETE)
                        .init(
                                JsonTypeInfo.Id.MINIMAL_CLASS,
                                null)
                        .inclusion(JsonTypeInfo.As.PROPERTY)
                        .typeProperty("type")
        )
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        messageWriter = mapper.writerFor(Message.class);
        messageReader = mapper.reader(Message.class);
    }

    public void open() {
        new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ServerHandler())
                .option(ChannelOption.TCP_NODELAY, true)
                .connect(this.host, this.port);
    }

    public void addMessageListener(MessageListener listener) {
        listeners.putIfAbsent(listener, Boolean.TRUE);
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    private void messageReceived(Message message) {
        for (MessageListener listener : listeners.keySet()) {
            listener.messageReceived(message);
        }
    }

    private class ServerHandler extends ChannelInitializer<NioSocketChannel> {

        @Override
        protected void initChannel(final NioSocketChannel ch)
                throws Exception {
            ch.pipeline()
                    .addLast(new HttpClientCodec())
                    .addLast(new NotificationHandler());

        }
    }

    private class NotificationHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(
                final ChannelHandlerContext ctx)
                throws Exception {
            super.channelActive(ctx);

            DefaultHttpRequest req = new DefaultHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.GET, "/game");
            HttpHeaders.setHeader(req, HttpHeaders.Names.CONTENT_TYPE,
                    "text/plain; charset=utf-8");
            HttpHeaders.setTransferEncodingChunked(req);
            ctx.writeAndFlush(req);
        }

        @Override
        public void channelRead(
                ChannelHandlerContext ctx,
                Object msg) throws Exception {
            if (msg instanceof LastHttpContent) {
                ctx.close();
            } else if (msg instanceof HttpContent) {
                if (!firstMessageDiscarded) {
                    firstMessageDiscarded = true;
                    return;
                }

                HttpContent c = (HttpContent) msg;

                String json = c.content().toString(CharsetUtil.UTF_8);

                Message m;
                try {
                    m = messageReader.readValue(json);
                } catch (IllegalArgumentException e) {
                    m = null;
                }
                messageReceived(m);
            }
        }
    }
}
