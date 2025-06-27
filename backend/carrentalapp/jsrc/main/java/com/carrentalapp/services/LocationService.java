package com.carrentalapp.services;

import com.carrentalapp.models.Location;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocationService {
    private final DynamoDbTable<Location> locationTable;

    @Inject
    public LocationService(DynamoDbTable<Location> locationTable) {
        this.locationTable = locationTable;
    }

    /**
     * Get all locations
     * @return List of all locations
     */
    public List<Location> getAllLocations() {
        try {
            PageIterable<Location> pagedResults = locationTable.scan();
            List<Location> locations = new ArrayList<>();

            pagedResults.items().forEach(locations::add);

            return locations;
        } catch (DynamoDbException e) {
            System.err.println("Error scanning locations: " + e.getMessage());
            throw new RuntimeException("Error scanning locations: " + e.getMessage(), e);
        }
    }

    /**
     * Get location by ID
     * @param locationId The location ID
     * @return Optional containing the location if found
     */
    public Optional<Location> getLocationById(String locationId) {
        try {
            Location location = locationTable.getItem(r -> r.key(k -> k.partitionValue(locationId)));
            return Optional.ofNullable(location);
        } catch (DynamoDbException e) {
            System.err.println("Error retrieving location by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Save a location
     * @param location The location to save
     */
    public void saveLocation(Location location) {
        try {
            locationTable.putItem(location);
        } catch (DynamoDbException e) {
            System.err.println("Error saving location: " + e.getMessage());
            throw new RuntimeException("Error saving location: " + e.getMessage(), e);
        }
    }
}