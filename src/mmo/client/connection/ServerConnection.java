/*
 * Copyright 2015 Florian Hassanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mmo.client.connection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Client connection to mmo-server.
 *
 * Allows for sending and receiving messages, as well as querying data endpoints
 * directly.
 *
 * This class operates asynchronously. Keep this in mind when integrating
 * this code into yours. Use suitable synchronisation mechanisms (like e.g.
 * javax.swing.SwingUtilities#invokeLater when interfacing with the swing/awt
 * event loop)
 *
 * You need to open the connection by explicitly calling #open()
 */
public class ServerConnection {
    private final String host;
    private final int port;
    private final String username;
    private final ConcurrentMap<MessageListener, Boolean> listeners =
            new ConcurrentHashMap<>();

    private final ObjectMapper mapper;
    private final ObjectWriter messageWriter;
    private final ObjectReader messageReader;

    private Channel notificationChannel;
    private Channel dataChannel;

    private boolean firstMessageDiscarded = false;
    private NioEventLoopGroup dataGroup = new NioEventLoopGroup(1);
    private NioEventLoopGroup notificationGroup = new NioEventLoopGroup(1);
    private DataHandler dataHandler = new DataHandler();

    /**
     * Connects to mmo server with hostname and port information and login
     * anonymously.
     *
     * @param host hostname or ip of server
     * @param port port to connect to
     */
    public ServerConnection(String host, int port) {
        this(host, port, null);
    }

    /**
     * Connects to mmo server with hostname and port and login with given
     * username.
     *
     * @param host hostname or ip of server
     * @param port port to connect to
     * @param username login name
     */
    public ServerConnection(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;

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
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        messageWriter = mapper.writerFor(Message.class);
        messageReader = mapper.reader(Message.class);
    }

    /**
     * Opens connection to server. This method must be called explicitly.
     */
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

    /**
     * Closes connection to server. Per default connections will stay open
     * indefinitely. Use this method to close the connection gracefully.
     */
    public void close() {
        notificationGroup.shutdownGracefully();
        dataGroup.shutdownGracefully();
    }

    /**
     * Registers a message listener. It will be notified about every message
     * received. If a message cannot be decoded, it will call the
     * MessageListener#messageReceived with a <code>Message</code> argument of
     * <code>null</code>.
     *
     * @param listener Listener to register
     */
    public void addMessageListener(MessageListener listener) {
        listeners.putIfAbsent(listener, Boolean.TRUE);
    }

    /**
     * Removes a message listener.
     *
     * @param listener Listener to remove
     */
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sends a <code>Message</code> to the server.
     *
     * @param message <code>Message</code> to send.
     * @throws JsonProcessingException On encoding errors
     */
    public void sendMessage(Message message) throws JsonProcessingException {
        notificationChannel.writeAndFlush(
                new DefaultHttpContent(
                        Unpooled.wrappedBuffer(
                                messageWriter.writeValueAsBytes(
                                        message
                                )
                        )
                )
        );
    }

    /**
     * Queries data from the server. Queries data from the given URI and decodes
     * it as value of the given class. The result is received asynchronously.
     * You may register a listener on the returned <code>Future</code> or
     * block on it to wait for the result to arrive.
     *
     * @param uri   URI to query data from
     * @param clazz Class to decode data to
     * @param <T>   <code>Class</code> type param
     * @return      <code>Future</code> of value to be received and decoded
     */
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
                    HttpMethod.GET,
                    "/game" +
                            (username == null
                                    ? ""
                                    : "/" + username));
            HttpHeaders.setHeader(req, HttpHeaders.Names.CONTENT_TYPE,
                    "text/plain; charset=utf-8");
            HttpHeaders.setTransferEncodingChunked(req);
            ctx.writeAndFlush(req);
        }

        @Override
        public void channelRead(
                ChannelHandlerContext ctx,
                Object msg) throws Exception {
            try {
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
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    private static class DataTask<T> {
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
