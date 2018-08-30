package nl.utwente.ing.model.bean;

import java.util.ArrayList;

public class PaymentRequest {

    private long id;
    private String description;
    private String due_date;
    private float amount;
    private long number_of_requests;
    private boolean filled;
    private ArrayList<Transaction> transactions;

    public PaymentRequest() {

    }

    public PaymentRequest(long id, String description, String due_date, float amount, long number_of_requests, boolean filled, ArrayList<Transaction> transactions) {
        this.id = id;
        this.description = description;
        this.due_date = due_date;
        this.amount = amount;
        this.number_of_requests = number_of_requests;
        this.filled = filled;
        this.transactions = transactions;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDue_date() {
        return due_date;
    }

    public void setDue_date(String due_date) {
        this.due_date = due_date;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public long getNumber_of_requests() {
        return number_of_requests;
    }

    public void setNumber_of_requests(long number_of_requests) {
        this.number_of_requests = number_of_requests;
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
    }
}


