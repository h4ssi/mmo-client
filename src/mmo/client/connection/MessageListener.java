package mmo.client.connection;

import mmo.client.message.Message;

public interface MessageListener {
    void messageReceived(Message message);
}
