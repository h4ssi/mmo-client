package mmo.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) {
		new Bootstrap()
				.group(new NioEventLoopGroup())
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<NioSocketChannel>() {

					@Override
					protected void initChannel(final NioSocketChannel ch)
							throws Exception {
						System.out.println("initialized");
						ch.pipeline().addLast(new HttpClientCodec())
								.addLast(new ChannelInboundHandlerAdapter() {
									@Override
									public void channelActive(
											final ChannelHandlerContext ctx)
											throws Exception {
										super.channelActive(ctx);
										System.out.println("active");

										DefaultHttpRequest req = new DefaultHttpRequest(
												HttpVersion.HTTP_1_1,
												HttpMethod.GET, "/");
										req.headers().set(
												HttpHeaders.Names.CONTENT_TYPE,
												"text/plain; charset=utf-8");
										req.headers()
												.set(HttpHeaders.Names.TRANSFER_ENCODING,
														HttpHeaders.Values.CHUNKED);
										ctx.writeAndFlush(req);

										HashedWheelTimer timer = new HashedWheelTimer();

										timer.newTimeout(new TimerTask() {

											@Override
											public void run(Timeout timeout)
													throws Exception {
												ByteBuf b = ctx.alloc()
														.buffer();
												b.writeBytes("ping".getBytes());
												ctx.writeAndFlush(
														new DefaultHttpContent(
																b))
														.addListener(
																new ChannelFutureListener() {

																	@Override
																	public void operationComplete(
																			ChannelFuture future)
																			throws Exception {
																		ctx.writeAndFlush(new DefaultLastHttpContent());

																	}
																});
											}
										}, 1500, TimeUnit.MILLISECONDS);
									}

									@Override
									public void channelRead(
											ChannelHandlerContext ctx,
											Object msg) throws Exception {
										if (msg instanceof LastHttpContent) {
											System.out.println("done");
											ctx.close();
										} else if (msg instanceof HttpContent) {
											HttpContent c = (HttpContent) msg;

											System.out.println(c
													.content()
													.toString(
															Charset.forName("UTF-8")));
										}
									}
								});

					}
				}).option(ChannelOption.TCP_NODELAY, true)
				.connect("localhost", 8080);
	}

}
