package com.carrentalapp.models;

public class BookingResponse {
    private String message;

    // Make this transient so it won't be included in JSON serialization
    private transient int statusCode;

    public BookingResponse() {
    }

    public BookingResponse(String message) {
        this.message = message;
        this.statusCode = 200; // Default to success
    }

    public BookingResponse(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}