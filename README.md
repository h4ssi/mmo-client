## mmo-client

This is the client part for mmo-server.

## Connecting to server

Login anonymously

```
String hostnameOrIp = "www.example.com";
int port = 80;
ServerConnection connection = new ServerConnection(hostnameOrIp, port);
```

Login with a given login name

```
String hostnameOrIp = "www.example.com";
int port = 80;
String name = "h4ssi";
ServerConnection connection = new ServerConnection(hostnameOrIp, port, name);
```

## Closing the connection

Per default the connection will stay established indefinitely. It must be 
closed explicitly:

```
Connection connection = ...;
connection.close();
```

## Message serialization

Messages are encoded as JSON. A POJO will be mapped automatically by its
class' name and its getter and setter methods.

E.g. when a message like this should be transferred:

```
{
  "type" : ".Chat",
  "id" : 12                   /* author local room  id : optional[int] */
  "message" : "Hello there!"  /* chat message : string */
}
```

A corresponding Java class is needed:

* its name must be `Chat` (corresponding to the `type` field above)
* its visibility must be `public`
* it must be placed directly in the package `mmo.client.message`
* it must implement the `mmo.client.message.Message` interface
* it must provide a public default constructor
  * `public Chat() {...}`
* it must provide a public setter and a public getter method for an (optional) 
  `int`  property of name `"id"`
  * `public Integer getId() {...}`
  * `public void setId(Integer id) {...}`
* and likewise for a `String` property named `message`  
  * `public String getMessage() {...}`
  * `public void setMessage(String message) {...}`

E.g.

```
package mmo.client.message;

public class Chat {
    ...
    public Chat() {...}
    
    public Integer getId() {...}
    public void setId(Integer id) {...}
 
    public String getMessage() {...}
    public void setMessage(String message) {...} 
}
```

## Sending messages

```
ServerConnection connection = ...;
Chat chat = ...;

connection.sendMessage(chat);
```

Sending a message is thread safe and thus can be called at any time without 
worrying about synchronization issues.

## Receiving messages

```
ServerConnection connection = ...;

connection.addMessageListener(new MessageListener() {
    @Override
    public void messageReceived(Message message) {
        if (message instanceof Chat) {
            doSomethingWith((Chat) message);
        }
    }
});
```

Notice: `Message`s are received asynchronously. The receiver logic must be 
synchronized manually (i.e. `doSomethingWith((Chat) message)` should take care
of potential synchronization issues).

E.g. if the `Chat` message is to be displayed on a `javax.swing` window, 
`SwingUtilities.invokeLater` could be used to enqueue handling of the `Chat` 
message on the `java.awt` event dispatcher thread.

## Data serialization

Besides sending and receiving messages, the server can be queried for data.

This data is JSON encoded as well. Mapping this data to POJOs is very similar
to the previously discussed "Message serialization":

Just apply these adaptations:

* The class' name _can_ be chosen freely
* Classes need _not_ implement the `Message` interface
* Classes _should_ be placed into the `mmo.client.data` package instead

E.g. for receiving data like

```
{
  "status" : "up",
  "messageOfTheDay" : "Chuck Norris only needs one (1) pokeball to catch legendery pokemon."
}
```

You may use this POJO

```
package mmo.client.data;


public class ServerInfo {
    ...
    public ServerInfo() {...}

    public String getStatus() {...}
    public void setStatus(String status) {...}

    public String getMessageOfTheDay() {...}
    public void setMessageOfTheDay(String messageOfTheDay) {...}
}
```

## Querying data

To query data from the server, all that is needed is the URI where the data 
is located, and the class used for its deserialization. 

In this example, the server's current status is to be queried:
 
The status data's URI reads `/status` and the data can be serialized into a 
`ServerInfo` object like described above.

Programmatically this can be done like so

```
ServerConnection = ...;
connection
    .getData("/status", ServerInfo.class)           // provide URI and class
    .addListener(new FutureListener<ServerInfo>() { // wait for data to arrive
        @Override
        public void operationComplete(Future<ServerInfo> future) {
            ServerInfo data = future.getNow();
            doSomethingWith(data);
        }
    });
```

Again be aware, that the data will arrive asynchronously.
Thus `getData()` does not directly return the data, but merely a `Future` 
(this is because it takes some time to fetch the data from the server).

As shown above, you may attach a `FutureListener`, to be notified when the data
actually arrives. Again in this case `doSomethingWith(data)` should take care 
of potential synchronization issues.

Initiating the query (i.e. calling `getData()`) itself is thread safe. Thus 
`getData()` can be called at any time without worrying about synchronization 
issues.

## License

Copyright 2015 Florian Hassanen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Powered by

* [netty](http://netty.io/) Copyright 2014 The Netty Project
* [jackson](http://wiki.fasterxml.com/JacksonHome)
