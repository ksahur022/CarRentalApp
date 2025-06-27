package com.carrentalapp.models;

public class ModifyBookingRequest {
    private String pickupDateTime;
    private String pickupLocationId;
    private String dropOffDateTime;
    private String dropOffLocationId;

    public String getPickupDateTime() {
        return pickupDateTime;
    }

    public void setPickupDateTime(String pickupDateTime) {
        this.pickupDateTime = pickupDateTime;
    }

    public String getPickupLocationId() {
        return pickupLocationId;
    }

    public void setPickupLocationId(String pickupLocationId) {
        this.pickupLocationId = pickupLocationId;
    }

    public String getDropOffDateTime() {
        return dropOffDateTime;
    }

    public void setDropOffDateTime(String dropOffDateTime) {
        this.dropOffDateTime = dropOffDateTime;
    }

    public String getDropOffLocationId() {
        return dropOffLocationId;
    }

    public void setDropOffLocationId(String dropOffLocationId) {
        this.dropOffLocationId = dropOffLocationId;
    }
}