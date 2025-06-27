package com.carrentalapp.Repository;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.models.Booking;
import com.carrentalapp.models.Car;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BookingRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String BOOKINGS_TABLE;
    private final String CARS_TABLE;

    @Inject
    public BookingRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.BOOKINGS_TABLE = System.getenv("Bookings");
        this.CARS_TABLE = System.getenv("Cars");
    }


    public Car getCarById(String carId, Context context) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("car_id", AttributeValue.builder().s(carId).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(CARS_TABLE)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.hasItem()) {
                return Car.fromDynamoDbItem(response.item());
            }

            return null;
        } catch (Exception e) {
            context.getLogger().log("Error getting car: " + e.getMessage());
            throw e;
        }
    }

    public boolean isCarAvailable(String carId, Instant pickupTime, Instant dropoffTime, Context context) {
        try {
            // First, check if the car status is AVAILABLE
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("car_id", AttributeValue.builder().s(carId).build());

            GetItemRequest carRequest = GetItemRequest.builder()
                    .tableName(CARS_TABLE)
                    .key(key)
                    .build();

            GetItemResponse carResponse = dynamoDbClient.getItem(carRequest);

            if (!carResponse.hasItem() || !carResponse.item().containsKey("status") ||
                    !carResponse.item().get("status").s().equals("AVAILABLE")) {

                // Then check if there are any overlapping bookings
                Map<String, AttributeValue> expressionValues = new HashMap<>();
                expressionValues.put(":carId", AttributeValue.builder().s(carId).build());
                expressionValues.put(":status", AttributeValue.builder().s("CANCELLED").build());

                ScanRequest scanRequest = ScanRequest.builder()
                        .tableName(BOOKINGS_TABLE)
                        .filterExpression("car_id = :carId AND booking_status <> :status")
                        .expressionAttributeValues(expressionValues)
                        .build();

                ScanResponse response = dynamoDbClient.scan(scanRequest);

                for (Map<String, AttributeValue> item : response.items()) {
                    Booking booking = Booking.fromDynamoDbItem(item);

                    // Check for overlap
                    if (booking.getPickupDatetimeAsInstant() != null && booking.getDropoffDatetimeAsInstant() != null) {
                        boolean overlap = (pickupTime.isBefore(booking.getDropoffDatetimeAsInstant()) &&
                                dropoffTime.isAfter(booking.getPickupDatetimeAsInstant()));

                        if (overlap) {
                            return false;
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            context.getLogger().log("Error checking car availability: " + e.getMessage());
            return false;
        }
    }


    public List<Booking> getBookingsByCarId(String carId, Context context) {
        try {
            // Create the expression values for querying the table
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":car_id", AttributeValue.builder().s(carId).build());
            expressionValues.put(":cancelledStatus", AttributeValue.builder().s("CANCELLED").build());

            // Create the ScanRequest to filter bookings for the given car that are not cancelled
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(BOOKINGS_TABLE)
                    .filterExpression("car_id = :car_id AND booking_status <> :cancelledStatus")
                    .expressionAttributeValues(expressionValues)
                    .build();

            // Perform the scan operation
            ScanResponse response = dynamoDbClient.scan(scanRequest);
            List<Booking> bookings = new ArrayList<>();

            // Iterate through the bookings and map them to Booking objects
            for (Map<String, AttributeValue> item : response.items()) {
                Booking booking = Booking.fromDynamoDbItem(item);

                // Add the booking to the list if it has a valid pickup and dropoff datetime
                if (booking.getPickupDatetimeAsInstant() != null && booking.getDropoffDatetimeAsInstant() != null) {
                    bookings.add(booking);
                }
            }

            // Return the list of bookings for the given car
            return bookings;

        } catch (Exception e) {
            context.getLogger().log("Error fetching bookings for car: " + e.getMessage());
            return Collections.emptyList();
        }

    }
}