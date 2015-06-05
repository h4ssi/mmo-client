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

package mmo.client.message;

/**
 * Base type for messages exchanged with mmo server.
 * Messages will be serialized to and deserialized and from messages
 * automatically.
 * <p/>
 * Getters and setters will be mapped to the corresponding json fields. E.g.
 * getData/setData will be mapped to json field "data"
 * <p/>
 * You may also use Jackson annotations to describe the serialization format
 * more specifically.
 */
public interface Message {
}
