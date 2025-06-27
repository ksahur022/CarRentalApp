package com.carrentalapp.services;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public class PopularCarsService {

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "Cars";
    private static final String AVAILABLE_STATUS = "AVAILABLE";

    @Inject
    public PopularCarsService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public Map<String, List<Map<String, String>>> getPopularCars() {
        System.out.println("Starting getPopularCars method");
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("#status = :statusValue")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(":statusValue", AttributeValue.builder().s(AVAILABLE_STATUS).build()))
                .build();

//        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        try {
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            System.out.println("Scan completed successfully");
            System.out.println("Number of available cars: " + scanResponse.items().size());

            List<Map<String, String>> popularAvailableCars = scanResponse.items().stream()
                    .map(this::mapToCar)
                    .sorted((c1, c2) -> Double.compare(Double.parseDouble(c2.get("serviceRating")), Double.parseDouble(c1.get("serviceRating"))))
                    .limit(4)
                    .collect(Collectors.toList());

            Map<String, List<Map<String, String>>> result = new HashMap<>();
            result.put("content", popularAvailableCars);
            System.out.println("Returning result with " + popularAvailableCars.size() + " popular available cars");
            return result;
        } catch (Exception e) {
            System.err.println("Error during scan: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private Map<String, String> mapToCar(Map<String, AttributeValue> item) {
        Map<String, String> carMap = new HashMap<>();
        carMap.put("carId", item.getOrDefault("car_id", AttributeValue.builder().s("").build()).s());
        carMap.put("model", item.getOrDefault("model", AttributeValue.builder().s("").build()).s());
        carMap.put("imageUrl", item.getOrDefault("image_url", AttributeValue.builder().s("").build()).s());
        carMap.put("pricePerDay", item.getOrDefault("price_per_day", AttributeValue.builder().n("0").build()).n());
        carMap.put("carRating", item.getOrDefault("car_rating", AttributeValue.builder().n("0").build()).n());
        carMap.put("serviceRating", item.getOrDefault("service_rating", AttributeValue.builder().n("0").build()).n());
        carMap.put("status", item.getOrDefault("status", AttributeValue.builder().s("").build()).s());
        carMap.put("location", item.getOrDefault("location_id", AttributeValue.builder().s("").build()).s());
        return carMap;
    }
}
