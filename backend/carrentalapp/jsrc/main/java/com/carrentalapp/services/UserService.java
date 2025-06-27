package com.carrentalapp.services;

import com.carrentalapp.Validator.SignUpValidator;
import com.carrentalapp.models.Role;
import com.carrentalapp.models.SignUpRequest;
import com.carrentalapp.models.User;

import javax.inject.Inject;
import java.util.*;

public class UserService {
    private final CognitoService cognitoService;
    private final DynamoDBService dynamoDBService;

    @Inject
    public UserService(CognitoService cognitoService, DynamoDBService dynamoDBService) {
        this.cognitoService = cognitoService;
        this.dynamoDBService = dynamoDBService;
    }

    public Map<String, Object> registerUser(SignUpRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return Map.of(
                    "statusCode", 400,
                    "message", "Email is required"
            );
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return Map.of(
                    "statusCode", 400,
                    "message", "Password is required"
            );
        }

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            return Map.of(
                    "statusCode", 400,
                    "message", "First name is required"
            );
        }

        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            return Map.of(
                    "statusCode", 400,
                    "message", "Last name is required"
            );
        }


        List<String> errors = SignUpValidator.validate(request);
        if (!errors.isEmpty()) {
            return Map.of(
                    "statusCode", 400,
                    "message", String.join(", ", errors)
            );
        }

        if (cognitoService.isEmailRegistered(request.getEmail())) {
            return Map.of(
                    "statusCode", 409,
                    "message", "A user with this email address already exists."
            );
        }

        Role role = determineUserRole(request.getEmail());

        String idToken = cognitoService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                role.name()
        );

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        // dynamoDBService.saveUser(user, idToken, role); // Uncomment when needed

        return Map.of(
                "statusCode", 201,
                "message", "User registered successfully"
        );
    }

    public Map<String, Object> authenticateUser(String email, String password) {
        try {

            if ((email == null || email.trim().isEmpty()) && (password == null || password.trim().isEmpty())) {
                return Map.of("statusCode", 400, "body",
                        Map.of("message", "Email and password are required")
                );
            }
            // Validate inputs
            if (email == null || email.trim().isEmpty()) {
                return Map.of(
                        "statusCode", 400,
                        "body", Map.of("message", "Email is required")
                );
            }

            if (password == null || password.trim().isEmpty()) {
                return Map.of(
                        "statusCode", 400,
                        "body", Map.of("message", "Password is required")
                );
            }
            // Combined check for both fields being missing or empty


            // Call Cognito service to authenticate user
            Map<String, Object> authResult = cognitoService.authenticateUser(email, password);

            // If authentication is successful
            if (authResult.containsKey("idToken")) {
                String idToken = (String) authResult.get("idToken");

                // Get user attributes from Cognito
                Map<String, String> userAttributes = cognitoService.getUserAttributes(email);

                // Create response with user details
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("idToken", idToken);
                responseBody.put("role", userAttributes.getOrDefault("custom:role", "Client")); // Changed default from "Customer" to "Client"
                responseBody.put("userId", email); // Using email as userId

                // Construct username from first and last name attributes
                String firstName = userAttributes.getOrDefault("given_name", "");
                String lastName = userAttributes.getOrDefault("family_name", "");
                responseBody.put("username", firstName + " " + lastName);

                // Add default user image URL
                responseBody.put("userImageUrl", "https://application.s3.eu-central-1.amazonaws.com/img/users/f47ac10b-58cc-4372-a567-0e02b2c3d479.png");

                return Map.of(
                        "statusCode", 200,
                        "body", responseBody
                );
            } else {
                // Authentication failed
                return Map.of(
                        "statusCode", 400,
                        "body", Map.of("message", authResult.getOrDefault("error", "Invalid password or Email"))
                );
            }
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 500,
                    "body", Map.of("message", "Authentication failed: " + e.getMessage())
            );
        }


    }


        private Role determineUserRole(String email) {
        if (dynamoDBService.isAdmin(email)) {
            return Role.ADMIN;
        }
        if (dynamoDBService.isSupportAgent(email)) {
            return Role.SUPPORTAGENT;
        }
        return Role.CLIENT;
    }
}
