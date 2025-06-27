package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.Repository.CarRepository;
import com.carrentalapp.models.Car;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CarDetailsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient;
    private final String CARS_TABLE;
    private final String LOCATIONS_TABLE;
    private final ObjectMapper objectMapper;
    private final CarRepository carRepository;

    @Inject
    public CarDetailsHandler(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.CARS_TABLE = System.getenv("Cars");
        this.LOCATIONS_TABLE = System.getenv("Locations");
        this.objectMapper = new ObjectMapper();
        this.carRepository = new CarRepository(dynamoDbClient, CARS_TABLE);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        context.getLogger().log("Processing car details request");

        try {
            // Extract car_id from path parameters
            Map<String, String> pathParameters = input.getPathParameters();
            if (pathParameters == null || !pathParameters.containsKey("car_id")) {
                return createErrorResponse(400, "Missing car_id path parameter");
            }

            String car_id = pathParameters.get("car_id");
            context.getLogger().log("Fetching details for car: " + car_id);

            // Get car details
            Optional<Car> carOptional = carRepository.getCarById(car_id);
            if (!carOptional.isPresent()) {
                return createErrorResponse(404, "Car not found with ID: " + car_id);
            }

            Car car = carOptional.get();
            String locationDisplay = "Unknown Location";
            if (car.getLocationId() != null && !car.getLocationId().isEmpty()) {
                context.getLogger().log("Found location ID in car: " + car.getLocationId());

                // Get location details from Locations table
                Map<String, AttributeValue> key = new HashMap<>();
                key.put("location_id", AttributeValue.builder().s(car.getLocationId()).build());

                GetItemRequest request = GetItemRequest.builder()
                        .tableName(LOCATIONS_TABLE)
                        .key(key)
                        .build();

                GetItemResponse response = dynamoDbClient.getItem(request);
                if (response.hasItem()) {
                    context.getLogger().log("Found location in database");

                    // Extract country and city
                    String country = response.item().containsKey("country") ?
                            response.item().get("country").s() : "";
                    String city = response.item().containsKey("city") ?
                            response.item().get("city").s() : "";

                    if (!country.isEmpty() && !city.isEmpty()) {
                        locationDisplay =  city;
                    }  else if (!city.isEmpty()) {
                        locationDisplay = city;
                    }

                    context.getLogger().log("Setting location display to: " + locationDisplay);
                } else {
                    context.getLogger().log("No location found with ID: " + car.getLocationId());
                }
            } else {
                context.getLogger().log("No location ID found in car");
            }
            // Build the response in the exact format required
            Map<String, Object> carDetails = new HashMap<>();
            carDetails.put("car_id", car.getCarId());
            carDetails.put("model", car.getMake() + " " + car.getModel() + " " + car.getYear());
            carDetails.put("pricePerDay", car.getPricePerDay().toString());
            carDetails.put("carRating", car.getRating().toString());
            carDetails.put("serviceRating", car.getServiceRating());
//            carDetails.put("location", car.getLocationName());
            carDetails.put("location", locationDisplay);
            carDetails.put("status", car.getStatus()); // Get status directly from car object
            carDetails.put("gearBoxType", car.getGearBoxType());
            carDetails.put("fuelType", car.getFuelType());
            carDetails.put("engineCapacity", car.getEngineCapacity());
            carDetails.put("fuelConsumption", car.getFuelConsumption());
            carDetails.put("passengerCapacity", car.getPassengerCapacity());
            carDetails.put("climateControlOption", car.getClimateControlOption());
            carDetails.put("category", car.getCategory());

            // Rename imageUrls to images as per the expected format
            carDetails.put("images", car.getImageUrls());

            // Create the response
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
            response.setBody(objectMapper.writeValueAsString(carDetails));
            return response;

        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
        try {
            response.setBody(objectMapper.writeValueAsString(Map.of("error", message)));
        } catch (Exception e) {
            response.setBody("{\"error\":\"" + message + "\"}");
        }
        return response;
    }
}