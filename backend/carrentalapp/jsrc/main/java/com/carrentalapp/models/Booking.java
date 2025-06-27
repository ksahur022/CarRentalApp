package com.carrentalapp.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@DynamoDbBean
public class Booking {
    private String bookingId;
    private String carId;
    private String clientId;
    private String pickupLocationId;
    private String dropoffLocationId;
    private String pickupDatetime;
    private String dropoffDatetime;
    private String bookingStatus;
    private String orderNumber;
    private String createdAt;

    public Booking() {
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("booking_id")  // Add this annotation to map to DynamoDB attribute name
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    @DynamoDbAttribute("car_id")  // Add attribute mapping
    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"client-index"})
    @DynamoDbAttribute("client_id")  // Add attribute mapping
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @DynamoDbAttribute("pickup_location_id")  // Add attribute mapping
    public String getPickupLocationId() {
        return pickupLocationId;
    }

    public void setPickupLocationId(String pickupLocationId) {
        this.pickupLocationId = pickupLocationId;
    }

    @DynamoDbAttribute("dropoff_location_id")  // Add attribute mapping
    public String getDropoffLocationId() {
        return dropoffLocationId;
    }

    public void setDropoffLocationId(String dropoffLocationId) {
        this.dropoffLocationId = dropoffLocationId;
    }

    @DynamoDbAttribute("pickup_datetime")  // Add attribute mapping
    public String getPickupDatetime() {
        return pickupDatetime;
    }

    public Instant getPickupDatetimeAsInstant() {
        if (pickupDatetime == null || pickupDatetime.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(pickupDatetime);
        } catch (DateTimeParseException e) {
            // Try parsing as LocalDateTime and convert to Instant
            return LocalDateTime.parse(pickupDatetime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }
    }

    public void setPickupDatetime(String pickupDatetime) {
        this.pickupDatetime = pickupDatetime;
    }

    @DynamoDbAttribute("dropoff_datetime")  // Add attribute mapping
    public String getDropoffDatetime() {
        return dropoffDatetime;
    }

    public Instant getDropoffDatetimeAsInstant() {
        if (dropoffDatetime == null || dropoffDatetime.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(dropoffDatetime);
        } catch (DateTimeParseException e) {
            // Try parsing as LocalDateTime and convert to Instant
            return LocalDateTime.parse(dropoffDatetime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }
    }

    public void setDropoffDatetime(String dropoffDatetime) {
        this.dropoffDatetime = dropoffDatetime;
    }

    @DynamoDbAttribute("booking_status")  // Add attribute mapping
    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    @DynamoDbAttribute("order_number")  // Add attribute mapping
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    @DynamoDbAttribute("created_at")  // Add attribute mapping
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // Helper method to convert from DynamoDB item
    public static Booking fromDynamoDbItem(Map<String, AttributeValue> item) {
        Booking booking = new Booking();

        booking.setBookingId(item.containsKey("booking_id") ? item.get("booking_id").s() : "");
        booking.setCarId(item.containsKey("car_id") ? item.get("car_id").s() : "");
        booking.setClientId(item.containsKey("client_id") ? item.get("client_id").s() : "");
        booking.setPickupLocationId(item.containsKey("pickup_location_id") ? item.get("pickup_location_id").s() : "");
        booking.setDropoffLocationId(item.containsKey("dropoff_location_id") ? item.get("dropoff_location_id").s() : "");
        booking.setPickupDatetime(item.containsKey("pickup_datetime") ? item.get("pickup_datetime").s() : "");
        booking.setDropoffDatetime(item.containsKey("dropoff_datetime") ? item.get("dropoff_datetime").s() : "");
        booking.setBookingStatus(item.containsKey("booking_status") ? item.get("booking_status").s() : "");
        booking.setOrderNumber(item.containsKey("order_number") ? item.get("order_number").s() : "");
        booking.setCreatedAt(item.containsKey("created_at") ? item.get("created_at").s() : "");

        return booking;
    }
}