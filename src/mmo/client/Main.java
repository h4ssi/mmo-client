package mmo.client;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import mmo.client.connection.MessageListener;
import mmo.client.connection.ServerConnection;
import mmo.client.data.ServerInfo;
import mmo.client.message.Message;

public class Main {

    public static void main(String[] args) {
        ServerConnection connection = new ServerConnection("localhost", 8080);

        connection.addMessageListener(new MessageListener() {
            @Override
            public void messageReceived(Message message) {
                System.out.println("message: " + message);
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
