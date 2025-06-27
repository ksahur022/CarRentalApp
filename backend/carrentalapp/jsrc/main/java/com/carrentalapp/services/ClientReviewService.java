package com.carrentalapp.services;

import com.carrentalapp.models.ClientReview;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;

@Singleton
public class ClientReviewService {
    private final DynamoDbClient dynamoDbClient;
    private final CognitoIdentityProviderClient cognitoClient;
    private final String reviewsTableName;
    private final String bookingsTableName;
    private final String carsTableName;
    private final String userPoolId;

    @Inject
    public ClientReviewService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.reviewsTableName = System.getenv("Reviews");
        this.bookingsTableName = System.getenv("Bookings");
        this.carsTableName = System.getenv("Cars");
        this.userPoolId = System.getenv("COGNITO_ID");

        // Initialize Cognito client with the same region as DynamoDB
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(System.getenv("REGION")))
                .build();
    }

    public Map<String, Object> submitReview(Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Extract request parameters
            String clientId = (String) request.get("clientId");
            String bookingId = (String) request.get("bookingId");
            String carId = (String) request.get("carId");

            // Handle both reviewText and feedbackText with special handling for empty strings
            String reviewText = null;
            if (request.containsKey("reviewText")) {
                String text = (String) request.get("reviewText");
                if (text != null && !text.trim().isEmpty()) {
                    reviewText = text;
                }
            }

            if (reviewText == null && request.containsKey("feedbackText")) {
                String text = (String) request.get("feedbackText");
                if (text != null && !text.trim().isEmpty()) {
                    reviewText = text;
                }
            }

            // Handle both rentalExperience and rating with special handling for empty strings
            Double rentalExperience = null;

            // Special handling for empty string rating
            if (request.containsKey("rating")) {
                Object ratingObj = request.get("rating");
                if (ratingObj != null) {
                    String ratingStr = ratingObj.toString();
                    if (!ratingStr.trim().isEmpty()) {
                        try {
                            rentalExperience = Double.parseDouble(ratingStr);
                        } catch (NumberFormatException e) {
                            response.put("message", "Rating must be a valid number");
                            response.put("statusCode", 400);
                            return response;
                        }
                    }
                }
            } else if (request.containsKey("rentalExperience")) {
                Object expObj = request.get("rentalExperience");
                if (expObj != null) {
                    String expStr = expObj.toString();
                    if (!expStr.trim().isEmpty()) {
                        try {
                            rentalExperience = Double.parseDouble(expStr);
                        } catch (NumberFormatException e) {
                            response.put("message", "Rating must be a valid number");
                            response.put("statusCode", 400);
                            return response;
                        }
                    }
                }
            }

            // Validate request
            if (clientId == null || clientId.isEmpty()) {
                response.put("message", "Client ID is required");
                response.put("statusCode", 400);
                return response;
            }
            if (bookingId == null || bookingId.isEmpty()) {
                response.put("message", "Booking ID is required");
                response.put("statusCode", 400);
                return response;
            }
            if (carId == null || carId.isEmpty()) {
                response.put("message", "Car ID is required");
                response.put("statusCode", 400);
                return response;
            }

            // Check for missing fields
            boolean isFeedbackMissing = (reviewText == null);
            boolean isRatingMissing = (rentalExperience == null);

            // Check if both rating and feedback are missing
            if (isFeedbackMissing && isRatingMissing) {
                response.put("message", "Rating and feedback are required");
                response.put("statusCode", 400);
                return response;
            }

            // Check if only feedback is missing
            if (isFeedbackMissing) {
                response.put("message", "Review text is required");
                response.put("statusCode", 400);
                return response;
            }

            // Check if only rating is missing
            if (isRatingMissing) {
                response.put("message", "Rating cannot be empty");
                response.put("statusCode", 400);
                return response;
            }

            // Check rating range
            if (rentalExperience < 1 || rentalExperience > 5) {
                response.put("message", "Rating must be between 1 and 5");
                response.put("statusCode", 400);
                return response;
            }

            // Verify client exists
            if (!clientExists(clientId)) {
                response.put("message", "Client ID is invalid");
                response.put("statusCode", 400);
                return response;
            }

            // Verify car exists
            if (!carExists(carId)) {
                response.put("message", "Car ID is invalid");
                response.put("statusCode", 400);
                return response;
            }

            // Verify booking exists
            if (!bookingExists(bookingId)) {
                response.put("message", "Booking ID is invalid");
                response.put("statusCode", 400);
                return response;
            }

            // Verify booking belongs to client
            if (!bookingBelongsToClient(bookingId, clientId)) {
                response.put("message", "Booking does not belong to client");
                response.put("statusCode", 400);
                return response;
            }

            // Check if review already exists for this booking
            if (reviewExistsForBooking(bookingId)) {
                response.put("message", "Review already submitted for this booking");
                response.put("statusCode", 409);
                return response;
            }

            // Get client details
            Map<String, String> clientDetails = getClientDetails(clientId);

            // Create review
            String reviewId = UUID.randomUUID().toString();

            // Create a ClientReview object
            ClientReview review = new ClientReview();
            review.setReviewId(reviewId);
            review.setAuthor(clientDetails.get("name"));
            review.setAuthorImageUrl(clientDetails.get("imageUrl"));
            review.setCarId(carId);
            review.setDate(Instant.now().toString());
            review.setRentalExperience(rentalExperience);
            review.setText(reviewText);
            review.setUserId(clientId);
            review.setBookingId(bookingId);  // Make sure this field exists in ClientReview

            // Save to DynamoDB using the toDynamoDbItem method from your ClientReview class
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(reviewsTableName)
                    .item(review.toDynamoDbItem())
                    .build();
            dynamoDbClient.putItem(putRequest);

            // Update car ratings
            updateCarRatings(carId);

            // Format response according to API spec
            response.clear();
            response.put("feedbackId", reviewId);
            response.put("systemMessage", "Feedback has been successfully created");
            response.put("statusCode", 201);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error submitting review: " + e.getMessage());
            response.put("statusCode", 400);
            return response;
        }
    }

    private boolean clientExists(String clientId) {
        try {
            // If clientId is an email address, check in Cognito user pool
            if (clientId.contains("@")) {
                System.out.println("Checking client with email: " + clientId);

                // Create a filter to find users by email
                ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                        .userPoolId(userPoolId)
                        .filter("email = \"" + clientId + "\"")
                        .limit(1)
                        .build();

                ListUsersResponse listUsersResponse = cognitoClient.listUsers(listUsersRequest);

                // If we found at least one user with this email
                boolean exists = !listUsersResponse.users().isEmpty();
                System.out.println("Client exists in Cognito: " + exists);
                return exists;
            } else {
                // For non-email IDs, check if it's a valid Cognito user ID
                try {
                    AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                            .userPoolId(userPoolId)
                            .username(clientId)
                            .build();

                    cognitoClient.adminGetUser(getUserRequest);
                    return true;
                } catch (UserNotFoundException e) {
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Error verifying client: " + e.getMessage());
            e.printStackTrace();

            // For debugging/testing, accept any email format
            if (clientId.contains("@")) {
                System.out.println("Accepting email format for testing: " + clientId);
                return true;
            }

            return false;
        }
    }

    private boolean carExists(String carId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("car_id", AttributeValue.builder().s(carId).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(carsTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);
            return response.hasItem();
        } catch (Exception e) {
            System.err.println("Error verifying car: " + e.getMessage());
            return false;
        }
    }

    private boolean bookingExists(String bookingId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("booking_id", AttributeValue.builder().s(bookingId).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(bookingsTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);
            return response.hasItem();
        } catch (Exception e) {
            System.err.println("Error verifying booking: " + e.getMessage());
            return false;
        }
    }

    private boolean bookingBelongsToClient(String bookingId, String clientId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("booking_id", AttributeValue.builder().s(bookingId).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(bookingsTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (!response.hasItem()) {
                return false;
            }

            // Check if the booking has client_id or client_email
            Map<String, AttributeValue> item = response.item();
            String bookingClientId = null;

            if (item.containsKey("client_id")) {
                bookingClientId = item.get("client_id").s();
            } else if (item.containsKey("client_email")) {
                bookingClientId = item.get("client_email").s();
            } else if (item.containsKey("email")) {
                bookingClientId = item.get("email").s();
            }

            if (bookingClientId == null) {
                System.err.println("Booking does not have client identifier");
                return false;
            }

            // If clientId is an email and booking has email
            if (clientId.contains("@") && bookingClientId.contains("@")) {
                return clientId.equalsIgnoreCase(bookingClientId);
            }

            // If clientId is an email but booking has user ID
            if (clientId.contains("@") && !bookingClientId.contains("@")) {
                // Get user ID from Cognito by email
                ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                        .userPoolId(userPoolId)
                        .filter("email = \"" + clientId + "\"")
                        .limit(1)
                        .build();

                ListUsersResponse listUsersResponse = cognitoClient.listUsers(listUsersRequest);

                if (!listUsersResponse.users().isEmpty()) {
                    String cognitoUserId = listUsersResponse.users().get(0).username();
                    return cognitoUserId.equals(bookingClientId);
                }
                return false;
            }

            // If booking has email but clientId is not an email
            if (!clientId.contains("@") && bookingClientId.contains("@")) {
                // Get email from Cognito by user ID
                try {
                    AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                            .userPoolId(userPoolId)
                            .username(clientId)
                            .build();

                    AdminGetUserResponse userResponse = cognitoClient.adminGetUser(getUserRequest);

                    for (AttributeType attribute : userResponse.userAttributes()) {
                        if (attribute.name().equals("email")) {
                            return attribute.value().equalsIgnoreCase(bookingClientId);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error getting user email: " + e.getMessage());
                }
                return false;
            }

            // Direct comparison for non-email IDs
            return clientId.equals(bookingClientId);
        } catch (Exception e) {
            System.err.println("Error verifying booking ownership: " + e.getMessage());
            return false;
        }
    }

    private boolean reviewExistsForBooking(String bookingId) {
        try {
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":bookingId", AttributeValue.builder().s(bookingId).build());

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(reviewsTableName)
                    .filterExpression("booking_id = :bookingId")
                    .expressionAttributeValues(expressionValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            return !response.items().isEmpty();
        } catch (Exception e) {
            System.err.println("Error checking existing review: " + e.getMessage());
            return false;
        }
    }

    private void updateCarRatings(String carId) {
        try {
            // Get all reviews for this car to calculate average
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":carId", AttributeValue.builder().s(carId).build());

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(reviewsTableName)
                    .filterExpression("car_id = :carId")
                    .expressionAttributeValues(expressionValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            // Calculate average rating
            double totalRating = 0;
            int count = 0;

            for (Map<String, AttributeValue> item : response.items()) {
                if (item.containsKey("rental_experience") && item.get("rental_experience").n() != null) {
                    totalRating += Double.parseDouble(item.get("rental_experience").n());
                    count++;
                }
            }

            if (count == 0) return;

            double avgRating = totalRating / count;
            avgRating = Math.round(avgRating * 10.0) / 10.0;

            // Update car record
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("car_id", AttributeValue.builder().s(carId).build());

            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put("car_rating", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().n(String.valueOf(avgRating)).build())
                    .action(AttributeAction.PUT)
                    .build());

            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(carsTableName)
                    .key(key)
                    .attributeUpdates(updates)
                    .build();

            dynamoDbClient.updateItem(updateRequest);
        } catch (Exception e) {
            System.err.println("Error updating car ratings: " + e.getMessage());
        }
    }

    private Map<String, String> getClientDetails(String clientId) {
        Map<String, String> details = new HashMap<>();
        try {
            if (clientId.contains("@")) {
                // Look up by email in Cognito
                ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                        .userPoolId(userPoolId)
                        .filter("email = \"" + clientId + "\"")
                        .limit(1)
                        .build();

                ListUsersResponse listUsersResponse = cognitoClient.listUsers(listUsersRequest);

                if (!listUsersResponse.users().isEmpty()) {
                    UserType user = listUsersResponse.users().get(0);
                    String firstName = "";
                    String lastName = "";

                    for (AttributeType attribute : user.attributes()) {
                        if (attribute.name().equals("given_name")) {
                            firstName = attribute.value();
                        } else if (attribute.name().equals("family_name")) {
                            lastName = attribute.value();
                        }
                    }

                    details.put("name", firstName + " " + lastName);
                    details.put("imageUrl", getDefaultUserImage());
                    return details;
                }
            } else {
                // Look up by username in Cognito
                try {
                    AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                            .userPoolId(userPoolId)
                            .username(clientId)
                            .build();

                    AdminGetUserResponse userResponse = cognitoClient.adminGetUser(getUserRequest);

                    String firstName = "";
                    String lastName = "";

                    for (AttributeType attribute : userResponse.userAttributes()) {
                        if (attribute.name().equals("given_name")) {
                            firstName = attribute.value();
                        } else if (attribute.name().equals("family_name")) {
                            lastName = attribute.value();
                        }
                    }

                    details.put("name", firstName + " " + lastName);
                    details.put("imageUrl", getDefaultUserImage());
                    return details;
                } catch (UserNotFoundException e) {
                    // User not found
                }
            }

            // Default if not found
            details.put("name", "Client " + clientId.substring(0, Math.min(8, clientId.length())));
            details.put("imageUrl", getDefaultUserImage());

        } catch (Exception e) {
            System.err.println("Error getting client details: " + e.getMessage());
            details.put("name", "Client " + clientId.substring(0, Math.min(8, clientId.length())));
            details.put("imageUrl", getDefaultUserImage());
        }

        return details;
    }

    private String getDefaultUserImage() {
        return "https://application.s3.eu-central-1.amazonaws.com/img/users/default-user.png";
    }
}