package com.carrentalapp.Repository;

//package com.carrentalapp.Repository;

import com.carrentalapp.models.Location;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class LocationRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public LocationRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    /**
     * Get a location by its ID
     * @param locationId The location ID to look up
     * @return Optional containing the location if found
     */
    public Optional<Location> getLocationById(String locationId) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("location_id", AttributeValue.builder().s(locationId).build()))
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (!response.hasItem() || response.item().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToLocation(response.item()));
        } catch (Exception e) {
            System.err.println("Error fetching location by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get all locations
     * @return List of all locations
     */
    public List<Location> getAllLocations() {
        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            List<Location> locations = response.items().stream()
                    .map(this::mapToLocation)
                    .collect(Collectors.toList());

            // Handle pagination if there are more results
            Map<String, AttributeValue> lastEvaluatedKey = response.lastEvaluatedKey();
            while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
                scanRequest = ScanRequest.builder()
                        .tableName(tableName)
                        .exclusiveStartKey(lastEvaluatedKey)
                        .build();

                response = dynamoDbClient.scan(scanRequest);
                locations.addAll(response.items().stream()
                        .map(this::mapToLocation)
                        .collect(Collectors.toList()));

                lastEvaluatedKey = response.lastEvaluatedKey();
            }

            return locations;
        } catch (DynamoDbException e) {
            System.err.println("Error scanning locations: " + e.getMessage());
            throw new RuntimeException("Error scanning locations: " + e.getMessage(), e);
        }
    }

    /**
     * Map DynamoDB item to Location object
     * @param item DynamoDB item
     * @return Location object
     */
    private Location mapToLocation(Map<String, AttributeValue> item) {
        Location location = new Location();

        if (item.containsKey("location_id")) {
            location.setLocationId(item.get("location_id").s());
        }

        if (item.containsKey("name")) {
            location.setName(item.get("name").s());
        }

        if (item.containsKey("address")) {
            location.setAddress(item.get("address").s());
        }

        if (item.containsKey("city")) {
            location.setCity(item.get("city").s());
        }

        if (item.containsKey("country")) {
            location.setCountry(item.get("country").s());
        }

        return location;
    }
}
