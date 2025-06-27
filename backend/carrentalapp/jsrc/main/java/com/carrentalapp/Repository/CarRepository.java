package com.carrentalapp.Repository;

import com.amazonaws.services.lambda.runtime.Context;
import com.carrentalapp.models.Car;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class CarRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public CarRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public Optional<Car> getCarById(String car_id) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("car_id", AttributeValue.builder().s(car_id).build()))
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (!response.hasItem() || response.item().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToCar(response.item()));
        } catch (Exception e) {
            System.err.println("Error fetching car by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Car mapToCar(Map<String, AttributeValue> item) {
        Car car = new Car();
        car.setCarId(item.containsKey("car_id") ? item.get("car_id").s() : "");
        car.setMake(item.containsKey("make") ? item.get("make").s() : "");
        car.setModel(item.containsKey("model") ? item.get("model").s() : "");
        car.setYear(item.containsKey("year") ? item.get("year").n() : "");
        car.setPricePerDay(item.containsKey("price_per_day") ? Double.parseDouble(item.get("price_per_day").n()) : 0.0);
        car.setRating(item.containsKey("car_rating") ? Double.parseDouble(item.get("car_rating").n()) : 0.0);
        car.setServiceRating(item.containsKey("service_rating") ? Double.parseDouble(item.get("service_rating").n()) : 0.0);
        car.setLocationId(item.containsKey("location_id") ? item.get("location_id").s() : "");
        car.setLocationName(item.containsKey("location_name") ? item.get("location_name").s() : "");
        car.setCategory(item.containsKey("category") ? item.get("category").s() : "");
        car.setGearBoxType(item.containsKey("gear_box_type") ? item.get("gear_box_type").s() : "");
        car.setFuelType(item.containsKey("fuel_type") ? item.get("fuel_type").s() : "");
        car.setStatus(item.containsKey("status") ? item.get("status").s() : "UNAVAILABLE"); // Add status field

        // Additional fields for detailed view
        car.setEngineCapacity(item.containsKey("engine_capacity") ? item.get("engine_capacity").s() : "");
        if (item.containsKey("fuel_consumption")) {
            AttributeValue fuelConsumptionAttr = item.get("fuel_consumption");
            try {
                if (fuelConsumptionAttr.n() != null && !fuelConsumptionAttr.n().isEmpty()) {
                    // If stored as a number
                    car.setFuelConsumption(Double.parseDouble(fuelConsumptionAttr.n()));
                } else if (fuelConsumptionAttr.s() != null && !fuelConsumptionAttr.s().isEmpty()) {
                    // If stored as a string
                    car.setFuelConsumption(Double.parseDouble(fuelConsumptionAttr.s()));
                } else {
                    car.setFuelConsumption(0.0); // Default value
                }
            } catch (Exception e) {
                car.setFuelConsumption(0.0); // Default on error
            }
        } else {
            car.setFuelConsumption(0.0); // Default if missing
        }
        car.setPassengerCapacity(item.containsKey("passenger_capacity") ? item.get("passenger_capacity").s() : "");
        car.setClimateControlOption(item.containsKey("climate_control_option") ? item.get("climate_control_option").s() : "");

        // Handle image URLs
        if (item.containsKey("imageUrls")) {
            if (item.get("imageUrls").hasL()) {
                car.setImageUrls(item.get("imageUrls").l().stream()
                        .map(av -> av.s())
                        .collect(Collectors.toList()));
            } else if (item.get("imageUrls").hasSs()) {
                car.setImageUrls(new ArrayList<>(item.get("imageUrls").ss()));
            }
        }
        else if (item.containsKey("image_urs")) {
            car.setImageUrls(List.of(item.get("image_url").s()));
        }
        else {
            car.setImageUrls(new ArrayList<>());
        }

        return car;
    }
    // Add this method to update all car records with default values
    public void updateAllCarStatuses() {
        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            for (Map<String, AttributeValue> item : response.items()) {
                if (item.containsKey("car_id")) {
                    String car_id = item.get("car_id").s();

                    // Create update expression with all fields that should have default values
                    Map<String, AttributeValueUpdate> updates = new HashMap<>();

                    // Only update status if it's missing
                    if (!item.containsKey("status")) {
                        updates.put("status", AttributeValueUpdate.builder()
                                .value(AttributeValue.builder().s("AVAILABLE").build())
                                .action(AttributeAction.PUT)
                                .build());
                    }

                    // Add other fields that should have default values if missing
                    if (!item.containsKey("gear_box_type") || item.get("gear_box_type").s().isEmpty()) {
                        updates.put("gear_box_type", AttributeValueUpdate.builder()
                                .value(AttributeValue.builder().s("Automatic").build())
                                .action(AttributeAction.PUT)
                                .build());
                    }

                    // Only perform update if there are fields to update
                    if (!updates.isEmpty()) {
                        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                                .tableName(tableName)
                                .key(Map.of("car_id", AttributeValue.builder().s(car_id).build()))
                                .attributeUpdates(updates)
                                .build();

                        dynamoDbClient.updateItem(updateRequest);
                    }
                }
            }

            // Handle pagination if needed
            while (response.hasLastEvaluatedKey()) {
                scanRequest = ScanRequest.builder()
                        .tableName(tableName)
                        .exclusiveStartKey(response.lastEvaluatedKey())
                        .build();

                response = dynamoDbClient.scan(scanRequest);

                // Process items as above
                for (Map<String, AttributeValue> item : response.items()) {
                    if (item.containsKey("car_id")) {
                        String car_id = item.get("car_id").s();

                        Map<String, AttributeValueUpdate> updates = new HashMap<>();

                        if (!item.containsKey("status")) {
                            updates.put("status", AttributeValueUpdate.builder()
                                    .value(AttributeValue.builder().s("AVAILABLE").build())
                                    .action(AttributeAction.PUT)
                                    .build());
                        }

                        if (!item.containsKey("gear_box_type") || item.get("gear_box_type").s().isEmpty()) {
                            updates.put("gear_box_type", AttributeValueUpdate.builder()
                                    .value(AttributeValue.builder().s("Automatic").build())
                                    .action(AttributeAction.PUT)
                                    .build());
                        }

                        if (!updates.isEmpty()) {
                            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                                    .tableName(tableName)
                                    .key(Map.of("car_id", AttributeValue.builder().s(car_id).build()))
                                    .attributeUpdates(updates)
                                    .build();

                            dynamoDbClient.updateItem(updateRequest);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating car records: " + e.getMessage());
        }
    }

    // Add this method to update a specific car record
    public void updateSpecificCar(String car_id) {
        Map<String, AttributeValue> key = Map.of("car_id", AttributeValue.builder().s(car_id).build());

        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("status", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s("AVAILABLE").build())
                .action(AttributeAction.PUT)
                .build());
        updates.put("gear_box_type", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s("Automatic").build())
                .action(AttributeAction.PUT)
                .build());
        updates.put("location_name", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s("Default Location").build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .attributeUpdates(updates)
                .build();

        try {
            dynamoDbClient.updateItem(request);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error updating car: " + e.getMessage(), e);
        }
    }
    // Add this method to your CarRepository or create a LocationRepository
    public String getLocationNameById(String locationId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("location_id", AttributeValue.builder().s(locationId).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName("Locations")
                .key(key)
                .build();

        try {
            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.hasItem()) {
                return response.item().get("location_name").s();
            }
        } catch (Exception e) {
            // Log error
        }

        return "Unknown Location";
    }
    // Get multiple cars by their IDs
    public List<Car> getCarsByIds(Collection<String> car_ids, Context context) {
        if (car_ids.isEmpty()) return Collections.emptyList();

        List<Car> cars = new ArrayList<>();
        List<String> car_idList = new ArrayList<>(car_ids);

        for (int i = 0; i < car_idList.size(); i += 100) {
            List<String> batch = car_idList.subList(i, Math.min(i + 100, car_idList.size()));

            Map<String, KeysAndAttributes> requestItems = Map.of(
                    tableName,
                    KeysAndAttributes.builder()
                            .keys(batch.stream()
                                    .map(id -> Map.of("car_id", AttributeValue.builder().s(id).build()))
                                    .collect(Collectors.toList()))
                            .build()
            );

            BatchGetItemRequest request = BatchGetItemRequest.builder()
                    .requestItems(requestItems)
                    .build();

            try {
                BatchGetItemResponse response = dynamoDbClient.batchGetItem(request);
                if (response.hasResponses() && response.responses().containsKey(tableName)) {
                    cars.addAll(response.responses().get(tableName).stream()
                            .map(this::mapToCar)
                            .collect(Collectors.toList()));
                }

                Map<String, KeysAndAttributes> unprocessed = response.unprocessedKeys();
                while (unprocessed != null && !unprocessed.isEmpty()) {
                    response = dynamoDbClient.batchGetItem(
                            BatchGetItemRequest.builder().requestItems(unprocessed).build()
                    );
                    if (response.hasResponses() && response.responses().containsKey(tableName)) {
                        cars.addAll(response.responses().get(tableName).stream()
                                .map(this::mapToCar)
                                .collect(Collectors.toList()));
                    }
                    unprocessed = response.unprocessedKeys();
                }

            } catch (DynamoDbException e) {
                context.getLogger().log("Error fetching batch cars: " + e.getMessage());
            }
        }

        return cars;
    }

    // Get cars based on filters - overloaded method without status parameter
    public List<Car> getFilteredCars(String locationId, String category, String gearBoxType,
                                     String engineType, Double minPrice, Double maxPrice) {
        return getFilteredCars(locationId, category, gearBoxType, engineType, minPrice, maxPrice, null);
    }

    // Get cars based on filters - with status parameter
    public List<Car> getFilteredCars(String locationId, String category, String gearBoxType,
                                     String engineType, Double minPrice, Double maxPrice, String status) {
        List<String> filters = new ArrayList<>();
        Map<String, AttributeValue> values = new HashMap<>();
        Map<String, String> names = new HashMap<>();

        if (locationId != null && !locationId.isEmpty()) {
            filters.add("location_id = :locationId");
            values.put(":locationId", AttributeValue.builder().s(locationId).build());
        }

        if (category != null && !category.isEmpty()) {
            filters.add("category = :category");
            values.put(":category", AttributeValue.builder().s(category).build());
        }

        if (gearBoxType != null && !gearBoxType.isEmpty()) {
            filters.add("gear_box_type = :gearBoxType");
            values.put(":gearBoxType", AttributeValue.builder().s(gearBoxType).build());
        }

        if (engineType != null && !engineType.isEmpty()) {
            filters.add("fuel_type = :fuelType");
            values.put(":fuelType", AttributeValue.builder().s(engineType).build());
        }

        if (minPrice != null) {
            filters.add("price_per_day >= :minPrice");
            values.put(":minPrice", AttributeValue.builder().n(minPrice.toString()).build());
        }

        if (maxPrice != null) {
            filters.add("price_per_day <= :maxPrice");
            values.put(":maxPrice", AttributeValue.builder().n(maxPrice.toString()).build());
        }

        // Add status filter if provided
        if (status != null && !status.isEmpty()) {
            filters.add("#status = :status");
            names.put("#status", "status");
            values.put(":status", AttributeValue.builder().s(status).build());
        }

        ScanRequest.Builder scanBuilder = ScanRequest.builder().tableName(tableName);

        if (!filters.isEmpty()) {
            scanBuilder.filterExpression(String.join(" AND ", filters));

            if (!values.isEmpty()) {
                scanBuilder.expressionAttributeValues(values);
            }

            if (!names.isEmpty()) {
                scanBuilder.expressionAttributeNames(names);
            }
        }

        List<Car> cars = new ArrayList<>();

        try {
            ScanResponse response = dynamoDbClient.scan(scanBuilder.build());
            cars.addAll(response.items().stream().map(this::mapToCar).collect(Collectors.toList()));

            while (response.hasLastEvaluatedKey() && !response.lastEvaluatedKey().isEmpty()) {
                scanBuilder.exclusiveStartKey(response.lastEvaluatedKey());
                response = dynamoDbClient.scan(scanBuilder.build());
                cars.addAll(response.items().stream().map(this::mapToCar).collect(Collectors.toList()));
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error scanning filtered cars: " + e.getMessage(), e);
        }

        return cars;
    }

    // Save a car record
    public Car saveCar(Car car) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("car_id", AttributeValue.builder().s(car.getCarId()).build());

        if (car.getMake() != null) {
            item.put("make", AttributeValue.builder().s(car.getMake()).build());
        }

        if (car.getModel() != null) {
            item.put("model", AttributeValue.builder().s(car.getModel()).build());
        }

        if (car.getYear() != null) {
            item.put("year", AttributeValue.builder().n(car.getYear()).build());
        }

        if (car.getPricePerDay() != null) {
            item.put("price_per_day", AttributeValue.builder().n(car.getPricePerDay().toString()).build());
        }

        if (car.getRating() != null) {
            item.put("car_rating", AttributeValue.builder().n(car.getRating().toString()).build());
        }

        if (car.getServiceRating() != null) {
            item.put("service_rating", AttributeValue.builder().n(car.getServiceRating().toString()).build());
        }

        if (car.getLocationId() != null) {
            item.put("location_id", AttributeValue.builder().s(car.getLocationId()).build());
        }

        if (car.getLocationName() != null) {
            item.put("location_name", AttributeValue.builder().s(car.getLocationName()).build());
        }

        if (car.getCategory() != null) {
            item.put("category", AttributeValue.builder().s(car.getCategory()).build());
        }

        if (car.getGearBoxType() != null) {
            item.put("gear_box_type", AttributeValue.builder().s(car.getGearBoxType()).build());
        }

        if (car.getFuelType() != null) {
            item.put("fuel_type", AttributeValue.builder().s(car.getFuelType()).build());
        }

        if (car.getEngineCapacity() != null) {
            item.put("engine_capacity", AttributeValue.builder().s(car.getEngineCapacity()).build());
        }

        if (car.getFuelConsumption() != null) {
            item.put("fuel_consumption", AttributeValue.builder().s(car.getFuelConsumption().toString()).build());
        }

        if (car.getPassengerCapacity() != null) {
            item.put("passenger_capacity", AttributeValue.builder().s(car.getPassengerCapacity()).build());
        }

        if (car.getClimateControlOption() != null) {
            item.put("climate_control_option", AttributeValue.builder().s(car.getClimateControlOption()).build());
        }

        if (car.getStatus() != null) {
            item.put("status", AttributeValue.builder().s(car.getStatus()).build());
        } else {
            item.put("status", AttributeValue.builder().s("UNAVAILABLE").build());
        }

        if (car.getImageUrls() != null && !car.getImageUrls().isEmpty()) {
            List<AttributeValue> imageUrlsAttributeValues = car.getImageUrls().stream()
                    .map(url -> AttributeValue.builder().s(url).build())
                    .collect(Collectors.toList());
            item.put("imageUrls", AttributeValue.builder().l(imageUrlsAttributeValues).build());

            // Set the first image as image_url for compatibility
//            item.put("image_url", AttributeValue.builder().s(car.getImageUrls().get(0)).build());
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        try {
            dynamoDbClient.putItem(request);
            return car;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error saving car: " + e.getMessage(), e);
        }
    }

    // Update car status
    public void updateCarStatus(String car_id, String newStatus) {
        Map<String, AttributeValue> key = Map.of("car_id", AttributeValue.builder().s(car_id).build());

        Map<String, AttributeValueUpdate> updates = Map.of(
                "status", AttributeValueUpdate.builder()
                        .value(AttributeValue.builder().s(newStatus).build())
                        .action(AttributeAction.PUT)
                        .build()
        );

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .attributeUpdates(updates)
                .build();

        try {
            dynamoDbClient.updateItem(request);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error updating car status: " + e.getMessage(), e);
        }
    }

    // Delete a car by ID
    public void deleteCar(String car_id) {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("car_id", AttributeValue.builder().s(car_id).build()))
                .build();

        try {
            dynamoDbClient.deleteItem(request);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Error deleting car: " + e.getMessage(), e);
        }
    }
}