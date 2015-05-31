package mmo.client;

import mmo.client.connection.MessageListener;
import mmo.client.connection.ServerConnection;
import mmo.client.message.Message;

public class Main {

    public static void main(String[] args) {
        ServerConnection connection = new ServerConnection("localhost", 8080);

        connection.addMessageListener(new MessageListener() {
            @Override
            public void messageReceived(Message message) {
                System.out.println("message");
            }
        });

        connection.open();
    }

}
