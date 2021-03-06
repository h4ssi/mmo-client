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

import mmo.client.message.Message;

/**
 * Listens for incoming messages from mmo server.
 */
public interface MessageListener {
    /**
     * Callback for received messages. Notice that messages will be received
     * asynchronously and must be synchronized manually.
     *
     * @param message The <code>Message</code> received
     */
    void messageReceived(Message message);
}
