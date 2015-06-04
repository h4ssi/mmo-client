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

package mmo.client.data;


public class ServerInfo {
    public ServerInfo() {

    }

    public ServerInfo(String status, String messageOfTheDay) {
        this.status = status;
        this.messageOfTheDay = messageOfTheDay;
    }

    private String status;

    private String messageOfTheDay;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessageOfTheDay() {
        return messageOfTheDay;
    }

    public void setMessageOfTheDay(String messageOfTheDay) {
        this.messageOfTheDay = messageOfTheDay;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "status='" + status + '\'' +
                ", messageOfTheDay='" + messageOfTheDay + '\'' +
                '}';
    }
}
