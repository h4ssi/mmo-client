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
