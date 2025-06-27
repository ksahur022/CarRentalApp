package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.models.AllBookingsResponse;
import com.carrentalapp.models.Booking;
import com.carrentalapp.models.BookingSummary;
import com.carrentalapp.models.Car;
import com.carrentalapp.models.Location;
import com.carrentalapp.services.BookingService;
import com.carrentalapp.services.CognitoService;
import com.carrentalapp.services.LocationService;
import com.google.gson.Gson;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class GetAllBookingsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final BookingService bookingService;
    private final CognitoService cognitoService;
    private final DynamoDbTable<Car> carTable;
    private final LocationService locationService;
    private final DynamoDbClient dynamoDbClient;
    private final Gson gson;

    @Inject
    public GetAllBookingsHandler(BookingService bookingService, CognitoService cognitoService,
                                 DynamoDbTable<Car> carTable, LocationService locationService,
                                 DynamoDbClient dynamoDbClient) {
        this.bookingService = bookingService;
        this.cognitoService = cognitoService;
        this.carTable = carTable;
        this.locationService = locationService;
        this.dynamoDbClient = dynamoDbClient;
        this.gson = new Gson();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            // Extract authorization header
            Map<String, String> headers = input.getHeaders();
            String authHeader = headers != null ? headers.get("Authorization") : null;

            // Validate authorization
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return createErrorResponse(401, "Unauthorized access");
            }

            String token = authHeader.substring(7);

            // Verify user is a support agent using the SupportAgent table
            try {
                // Get user email from token
                String userEmail = cognitoService.getEmailFromToken(token);

                // Check if the email exists in the SupportAgent table
                boolean isSupportAgent = checkIfSupportAgent(userEmail);

                if (!isSupportAgent) {
                    return createErrorResponse(403, "Access forbidden. Support Agent role required.");
                }
            } catch (Exception e) {
                context.getLogger().log("Authorization error: " + e.getMessage());
                return createErrorResponse(401, "Invalid token");
            }

            // Extract query parameters
            Map<String, String> queryParams = input.getQueryStringParameters();
            String dateFrom = null;
            String dateTo = null;
            String clientId = null;

            if (queryParams != null) {
                dateFrom = queryParams.get("dateFrom");
                dateTo = queryParams.get("dateTo");
                clientId = queryParams.get("clientId");
            }

            // Get bookings with optional filters
            List<Booking> bookings = bookingService.getAllBookings(dateFrom, dateTo, clientId);

            // Process bookings to create summaries
            List<BookingSummary> bookingSummaries = createBookingSummaries(bookings);

            // Create response
            AllBookingsResponse response = new AllBookingsResponse(bookingSummaries);

            // Return successful response
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(response))
                    .withHeaders(createCorsHeaders());
        } catch (Exception e) {
            context.getLogger().log("Error in GetAllBookingsHandler: " + e.getMessage());
            return createErrorResponse(500, "Error retrieving bookings: " + e.getMessage());
        }
    }

    // Check if user email exists in SupportAgent table
    private boolean checkIfSupportAgent(String email) {
        try {
            // Get the SupportAgent table name from environment variable
            String tableName = System.getenv("SupportAgent");
            if (tableName == null) {
                tableName = "SupportAgent"; // Fallback to default name
            }

            // Create a scan request to find the email in the table
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":email", AttributeValue.builder().s(email).build());

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("email = :email")
                    .expressionAttributeValues(expressionValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            // If any items are returned, the email exists in the SupportAgent table
            return !response.items().isEmpty();
        } catch (Exception e) {
            System.err.println("Error checking if user is a support agent: " + e.getMessage());
            return false;
        }
    }

    private List<BookingSummary> createBookingSummaries(List<Booking> bookings) {
        // Create booking summaries
        return bookings.stream().map(booking -> {
            BookingSummary summary = new BookingSummary();
            summary.setBookingId(booking.getBookingId());
            summary.setBookingNumber(booking.getOrderNumber());

            // Format booking period
            try {
                LocalDateTime pickupDateTime = LocalDateTime.parse(booking.getPickupDatetime());
                LocalDateTime dropoffDateTime = LocalDateTime.parse(booking.getDropoffDatetime());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
                summary.setBookingPeriod(pickupDateTime.format(formatter) + " - " + dropoffDateTime.format(formatter));
            } catch (Exception e) {
                summary.setBookingPeriod("Unknown period");
            }

            // Set car model
            Car car = carTable.getItem(r -> r.key(k -> k.partitionValue(booking.getCarId())));
            if (car != null) {
                summary.setCarModel(car.getMake() + " " + car.getModel());
            } else {
                summary.setCarModel("Unknown car");
            }

            // Set client name from Cognito
            String clientEmail = booking.getClientId(); // assuming this is an email
            String clientName = "Unknown client";

            try {
                // Try to get user attributes from Cognito
                Map<String, String> userAttributes = cognitoService.getUserAttributes(clientEmail);
                String firstName = userAttributes.getOrDefault("given_name", "");
                String lastName = userAttributes.getOrDefault("family_name", "");

                if (!firstName.isEmpty() || !lastName.isEmpty()) {
                    clientName = (firstName + " " + lastName).trim();
                } else {
                    // Fallback: Extract name from email
                    String nameFromEmail = clientEmail.split("@")[0];
                    nameFromEmail = nameFromEmail.replace("_", " ").replace(".", " ");

                    // Convert to title case
                    String[] parts = nameFromEmail.split(" ");
                    StringBuilder nameBuilder = new StringBuilder();
                    for (String part : parts) {
                        if (!part.isEmpty()) {
                            nameBuilder.append(Character.toUpperCase(part.charAt(0)))
                                    .append(part.substring(1))
                                    .append(" ");
                        }
                    }
                    clientName = nameBuilder.toString().trim();
                }
            } catch (Exception e) {
                // If all else fails, use the email as the name
                clientName = clientEmail;
            }

            summary.setClientName(clientName);

            // Format created date
            try {
                LocalDateTime createdAt = LocalDateTime.parse(booking.getCreatedAt());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                summary.setDate(createdAt.format(formatter));
            } catch (Exception e) {
                summary.setDate("Unknown date");
            }

            // Set location
            Optional<Location> locationOpt = locationService.getLocationById(booking.getPickupLocationId());
            if (locationOpt.isPresent()) {
                summary.setLocation(locationOpt.get().getName());
            } else {
                summary.setLocation("Unknown location");
            }

            // Set madeBy based on whether the clientId exists in the SupportAgent table
            String madeBy = "Client"; // Default value

            try {
                // Check if the clientId (which is an email) exists in the SupportAgent table
                boolean isClientSupportAgent = checkIfSupportAgent(booking.getClientId());

                if (isClientSupportAgent) {
                    madeBy = "Support Agent";
                }
            } catch (Exception e) {
                // Log error but continue with default "Client"
                System.err.println("Error determining who made the booking: " + e.getMessage());
            }

            summary.setMadeBy(madeBy);

            return summary;
        }).collect(Collectors.toList());
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        AllBookingsResponse errorResponse = new AllBookingsResponse();
        errorResponse.setContent(new ArrayList<>());
        errorResponse.setStatusCode(statusCode);
        errorResponse.setMessage(message);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(gson.toJson(errorResponse))
                .withHeaders(createCorsHeaders());
    }

    private Map<String, String> createCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key");
        headers.put("Access-Control-Allow-Methods", "GET,OPTIONS");
        return headers;
    }
}