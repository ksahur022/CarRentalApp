package com.carrentalapp.services;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;  // Changed from QuerySpec to ScanSpec
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.UUID;

@Singleton
public class CarService {
    private final AmazonDynamoDB dynamoDBClient;
    private final DynamoDB dynamoDB;

    @Inject
    public CarService(AmazonDynamoDB dynamoDBClient) {
        this.dynamoDBClient = dynamoDBClient;
        this.dynamoDB = new DynamoDB(dynamoDBClient);
    }

    public boolean isCarAvailable(UUID carId, LocalDateTime pickupDateTime, LocalDateTime dropOffDateTime) {
        // First check if car status is AVAILABLE
        Table carsTable = dynamoDB.getTable("Cars");
        GetItemSpec spec = new GetItemSpec()
                .withPrimaryKey("car_id", carId.toString());

        Item carItem = carsTable.getItem(spec);
        if (carItem == null || !carItem.getString("status").equals("AVAILABLE")) {
            return false;
        }

        // Then check if there are no overlapping bookings
        Table bookingsTable = dynamoDB.getTable("Bookings");
        ScanSpec bookingSpec = new ScanSpec()  // Changed from QuerySpec to ScanSpec
                .withFilterExpression("car_id = :carId AND booking_status = :status AND " +
                        "((pickup_datetime <= :dropoff AND dropoff_datetime >= :pickup) OR " +
                        "(pickup_datetime >= :pickup AND pickup_datetime <= :dropoff))")
                .withValueMap(new ValueMap()
                        .withString(":carId", carId.toString())
                        .withString(":status", "RESERVED")
                        .withString(":pickup", pickupDateTime.toString())
                        .withString(":dropoff", dropOffDateTime.toString()));

        int count = 0;
        for (Item item : bookingsTable.scan(bookingSpec)) {
            count++;
            break;
        }

        return count == 0;
    }

    public String getCarModel(UUID carId) {
        Table carsTable = dynamoDB.getTable("Cars");
        GetItemSpec spec = new GetItemSpec()
                .withPrimaryKey("car_id", carId.toString());

        Item carItem = carsTable.getItem(spec);
        return carItem != null ? carItem.getString("model") : "Unknown Car";
    }

    public String getCarImageUrl(UUID carId) {
        Table carsTable = dynamoDB.getTable("Cars");
        GetItemSpec spec = new GetItemSpec()
                .withPrimaryKey("car_id", carId.toString());

        Item carItem = carsTable.getItem(spec);
        return carItem != null ? carItem.getString("image_url") : "";
    }

    public void updateCarStatus(UUID carId, String status) {
        Table carsTable = dynamoDB.getTable("Cars");
        UpdateItemSpec updateSpec = new UpdateItemSpec()
                .withPrimaryKey("car_id", carId.toString())
                .withUpdateExpression("set #status = :status")
                .withNameMap(new NameMap().with("#status", "status"))
                .withValueMap(new ValueMap().withString(":status", status));

        carsTable.updateItem(updateSpec);
    }
}
