package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.models.ClientReview;
import com.carrentalapp.models.ClientReviewResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import javax.inject.Inject;
import java.util.*;

public class ClientReviewHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson;

    @Inject
    public ClientReviewHandler() {
        this.gson = new GsonBuilder().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            // Check if this is a request for recent feedbacks
            String path = requestEvent.getPath();
            if (path != null && path.contains("feedbacks/recent")) {
                return handleRecentFeedbacks();
            }

            // Extract path parameters
            Map<String, String> pathParameters = requestEvent.getPathParameters();
            String carId = pathParameters != null ? pathParameters.get("car_id") : null;

            // Check if carId is null or empty
            if (carId == null) {
                return createErrorResponse(400, "Car ID cannot be empty");
            }

            // Check if carId is empty string
            if (carId.trim().isEmpty()) {
                return createErrorResponse(400, "Car ID cannot be empty");
            }

            // Validate carId format (assuming UUID format)
            try {
                UUID.fromString(carId);
            } catch (IllegalArgumentException e) {
                return createErrorResponse(400, "Invalid Car ID format");
            }

            // Extract query parameters
            Map<String, String> queryParameters = requestEvent.getQueryStringParameters();
            int page = getQueryParamAsInt(queryParameters, "page", 0);
            int size = getQueryParamAsInt(queryParameters, "size", 10);
            String sort = getQueryParam(queryParameters, "sort", "DATE");
            String direction = getQueryParam(queryParameters, "direction", "DESC");

            // Fetch client reviews from DynamoDB
            try {
                ClientReviewResponse response = fetchClientReviews(carId, page, size, sort, direction);

                // Return successful response
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(gson.toJson(response))
                        .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
            } catch (ReviewNotFoundException e) {
                // Return 404 if no reviews found
                return createErrorResponse(404, e.getMessage());
            }

        } catch (Exception e) {
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    // New method to handle recent feedbacks
    private APIGatewayProxyResponseEvent handleRecentFeedbacks() {
        try {
            // Initialize DynamoDB client
            DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                    .region(Region.of(System.getenv("REGION")))
                    .build();

            // Create a scan request to get recent feedback data
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(System.getenv("Reviews"))
                    .limit(10) // Limit to 10 recent feedbacks
                    .build();

            // Execute the scan
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

            // Convert items to feedback objects
            List<Map<String, Object>> content = new ArrayList<>();
            for (Map<String, AttributeValue> item : scanResponse.items()) {
                Map<String, Object> feedback = new HashMap<>();

                // Map DynamoDB attributes to the required response fields
                feedback.put("feedbackId", getAttributeValue(item, "id", ""));
                feedback.put("author", getAttributeValue(item, "author", ""));
                feedback.put("carImageUrl", getAttributeValue(item, "car_image_url", ""));
                feedback.put("carModel", getAttributeValue(item, "car_model", ""));
                feedback.put("date", getAttributeValue(item, "date", ""));
                feedback.put("feedbackText", getAttributeValue(item, "feedback_text", ""));
                feedback.put("orderHistory", getAttributeValue(item, "order_history", ""));
                feedback.put("rating", getAttributeValue(item, "rating", ""));

                content.add(feedback);
            }

            // Sort feedbacks by date (newest first)
            content.sort((f1, f2) -> {
                String date1 = (String) f1.get("date");
                String date2 = (String) f2.get("date");
                return date2.compareTo(date1);
            });

            // Create response map
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("content", content);

            // Return successful response
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(responseMap))
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (Exception e) {
            return createErrorResponse(500, "Error fetching recent feedbacks: " + e.getMessage());
        }
    }

    private String getAttributeValue(Map<String, AttributeValue> item, String key, String defaultValue) {
        if (item != null && item.containsKey(key) && item.get(key).s() != null) {
            return item.get(key).s();
        }
        return defaultValue;
    }

    // Custom exception for no reviews found
    private static class ReviewNotFoundException extends Exception {
        public ReviewNotFoundException(String message) {
            super(message);
        }
    }

    private int getQueryParamAsInt(Map<String, String> queryParams, String paramName, int defaultValue) {
        if (queryParams == null || !queryParams.containsKey(paramName)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(queryParams.get(paramName));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getQueryParam(Map<String, String> queryParams, String paramName, String defaultValue) {
        if (queryParams == null || !queryParams.containsKey(paramName)) {
            return defaultValue;
        }
        return queryParams.get(paramName);
    }

    private ClientReviewResponse fetchClientReviews(String carId, int page, int size, String sort, String direction)
            throws ReviewNotFoundException {
        try {
            // Initialize DynamoDB client
            DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                    .region(Region.of(System.getenv("REGION")))
                    .build();

            // Use scan with filter instead of query (more compatible if GSI doesn't exist)
            Map<String, String> expressionAttrNames = new HashMap<>();
            expressionAttrNames.put("#carId", "car_id");

            Map<String, AttributeValue> expressionAttrValues = new HashMap<>();
            expressionAttrValues.put(":carIdValue", AttributeValue.builder().s(carId).build());

            // Use scan with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(System.getenv("Reviews"))
                    .filterExpression("#carId = :carIdValue")
                    .expressionAttributeNames(expressionAttrNames)
                    .expressionAttributeValues(expressionAttrValues)
                    .build();

            // Execute the scan
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

            // Convert items to ClientReview objects
            List<ClientReview> allReviews = new ArrayList<>();
            for (Map<String, AttributeValue> item : scanResponse.items()) {
                allReviews.add(ClientReview.fromDynamoDbItem(item));
            }

            // If no reviews found, throw exception
            if (allReviews.isEmpty()) {
                throw new ReviewNotFoundException("Car reviews not found");
            }

            // Sort the results in memory based on date if needed
            if ("DATE".equals(sort)) {
                if ("DESC".equals(direction)) {
                    allReviews.sort((r1, r2) -> r2.getDate().compareTo(r1.getDate()));
                } else {
                    allReviews.sort((r1, r2) -> r1.getDate().compareTo(r2.getDate()));
                }
            }

            // Apply pagination in memory
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, allReviews.size());
            List<ClientReview> pagedReviews = (startIndex < allReviews.size())
                    ? allReviews.subList(startIndex, endIndex)
                    : new ArrayList<>();

            // Return the response
            return new ClientReviewResponse(pagedReviews, page, allReviews.size(),
                    (int) Math.ceil((double) allReviews.size() / size));

        } catch (ReviewNotFoundException e) {
            // Re-throw the exception
            throw e;
        } catch (Exception e) {
            // Log the error
            System.err.println("Error fetching reviews: " + e.getMessage());
            e.printStackTrace();

            // Throw as ReviewNotFoundException
            throw new ReviewNotFoundException("Error retrieving car reviews");
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", message);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(gson.toJson(errorResponse))
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
    }
}