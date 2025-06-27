package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.models.BookingResponse;
import com.carrentalapp.models.ModifyBookingRequest;
import com.carrentalapp.services.BookingService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class ModifyBookingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final BookingService bookingService;
    private final Gson gson;

    @Inject
    public ModifyBookingHandler(BookingService bookingService) {
        this.bookingService = bookingService;
        this.gson = new GsonBuilder().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        // Create headers for CORS
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "PUT, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");

        // Handle OPTIONS request for CORS preflight
        if ("OPTIONS".equals(input.getHttpMethod())) {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setHeaders(headers);
            return response;
        }

        try {
            // Get bookingId from path parameters
            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("bookingId")) {
                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(400);
                response.setHeaders(headers);

                Map<String, String> errorBody = new HashMap<>();
                errorBody.put("message", "Booking ID is required");
                response.setBody(gson.toJson(errorBody));
                return response;
            }

            String bookingId = pathParameters.get("bookingId");

            // Parse request body
            String requestBody = input.getBody();
            if (requestBody == null || requestBody.isEmpty()) {
                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(400);
                response.setHeaders(headers);

                Map<String, String> errorBody = new HashMap<>();
                errorBody.put("message", "Request body is required");
                response.setBody(gson.toJson(errorBody));
                return response;
            }

            ModifyBookingRequest request = gson.fromJson(requestBody, ModifyBookingRequest.class);

            // Modify booking
            BookingResponse serviceResponse = bookingService.modifyBooking(bookingId, request);

            // Create API Gateway response
            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
            apiResponse.setStatusCode(serviceResponse.getStatusCode());
            apiResponse.setHeaders(headers);

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", serviceResponse.getMessage());
            apiResponse.setBody(gson.toJson(responseBody));

            return apiResponse;

        } catch (Exception e) {
            // Handle errors
            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
            apiResponse.setStatusCode(500);
            apiResponse.setHeaders(headers);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error modifying booking: " + e.getMessage());
            apiResponse.setBody(gson.toJson(errorResponse));

            return apiResponse;
        }
    }
}