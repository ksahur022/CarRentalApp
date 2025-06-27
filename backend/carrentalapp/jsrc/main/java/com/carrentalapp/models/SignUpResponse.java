package com.carrentalapp.models;

//package com.carrentalapp.models;

import java.util.Objects;

public class SignUpResponse {

    private final String message;

    public SignUpResponse(String message) {
        this.message = Objects.requireNonNull(message, "Message cannot be null");
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SignUpResponse{message='" + message + "'}";
    }
}
