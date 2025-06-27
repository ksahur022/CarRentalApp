package com.carrentalapp.models;

import org.json.JSONObject;
import java.util.*;

public class SignUpRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
//    private String confirmPassword;

    public SignUpRequest(String email, String password, String firstName, String lastName) {
        if (email == null && password == null  && firstName == null && lastName == null) {
            throw new IllegalArgumentException("Missing or incomplete data.");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (firstName == null) {
            throw new IllegalArgumentException("First name is required.");
        }
        if (lastName == null) {
            throw new IllegalArgumentException("Last name is required.");
        }


        this.email = email;
        this.password = password;
//        this.confirmPassword = confirmPassword;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

//    public String getConfirmPassword() { return confirmPassword; }
//    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public static SignUpRequest fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);

        // Allowed keys
        List<String> allowedKeys = List.of("email", "password", "firstName", "lastName");

        // Check for unexpected fields
        for (String key : json.keySet()) {
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("Missing or incomplete data.");
            }
        }

        String email = json.optString("email", null);
        String password = json.optString("password", null);
        String firstName = json.optString("firstName", null);
        String lastName = json.optString("lastName", null);

        return new SignUpRequest(email, password, firstName, lastName);
    }
}
