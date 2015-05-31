package mmo.client.connection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import mmo.client.message.Message;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConnection {
    private final String host;
    private final int port;
    private final Map<MessageListener, Boolean> listeners = new
            ConcurrentHashMap<>();

    private final ObjectMapper mapper;
    private final ObjectWriter messageWriter;
    private final ObjectReader messageReader;

    private Channel notificationChannel;
    private Channel dataChannel;

    private boolean firstMessageDiscarded = false;
    private NioEventLoopGroup dataGroup = new NioEventLoopGroup(1);
    private NioEventLoopGroup notificationGroup = new NioEventLoopGroup(1);
    private DataHandler dataHandler = new DataHandler();

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;

        mapper = new ObjectMapper().setDefaultTyping(
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
                .group(notificationGroup)
                .channel(NioSocketChannel.class)
                .handler(new NotificationInitializer())
                .option(ChannelOption.TCP_NODELAY, true)
                .connect(this.host, this.port);
        new Bootstrap()
                .group(dataGroup)
                .channel(NioSocketChannel.class)
                .handler(new DataInitializer())
                .option(ChannelOption.TCP_NODELAY, true)
                .connect(this.host, this.port);
    }

    public void close() {
        notificationGroup.shutdownGracefully();
        dataGroup.shutdownGracefully();
    }

    public void addMessageListener(MessageListener listener) {
        listeners.putIfAbsent(listener, Boolean.TRUE);
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public <T> Future<T> getData(final String uri, final Class<T> clazz) {
        final Promise<T> promise = new DefaultPromise<>(dataGroup.next());
        dataGroup.submit(new Runnable() {
            @Override
            public void run() {
                dataHandler.getData(uri, clazz, promise);
            }
        });
        return promise;
    }

    private void messageReceived(Message message) {
        for (MessageListener listener : listeners.keySet()) {
            listener.messageReceived(message);
        }
    }

    private class NotificationInitializer extends
            ChannelInitializer<NioSocketChannel> {

        @Override
        protected void initChannel(final NioSocketChannel ch)
                throws Exception {
            notificationChannel = ch;
            ch.pipeline()
                    .addLast(new HttpClientCodec())
                    .addLast(new NotificationHandler());

        }
    }

    private class DataInitializer extends ChannelInitializer<NioSocketChannel> {

        @Override
        protected void initChannel(final NioSocketChannel ch)
                throws Exception {
            dataChannel = ch;
            ch.pipeline()
                    .addLast(new HttpClientCodec())
                    .addLast(dataHandler);

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

    public static class DataTask<T> {
        public String uri;
        public Class<T> clazz;
        public Promise<T> promise;

        public DataTask(String uri, Class<T> clazz, Promise<T> promise) {
            this.uri = uri;
            this.clazz = clazz;
            this.promise = promise;
        }
    }

    private class DataHandler extends ChannelInboundHandlerAdapter {
        private LinkedList<DataTask> queue = new LinkedList<>();
        private boolean waitingForResponse = true;

        public <T> void getData(String uri, Class<T> clazz, Promise<T>
                promise) {
            queue.addLast(new DataTask<>(uri, clazz, promise));
            workQueue();
        }

        private void workQueue() {
            if (queue.isEmpty()) {
                return;
            }
            if (!waitingForResponse) {
                waitingForResponse = true;

                HttpRequest req = new DefaultFullHttpRequest(HttpVersion
                        .HTTP_1_1, HttpMethod.GET, queue.getFirst().uri);

                dataChannel.writeAndFlush(req);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            waitingForResponse = false;
            workQueue();
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws
                Exception {
            try {
                if (msg instanceof LastHttpContent) {
                    LastHttpContent res = (LastHttpContent) msg;

                    DataTask task = queue.removeFirst();

                    String json = res.content().toString(CharsetUtil.UTF_8);

                    task.promise.setSuccess(mapper.readValue(json, task.clazz));

                    waitingForResponse = false;
                }

                workQueue();
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }
}
