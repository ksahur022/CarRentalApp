package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.services.UserService;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SigninHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final UserService userService;
    // Reuse headers map to avoid creating it for each request
    private static final Map<String, String> HEADERS = Collections.singletonMap("Content-Type", "application/json");

    @Inject
    public SigninHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            // Parse request body - only parse once
            String requestBody = request.getBody();
            if (requestBody == null || requestBody.isEmpty()) {
                return createErrorResponse(400, "Request body cannot be empty");
            }

            // Fast fail for missing credentials
            JSONObject jsonRequest = new JSONObject(requestBody);
            String email = jsonRequest.optString("email", null);
            String password = jsonRequest.optString("password", null);

            if (email == null || email.isEmpty()) {
                return createErrorResponse(400, "Email is required");
            }
            if (password == null || password.isEmpty()) {
                return createErrorResponse(400, "Password is required");
            }

            // Call user service to authenticate
            Map<String, Object> response = userService.authenticateUser(email, password);

            int statusCode = (Integer) response.get("statusCode");
            Map<String, Object> body = (Map<String, Object>) response.get("body");

            // For successful login
            if (statusCode == 200) {
                // Create response with only the needed fields
                JSONObject responseBody = new JSONObject();

                // Pre-check for null values to avoid multiple null checks
                Object idToken = body.get("idToken");
                Object role = body.get("role");
                Object userId = body.get("userId");
                Object username = body.get("username");
                Object userImageUrl = body.getOrDefault("userImageUrl",
                        "https://application.s3.eu-central-1.amazonaws.com/img/users/f47ac10b-58cc-4372-a567-0e02b2c3d479.png");

                // Add fields in the exact order you want
                responseBody.put("idToken", idToken);
                responseBody.put("role", role);
                responseBody.put("userId", userId);
                responseBody.put("userImageUrl", userImageUrl);
                responseBody.put("username", username);

                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(statusCode)
                        .withBody(responseBody.toString())
                        .withHeaders(HEADERS);
            } else {
                // For error cases - simplified error handling
                String errorMessage = body.containsKey("error") ?
                        (String) body.get("error") :
                        body.containsKey("message") ?
                                (String) body.get("message") :
                                "Authentication failed";

                return createErrorResponse(statusCode, errorMessage);
            }
        } catch (IllegalArgumentException e) {
            return createErrorResponse(400, "Invalid username/password supplied");
        } catch (Exception e) {
            // Changed from 500 to 400 as default error code
            return createErrorResponse(400, "Request Error: " + e.getMessage());
        }
    }

    // Helper method to create error responses
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        JSONObject responseBody = new JSONObject();
        responseBody.put("message", message);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(responseBody.toString())
                .withHeaders(HEADERS);
    }
}
