package com.carrentalapp.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DynamoDbBean
public class Car {
    private String carId;  // Changed from car_id to carId
    private String make;
    private String model;
    private String year;
    private Double pricePerDay;
    private Double rating;
    private Double serviceRating;
    private String locationId;
    private String locationName;
    private String category;
    private String gearBoxType;
    private String fuelType;
    private String engineCapacity;
    private Double fuelConsumption;
    private String passengerCapacity;
    private String climateControlOption;
    private List<String> imageUrls;
    private String status; // AVAILABLE, BOOKED, MAINTENANCE

    public Car() {
        this.imageUrls = new ArrayList<>();
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("car_id")
    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(Double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Double getServiceRating() {
        return serviceRating;
    }

    public void setServiceRating(Double serviceRating) {
        this.serviceRating = serviceRating;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getGearBoxType() {
        return gearBoxType;
    }

    public void setGearBoxType(String gearBoxType) {
        this.gearBoxType = gearBoxType;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getEngineCapacity() {
        return engineCapacity;
    }

    public void setEngineCapacity(String engineCapacity) {
        this.engineCapacity = engineCapacity;
    }

    public Double getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(Double fuelConsumption) {
        this.fuelConsumption = fuelConsumption;
    }

    public String getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(String passengerCapacity) {
        this.passengerCapacity = passengerCapacity;
    }

    public String getClimateControlOption() {
        return climateControlOption;
    }

    public void setClimateControlOption(String climateControlOption) {
        this.climateControlOption = climateControlOption;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Static helper method to convert DynamoDB item to Car object
    public static Car fromDynamoDbItem(Map<String, AttributeValue> item) {
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

        // Additional fields with safer parsing
        car.setEngineCapacity(item.containsKey("engine_capacity") ? item.get("engine_capacity").s() : "");

        // Fix for fuel_consumption - handle both number and string formats safely
        if (item.containsKey("fuel_consumption")) {
            AttributeValue fuelConsumptionAttr = item.get("fuel_consumption");
            try {
                if (fuelConsumptionAttr.n() != null && !fuelConsumptionAttr.n().isEmpty()) {
                    car.setFuelConsumption(Double.parseDouble(fuelConsumptionAttr.n()));
                } else if (fuelConsumptionAttr.s() != null && !fuelConsumptionAttr.s().isEmpty()) {
                    car.setFuelConsumption(Double.parseDouble(fuelConsumptionAttr.s()));
                } else {
                    car.setFuelConsumption(0.0);
                }
            } catch (Exception e) {
                car.setFuelConsumption(0.0); // Default value on parsing error
            }
        } else {
            car.setFuelConsumption(0.0);
        }
        car.setPassengerCapacity(item.containsKey("passenger_capacity") ? item.get("passenger_capacity").s() : "");
        car.setClimateControlOption(item.containsKey("climate_control_option") ? item.get("climate_control_option").s() : "");

        // Handle image URLs
        if (item.containsKey("image_url")) {
            car.setImageUrls(List.of(item.get("image_url").s()));
        } else if (item.containsKey("imageUrls")) {
            if (item.get("imageUrls").hasL()) {
                car.setImageUrls(item.get("imageUrls").l().stream()
                        .map(av -> av.s())
                        .collect(Collectors.toList()));
            } else if (item.get("imageUrls").hasSs()) {
                car.setImageUrls(new ArrayList<>(item.get("imageUrls").ss()));
            }
        }

        return car;
    }

    // Convert Car object to DynamoDB item
    public Map<String, AttributeValue> toDynamoDbItem() {
        Map<String, AttributeValue> item = new HashMap<>();

        if (carId != null) item.put("car_id", AttributeValue.builder().s(carId).build());
        if (make != null) item.put("make", AttributeValue.builder().s(make).build());
        if (model != null) item.put("model", AttributeValue.builder().s(model).build());
        if (year != null) item.put("year", AttributeValue.builder().n(year).build());
        if (pricePerDay != null) item.put("price_per_day", AttributeValue.builder().n(pricePerDay.toString()).build());
        if (rating != null) item.put("car_rating", AttributeValue.builder().n(rating.toString()).build());
        if (serviceRating != null) item.put("service_rating", AttributeValue.builder().n(serviceRating.toString()).build());
        if (locationId != null) item.put("location_id", AttributeValue.builder().s(locationId).build());
        if (locationName != null) item.put("location_name", AttributeValue.builder().s(locationName).build());
        if (category != null) item.put("category", AttributeValue.builder().s(category).build());
        if (gearBoxType != null) item.put("gear_box_type", AttributeValue.builder().s(gearBoxType).build());
        if (fuelType != null) item.put("fuel_type", AttributeValue.builder().s(fuelType).build());
        if (status != null) item.put("status", AttributeValue.builder().s(status).build());
        else item.put("status", AttributeValue.builder().s("UNAVAILABLE").build());

        if (engineCapacity != null) item.put("engine_capacity", AttributeValue.builder().s(engineCapacity).build());
        if (fuelConsumption != null) item.put("fuel_consumption", AttributeValue.builder().n(String.valueOf(fuelConsumption)).build());
        if (passengerCapacity != null) item.put("passenger_capacity", AttributeValue.builder().s(passengerCapacity).build());
        if (climateControlOption != null) item.put("climate_control_option", AttributeValue.builder().s(climateControlOption).build());

        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<AttributeValue> imageUrlsAttributeValues = imageUrls.stream()
                    .map(url -> AttributeValue.builder().s(url).build())
                    .collect(Collectors.toList());
            item.put("imageUrls", AttributeValue.builder().l(imageUrlsAttributeValues).build());

            // Set the first image as image_url for compatibility
            item.put("image_url", AttributeValue.builder().s(imageUrls.get(0)).build());
        }

        return item;
    }
}