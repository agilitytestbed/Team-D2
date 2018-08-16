package nl.utwente.ing.model.bean;

public class BalanceHistoryPoint {

    private float open;
    private float close;
    private float volume;
    private long timeStamp;

    public BalanceHistoryPoint(float open, float close, float volume, long timeStamp) {
        this.open = open;
        this.close = close;
        this.volume = volume;
        this.timeStamp = timeStamp;
    }

    public float getOpen() {
        return open;
    }

    public void setOpen(float open) {
        this.open = open;
    }

    public float getClose() {
        return close;
    }

    public void setClose(float close) {
        this.close = close;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}

