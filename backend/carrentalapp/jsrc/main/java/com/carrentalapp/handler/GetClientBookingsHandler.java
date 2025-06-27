package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.models.ClientBookingsResponse;
import com.carrentalapp.services.BookingService;
import com.carrentalapp.services.CognitoService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class GetClientBookingsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final BookingService bookingService;
    private final CognitoService cognitoService;
    private final Gson gson;

    @Inject
    public GetClientBookingsHandler(BookingService bookingService, CognitoService cognitoService) {
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
        headers.put("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");

        // Handle OPTIONS request for CORS preflight
        if ("OPTIONS".equals(input.getHttpMethod())) {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setHeaders(headers);
            return response;
        }

        try {
            // Get userId from path parameters
            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("userId")) {
                // TC053: Empty UserId - return 400
                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(400);
                response.setHeaders(headers);

                Map<String, String> errorBody = new HashMap<>();
                errorBody.put("message", "User ID is required");
                response.setBody(gson.toJson(errorBody));
                return response;
            }

            String userId = pathParameters.get("userId");

            // TC053: Empty UserId
            if (userId == null || userId.isEmpty()) {
                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(400);
                response.setHeaders(headers);

                Map<String, String> errorBody = new HashMap<>();
                errorBody.put("message", "User ID is required");
                response.setBody(gson.toJson(errorBody));
                return response;
            }

            // Get client bookings from service
            ClientBookingsResponse serviceResponse = bookingService.getClientBookings(userId);

            // Create API Gateway response
            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();

            // Set status code based on service response
            apiResponse.setStatusCode(serviceResponse.getStatusCode());
            apiResponse.setHeaders(headers);

            if (serviceResponse.getStatusCode() != 200) {
                Map<String, String> errorBody = new HashMap<>();
                errorBody.put("message", serviceResponse.getMessage());
                apiResponse.setBody(gson.toJson(errorBody));
            } else {
                apiResponse.setBody(gson.toJson(serviceResponse));
            }

            return apiResponse;

        } catch (Exception e) {
            // Handle errors
            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
            apiResponse.setStatusCode(500);
            apiResponse.setHeaders(headers);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving client bookings: " + e.getMessage());
            apiResponse.setBody(gson.toJson(errorResponse));

            return apiResponse;
        }
    }
}