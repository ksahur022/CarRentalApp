package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.models.BookingRequest;
import com.carrentalapp.models.BookingResponse;
import com.carrentalapp.services.BookingService;
import com.carrentalapp.services.CognitoService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CarBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final BookingService bookingService;
    private final CognitoService cognitoService;
    private final Gson gson;

    @Inject
    public CarBookingHandler(BookingService bookingService, CognitoService cognitoService) {
        this.bookingService = bookingService;
        this.cognitoService = cognitoService;
        this.gson = new GsonBuilder().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        // Create headers for CORS
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");

        // Handle OPTIONS request for CORS preflight
        if ("OPTIONS".equals(input.getHttpMethod())) {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setHeaders(headers);
            return response;
        }

        // TC049: Check for Authorization header
        Map<String, String> requestHeaders = input.getHeaders();
        if (requestHeaders == null || !requestHeaders.containsKey("Authorization")) {
            // Return 401 Unauthorized if no Authorization header
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(401);
            response.setHeaders(headers);

            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Unauthorized. Authentication required.");
            response.setBody(gson.toJson(errorBody));
            return response;
        }

        // Extract token from Authorization header
        String authHeader = requestHeaders.get("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Remove "Bearer " prefix
        } else {
            // Invalid Authorization format
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(401);
            response.setHeaders(headers);

            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Invalid Authorization format. Use 'Bearer <token>'");
            response.setBody(gson.toJson(errorBody));
            return response;
        }

        try {
            // Parse request body
            String requestBody = input.getBody();
            BookingRequest request = gson.fromJson(requestBody, BookingRequest.class);

            // CHANGED: Extract the email from the token instead of the sub ID
            String userEmail = getEmailFromToken(token);

            // Check if the clientId in the request matches a valid user
            if (!cognitoService.isEmailRegistered(request.getClientId())) {
                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(404);
                response.setHeaders(headers);

                Map<String, String> errorBody = new HashMap<>();
                errorBody.put("message", "User not found. The client ID in the request does not exist.");
                response.setBody(gson.toJson(errorBody));
                return response;
            }

            // Process booking
            BookingResponse response = bookingService.createBooking(request);

            // Create API Gateway response
            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
            apiResponse.setStatusCode(response.getStatusCode());
            apiResponse.setHeaders(headers);

            // Create a message-only response object
            Map<String, String> messageOnly = new HashMap<>();
            messageOnly.put("message", response.getMessage());

            // Use the message-only map for the response body
            apiResponse.setBody(gson.toJson(messageOnly));
            return apiResponse;

        } catch (Exception e) {
            // Handle errors
            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
            apiResponse.setStatusCode(500);
            apiResponse.setHeaders(headers);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error processing booking request: " + e.getMessage());
            apiResponse.setBody(gson.toJson(errorResponse));

            return apiResponse;
        }
    }

    /**
     * Extract the email from the ID token
     */
    private String getEmailFromToken(String idToken) {
        try {
            // For ID tokens, we need to decode and parse the JWT
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid token format");
            }

            // Decode the payload (second part)
            String base64Payload = parts[1];
            while (base64Payload.length() % 4 != 0) {
                base64Payload += "=";
            }

            String payload = new String(Base64.getUrlDecoder().decode(base64Payload), StandardCharsets.UTF_8);

            // Parse the JSON
            JsonObject jsonPayload = gson.fromJson(payload, JsonObject.class);

            // Extract the "email" claim
            if (jsonPayload.has("email")) {
                return jsonPayload.get("email").getAsString();
            }

            throw new RuntimeException("Email not found in token");
        } catch (Exception e) {
            throw new RuntimeException("Error extracting email from token: " + e.getMessage(), e);
        }
    }
}