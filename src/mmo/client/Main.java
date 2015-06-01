package mmo.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import mmo.client.connection.MessageListener;
import mmo.client.connection.ServerConnection;
import mmo.client.data.ServerInfo;
import mmo.client.message.Chat;
import mmo.client.message.Message;

public class Main {

    public static void main(String[] args) {
        final ServerConnection connection =
                new ServerConnection("localhost", 8080);

        connection.addMessageListener(new MessageListener() {
            private boolean didGreeting = false;

            @Override
            public void messageReceived(Message message) {
                System.out.println("message: " + message);

                if (!didGreeting) {
                    try {
                        connection.sendMessage(new Chat("hello there!"));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace(); // TODO
                    }
                    didGreeting = true;
                }
            }
        });

        connection.open();

        connection
                .getData("/status", ServerInfo.class)
                .addListener(new FutureListener<ServerInfo>() {

                    @Override
                    public void operationComplete(Future<ServerInfo> future)
                            throws
                            Exception {
                        System.out.println(future.get());
                    }
                });
    }

}
