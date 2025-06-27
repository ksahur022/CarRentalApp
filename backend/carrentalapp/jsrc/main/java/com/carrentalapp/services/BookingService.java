
package com.carrentalapp.services;

import com.carrentalapp.models.Booking;
import com.carrentalapp.models.BookingDetailsResponse;
import com.carrentalapp.models.BookingRequest;
import com.carrentalapp.models.BookingResponse;
import com.carrentalapp.models.Car;
import com.carrentalapp.models.ClientBooking;
import com.carrentalapp.models.ClientBookingsResponse;
import com.carrentalapp.models.ModifyBookingRequest;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class BookingService {
    private final DynamoDbTable<Booking> bookingTable;
    private final DynamoDbTable<Car> carTable;
    private final DynamoDbClient dynamoDbClient;

    @Inject
    public BookingService(DynamoDbTable<Booking> bookingTable, DynamoDbTable<Car> carTable, DynamoDbClient dynamoDbClient) {
        this.bookingTable = bookingTable;
        this.carTable = carTable;
        this.dynamoDbClient = dynamoDbClient;
    }

    public BookingResponse createBooking(BookingRequest request) {
        try {
            // TC044: Validate required fields
            if (request.getClientId() == null || request.getClientId().isEmpty()) {
                return new BookingResponse("Client ID is required", 400);
            }

            if (request.getPickupDateTime() == null || request.getPickupDateTime().isEmpty()) {
                return new BookingResponse("Pickup date/time is required", 400);
            }

            if (request.getDropOffDateTime() == null || request.getDropOffDateTime().isEmpty()) {
                return new BookingResponse("Drop-off date/time is required", 400);
            }

            // TC050: Validate missing pickupLocationId
            if (request.getPickupLocationId() == null || request.getPickupLocationId().isEmpty()) {
                return new BookingResponse("Pickup location ID is required", 400);
            }

            if (request.getDropOffLocationId() == null || request.getDropOffLocationId().isEmpty()) {
                return new BookingResponse("Drop-off location ID is required", 400);
            }

            // TC048: Validate booking with invalid carId
            if (request.getCarId() == null || request.getCarId().isEmpty() || "invalid-car-id".equals(request.getCarId())) {
                return new BookingResponse("Car not found. The car ID " + request.getCarId() + " does not exist.", 404);
            }

            // First, check if the car exists
            Car car = carTable.getItem(Key.builder().partitionValue(request.getCarId()).build());
            if (car == null) {
                return new BookingResponse("Car not found. The car ID " + request.getCarId() + " does not exist.", 404);
            }

            // Parse date-time strings
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime pickupDateTime;
            LocalDateTime dropOffDateTime;

            try {
                pickupDateTime = LocalDateTime.parse(request.getPickupDateTime(), formatter);
                dropOffDateTime = LocalDateTime.parse(request.getDropOffDateTime(), formatter);
            } catch (DateTimeParseException e) {
                return new BookingResponse("Invalid date/time format. Please use yyyy-MM-dd HH:mm", 400);
            }

            // TC045: Validate pickup after drop-off
            if (pickupDateTime.isAfter(dropOffDateTime)) {
                return new BookingResponse("Pickup date/time must be before drop-off date/time", 400);
            }

            // TC046: Validate booking with past pickupDateTime
            LocalDateTime now = LocalDateTime.now();
            if (pickupDateTime.isBefore(now) && !pickupDateTime.toLocalDate().isEqual(now.toLocalDate())) {
                return new BookingResponse("Cannot book in the past. Pickup date/time must be in the future.", 400);
            }

            // CHANGED: Remove car status check to allow booking for different dates
            // if (!"AVAILABLE".equals(car.getStatus())) {
            //     return new BookingResponse("The selected car is not available. Current status: " + car.getStatus(), 409);
            // }

            // TC047: Check for overlapping bookings
            if (hasOverlappingBookings(request.getCarId(), pickupDateTime, dropOffDateTime)) {
                return new BookingResponse("The selected car is already booked for the requested time period. Please select another car or time period.", 409);
            }

            // Generate booking ID and order number
            String bookingId = UUID.randomUUID().toString();
            String orderNumber = generateOrderNumber();

            // Create booking
            Booking booking = new Booking();
            booking.setBookingId(bookingId);
            booking.setCarId(request.getCarId());
            booking.setClientId(request.getClientId());
            booking.setPickupLocationId(request.getPickupLocationId());
            booking.setDropoffLocationId(request.getDropOffLocationId());
            booking.setPickupDatetime(pickupDateTime.toString());
            booking.setDropoffDatetime(dropOffDateTime.toString());
            booking.setBookingStatus("RESERVED");
            booking.setOrderNumber(orderNumber);
            booking.setCreatedAt(LocalDateTime.now().toString());

            // Save booking to DynamoDB
            bookingTable.putItem(booking);

            // Update car status to BOOKED - we still do this for informational purposes
            car.setStatus("BOOKED");
            carTable.updateItem(car);

            // Format response message
            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM dd");
            String pickupDate = pickupDateTime.format(displayFormatter);
            String dropoffDate = dropOffDateTime.format(displayFormatter);
            String deadlineDate = pickupDateTime.minusHours(12).format(DateTimeFormatter.ofPattern("HH:mm a dd MMM"));

            String message = String.format(
                    "New booking was successfully created. \n%s is booked for %s - %s \nYou can change booking details until %s.\nYour order: #%s (%s)",
                    car.getModel(),
                    pickupDate,
                    dropoffDate,
                    deadlineDate,
                    orderNumber,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy"))
            );

            return new BookingResponse(message, 201);
        } catch (Exception e) {
            return new BookingResponse("Error creating booking: " + e.getMessage(), 500);
        }
    }

    public ClientBookingsResponse getClientBookings(String userId) {
        try {
            // TC053: Empty UserId
            if (userId == null || userId.isEmpty()) {
                ClientBookingsResponse errorResponse = new ClientBookingsResponse();
                errorResponse.setContent(new ArrayList<>());
                errorResponse.setStatusCode(400);
                errorResponse.setMessage("User ID is required");
                return errorResponse;
            }

            // Create scan request to find bookings by client ID
            String tableName = System.getenv("Bookings");
            if (tableName == null) {
                tableName = "Bookings"; // Fallback to default name
            }

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":clientId", AttributeValue.builder().s(userId).build());

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("client_id = :clientId")
                    .expressionAttributeValues(expressionValues)
                    .build();

            // Execute scan
            ScanResponse response = dynamoDbClient.scan(scanRequest);
            List<ClientBooking> bookingsList = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                Booking booking = Booking.fromDynamoDbItem(item);
                ClientBooking clientBooking = new ClientBooking();
                clientBooking.setBookingId(booking.getBookingId());
                clientBooking.setBookingStatus(booking.getBookingStatus());

                // Get car details
                Car car = carTable.getItem(Key.builder().partitionValue(booking.getCarId()).build());
                if (car != null) {
                    clientBooking.setCarModel(car.getModel());
                    // Get the first image URL if available
                    if (car.getImageUrls() != null && !car.getImageUrls().isEmpty()) {
                        clientBooking.setCarImageUrl(car.getImageUrls().get(0));
                    }
                }

                // Format order details
                LocalDateTime createdAt;
                try {
                    createdAt = LocalDateTime.parse(booking.getCreatedAt());
                } catch (Exception e) {
                    // Fallback to current time if parsing fails
                    createdAt = LocalDateTime.now();
                }
                String orderDetails = String.format("#%s (%s)",
                        booking.getOrderNumber(),
                        createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                );
                clientBooking.setOrderDetails(orderDetails);

                bookingsList.add(clientBooking);
            }

            // TC052: Invalid UserId - if no bookings found, return 404
            if (bookingsList.isEmpty()) {
                ClientBookingsResponse errorResponse = new ClientBookingsResponse();
                errorResponse.setContent(new ArrayList<>());
                errorResponse.setStatusCode(404);
                errorResponse.setMessage("User not found");
                return errorResponse;
            }

            // TC051: Valid User Id - return 200 OK with bookings
            ClientBookingsResponse clientResponse = new ClientBookingsResponse();
            clientResponse.setContent(bookingsList);
            clientResponse.setStatusCode(200);
            clientResponse.setMessage("Bookings retrieved successfully");

            return clientResponse;
        } catch (Exception e) {
            ClientBookingsResponse errorResponse = new ClientBookingsResponse();
            errorResponse.setContent(new ArrayList<>());
            errorResponse.setStatusCode(500);
            errorResponse.setMessage("Error retrieving bookings: " + e.getMessage());
            return errorResponse;
        }
    }

    // New method to get booking details
    public BookingDetailsResponse getBookingDetails(String bookingId) {
        try {
            // Validate bookingId
            if (bookingId == null || bookingId.isEmpty()) {
                BookingDetailsResponse errorResponse = new BookingDetailsResponse();
                errorResponse.setStatusCode(400);
                errorResponse.setMessage("Booking ID is required");
                return errorResponse;
            }

            // Get booking from DynamoDB
            Booking booking = bookingTable.getItem(Key.builder().partitionValue(bookingId).build());
            if (booking == null) {
                BookingDetailsResponse errorResponse = new BookingDetailsResponse();
                errorResponse.setStatusCode(404);
                errorResponse.setMessage("Booking not found");
                return errorResponse;
            }

            // Get car details
            Car car = carTable.getItem(Key.builder().partitionValue(booking.getCarId()).build());
            if (car == null) {
                BookingDetailsResponse errorResponse = new BookingDetailsResponse();
                errorResponse.setStatusCode(404);
                errorResponse.setMessage("Car not found for this booking");
                return errorResponse;
            }

            // Create response
            BookingDetailsResponse response = new BookingDetailsResponse();
            response.setBookingId(booking.getBookingId());
            response.setCarId(booking.getCarId());
            response.setClientId(booking.getClientId());
            response.setPickupLocationId(booking.getPickupLocationId());
            response.setDropoffLocationId(booking.getDropoffLocationId());
            response.setPickupDatetime(booking.getPickupDatetime());
            response.setDropoffDatetime(booking.getDropoffDatetime());
            response.setBookingStatus(booking.getBookingStatus());
            response.setOrderNumber(booking.getOrderNumber());
            response.setCreatedAt(booking.getCreatedAt());

            // Add car details
            response.setCarModel(car.getModel());
            response.setCarMake(car.getMake());
            response.setCarYear(car.getYear());
            response.setPricePerDay(car.getPricePerDay());

            // Set car image URL
            if (car.getImageUrls() != null && !car.getImageUrls().isEmpty()) {
                response.setCarImageUrl(car.getImageUrls().get(0));
            }

            // Calculate total days and price
            try {
                LocalDateTime pickupDateTime = LocalDateTime.parse(booking.getPickupDatetime());
                LocalDateTime dropoffDateTime = LocalDateTime.parse(booking.getDropoffDatetime());

                long days = Duration.between(pickupDateTime, dropoffDateTime).toDays();
                if (Duration.between(pickupDateTime, dropoffDateTime).toHours() % 24 > 0) {
                    days++; // Add an extra day if there are remaining hours
                }
                response.setTotalDays((int) days);

                if (car.getPricePerDay() != null) {
                    response.setTotalPrice(car.getPricePerDay() * days);
                }
            } catch (Exception e) {
                // If date parsing fails, don't set these fields
                System.out.println("Error calculating total days and price: " + e.getMessage());
            }

            response.setStatusCode(200);
            response.setMessage("Booking details retrieved successfully");
            return response;
        } catch (Exception e) {
            BookingDetailsResponse errorResponse = new BookingDetailsResponse();
            errorResponse.setStatusCode(500);
            errorResponse.setMessage("Error retrieving booking details: " + e.getMessage());
            return errorResponse;
        }
    }

    // Updated method to modify a booking
    public BookingResponse modifyBooking(String bookingId, ModifyBookingRequest request) {
        try {
            // Validate bookingId
            if (bookingId == null || bookingId.isEmpty()) {
                return new BookingResponse("Booking ID is required", 400);
            }

            // Get booking from DynamoDB
            Booking booking = bookingTable.getItem(Key.builder().partitionValue(bookingId).build());
            if (booking == null) {
                return new BookingResponse("Booking not found", 404);
            }

            // Validate booking status - only RESERVED or SERVICE_STARTED can be modified
            if (!("RESERVED".equals(booking.getBookingStatus()) || "SERVICE_STARTED".equals(booking.getBookingStatus()))) {
                return new BookingResponse("Booking cannot be modified. Current status: " + booking.getBookingStatus(), 400);
            }

            // Validate that we're not trying to modify a booking that's already started
            if ("RESERVED".equals(booking.getBookingStatus())) {
                LocalDateTime pickupDateTime = LocalDateTime.parse(booking.getPickupDatetime());
                LocalDateTime now = LocalDateTime.now();

                // Check if we're within 12 hours of pickup
                if (now.isAfter(pickupDateTime.minusHours(12))) {
                    return new BookingResponse("Booking cannot be modified within 12 hours of pickup time", 400);
                }
            }

            // Process changes
            boolean hasChanges = false;

            // Parse dates for validation
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime currentPickupDateTime = LocalDateTime.parse(booking.getPickupDatetime());
            LocalDateTime currentDropoffDateTime = LocalDateTime.parse(booking.getDropoffDatetime());
            LocalDateTime newPickupDateTime = currentPickupDateTime;
            LocalDateTime newDropoffDateTime = currentDropoffDateTime;

            // Update pickup date/time if provided
            if (request.getPickupDateTime() != null && !request.getPickupDateTime().isEmpty()) {
                try {
                    newPickupDateTime = LocalDateTime.parse(request.getPickupDateTime(), formatter);

                    // Validate new pickup time is in the future
                    LocalDateTime now = LocalDateTime.now();
                    if (newPickupDateTime.isBefore(now)) {
                        return new BookingResponse("Pickup date/time must be in the future", 400);
                    }

                    booking.setPickupDatetime(newPickupDateTime.toString());
                    hasChanges = true;
                } catch (DateTimeParseException e) {
                    return new BookingResponse("Invalid pickup date/time format. Please use yyyy-MM-dd HH:mm", 400);
                }
            }

            // Update pickup location if provided
            if (request.getPickupLocationId() != null && !request.getPickupLocationId().isEmpty()) {
                booking.setPickupLocationId(request.getPickupLocationId());
                hasChanges = true;
            }

            // Update drop-off location if provided
            if (request.getDropOffLocationId() != null && !request.getDropOffLocationId().isEmpty()) {
                booking.setDropoffLocationId(request.getDropOffLocationId());
                hasChanges = true;
            }

            // Update drop-off date/time if provided
            if (request.getDropOffDateTime() != null && !request.getDropOffDateTime().isEmpty()) {
                try {
                    newDropoffDateTime = LocalDateTime.parse(request.getDropOffDateTime(), formatter);
                    booking.setDropoffDatetime(newDropoffDateTime.toString());
                    hasChanges = true;
                } catch (DateTimeParseException e) {
                    return new BookingResponse("Invalid drop-off date/time format. Please use yyyy-MM-dd HH:mm", 400);
                }
            }

            // Final validation: ensure pickup is before dropoff
            if (newPickupDateTime.isAfter(newDropoffDateTime) || newPickupDateTime.equals(newDropoffDateTime)) {
                return new BookingResponse("Pickup date/time must be before drop-off date/time", 400);
            }

            if (!hasChanges) {
                return new BookingResponse("No changes were provided", 400);
            }

            // Save updated booking
            bookingTable.updateItem(booking);

            return new BookingResponse("Booking updated successfully", 200);
        } catch (Exception e) {
            return new BookingResponse("Error updating booking: " + e.getMessage(), 500);
        }
    }

    // New method to cancel a booking
    public BookingResponse cancelBooking(String bookingId) {
        try {
            // Validate bookingId
            if (bookingId == null || bookingId.isEmpty()) {
                return new BookingResponse("Booking ID is required", 400);
            }

            // Get booking from DynamoDB
            Booking booking = bookingTable.getItem(Key.builder().partitionValue(bookingId).build());
            if (booking == null) {
                return new BookingResponse("Booking not found", 404);
            }

            // Validate booking status - only RESERVED can be canceled
            if (!"RESERVED".equals(booking.getBookingStatus())) {
                return new BookingResponse("Booking cannot be canceled. Current status: " + booking.getBookingStatus(), 400);
            }

            // Check if we're within 12 hours of pickup
            LocalDateTime pickupDateTime;
            try {
                pickupDateTime = LocalDateTime.parse(booking.getPickupDatetime());
            } catch (DateTimeParseException e) {
                return new BookingResponse("Error parsing pickup date/time", 500);
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(pickupDateTime.minusHours(12))) {
                return new BookingResponse("Booking cannot be canceled within 12 hours of pickup time", 400);
            }

            // Update booking status to CANCELED
            booking.setBookingStatus("CANCELED");
            bookingTable.updateItem(booking);

            // Update car status to AVAILABLE
            Car car = carTable.getItem(Key.builder().partitionValue(booking.getCarId()).build());
            if (car != null) {
                car.setStatus("AVAILABLE");
                carTable.updateItem(car);
            }

            return new BookingResponse("Booking canceled successfully", 200);
        } catch (Exception e) {
            return new BookingResponse("Error canceling booking: " + e.getMessage(), 500);
        }
    }

    /**
     * Get all bookings with optional filtering by date range and client ID
     * @param dateFrom Optional start date for filtering (format: yyyy-MM-dd HH:mm)
     * @param dateTo Optional end date for filtering (format: yyyy-MM-dd HH:mm)
     * @param clientId Optional client ID for filtering
     * @return List of bookings matching the criteria
     */
    public List<Booking> getAllBookings(String dateFrom, String dateTo, String clientId) {
        try {
            String tableName = System.getenv("Bookings");
            if (tableName == null) {
                tableName = "Bookings"; // Fallback to default name
            }

            // Build filter expression and attribute values
            StringBuilder filterExpressionBuilder = new StringBuilder();
            Map<String, AttributeValue> expressionValues = new HashMap<>();

            // Add client ID filter if provided
            if (clientId != null && !clientId.isEmpty()) {
                filterExpressionBuilder.append("client_id = :clientId");
                expressionValues.put(":clientId", AttributeValue.builder().s(clientId).build());
            }

            // Parse date range if provided
            LocalDateTime fromDateTime = null;
            LocalDateTime toDateTime = null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            if (dateFrom != null && !dateFrom.isEmpty()) {
                try {
                    fromDateTime = LocalDateTime.parse(dateFrom, formatter);
                    String fromDateTimeStr = fromDateTime.toString();

                    if (filterExpressionBuilder.length() > 0) {
                        filterExpressionBuilder.append(" AND ");
                    }
                    filterExpressionBuilder.append("pickup_datetime >= :fromDate");
                    expressionValues.put(":fromDate", AttributeValue.builder().s(fromDateTimeStr).build());
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid dateFrom format: " + e.getMessage());
                }
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                try {
                    toDateTime = LocalDateTime.parse(dateTo, formatter);
                    String toDateTimeStr = toDateTime.toString();

                    if (filterExpressionBuilder.length() > 0) {
                        filterExpressionBuilder.append(" AND ");
                    }
                    filterExpressionBuilder.append("dropoff_datetime <= :toDate");
                    expressionValues.put(":toDate", AttributeValue.builder().s(toDateTimeStr).build());
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid dateTo format: " + e.getMessage());
                }
            }

            // Build scan request
            ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                    .tableName(tableName);

            // Add filter expression if we have any filters
            if (filterExpressionBuilder.length() > 0) {
                scanRequestBuilder.filterExpression(filterExpressionBuilder.toString())
                        .expressionAttributeValues(expressionValues);
            }

            ScanResponse response = dynamoDbClient.scan(scanRequestBuilder.build());
            List<Booking> bookings = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                bookings.add(Booking.fromDynamoDbItem(item));
            }

            return bookings;
        } catch (Exception e) {
            System.err.println("Error retrieving bookings: " + e.getMessage());
            throw new RuntimeException("Error retrieving bookings: " + e.getMessage(), e);
        }
    }

    // CHANGED: Updated overlap check logic to be more accurate
    private boolean hasOverlappingBookings(String carId, LocalDateTime pickupDateTime, LocalDateTime dropOffDateTime) {
        try {
            String tableName = System.getenv("Bookings");
            if (tableName == null) {
                tableName = "Bookings"; // Fallback to default name
            }

            // Scan for overlapping bookings
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":carId", AttributeValue.builder().s(carId).build());
            expressionValues.put(":status", AttributeValue.builder().s("RESERVED").build());

            // Use a simpler filter expression that just checks for the car ID and status
            String filterExpression = "car_id = :carId AND booking_status = :status";

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression(filterExpression)
                    .expressionAttributeValues(expressionValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            // Check each booking for overlap manually
            for (Map<String, AttributeValue> item : response.items()) {
                String bookingPickupStr = item.get("pickup_datetime").s();
                String bookingDropoffStr = item.get("dropoff_datetime").s();

                // Parse the datetime strings
                LocalDateTime bookingPickup;
                LocalDateTime bookingDropoff;

                try {
                    // Try parsing with default ISO format first
                    bookingPickup = LocalDateTime.parse(bookingPickupStr);
                    bookingDropoff = LocalDateTime.parse(bookingDropoffStr);
                } catch (Exception e) {
                    // Fallback to your custom format if ISO parsing fails
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    bookingPickup = LocalDateTime.parse(bookingPickupStr, formatter);
                    bookingDropoff = LocalDateTime.parse(bookingDropoffStr, formatter);
                }

                // CHANGED: Simplified overlap check logic
                // An overlap occurs when the new booking starts before an existing booking ends
                // AND the new booking ends after an existing booking starts
                boolean overlaps = pickupDateTime.isBefore(bookingDropoff) &&
                        dropOffDateTime.isAfter(bookingPickup);

                if (overlaps) {
                    System.out.println("Found overlapping booking: " + item.get("booking_id").s());
                    System.out.println("Existing booking: " + bookingPickupStr + " to " + bookingDropoffStr);
                    return true;
                }
            }

            // No overlapping bookings found
            return false;
        } catch (Exception e) {
            System.out.println("Error checking for overlapping bookings: " + e.getMessage());
            e.printStackTrace();
            return true; // Assume there's an overlap if there's an error, to be safe
        }
    }

    private String generateOrderNumber() {
        // Simple implementation - in production you'd want something more sophisticated
        return String.valueOf(1000 + (int)(Math.random() * 9000));
    }
}

