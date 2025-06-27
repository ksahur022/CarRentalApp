package com.carrentalapp.models;

import java.util.List;
import java.util.stream.Collectors;

public class LocationsResponse {
    private List<LocationDto> locations;
    private String message;

    public LocationsResponse() {
    }

    public LocationsResponse(List<Location> locations) {
        this.locations = locations.stream()
                .map(LocationDto::fromLocation)
                .collect(Collectors.toList());
        this.message = "Successfully retrieved " + locations.size() + " locations";
    }

    public List<LocationDto> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationDto> locations) {
        this.locations = locations;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // DTO for Location to control what data is sent to the client
    public static class LocationDto {
        private String locationId;
        private String name;
        private String address;
        private String city;
        private String country;

        public static LocationDto fromLocation(Location location) {
            LocationDto dto = new LocationDto();
            dto.setLocationId(location.getLocationId());
            dto.setName(location.getName());
            dto.setAddress(location.getAddress());
            dto.setCity(location.getCity());
            dto.setCountry(location.getCountry());
            return dto;
        }

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
    }
}