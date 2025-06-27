package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.DaggerAppComponent;
import com.carrentalapp.services.ClientReviewService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTDecodeException;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

public class ClientReviewSubmitHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final ClientReviewService clientReviewService;
    private final Gson gson;

    public ClientReviewSubmitHandler() {
        // Initialize with Dagger
        this.clientReviewService = DaggerAppComponent.create().clientReviewService();
        this.gson = new Gson();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Processing client review submission");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");

        try {
            // Get 'Authorization' header
            Map<String, String> requestHeaders = request.getHeaders();
            String authorizationHeader = requestHeaders != null ? requestHeaders.get("Authorization") : null;

            // Check for missing header - try with case-insensitive search
            if (authorizationHeader == null && requestHeaders != null) {
                for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                    if ("authorization".equalsIgnoreCase(entry.getKey())) {
                        authorizationHeader = entry.getValue();
                        break;
                    }
                }
            }

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                // Missing or invalid Authorization header
                context.getLogger().log("Authentication token is not provided or invalid format");
                response.setStatusCode(401);
                response.setHeaders(headers);
                response.setBody("{\"message\": \"Authentication token is required in format 'Bearer <token>'\"}");
                return response;
            }

            // Extract token from "Bearer <TOKEN>" format
            String token = authorizationHeader.substring(7);
            context.getLogger().log("Token received: " + token.substring(0, Math.min(token.length(), 20)) + "...");

            // Validate token
            if (!isTokenValid(token, context)) {
                // Invalid token
                response.setStatusCode(401);
                response.setHeaders(headers);
                response.setBody("{\"message\": \"Invalid authentication token.\"}");
                return response;
            }

            // Parse request body
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> requestBody = gson.fromJson(request.getBody(), type);

            context.getLogger().log("Request body: " + request.getBody());

            // Submit review
            Map<String, Object> serviceResponse = clientReviewService.submitReview(requestBody);

            // Prepare response
            int statusCode = (int) serviceResponse.getOrDefault("statusCode", 200);
            serviceResponse.remove("statusCode");

            return response
                    .withStatusCode(statusCode)
                    .withHeaders(headers)
                    .withBody(gson.toJson(serviceResponse));

        } catch (Exception e) {
            context.getLogger().log("Error processing review: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error processing review: " + e.getMessage());

            return response
                    .withStatusCode(400)
                    .withHeaders(headers)
                    .withBody(gson.toJson(errorResponse));
        }
    }

    /**
     * Checks if the given token is valid by decoding and verifying basic JWT claims.
     *
     * @param token Bearer token
     * @param context Lambda context for logging
     * @return true if the token is valid, false otherwise
     */
    private boolean isTokenValid(String token, Context context) {
        try {
            // Basic token structure validation
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                context.getLogger().log("Token does not have three parts");
                return false;
            }

            // Decode the JWT token (without verification)
            DecodedJWT jwt = JWT.decode(token);

            // Log token details for debugging
            context.getLogger().log("Token subject: " + jwt.getSubject());
            context.getLogger().log("Token issuer: " + jwt.getIssuer());

            // Basic token expiration check
            Date now = new Date();
            if (jwt.getExpiresAt() != null && now.after(jwt.getExpiresAt())) {
                context.getLogger().log("Token is expired");
                return false;
            }

            // Check token issuer (Cognito User Pool URL)
            String cognitoId = System.getenv("COGNITO_ID");
            String region = System.getenv("REGION");
            if (cognitoId != null && region != null) {
                String expectedIssuer = "https://cognito-idp." + region + ".amazonaws.com/" + cognitoId;
                if (jwt.getIssuer() == null || !jwt.getIssuer().equals(expectedIssuer)) {
                    context.getLogger().log("Token has invalid issuer. Expected: " + expectedIssuer + ", Got: " + jwt.getIssuer());
                    return false;
                }
            }

            // Decode token payload to check claims
            try {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                Map<String, Object> claims = gson.fromJson(payload, Map.class);

                // Check client_id or aud claim
                String clientId = System.getenv("CLIENT_ID");
                if (clientId != null) {
                    Object tokenClientId = claims.get("client_id");
                    if (tokenClientId == null) {
                        tokenClientId = claims.get("aud");
                    }

                    if (tokenClientId == null) {
                        context.getLogger().log("Token is missing client_id/aud claim");
                        return false;
                    }

                    // Handle case when aud is an array
                    boolean clientIdMatch = false;
                    if (tokenClientId instanceof String) {
                        clientIdMatch = clientId.equals(tokenClientId);
                    } else if (tokenClientId instanceof java.util.ArrayList) {
                        for (Object aud : (java.util.ArrayList)tokenClientId) {
                            if (clientId.equals(aud)) {
                                clientIdMatch = true;
                                break;
                            }
                        }
                    }

                    if (!clientIdMatch) {
                        context.getLogger().log("Token has invalid client_id. Expected: " + clientId + ", Got: " + tokenClientId);
                        return false;
                    }
                }

                // Check token_use claim (should be 'access' or 'id')
                String tokenUse = (String) claims.get("token_use");
                if (tokenUse == null) {
                    context.getLogger().log("Token is missing token_use claim");
                    // Some tokens might not have token_use, especially third-party tokens, so we don't fail here
                } else if (!tokenUse.equals("access") && !tokenUse.equals("id")) {
                    context.getLogger().log("Token has invalid token_use: " + tokenUse);
                    return false;
                }

            } catch (IllegalArgumentException | com.google.gson.JsonSyntaxException e) {
                context.getLogger().log("Error parsing token payload: " + e.getMessage());
                return false;
            }

            context.getLogger().log("Token validation successful");
            return true;

        } catch (JWTDecodeException e) {
            context.getLogger().log("Invalid JWT format: " + e.getMessage());
            return false;
        } catch (Exception e) {
            context.getLogger().log("Token validation failed: " + e.getMessage());
            return false;
        }
    }
}