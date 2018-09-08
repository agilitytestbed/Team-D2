package nl.utwente.ing.model.bean;

public class Message {

    private long id;
    private String message;
    private String date;
    private boolean read;
    private String type;

    public Message(){}

    public Message(long id, String message, String date, boolean read, String type){
        this.id = id;
        this.message = message;
        this.date = date;
        this.read = read;
        this.type = type;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
