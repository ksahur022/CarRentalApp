//package com.carrentalapp.handler;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
//import com.carrentalapp.models.Role;
//import com.carrentalapp.models.SupportAgent;
//import com.carrentalapp.services.CognitoService;
//import com.carrentalapp.services.DynamoDBService;
//import com.google.gson.Gson;
//
//import javax.inject.Inject;
//import java.util.HashMap;
//import java.util.Map;
//
//public class AdminManagementHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
//
//    private final CognitoService cognitoService;
//    private final DynamoDBService dynamoDBService;
//    private final Gson gson = new Gson();
//
//    @Inject
//    public AdminManagementHandler(CognitoService cognitoService, DynamoDBService dynamoDBService) {
//        this.cognitoService = cognitoService;
//        this.dynamoDBService = dynamoDBService;
//    }
//
//    @Override
//    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
//        try {
//            // Extract path and method to determine operation
//            String path = request.getPath();
//            String method = request.getHttpMethod();
//
//            // Get the access token from the Authorization header
//            String authHeader = request.getHeaders().get("Authorization");
//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                return createResponse(401, "Authorization header with Bearer token is required");
//            }
//
//            String token = authHeader.substring(7); // Remove "Bearer " prefix
//
//            // Get the email of the requester from the token
//            String requesterEmail = cognitoService.getEmailFromToken(token);
//
//            // Check if the requester is an admin
//            if (!dynamoDBService.isAdmin(requesterEmail)) {
//                return createResponse(403, "Only administrators can perform this operation");
//            }
//
//            // Parse the request body
//            Map<String, String> requestBody = gson.fromJson(request.getBody(), Map.class);
//            String targetEmail = requestBody.get("email");
//
//            if (targetEmail == null || targetEmail.trim().isEmpty()) {
//                return createResponse(400, "Email is required");
//            }
//
//            // Handle different operations based on path and method
//            if (path.endsWith("/admin") && method.equals("POST")) {
//                return addAdmin(targetEmail);
//            } else if (path.endsWith("/admin") && method.equals("DELETE")) {
//                return removeAdmin(targetEmail);
//            } else if (path.endsWith("/supportagent") && method.equals("POST")) {
//                return addSupportAgent(targetEmail);
//            } else if (path.endsWith("/supportagent") && method.equals("DELETE")) {
//                return removeSupportAgent(targetEmail);
//            } else {
//                return createResponse(400, "Invalid operation");
//            }
//
//        } catch (Exception e) {
//            return createResponse(500, "Error processing request: " + e.getMessage());
//        }
//    }
//
//    private APIGatewayProxyResponseEvent addAdmin(String email) {
//        try {
//            // Check if the user is already an admin
//            if (dynamoDBService.isAdmin(email)) {
//                return createResponse(409, "User is already an admin");
//            }
//
//            // Add the user to the admin table
//            dynamoDBService.addAdmin(email);
//
//            // Update the user's role in Cognito if they exist
//            if (cognitoService.isEmailRegistered(email)) {
//                cognitoService.updateUserRole(email, Role.ADMIN.name());
//            }
//
//            return createResponse(200, "User successfully added as admin");
//        } catch (Exception e) {
//            return createResponse(500, "Error adding admin: " + e.getMessage());
//        }
//    }
//
//    private APIGatewayProxyResponseEvent removeAdmin(String email) {
//        try {
//            // Check if the user is an admin
//            if (!dynamoDBService.isAdmin(email)) {
//                return createResponse(404, "User is not an admin");
//            }
//
//            // Remove the user from the admin table
//            dynamoDBService.removeAdmin(email);
//
//            // Update the user's role in Cognito if they exist
//            if (cognitoService.isEmailRegistered(email)) {
//                // Check if they're a support agent
//                if (dynamoDBService.isSupportAgent(email)) {
//                    cognitoService.updateUserRole(email, Role.SUPPORTAGENT.name());
//                } else {
//                    cognitoService.updateUserRole(email, Role.CLIENT.name());
//                }
//            }
//
//            return createResponse(200, "User successfully removed from admin role");
//        } catch (Exception e) {
//            return createResponse(500, "Error removing admin: " + e.getMessage());
//        }
//    }
//
//    private APIGatewayProxyResponseEvent addSupportAgent(String email) {
//        try {
//            // Check if the user is already a support agent
//            if (dynamoDBService.isSupportAgent(email)) {
//                return createResponse(409, "User is already a support agent");
//            }
//
//            // Add the user to the support agent table
//            SupportAgent supportAgent = new SupportAgent();
//            supportAgent.setEmail(email);
//            dynamoDBService.saveSupportAgent(supportAgent);
//
//            // Update the user's role in Cognito if they exist and aren't an admin
//            if (cognitoService.isEmailRegistered(email) && !dynamoDBService.isAdmin(email)) {
//                cognitoService.updateUserRole(email, Role.SUPPORTAGENT.name());
//            }
//
//            return createResponse(200, "User successfully added as support agent");
//        } catch (Exception e) {
//            return createResponse(500, "Error adding support agent: " + e.getMessage());
//        }
//    }
//
//    private APIGatewayProxyResponseEvent removeSupportAgent(String email) {
//        try {
//            // Implementation needed: Remove support agent
//            // You'll need to add a method to DynamoDBService to remove a support agent
//
//            // Update the user's role in Cognito if they exist and aren't an admin
//            if (cognitoService.isEmailRegistered(email) && !dynamoDBService.isAdmin(email)) {
//                cognitoService.updateUserRole(email, Role.CLIENT.name());
//            }
//
//            return createResponse(200, "User successfully removed from support agent role");
//        } catch (Exception e) {
//            return createResponse(500, "Error removing support agent: " + e.getMessage());
//        }
//    }
//
//    private APIGatewayProxyResponseEvent createResponse(int statusCode, String message) {
//        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
//        response.setStatusCode(statusCode);
//
//        Map<String, String> responseBody = new HashMap<>();
//        responseBody.put("message", message);
//        response.setBody(gson.toJson(responseBody));
//
//        return response;
//    }
//}