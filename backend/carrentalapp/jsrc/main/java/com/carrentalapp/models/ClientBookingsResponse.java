package com.carrentalapp.models;

import java.util.List;

public class ClientBookingsResponse {
    private List<ClientBooking> content;
    private transient int statusCode;
    private transient String message;

    public ClientBookingsResponse() {
        // Default constructor
    }

    public List<ClientBooking> getContent() {
        return content;
    }

    public void setContent(List<ClientBooking> content) {
        this.content = content;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}