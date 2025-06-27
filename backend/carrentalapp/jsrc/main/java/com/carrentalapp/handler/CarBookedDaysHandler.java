package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.Repository.BookingRepository;
import com.carrentalapp.models.Booking;
import com.google.gson.Gson;

import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CarBookedDaysHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final BookingRepository bookingRepository;

    @Inject
    public CarBookedDaysHandler(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String carId = request.getPathParameters().get("car_id");

        try {
            // Fetch bookings for the car
            List<Booking> bookings = bookingRepository.getBookingsByCarId(carId, context);
            if (bookings.isEmpty()) {
                // No bookings found for the given carId, return 404
                APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
                errorResponse.setStatusCode(404);
                errorResponse.setBody("{\"message\": \"No bookings found for car with ID: " + carId + "\"}");
                return errorResponse;
            }
            Set<String> bookedDates = new HashSet<>();

            // Loop through the bookings to extract booked dates
            for (Booking booking : bookings) {
                if (booking.getPickupDatetimeAsInstant() != null && booking.getDropoffDatetimeAsInstant() != null) {
                    Instant pickupInstant = booking.getPickupDatetimeAsInstant();
                    Instant dropoffInstant = booking.getDropoffDatetimeAsInstant();

                    // Convert Instant to LocalDate (with the system default time zone)
                    LocalDate pickupDate = pickupInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate dropoffDate = dropoffInstant.atZone(ZoneId.systemDefault()).toLocalDate();

                    // Add all dates between pickup and dropoff to the bookedDates set
                    while (!pickupDate.isAfter(dropoffDate)) {
                        bookedDates.add(pickupDate.format(DateTimeFormatter.ISO_LOCAL_DATE));  // Format as "yyyy-MM-dd"
                        pickupDate = pickupDate.plusDays(1);
                    }
                }
            }

            // Sort the booked dates
            List<String> sortedBookedDates = bookedDates.stream()
                    .sorted()
                    .collect(Collectors.toList());

            // Prepare the response with the sorted booked dates
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("content", sortedBookedDates);

            // Convert map to JSON and return response
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody(new Gson().toJson(responseMap));  // Use Gson to convert to JSON
            return response;

        } catch (Exception e) {
            // Error handling and logging
            context.getLogger().log("Error fetching booked days: " + e.getMessage());
            APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
            errorResponse.setStatusCode(500);
            errorResponse.setBody("{\"message\": \"Internal Server Error\"}");
            return errorResponse;
        }
    }
}