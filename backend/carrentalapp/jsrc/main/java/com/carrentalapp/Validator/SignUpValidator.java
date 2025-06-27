package com.carrentalapp.Validator;

import com.carrentalapp.models.SignUpRequest;

import java.util.ArrayList;
import java.util.List;

public class SignUpValidator {

    public static List<String> validate(SignUpRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getFirstName() == null || !isValidName(request.getFirstName())) {
            errors.add("First name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.");
        }
        if (request.getLastName() == null || !isValidName(request.getLastName())) {
            errors.add("Last name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.");
        }
        if (request.getEmail() == null || !isValidEmail(request.getEmail())) {
            errors.add("Invalid email format. Please ensure it follows the format username@domain.com.");
        }
        if (request.getPassword() == null || !isValidPassword(request.getPassword())) {
            errors.add("Password must be 8-16 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }


        return errors;
    }

    private static boolean isValidName(String name) {
        return name.matches("^[A-Za-z'\\-]{1,49}$");
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private static boolean isValidPassword(String password) {
        return password.length() >= 8 && password.length() <= 16 &&
                password.chars().anyMatch(Character::isUpperCase) &&
                password.chars().anyMatch(Character::isLowerCase) &&
                password.chars().anyMatch(Character::isDigit) &&
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }
}