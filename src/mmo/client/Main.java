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
                    public void operationComplete(Future<ServerInfo> future) {
                        System.out.println(future.getNow());
                    }
                });
    }

}
