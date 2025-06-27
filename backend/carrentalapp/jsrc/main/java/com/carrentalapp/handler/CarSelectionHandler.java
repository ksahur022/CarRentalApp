//package com.carrentalapp.handler;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
//import com.carrentalapp.Repository.CarRepository;
//import com.carrentalapp.models.Car;
//import com.carrentalapp.models.Location;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
//import software.amazon.awssdk.services.dynamodb.model.*;
//
//import javax.inject.Inject;
////import javax.xml.stream.Location;
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.format.DateTimeParseException;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class CarSelectionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
//    private final DynamoDbClient dynamoDbClient;
//    private final String CARS_TABLE;
//    private final String LOCATIONS_TABLE;
//    private final ObjectMapper objectMapper;
//    private final CarRepository carRepository;
//
//    @Inject
//    public CarSelectionHandler(DynamoDbClient dynamoDbClient) {
//        this.dynamoDbClient = dynamoDbClient;
//        this.CARS_TABLE = System.getenv("Cars");
//        this.LOCATIONS_TABLE = System.getenv("Locations");
//        this.objectMapper = new ObjectMapper();
//        this.carRepository = new CarRepository(dynamoDbClient, CARS_TABLE);
//        this.locationRepository = new LocationRepository(dynamoDbClient, LOCATIONS_TABLE); // Initialize
//
//    }
//
//    @Override
//    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
//        context.getLogger().log("Processing car filtering request");
//
//        try {
//            Map<String, String> queryParams = input.getQueryStringParameters();
//            if (queryParams == null) {
//                queryParams = new HashMap<>();
//            }
//
//            String pickupLocationId = queryParams.get("pickupLocationId");
//            String pickupLocationName = queryParams.get("pickupLocationName");
//            String dropOffLocationId = queryParams.get("dropOffLocationId");
//            String pickupDateTimeStr = queryParams.get("pickupDateTime");
//            String dropOffDateTimeStr = queryParams.get("dropOffDateTime");
//            String category = queryParams.get("category");
//            String gearBoxType = queryParams.get("gearBoxType");
//            String engineType = queryParams.get("engineType");
//            String minPriceStr = queryParams.get("minPrice");
//            String maxPriceStr = queryParams.get("maxPrice");
//            String serviceRatingStr = queryParams.get("serviceRating");
//            // If we have a location name but no ID, look up the ID
//            if ((pickupLocationId == null || pickupLocationId.isEmpty()) &&
//                    pickupLocationName != null && !pickupLocationName.isEmpty()) {
//
//                context.getLogger().log("Looking up location ID for name: " + pickupLocationName);
//
//                // Find location by name
//                List<Location> locations = locationRepository.getAllLocations();
//                for (Location location : locations) {
//                    if (pickupLocationName.equalsIgnoreCase(location.getName())) {
//                        pickupLocationId = location.getLocationId();
//                        context.getLogger().log("Found location ID: " + pickupLocationId);
//                        break;
//                    }
//                }
//                if (pickupLocationId == null) {
//                    context.getLogger().log("No location found with name: " + pickupLocationName);
//                    return createErrorResponse(400, "Invalid pickup location name");
//                }
//            }
//            // Parse service rating filter if provided
//            final Double minServiceRating;  // Make it effectively final for lambda
//            if (serviceRatingStr != null && !serviceRatingStr.isEmpty()) {
//                try {
//                    double rating = Double.parseDouble(serviceRatingStr);
//                    if (rating < 0 || rating > 5) {
//                        return createErrorResponse(400, "Service rating must be between 0 and 5");
//                    }
//                    minServiceRating = rating;
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid service rating format");
//                }
//            } else {
//                minServiceRating = null;
//            }
//
//            int page = 1;
//            int size = 8;
//            if (queryParams.containsKey("page")) {
//                try {
//                    page = Integer.parseInt(queryParams.get("page"));
//                    if (page < 1) page = 1;
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid page parameter");
//                }
//            }
//            if (queryParams.containsKey("size")) {
//                try {
//                    size = Integer.parseInt(queryParams.get("size"));
//                    if (size < 1) size = 8;
//                    if (size > 50) size = 50;
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid size parameter");
//                }
//            }
//            int internalPage = page - 1;
//
//            Instant pickupDateTime = null;
//            Instant dropOffDateTime = null;
//            if (pickupDateTimeStr != null && !pickupDateTimeStr.isEmpty()) {
//                try {
//                    pickupDateTimeStr = pickupDateTimeStr.endsWith("Z") ? pickupDateTimeStr : pickupDateTimeStr + "Z";
//                    pickupDateTime = Instant.parse(pickupDateTimeStr);
//                } catch (DateTimeParseException e) {
//                    return createErrorResponse(400, "Invalid pickup date format. Use ISO 8601 format (YYYY-MM-DDTHH:MM:SS)");
//                }
//            }
//            if (dropOffDateTimeStr != null && !dropOffDateTimeStr.isEmpty()) {
//                try {
//                    dropOffDateTimeStr = dropOffDateTimeStr.endsWith("Z") ? dropOffDateTimeStr : dropOffDateTimeStr + "Z";
//                    dropOffDateTime = Instant.parse(dropOffDateTimeStr);
//                } catch (DateTimeParseException e) {
//                    return createErrorResponse(400, "Invalid drop-off date format. Use ISO 8601 format (YYYY-MM-DDTHH:MM:SS)");
//                }
//            }
//
//            if (pickupDateTime != null && dropOffDateTime != null) {
//                if (dropOffDateTime.isBefore(pickupDateTime)) {
//                    return createErrorResponse(400, "Drop-off date must be after pickup date");
//                }
//                long minRentalSeconds = 4 * 60 * 60;
//                if (dropOffDateTime.getEpochSecond() - pickupDateTime.getEpochSecond() < minRentalSeconds) {
//                    return createErrorResponse(400, "Minimum rental period is 4 hours");
//                }
//            }
//
//            Double minPrice = null;
//            Double maxPrice = null;
//            if (minPriceStr != null && !minPriceStr.isEmpty()) {
//                try {
//                    minPrice = Double.parseDouble(minPriceStr);
//                    if (minPrice < 0) return createErrorResponse(400, "Minimum price cannot be negative");
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid minimum price format");
//                }
//            }
//            if (maxPriceStr != null && !maxPriceStr.isEmpty()) {
//                try {
//                    maxPrice = Double.parseDouble(maxPriceStr);
//                    if (maxPrice < 0) return createErrorResponse(400, "Maximum price cannot be negative");
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid maximum price format");
//                }
//            }
//            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
//                return createErrorResponse(400, "Minimum price cannot be greater than maximum price");
//            }
//
//            Map<String, String> carAvailabilityMap = getCarAvailabilityMap(context);
//            Set<String> availableCarIds = new HashSet<>();
//            if (pickupDateTime != null && dropOffDateTime != null) {
//                availableCarIds = getAvailableCarIds(
//                        pickupDateTime, dropOffDateTime, pickupLocationId,
//                        category, gearBoxType, engineType, minPrice, maxPrice, context
//                );
//                if (availableCarIds.isEmpty()) {
//                    Map<String, Object> responseBody = new HashMap<>();
//                    responseBody.put("content", new ArrayList<>());
//                    responseBody.put("currentPage", page);
//                    responseBody.put("totalElements", 0);
//                    responseBody.put("totalPages", 0);
//                    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
//                    response.setStatusCode(200);
//                    response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
//                    response.setBody(objectMapper.writeValueAsString(responseBody));
//                    return response;
//                }
//            }
//            // Enrich cars with location information if needed
//            if (filteredCars != null && !filteredCars.isEmpty()) {
//                Map<String, Location> locationCache = new HashMap<>(); // Cache to avoid repeated lookups
//
//                for (Car car : filteredCars) {
//                    if (car.getLocationId() != null && !car.getLocationId().isEmpty() &&
//                            (car.getLocationName() == null || car.getLocationName().isEmpty())) {
//
//                        // Check cache first
//                        Location location = locationCache.get(car.getLocationId());
//                        if (location == null) {
//                            // Not in cache, look it up
//                            Optional<Location> locationOpt = locationRepository.getLocationById(car.getLocationId());
//                            if (locationOpt.isPresent()) {
//                                location = locationOpt.get();
//                                locationCache.put(car.getLocationId(), location);
//                            }
//                        }
//
//                        // Set location name if found
//                        if (location != null) {
//                            car.setLocationName(location.getName());
//                        }
//                    }
//                }
//            }
//
//            List<Car> filteredCars = pickupDateTime != null && dropOffDateTime != null
//                    ? carRepository.getCarsByIds(availableCarIds, context)
//                    : carRepository.getFilteredCars(pickupLocationId, category, gearBoxType, engineType, minPrice, maxPrice, "AVAILABLE");
//
//            // Existing pagination code
//            int totalElements = filteredCars.size();
//            int totalPages = (int) Math.ceil((double) totalElements / size);
//            int startIndex = internalPage * size;
//            int endIndex = Math.min(startIndex + size, totalElements);
//            List<Car> pagedCars = (startIndex < totalElements) ? filteredCars.subList(startIndex, endIndex) : new ArrayList<>();
//
//            // Convert cars to the simplified response format
//            List<Map<String, Object>> carsContent = pagedCars.stream()
//                    .map(car -> {
//                        Map<String, Object> carMap = new HashMap<>();
//                        String car_id = car.getCarId();
//
//                        carMap.put("car_id", car_id);
//                        carMap.put("model", car.getMake() + " " + car.getModel() + " " + car.getYear());
//                        carMap.put("pricePerDay", car.getPricePerDay().toString());
//                        carMap.put("carRating", car.getRating().toString());
//                        carMap.put("serviceRating", car.getServiceRating());
//                        carMap.put("location", car.getLocationName());
//                        carMap.put("status", car.getStatus()); // Get status directly from car object
//
//                        // Get first image URL if available
//                        List<String> imageUrls = car.getImageUrls();
//                        if (imageUrls != null && !imageUrls.isEmpty()) {
//                            carMap.put("imageUrl", imageUrls.get(0));
//                        } else {
//                            carMap.put("imageUrl", "");
//                        }
//
//                        return carMap;
//                    })
//                    // Filter by service rating if specified - using the final variable
//                    .filter(carMap -> {
//                        if (minServiceRating == null) return true;
//                        try {
//                            double rating = Double.parseDouble(carMap.get("serviceRating").toString());
//                            return rating >= minServiceRating;
//                        } catch (Exception e) {
//                            return false;
//                        }
//                    })
//                    .collect(Collectors.toList());
//
//            // Recalculate pagination after service rating filtering
//            if (minServiceRating != null) {
//                totalElements = carsContent.size();
//                totalPages = (int) Math.ceil((double) totalElements / size);
//                startIndex = internalPage * size;
//                endIndex = Math.min(startIndex + size, totalElements);
//
//                if (startIndex >= totalElements) {
//                    carsContent = new ArrayList<>();
//                } else {
//                    carsContent = carsContent.subList(startIndex, endIndex);
//                }
//            }
//
//            // Build the response body with the exact structure needed
//            Map<String, Object> responseBody = new HashMap<>();
//            responseBody.put("content", carsContent);
//            responseBody.put("currentPage", page);
//            responseBody.put("totalElements", totalElements);
//            responseBody.put("totalPages", totalPages);
//
//            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
//            response.setStatusCode(200);
//            response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
//            response.setBody(objectMapper.writeValueAsString(responseBody));
//            return response;
//
//        } catch (Exception e) {
//            context.getLogger().log("Error processing request: " + e.getMessage());
//            return createErrorResponse(500, "Internal server error: " + e.getMessage());
//        }
//    }
//
//    private Set<String> getAvailableCarIds(Instant pickupDateTime, Instant dropOffDateTime, String locationId,
//                                           String category, String gearBoxType, String engineType,
//                                           Double minPrice, Double maxPrice, Context context) {
//        context.getLogger().log("Checking for available cars...");
//
//        // Build filter expressions for available cars
//        List<String> filters = new ArrayList<>();
//        Map<String, AttributeValue> expressionValues = new HashMap<>();
//        Map<String, String> expressionNames = new HashMap<>();
//
//        // Filter for AVAILABLE status
//        filters.add("#status = :availableStatus");
//        expressionNames.put("#status", "status");
//        expressionValues.put(":availableStatus", AttributeValue.builder().s("AVAILABLE").build());
//
//        // Add location filter if specified
//        if (locationId != null && !locationId.isEmpty()) {
//            filters.add("location_id = :locationId");
//            expressionValues.put(":locationId", AttributeValue.builder().s(locationId).build());
//        }
//
//        // Add category filter if specified
//        if (category != null && !category.isEmpty()) {
//            filters.add("category = :category");
//            expressionValues.put(":category", AttributeValue.builder().s(category).build());
//        }
//
//        // Add gearbox filter if specified
//        if (gearBoxType != null && !gearBoxType.isEmpty()) {
//            filters.add("gear_box_type = :gearBoxType");
//            expressionValues.put(":gearBoxType", AttributeValue.builder().s(gearBoxType).build());
//        }
//
//        // Add engine/fuel type filter if specified
//        if (engineType != null && !engineType.isEmpty()) {
//            filters.add("fuel_type = :fuelType");
//            expressionValues.put(":fuelType", AttributeValue.builder().s(engineType).build());
//        }
//
//        // Add price range filters if specified
//        if (minPrice != null) {
//            filters.add("price_per_day >= :minPrice");
//            expressionValues.put(":minPrice", AttributeValue.builder().n(minPrice.toString()).build());
//        }
//        if (maxPrice != null) {
//            filters.add("price_per_day <= :maxPrice");
//            expressionValues.put(":maxPrice", AttributeValue.builder().n(maxPrice.toString()).build());
//        }
//
//        // Build the scan request
//        ScanRequest.Builder scanBuilder = ScanRequest.builder()
//                .tableName(CARS_TABLE)
//                .filterExpression(String.join(" AND ", filters))
//                .expressionAttributeValues(expressionValues);
//
//        if (!expressionNames.isEmpty()) {
//            scanBuilder.expressionAttributeNames(expressionNames);
//        }
//
//        Set<String> availableCarIds = new HashSet<>();
//
//        try {
//            ScanResponse response = dynamoDbClient.scan(scanBuilder.build());
//
//            for (Map<String, AttributeValue> item : response.items()) {
//                if (item.containsKey("car_id")) {
//                    availableCarIds.add(item.get("car_id").s());
//                }
//            }
//
//            // Handle pagination
//            while (response.hasLastEvaluatedKey()) {
//                scanBuilder.exclusiveStartKey(response.lastEvaluatedKey());
//                response = dynamoDbClient.scan(scanBuilder.build());
//
//                for (Map<String, AttributeValue> item : response.items()) {
//                    if (item.containsKey("car_id")) {
//                        availableCarIds.add(item.get("car_id").s());
//                    }
//                }
//            }
//
//            context.getLogger().log("Found " + availableCarIds.size() + " available cars");
//
//        } catch (Exception e) {
//            context.getLogger().log("Error checking car availability: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return availableCarIds;
//    }
//
//    private List<String> getDatesInRange(Instant start, Instant end) {
//        List<String> dates = new ArrayList<>();
//        LocalDate current = start.atZone(ZoneId.of("UTC")).toLocalDate();
//        LocalDate endDate = end.atZone(ZoneId.of("UTC")).toLocalDate();
//        while (!current.isAfter(endDate)) {
//            dates.add(current.toString());
//            current = current.plusDays(1);
//        }
//        return dates;
//    }
//
//    private Map<String, String> getCarAvailabilityMap(Context context) {
//        Map<String, String> availabilityMap = new HashMap<>();
//
//        try {
//            ScanRequest scanRequest = ScanRequest.builder()
//                    .tableName(CARS_TABLE)
//                    .projectionExpression("car_id, #status")
//                    .expressionAttributeNames(Map.of("#status", "status"))
//                    .build();
//
//            ScanResponse response = dynamoDbClient.scan(scanRequest);
//
//            for (Map<String, AttributeValue> item : response.items()) {
//                if (!item.containsKey("car_id") || !item.containsKey("status")) continue;
//                String car_id = item.get("car_id").s();
//                String status = item.get("status").s();
//                availabilityMap.put(car_id, status);
//            }
//
//            // Handle pagination if there are more results
//            Map<String, AttributeValue> lastEvaluatedKey = response.lastEvaluatedKey();
//            while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
//                scanRequest = ScanRequest.builder()
//                        .tableName(CARS_TABLE)
//                        .projectionExpression("car_id, #status")
//                        .expressionAttributeNames(Map.of("#status", "status"))
//                        .exclusiveStartKey(lastEvaluatedKey)
//                        .build();
//
//                response = dynamoDbClient.scan(scanRequest);
//
//                for (Map<String, AttributeValue> item : response.items()) {
//                    if (!item.containsKey("car_id") || !item.containsKey("status")) continue;
//                    String car_id = item.get("car_id").s();
//                    String status = item.get("status").s();
//                    availabilityMap.put(car_id, status);
//                }
//
//                lastEvaluatedKey = response.lastEvaluatedKey();
//            }
//
//        } catch (Exception e) {
//            context.getLogger().log("Error fetching car availability: " + e.getMessage());
//        }
//
//        return availabilityMap;
//    }
//
//    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
//        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
//        response.setStatusCode(statusCode);
//        response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
//        try {
//            response.setBody(objectMapper.writeValueAsString(Map.of("error", message)));
//        } catch (JsonProcessingException e) {
//            response.setBody("{\"error\":\"" + message + "\"}");
//        }
//        return response;
//    }
//}



//package com.carrentalapp.handler;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
//import com.carrentalapp.Repository.CarRepository;
//import com.carrentalapp.Repository.LocationRepository;
//import com.carrentalapp.models.Car;
//import com.carrentalapp.models.Location;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
//import software.amazon.awssdk.services.dynamodb.model.*;
//
//import javax.inject.Inject;
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.format.DateTimeParseException;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class CarSelectionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
//    private final DynamoDbClient dynamoDbClient;
//    private final String CARS_TABLE;
//    private final String LOCATIONS_TABLE;
//    private final ObjectMapper objectMapper;
//    private final CarRepository carRepository;
//    private final LocationRepository locationRepository;
//
//    @Inject
//    public CarSelectionHandler(DynamoDbClient dynamoDbClient) {
//        this.dynamoDbClient = dynamoDbClient;
//        this.CARS_TABLE = System.getenv("Cars");
//        this.LOCATIONS_TABLE = System.getenv("Locations");
//        this.objectMapper = new ObjectMapper();
//        this.carRepository = new CarRepository(dynamoDbClient, CARS_TABLE);
//        this.locationRepository = new LocationRepository(dynamoDbClient, LOCATIONS_TABLE);
//    }
//
//    @Override
//    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
//        context.getLogger().log("Processing car filtering request");
//
//        try {
//            Map<String, String> queryParams = input.getQueryStringParameters();
//            if (queryParams == null) {
//                queryParams = new HashMap<>();
//            }
//
//            String pickupLocationId = queryParams.get("pickupLocationId");
//            String pickupLocationName = queryParams.get("pickupLocationName");
//            String dropOffLocationId = queryParams.get("dropOffLocationId");

//            String pickupDateTimeStr = queryParams.get("pickupDateTime");
//            String dropOffDateTimeStr = queryParams.get("dropOffDateTime");
//            String category = queryParams.get("category");
//            String gearBoxType = queryParams.get("gearBoxType");
//            String engineType = queryParams.get("engineType");
//            String minPriceStr = queryParams.get("minPrice");
//            String maxPriceStr = queryParams.get("maxPrice");
//            String serviceRatingStr = queryParams.get("serviceRating");
//
//            // If we have a location name but no ID, look up the ID
//            if ((pickupLocationId == null || pickupLocationId.isEmpty()) &&
//                    pickupLocationName != null && !pickupLocationName.isEmpty()) {
//
//                context.getLogger().log("Looking up location ID for name: " + pickupLocationName);
//
//                // Find location by name
//                List<Location> locations = locationRepository.getAllLocations();
//                for (Location location : locations) {
//                    if (pickupLocationName.equalsIgnoreCase(location.getName())) {
//                        pickupLocationId = location.getLocationId();
//                        context.getLogger().log("Found location ID: " + pickupLocationId);
//                        break;
//                    }
//                }
//                if (pickupLocationId == null) {
//                    context.getLogger().log("No location found with name: " + pickupLocationName);
//                    // Instead of returning an error, we'll just not apply the location filter
//                }
//            }
//
//            // Parse service rating filter if provided
//            final Double minServiceRating;  // Make it effectively final for lambda
//            if (serviceRatingStr != null && !serviceRatingStr.isEmpty()) {
//                try {
//                    double rating = Double.parseDouble(serviceRatingStr);
//                    if (rating < 0 || rating > 5) {
//                        return createErrorResponse(400, "Service rating must be between 0 and 5");
//                    }
//                    minServiceRating = rating;
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid service rating format");
//                }
//            } else {
//                minServiceRating = null;
//            }
//
//            int page = 1;
//            int size = 8;
//            if (queryParams.containsKey("page")) {
//                try {
//                    page = Integer.parseInt(queryParams.get("page"));
//                    if (page < 1) page = 1;
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid page parameter");
//                }
//            }
//            if (queryParams.containsKey("size")) {
//                try {
//                    size = Integer.parseInt(queryParams.get("size"));
//                    if (size < 1) size = 8;
//                    if (size > 50) size = 50;
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid size parameter");
//                }
//            }
//            int internalPage = page - 1;
//
//            Instant pickupDateTime = null;
//            Instant dropOffDateTime = null;
//            if (pickupDateTimeStr != null && !pickupDateTimeStr.isEmpty()) {
//                try {
//                    pickupDateTimeStr = pickupDateTimeStr.endsWith("Z") ? pickupDateTimeStr : pickupDateTimeStr + "Z";
//                    pickupDateTime = Instant.parse(pickupDateTimeStr);
//                } catch (DateTimeParseException e) {
//                    return createErrorResponse(400, "Invalid pickup date format. Use ISO 8601 format (YYYY-MM-DDTHH:MM:SS)");
//                }
//            }
//            if (dropOffDateTimeStr != null && !dropOffDateTimeStr.isEmpty()) {
//                try {
//                    dropOffDateTimeStr = dropOffDateTimeStr.endsWith("Z") ? dropOffDateTimeStr : dropOffDateTimeStr + "Z";
//                    dropOffDateTime = Instant.parse(dropOffDateTimeStr);
//                } catch (DateTimeParseException e) {
//                    return createErrorResponse(400, "Invalid drop-off date format. Use ISO 8601 format (YYYY-MM-DDTHH:MM:SS)");
//                }
//            }
//
//            if (pickupDateTime != null && dropOffDateTime != null) {
//                if (dropOffDateTime.isBefore(pickupDateTime)) {
//                    return createErrorResponse(400, "Drop-off date must be after pickup date");
//                }
//                long minRentalSeconds = 4 * 60 * 60;
//                if (dropOffDateTime.getEpochSecond() - pickupDateTime.getEpochSecond() < minRentalSeconds) {
//                    return createErrorResponse(400, "Minimum rental period is 4 hours");
//                }
//            }
//
//            Double minPrice = null;
//            Double maxPrice = null;
//            if (minPriceStr != null && !minPriceStr.isEmpty()) {
//                try {
//                    minPrice = Double.parseDouble(minPriceStr);
//                    if (minPrice < 0) return createErrorResponse(400, "Minimum price cannot be negative");
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid minimum price format");
//                }
//            }
//            if (maxPriceStr != null && !maxPriceStr.isEmpty()) {
//                try {
//                    maxPrice = Double.parseDouble(maxPriceStr);
//                    if (maxPrice < 0) return createErrorResponse(400, "Maximum price cannot be negative");
//                } catch (NumberFormatException e) {
//                    return createErrorResponse(400, "Invalid maximum price format");
//                }
//            }
//            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
//                return createErrorResponse(400, "Minimum price cannot be greater than maximum price");
//            }
//
//            Map<String, String> carAvailabilityMap = getCarAvailabilityMap(context);
//            Set<String> availableCarIds = new HashSet<>();
//
//            if (pickupDateTime != null && dropOffDateTime != null) {
//                availableCarIds = getAvailableCarIds(
//                        pickupDateTime, dropOffDateTime, pickupLocationId,
//                        category, gearBoxType, engineType, minPrice, maxPrice, context
//                );
//                if (availableCarIds.isEmpty()) {
//                    Map<String, Object> responseBody = new HashMap<>();
//                    responseBody.put("content", new ArrayList<>());
//                    responseBody.put("currentPage", page);
//                    responseBody.put("totalElements", 0);
//                    responseBody.put("totalPages", 0);
//                    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
//                    response.setStatusCode(200);
//                    response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
//                    response.setBody(objectMapper.writeValueAsString(responseBody));
//                    return response;
//                }
//            }
//
//            // Get filtered cars based on parameters
//            List<Car> filteredCars = pickupDateTime != null && dropOffDateTime != null
//                    ? carRepository.getCarsByIds(availableCarIds, context)
//                    : carRepository.getFilteredCars(pickupLocationId, category, gearBoxType, engineType, minPrice, maxPrice, "AVAILABLE");
//
//            // Enrich cars with location information if needed
//            if (filteredCars != null && !filteredCars.isEmpty()) {
//                Map<String, Location> locationCache = new HashMap<>(); // Cache to avoid repeated lookups
//
//                for (Car car : filteredCars) {
//                    if (car.getLocationId() != null && !car.getLocationId().isEmpty() &&
//                            (car.getLocationName() == null || car.getLocationName().isEmpty())) {
//
//                        // Check cache first
//                        Location location = locationCache.get(car.getLocationId());
//                        if (location == null) {
//                            // Not in cache, look it up
//                            Optional<Location> locationOpt = locationRepository.getLocationById(car.getLocationId());
//                            if (locationOpt.isPresent()) {
//                                location = locationOpt.get();
//                                locationCache.put(car.getLocationId(), location);
//                            }
//                        }
//
//                        // Set location name if found
//                        if (location != null) {
//                            car.setLocationName(location.getName());
//                        }
//                    }
//                }
//            }
//
//            // Existing pagination code
//            int totalElements = filteredCars.size();
//            int totalPages = (int) Math.ceil((double) totalElements / size);
//            int startIndex = internalPage * size;
//            int endIndex = Math.min(startIndex + size, totalElements);
//            List<Car> pagedCars = (startIndex < totalElements) ? filteredCars.subList(startIndex, endIndex) : new ArrayList<>();
//
//            // Convert cars to the simplified response format
//            List<Map<String, Object>> carsContent = pagedCars.stream()
//                    .map(car -> {
//                        Map<String, Object> carMap = new HashMap<>();
//                        String car_id = car.getCarId();
//
//                        carMap.put("car_id", car_id);
//                        carMap.put("model", car.getMake() + " " + car.getModel() + " " + car.getYear());
//                        carMap.put("pricePerDay", car.getPricePerDay().toString());
//                        carMap.put("carRating", car.getRating().toString());
//                        carMap.put("serviceRating", car.getServiceRating());
//                        carMap.put("location", car.getLocationName());
//                        carMap.put("status", car.getStatus()); // Get status directly from car object
//
//                        // Get first image URL if available
//                        List<String> imageUrls = car.getImageUrls();
//                        if (imageUrls != null && !imageUrls.isEmpty()) {
//                            carMap.put("imageUrl", imageUrls.get(0));
//                        } else {
//                            carMap.put("imageUrl", "");
//                        }
//
//                        return carMap;
//                    })
//                    // Filter by service rating if specified - using the final variable
//                    .filter(carMap -> {
//                        if (minServiceRating == null) return true;
//                        try {
//                            double rating = Double.parseDouble(carMap.get("serviceRating").toString());
//                            return rating >= minServiceRating;
//                        } catch (Exception e) {
//                            return false;
//                        }
//                    })
//                    .collect(Collectors.toList());
//
//            // Recalculate pagination after service rating filtering
//            if (minServiceRating != null) {
//                totalElements = carsContent.size();
//                totalPages = (int) Math.ceil((double) totalElements / size);
//                startIndex = internalPage * size;
//                endIndex = Math.min(startIndex + size, totalElements);
//
//                if (startIndex >= totalElements) {
//                    carsContent = new ArrayList<>();
//                } else {
//                    carsContent = carsContent.subList(startIndex, endIndex);
//                }
//            }
//
//            // Build the response body with the exact structure needed
//            Map<String, Object> responseBody = new HashMap<>();
//            responseBody.put("content", carsContent);
//            responseBody.put("currentPage", page);
//            responseBody.put("totalElements", totalElements);
//            responseBody.put("totalPages", totalPages);
//
//            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
//            response.setStatusCode(200);
//            response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
//            response.setBody(objectMapper.writeValueAsString(responseBody));
//            return response;
//
//        } catch (Exception e) {
//            context.getLogger().log("Error processing request: " + e.getMessage());
//            return createErrorResponse(500, "Internal server error: " + e.getMessage());
//        }
//    }
//
//    private Set<String> getAvailableCarIds(Instant pickupDateTime, Instant dropOffDateTime, String locationId,
//                                           String category, String gearBoxType, String engineType,
//                                           Double minPrice, Double maxPrice, Context context) {
//        context.getLogger().log("Checking for available cars...");
//
//        // Build filter expressions for available cars
//        List<String> filters = new ArrayList<>();
//        Map<String, AttributeValue> expressionValues = new HashMap<>();
//        Map<String, String> expressionNames = new HashMap<>();
//
//        // Filter for AVAILABLE status
//        filters.add("#status = :availableStatus");
//        expressionNames.put("#status", "status");
//        expressionValues.put(":availableStatus", AttributeValue.builder().s("AVAILABLE").build());
//
//        // Add location filter if specified
//        if (locationId != null && !locationId.isEmpty()) {
//            filters.add("location_id = :locationId");
//            expressionValues.put(":locationId", AttributeValue.builder().s(locationId).build());
//        }
//
//        // Add category filter if specified
//        if (category != null && !category.isEmpty()) {
//            filters.add("category = :category");
//            expressionValues.put(":category", AttributeValue.builder().s(category).build());
//        }
//
//        // Add gearbox filter if specified
//        if (gearBoxType != null && !gearBoxType.isEmpty()) {
//            filters.add("gear_box_type = :gearBoxType");
//            expressionValues.put(":gearBoxType", AttributeValue.builder().s(gearBoxType).build());
//        }
//
//        // Add engine/fuel type filter if specified
//        if (engineType != null && !engineType.isEmpty()) {
//            filters.add("fuel_type = :fuelType");
//            expressionValues.put(":fuelType", AttributeValue.builder().s(engineType).build());
//        }
//
//        // Add price range filters if specified
//        if (minPrice != null) {
//            filters.add("price_per_day >= :minPrice");
//            expressionValues.put(":minPrice", AttributeValue.builder().n(minPrice.toString()).build());
//        }
//        if (maxPrice != null) {
//            filters.add("price_per_day <= :maxPrice");
//            expressionValues.put(":maxPrice", AttributeValue.builder().n(maxPrice.toString()).build());
//        }
//
//        // Build the scan request
//        ScanRequest.Builder scanBuilder = ScanRequest.builder()
//                .tableName(CARS_TABLE)
//                .filterExpression(String.join(" AND ", filters))
//                .expressionAttributeValues(expressionValues);
//
//        if (!expressionNames.isEmpty()) {
//            scanBuilder.expressionAttributeNames(expressionNames);
//        }
//
//        Set<String> availableCarIds = new HashSet<>();
//
//        try {
//            ScanResponse response = dynamoDbClient.scan(scanBuilder.build());
//
//            for (Map<String, AttributeValue> item : response.items()) {
//                if (item.containsKey("car_id")) {
//                    availableCarIds.add(item.get("car_id").s());
//                }
//            }
//
//            // Handle pagination
//            while (response.hasLastEvaluatedKey()) {
//                scanBuilder.exclusiveStartKey(response.lastEvaluatedKey());
//                response = dynamoDbClient.scan(scanBuilder.build());
//
//                for (Map<String, AttributeValue> item : response.items()) {
//                    if (item.containsKey("car_id")) {
//                        availableCarIds.add(item.get("car_id").s());
//                    }
//                }
//            }
//
//            context.getLogger().log("Found " + availableCarIds.size() + " available cars");
//
//        } catch (Exception e) {
//            context.getLogger().log("Error checking car availability: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return availableCarIds;
//    }
//
//    private List<String> getDatesInRange(Instant start, Instant end) {
//        List<String> dates = new ArrayList<>();
//        LocalDate current = start.atZone(ZoneId.of("UTC")).toLocalDate();
//        LocalDate endDate = end.atZone(ZoneId.of("UTC")).toLocalDate();
//        while (!current.isAfter(endDate)) {
//            dates.add(current.toString());
//            current = current.plusDays(1);
//        }
//        return dates;
//    }
//
//    private Map<String, String> getCarAvailabilityMap(Context context) {
//        Map<String, String> availabilityMap = new HashMap<>();
//
//        try {
//            ScanRequest scanRequest = ScanRequest.builder()
//                    .tableName(CARS_TABLE)
//                    .projectionExpression("car_id, #status")
//                    .expressionAttributeNames(Map.of("#status", "status"))
//                    .build();
//
//            ScanResponse response = dynamoDbClient.scan(scanRequest);
//
//            for (Map<String, AttributeValue> item : response.items()) {
//                if (!item.containsKey("car_id") || !item.containsKey("status")) continue;
//                String car_id = item.get("car_id").s();
//                String status = item.get("status").s();
//                availabilityMap.put(car_id, status);
//            }
//
//            // Handle pagination if there are more results
//            Map<String, AttributeValue> lastEvaluatedKey = response.lastEvaluatedKey();
//            while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
//                scanRequest = ScanRequest.builder()
//                        .tableName(CARS_TABLE)
//                        .projectionExpression("car_id, #status")
//                        .expressionAttributeNames(Map.of("#status", "status"))
//                        .exclusiveStartKey(lastEvaluatedKey)
//                        .build();
//
//                response = dynamoDbClient.scan(scanRequest);
//
//                for (Map<String, AttributeValue> item : response.items()) {
//                    if (!item.containsKey("car_id") || !item.containsKey("status")) continue;
//                    String car_id = item.get("car_id").s();
//                    String status = item.get("status").s();
//                    availabilityMap.put(car_id, status);
//                }
//
//                lastEvaluatedKey = response.lastEvaluatedKey();
//            }
//
//        } catch (Exception e) {
//            context.getLogger().log("Error fetching car availability: " + e.getMessage());
//        }
//
//        return availabilityMap;
//    }
//
//    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
//        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
//        response.setStatusCode(statusCode);
//        response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
//        try {
//            response.setBody(objectMapper.writeValueAsString(Map.of("error", message)));
//        } catch (JsonProcessingException e) {
//            response.setBody("{\"error\":\"" + message + "\"}");
//        }
//        return response;
//    }
//}


package com.carrentalapp.handler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.Repository.CarRepository;
import com.carrentalapp.Repository.LocationRepository;
import com.carrentalapp.models.Car;
import com.carrentalapp.models.Location;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CarSelectionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient;
    private final String CARS_TABLE;
    private final String LOCATIONS_TABLE;
    private final ObjectMapper objectMapper;
    private final CarRepository carRepository;
    private final LocationRepository locationRepository;

    @Inject
    public CarSelectionHandler(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        // Add fallbacks for environment variables
        this.CARS_TABLE = System.getenv("Cars") != null ? System.getenv("Cars") : "Cars";
        this.LOCATIONS_TABLE = System.getenv("Locations") != null ? System.getenv("Locations") : "Locations";
        System.out.println("Cars table: " + CARS_TABLE);
        System.out.println("Locations table: " + LOCATIONS_TABLE);

        this.objectMapper = new ObjectMapper();
        this.carRepository = new CarRepository(dynamoDbClient, CARS_TABLE);
        this.locationRepository = new LocationRepository(dynamoDbClient, LOCATIONS_TABLE);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        context.getLogger().log("Processing car filtering request");

        // Check environment variables first
        if (CARS_TABLE == null || CARS_TABLE.isEmpty() || LOCATIONS_TABLE == null || LOCATIONS_TABLE.isEmpty()) {
            context.getLogger().log("ERROR: Missing required environment variables. Cars table: " +
                    CARS_TABLE + ", Locations table: " + LOCATIONS_TABLE);
            return createErrorResponse(500, "Server configuration error: Missing table names");
        }

        try {
            Map<String, String> queryParams = input.getQueryStringParameters();
            if (queryParams == null) {
                queryParams = new HashMap<>();
            }

            String pickupLocationId = queryParams.get("pickupLocationId");
            String pickupLocationName = queryParams.get("pickupLocationName");
            String dropOffLocationId = queryParams.get("dropOffLocationId");
            String dropOffLocationName = queryParams.get("dropOffLocationName");
            String pickupDateTimeStr = queryParams.get("pickupDateTime");
            String dropOffDateTimeStr = queryParams.get("dropOffDateTime");
            String category = queryParams.get("category");
            String gearBoxType = queryParams.get("gearBoxType");
            String engineType = queryParams.get("engineType");
            String minPriceStr = queryParams.get("minPrice");
            String maxPriceStr = queryParams.get("maxPrice");
            String serviceRatingStr = queryParams.get("serviceRating");

            // Log all query parameters for debugging
            context.getLogger().log("Query parameters: " + queryParams);
            if ((dropOffLocationId != null && !dropOffLocationId.isEmpty()) &&
                    (pickupLocationId == null || pickupLocationId.isEmpty()) &&
                    (pickupLocationName == null || pickupLocationName.isEmpty())) {
                return createErrorResponse(400, "Pickup location must be provided when drop-off location is specified");
            }
            if ((pickupLocationId != null && !pickupLocationId.isEmpty()) &&
                    (dropOffLocationId == null || dropOffLocationId.isEmpty()) &&
                    (dropOffLocationId == null || dropOffLocationId.isEmpty())) {
                return createErrorResponse(400, "Drop Off location must be provided when drop-off location is specified");
            }
            // Check if pickup location exists if ID is provided
            boolean pickupLocationExists = true;
            String pickupLocationNameValue = "";

            if (pickupLocationId != null && !pickupLocationId.isEmpty()) {
                Optional<Location> locationOpt = locationRepository.getLocationById(pickupLocationId);
                if (locationOpt.isPresent()) {
                    pickupLocationNameValue = locationOpt.get().getName();
                    context.getLogger().log("Found pickup location: " + pickupLocationNameValue + " with ID: " + pickupLocationId);
                } else {
                    pickupLocationExists = false;
                    context.getLogger().log("Pickup location ID not found: " + pickupLocationId);
                }
            }

            // If pickup location ID was provided but doesn't exist, return error message
            if (pickupLocationId != null && !pickupLocationId.isEmpty() && !pickupLocationExists) {
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("message", "Invalid location ID: " + pickupLocationId);

                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(200);
                response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
                response.setBody(objectMapper.writeValueAsString(responseBody));
                return response;
            }

            // Check if drop-off location exists
            boolean dropOffLocationExists = true;
            String dropOffLocationNameValue = "";

            if (dropOffLocationId != null && !dropOffLocationId.isEmpty()) {
                Optional<Location> locationOpt = locationRepository.getLocationById(dropOffLocationId);
                if (locationOpt.isPresent()) {
                    dropOffLocationNameValue = locationOpt.get().getName();
                    context.getLogger().log("Found drop-off location: " + dropOffLocationNameValue + " with ID: " + dropOffLocationId);
                } else {
                    dropOffLocationExists = false;
                    context.getLogger().log("Drop-off location ID not found: " + dropOffLocationId);
                }
            }


            // If drop-off location ID was provided but doesn't exist, return error message
            if (dropOffLocationId != null && !dropOffLocationId.isEmpty() && !dropOffLocationExists) {
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("message", "Invalid drop-off location ID: " + dropOffLocationId);

                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(200);
                response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
                response.setBody(objectMapper.writeValueAsString(responseBody));
                return response;
            }

            // Now check if pickup and drop-off locations are the same (only if both exist)
            boolean samePickupDropLocation = pickupLocationExists && dropOffLocationExists &&
                    dropOffLocationId != null && pickupLocationId != null &&
                    !dropOffLocationId.isEmpty() && !pickupLocationId.isEmpty() &&
                    dropOffLocationId.equals(pickupLocationId);

            if (samePickupDropLocation) {
                context.getLogger().log("Pickup and drop-off locations are the same: " + pickupLocationNameValue);
            }

            // If we have a location name but no ID, look up the ID
            if ((pickupLocationId == null || pickupLocationId.isEmpty()) &&
                    pickupLocationName != null && !pickupLocationName.isEmpty()) {

                context.getLogger().log("Looking up location ID for name: " + pickupLocationName);

                // Find location by name
                List<Location> locations = locationRepository.getAllLocations();
                for (Location location : locations) {
                    if (pickupLocationName.equalsIgnoreCase(location.getName())) {
                        pickupLocationId = location.getLocationId();
                        pickupLocationNameValue = location.getName();
                        pickupLocationExists = true;
                        context.getLogger().log("Found location ID: " + pickupLocationId + " for name: " + pickupLocationName);
                        break;
                    }
                }
                if (pickupLocationId == null) {
                    context.getLogger().log("No location found with name: " + pickupLocationName);
                    pickupLocationExists = false;

                    // Return message for invalid location name
                    Map<String, Object> responseBody = new HashMap<>();
                    responseBody.put("message", "Invalid location name: " + pickupLocationName);

                    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                    response.setStatusCode(200);
                    response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
                    response.setBody(objectMapper.writeValueAsString(responseBody));
                    return response;
                }
            }

            // Check if there are cars at the specified location - FOR DEBUGGING ONLY
            if (pickupLocationId != null && !pickupLocationId.isEmpty()) {
                try {
                    // Use a scan operation to check if cars exist at this location
                    ScanRequest scanRequest = ScanRequest.builder()
                            .tableName(CARS_TABLE)
                            .filterExpression("location_id = :locId")
                            .expressionAttributeValues(Map.of(":locId", AttributeValue.builder().s(pickupLocationId).build()))
                            .build();

                    ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
                    boolean carsExistAtLocation = !scanResponse.items().isEmpty();
                    context.getLogger().log("DEBUG: Cars exist at location " + pickupLocationId + ": " + carsExistAtLocation +
                            ", Count: " + scanResponse.count());

                    // Log the first few cars for debugging
                    if (carsExistAtLocation) {
                        int count = 0;
                        for (Map<String, AttributeValue> item : scanResponse.items()) {
                            if (count >= 3) break; // Log at most 3 cars

                            String carId = item.containsKey("car_id") ? item.get("car_id").s() : "unknown";
                            String status = item.containsKey("status") ? item.get("status").s() : "unknown";
                            String make = item.containsKey("make") ? item.get("make").s() : "unknown";
                            String model = item.containsKey("model") ? item.get("model").s() : "unknown";

                            context.getLogger().log("DEBUG: Car at location - ID: " + carId +
                                    ", Status: " + status +
                                    ", Make/Model: " + make + " " + model);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    context.getLogger().log("Error in debug check for cars at location: " + e.getMessage());
                }
            }

            // Parse service rating filter if provided
            final Double minServiceRating;  // Make it effectively final for lambda
            if (serviceRatingStr != null && !serviceRatingStr.isEmpty()) {
                try {
                    double rating = Double.parseDouble(serviceRatingStr);
                    if (rating < 0 || rating > 5) {
                        return createErrorResponse(400, "Service rating must be between 0 and 5");
                    }
                    minServiceRating = rating;
                } catch (NumberFormatException e) {
                    return createErrorResponse(400, "Invalid service rating format");
                }
            } else {
                minServiceRating = null;
            }

            int page = 1;
            int size = 8;
            if (queryParams.containsKey("page")) {
                try {
                    page = Integer.parseInt(queryParams.get("page"));
                    if (page < 1) page = 1;
                } catch (NumberFormatException e) {
                    return createErrorResponse(400, "Invalid page parameter");
                }
            }
            if (queryParams.containsKey("size")) {
                try {
                    size = Integer.parseInt(queryParams.get("size"));
                    if (size < 1) size = 8;
                    if (size > 50) size = 50;
                } catch (NumberFormatException e) {
                    return createErrorResponse(400, "Invalid size parameter");
                }
            }
            int internalPage = page - 1;

            Instant pickupDateTime = null;
            Instant dropOffDateTime = null;
            if (pickupDateTimeStr != null && !pickupDateTimeStr.isEmpty()) {
                try {
                    pickupDateTimeStr = pickupDateTimeStr.endsWith("Z") ? pickupDateTimeStr : pickupDateTimeStr + "Z";
                    pickupDateTime = Instant.parse(pickupDateTimeStr);
                } catch (DateTimeParseException e) {
                    return createErrorResponse(400, "Invalid pickup date format. Use ISO 8601 format (YYYY-MM-DDTHH:MM:SS)");
                }
            }
            if (dropOffDateTimeStr != null && !dropOffDateTimeStr.isEmpty()) {
                try {
                    dropOffDateTimeStr = dropOffDateTimeStr.endsWith("Z") ? dropOffDateTimeStr : dropOffDateTimeStr + "Z";
                    dropOffDateTime = Instant.parse(dropOffDateTimeStr);
                } catch (DateTimeParseException e) {
                    return createErrorResponse(400, "Invalid drop-off date format. Use ISO 8601 format (YYYY-MM-DDTHH:MM:SS)");
                }
            }

            if (pickupDateTime != null && dropOffDateTime != null) {
                if (dropOffDateTime.isBefore(pickupDateTime)) {
                    return createErrorResponse(400, "Drop-off date must be after pickup date");
                }
                long minRentalSeconds = 4 * 60 * 60;
                if (dropOffDateTime.getEpochSecond() - pickupDateTime.getEpochSecond() < minRentalSeconds) {
                    return createErrorResponse(400, "Minimum rental period is 4 hours");
                }
            }

            Double minPrice = null;
            Double maxPrice = null;
            if (minPriceStr != null && !minPriceStr.isEmpty()) {
                try {
                    minPrice = Double.parseDouble(minPriceStr);
                    if (minPrice < 0) return createErrorResponse(400, "Minimum price cannot be negative");
                } catch (NumberFormatException e) {
                    return createErrorResponse(400, "Invalid minimum price format");
                }
            }
            if (maxPriceStr != null && !maxPriceStr.isEmpty()) {
                try {
                    maxPrice = Double.parseDouble(maxPriceStr);
                    if (maxPrice < 0) return createErrorResponse(400, "Maximum price cannot be negative");
                } catch (NumberFormatException e) {
                    return createErrorResponse(400, "Invalid maximum price format");
                }
            }
            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                return createErrorResponse(400, "Minimum price cannot be greater than maximum price");
            }

            Map<String, String> carAvailabilityMap = getCarAvailabilityMap(context);
            Set<String> availableCarIds = new HashSet<>();

            if (pickupDateTime != null && dropOffDateTime != null) {
                availableCarIds = getAvailableCarIds(
                        pickupDateTime, dropOffDateTime, pickupLocationId,
                        category, gearBoxType, engineType, minPrice, maxPrice, context
                );
                if (availableCarIds.isEmpty()) {
                    String message;
                    if (pickupLocationId != null && !pickupLocationId.isEmpty()) {
                        message = "No cars available at " + pickupLocationNameValue + " for the selected dates";
                    } else {
                        message = "No cars available for the selected dates";
                    }

                    Map<String, Object> responseBody = new HashMap<>();
                    responseBody.put("content", new ArrayList<>());
                    responseBody.put("currentPage", page);
                    responseBody.put("totalElements", 0);
                    responseBody.put("totalPages", 0);
                    responseBody.put("message", message);

                    // Add note about same pickup/dropoff location if applicable
                    if (samePickupDropLocation) {
                        responseBody.put("note", "Pickup and drop-off locations are the same. You'll return the car to the same location where you picked it up.");
                    }

                    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                    response.setStatusCode(200);
                    response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
                    response.setBody(objectMapper.writeValueAsString(responseBody));
                    return response;
                }
            }

            // Get filtered cars based on parameters
            List<Car> filteredCars;
            if (pickupDateTime != null && dropOffDateTime != null) {
                filteredCars = carRepository.getCarsByIds(availableCarIds, context);
                context.getLogger().log("Retrieved " + filteredCars.size() + " cars by IDs for date range");
            } else {
                // IMPORTANT: We're removing the status filter here as it might be causing issues
                filteredCars = carRepository.getFilteredCars(pickupLocationId, category, gearBoxType, engineType, minPrice, maxPrice, "AVAILABLE");
                context.getLogger().log("Retrieved " + filteredCars.size() + " cars without date filter");
            }

            // Log the filtered cars for debugging
            context.getLogger().log("Filtered cars count: " + filteredCars.size());
            if (!filteredCars.isEmpty()) {
                int count = 0;
                for (Car car : filteredCars) {
                    if (count >= 3) break; // Log at most 3 cars
                    context.getLogger().log("Car: " + car.getCarId() + ", " + car.getMake() + " " + car.getModel() +
                            ", Status: " + car.getStatus() + ", Location: " + car.getLocationId());
                    count++;
                }
            }

            // If no cars found, return appropriate message
            if (filteredCars.isEmpty()) {
                String message;
                if (pickupLocationId != null && !pickupLocationId.isEmpty()) {
                    message = "No cars available at " + pickupLocationNameValue;
                } else {
                    message = "No cars match the selected filters";
                }

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("content", new ArrayList<>());
                responseBody.put("currentPage", page);
                responseBody.put("totalElements", 0);
                responseBody.put("totalPages", 0);
                responseBody.put("message", message);

                // Add note about same pickup/dropoff location if applicable
                if (samePickupDropLocation) {
                    responseBody.put("note", "Pickup and drop-off locations are the same. You'll return the car to the same location where you picked it up.");
                }

                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(200);
                response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
                response.setBody(objectMapper.writeValueAsString(responseBody));
                return response;
            }

            // Enrich cars with location information if needed
            if (filteredCars != null && !filteredCars.isEmpty()) {
                Map<String, Location> locationCache = new HashMap<>(); // Cache to avoid repeated lookups

                for (Car car : filteredCars) {
                    if (car.getLocationId() != null && !car.getLocationId().isEmpty() &&
                            (car.getLocationName() == null || car.getLocationName().isEmpty())) {

                        // Check cache first
                        Location location = locationCache.get(car.getLocationId());
                        if (location == null) {
                            // Not in cache, look it up
                            Optional<Location> locationOpt = locationRepository.getLocationById(car.getLocationId());
                            if (locationOpt.isPresent()) {
                                location = locationOpt.get();
                                locationCache.put(car.getLocationId(), location);
                            }
                        }

                        // Set location name if found
                        if (location != null) {
                            car.setLocationName(location.getName());
                        }
                    }
                }
            }

            // Filter cars by AVAILABLE status after retrieving them
            List<Car> availableCars = filteredCars.stream()
                    .filter(car -> "AVAILABLE".equals(car.getStatus()))
                    .collect(Collectors.toList());

            context.getLogger().log("After status filtering: " + availableCars.size() + " available cars");

            // If no available cars after status filtering
            if (availableCars.isEmpty()) {
                String message;
                if (pickupLocationId != null && !pickupLocationId.isEmpty()) {
                    message = "No available cars at " + pickupLocationNameValue;
                } else {
                    message = "No available cars match the selected filters";
                }

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("content", new ArrayList<>());
                responseBody.put("currentPage", page);
                responseBody.put("totalElements", 0);
                responseBody.put("totalPages", 0);
                responseBody.put("message", message);

                // Add note about same pickup/dropoff location if applicable
                if (samePickupDropLocation) {
                    responseBody.put("note", "Pickup and drop-off locations are the same. You'll return the car to the same location where you picked it up.");
                }

                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(200);
                response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
                response.setBody(objectMapper.writeValueAsString(responseBody));
                return response;
            }

            // Use the available cars for pagination
            filteredCars = availableCars;

            // Existing pagination code
            int totalElements = filteredCars.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int startIndex = internalPage * size;
            int endIndex = Math.min(startIndex + size, totalElements);
            List<Car> pagedCars = (startIndex < totalElements) ? filteredCars.subList(startIndex, endIndex) : new ArrayList<>();

            // Convert cars to the simplified response format
            List<Map<String, Object>> carsContent = pagedCars.stream()
                    .map(car -> {
                        Map<String, Object> carMap = new HashMap<>();
                        String car_id = car.getCarId();

                        carMap.put("car_id", car_id);
                        carMap.put("model", car.getMake() + " " + car.getModel() + " " + car.getYear());
                        carMap.put("pricePerDay", car.getPricePerDay().toString());
                        carMap.put("carRating", car.getRating().toString());
                        carMap.put("serviceRating", car.getServiceRating());
                        carMap.put("location", car.getLocationName());
                        carMap.put("status", car.getStatus()); // Get status directly from car object

                        // Get first image URL if available
                        List<String> imageUrls = car.getImageUrls();
                        if (imageUrls != null && !imageUrls.isEmpty()) {
                            carMap.put("imageUrl", imageUrls.get(0));
                        } else {
                            carMap.put("imageUrl", "");
                        }

                        return carMap;
                    })
                    // Filter by service rating if specified - using the final variable
                    .filter(carMap -> {
                        if (minServiceRating == null) return true;
                        try {
                            double rating = Double.parseDouble(carMap.get("serviceRating").toString());
                            return rating >= minServiceRating;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            // Recalculate pagination after service rating filtering
            if (minServiceRating != null) {
                totalElements = carsContent.size();
                totalPages = (int) Math.ceil((double) totalElements / size);
                startIndex = internalPage * size;
                endIndex = Math.min(startIndex + size, totalElements);

                if (startIndex >= totalElements) {
                    carsContent = new ArrayList<>();
                } else {
                    carsContent = carsContent.subList(startIndex, endIndex);
                }
            }

            // Check if after service rating filtering we have no results
            if (carsContent.isEmpty() && minServiceRating != null) {
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("content", new ArrayList<>());
                responseBody.put("currentPage", page);
                responseBody.put("totalElements", 0);
                responseBody.put("totalPages", 0);
                responseBody.put("message", "No cars match the minimum service rating of " + minServiceRating);

                // Add note about same pickup/dropoff location if applicable
                if (samePickupDropLocation) {
                    responseBody.put("note", "Pickup and drop-off locations are the same. You'll return the car to the same location where you picked it up.");
                }

                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(200);
                response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
                response.setBody(objectMapper.writeValueAsString(responseBody));
                return response;
            }

            // Build the response body with the exact structure needed
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("content", carsContent);
            responseBody.put("currentPage", page);
            responseBody.put("totalElements", totalElements);
            responseBody.put("totalPages", totalPages);

            // Add note about same pickup/dropoff location if applicable
            if (samePickupDropLocation) {
                responseBody.put("note", "Pickup and drop-off locations are the same. You'll return the car to the same location where you picked it up.");
            }

            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
            response.setBody(objectMapper.writeValueAsString(responseBody));
            return response;

        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    private Set<String> getAvailableCarIds(Instant pickupDateTime, Instant dropOffDateTime, String locationId,
                                           String category, String gearBoxType, String engineType,
                                           Double minPrice, Double maxPrice, Context context) {
        context.getLogger().log("Checking for available cars...");

        // Build filter expressions for available cars
        List<String> filters = new ArrayList<>();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        Map<String, String> expressionNames = new HashMap<>();

        // Filter for AVAILABLE status
        filters.add("#status = :availableStatus");
        expressionNames.put("#status", "status");
        expressionValues.put(":availableStatus", AttributeValue.builder().s("AVAILABLE").build());

        // Add location filter if specified
        if (locationId != null && !locationId.isEmpty()) {
            filters.add("location_id = :locationId");
            expressionValues.put(":locationId", AttributeValue.builder().s(locationId).build());
        }

        // Add category filter if specified
        if (category != null && !category.isEmpty()) {
            filters.add("category = :category");
            expressionValues.put(":category", AttributeValue.builder().s(category).build());
        }

        // Add gearbox filter if specified
        if (gearBoxType != null && !gearBoxType.isEmpty()) {
            filters.add("gear_box_type = :gearBoxType");
            expressionValues.put(":gearBoxType", AttributeValue.builder().s(gearBoxType).build());
        }

        // Add engine/fuel type filter if specified
        if (engineType != null && !engineType.isEmpty()) {
            filters.add("fuel_type = :fuelType");
            expressionValues.put(":fuelType", AttributeValue.builder().s(engineType).build());
        }

        // Add price range filters if specified
        if (minPrice != null) {
            filters.add("price_per_day >= :minPrice");
            expressionValues.put(":minPrice", AttributeValue.builder().n(minPrice.toString()).build());
        }
        if (maxPrice != null) {
            filters.add("price_per_day <= :maxPrice");
            expressionValues.put(":maxPrice", AttributeValue.builder().n(maxPrice.toString()).build());
        }

        // Build the scan request
        ScanRequest.Builder scanBuilder = ScanRequest.builder()
                .tableName(CARS_TABLE)
                .filterExpression(String.join(" AND ", filters))
                .expressionAttributeValues(expressionValues);

        if (!expressionNames.isEmpty()) {
            scanBuilder.expressionAttributeNames(expressionNames);
        }

        Set<String> availableCarIds = new HashSet<>();

        try {
            ScanResponse response = dynamoDbClient.scan(scanBuilder.build());

            for (Map<String, AttributeValue> item : response.items()) {
                if (item.containsKey("car_id")) {
                    availableCarIds.add(item.get("car_id").s());
                }
            }

            // Handle pagination
            while (response.hasLastEvaluatedKey()) {
                scanBuilder.exclusiveStartKey(response.lastEvaluatedKey());
                response = dynamoDbClient.scan(scanBuilder.build());

                for (Map<String, AttributeValue> item : response.items()) {
                    if (item.containsKey("car_id")) {
                        availableCarIds.add(item.get("car_id").s());
                    }
                }
            }

            context.getLogger().log("Found " + availableCarIds.size() + " available cars");

        } catch (Exception e) {
            context.getLogger().log("Error checking car availability: " + e.getMessage());
            e.printStackTrace();
        }

        return availableCarIds;
    }

    private List<String> getDatesInRange(Instant start, Instant end) {
        List<String> dates = new ArrayList<>();
        LocalDate current = start.atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate endDate = end.atZone(ZoneId.of("UTC")).toLocalDate();
        while (!current.isAfter(endDate)) {
            dates.add(current.toString());
            current = current.plusDays(1);
        }
        return dates;
    }

    private Map<String, String> getCarAvailabilityMap(Context context) {
        Map<String, String> availabilityMap = new HashMap<>();

        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(CARS_TABLE)
                    .projectionExpression("car_id, #status")
                    .expressionAttributeNames(Map.of("#status", "status"))
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            for (Map<String, AttributeValue> item : response.items()) {
                if (!item.containsKey("car_id") || !item.containsKey("status")) continue;
                String car_id = item.get("car_id").s();
                String status = item.get("status").s();
                availabilityMap.put(car_id, status);
            }

            // Handle pagination if there are more results
            Map<String, AttributeValue> lastEvaluatedKey = response.lastEvaluatedKey();
            while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
                scanRequest = ScanRequest.builder()
                        .tableName(CARS_TABLE)
                        .projectionExpression("car_id, #status")
                        .expressionAttributeNames(Map.of("#status", "status"))
                        .exclusiveStartKey(lastEvaluatedKey)
                        .build();

                response = dynamoDbClient.scan(scanRequest);

                for (Map<String, AttributeValue> item : response.items()) {
                    if (!item.containsKey("car_id") || !item.containsKey("status")) continue;
                    String car_id = item.get("car_id").s();
                    String status = item.get("status").s();
                    availabilityMap.put(car_id, status);
                }

                lastEvaluatedKey = response.lastEvaluatedKey();
            }

        } catch (Exception e) {
            context.getLogger().log("Error fetching car availability: " + e.getMessage());
        }

        return availabilityMap;
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"));
        try {
            response.setBody(objectMapper.writeValueAsString(Map.of("error", message)));
        } catch (JsonProcessingException e) {
            response.setBody("{\"error\":\"" + message + "\"}");
        }
        return response;
    }
}