package mmo.client.message;

public class Chat implements Message {
    private String message;

    public Chat() {
    }

    public Chat(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "message='" + message + '\'' +
                '}';
    }
}
