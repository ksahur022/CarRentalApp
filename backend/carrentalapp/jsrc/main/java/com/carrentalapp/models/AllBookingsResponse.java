package com.carrentalapp.models;

import java.util.List;

public class AllBookingsResponse {
    private List<BookingSummary> content;
    private String message;
    private int statusCode;

    public AllBookingsResponse() {
    }

    public AllBookingsResponse(List<BookingSummary> content) {
        this.content = content;
        this.statusCode = 200;
        this.message = "Successfully retrieved " + content.size() + " bookings";
    }

    public List<BookingSummary> getContent() {
        return content;
    }

    public void setContent(List<BookingSummary> content) {
        this.content = content;
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