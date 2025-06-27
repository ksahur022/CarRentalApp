package com.carrentalapp.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

@DynamoDbBean
public class Location {
    private String locationId;
    private String name;
    private String address;
    private String city;
    private String country;

    public Location() {
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("location_id")
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    // Static helper method to convert DynamoDB item to Location object
    public static Location fromDynamoDbItem(Map<String, AttributeValue> item) {
        Location location = new Location();

        // Check if attributes exist and get their values
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

    // Convert Location object to DynamoDB item
    public Map<String, AttributeValue> toDynamoDbItem() {
        Map<String, AttributeValue> item = new HashMap<>();

        if (locationId != null) {
            item.put("location_id", AttributeValue.builder().s(locationId).build());
        }

        if (name != null) {
            item.put("name", AttributeValue.builder().s(name).build());
        }

        if (address != null) {
            item.put("address", AttributeValue.builder().s(address).build());
        }

        if (city != null) {
            item.put("city", AttributeValue.builder().s(city).build());
        }

        if (country != null) {
            item.put("country", AttributeValue.builder().s(country).build());
        }

        return item;
    }

    @Override
    public String toString() {
        return "Location{" +
                "locationId='" + locationId + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}