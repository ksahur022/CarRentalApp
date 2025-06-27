package com.carrentalapp.di;

import com.carrentalapp.models.Admin;
import com.carrentalapp.models.Booking;
import com.carrentalapp.models.Car;
import com.carrentalapp.models.Location;
import com.carrentalapp.models.SupportAgent;
import com.carrentalapp.services.BookingService;
import com.carrentalapp.services.LocationService;
import com.carrentalapp.services.PopularCarsService;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Singleton;

@Module
public class DynamoDBModule {

    @Provides
    @Singleton
    DynamoDbClient provideDynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(System.getenv("REGION")))
                .build();
    }

    @Provides
    @Singleton
    DynamoDbEnhancedClient provideDynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Provides
    @Singleton
    DynamoDbTable<SupportAgent> provideSupportAgentTable(DynamoDbEnhancedClient enhancedClient) {
        String TABLE_NAME = System.getenv("SupportAgent");  // Ensure this points to your support agent table
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(SupportAgent.class));
    }

    @Provides
    @Singleton
    DynamoDbTable<Admin> provideAdminTable(DynamoDbEnhancedClient enhancedClient) {
        String TABLE_NAME = System.getenv("Admin");  // Make sure this environment variable is set
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Admin.class));
    }

    @Provides
    @Singleton
    DynamoDbTable<Car> provideCarTable(DynamoDbEnhancedClient enhancedClient) {
        String TABLE_NAME = System.getenv("Cars");  // Make sure to set this environment variable
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Car.class));
    }

    // New provider for Bookings table
    @Provides
    @Singleton
    DynamoDbTable<Booking> provideBookingTable(DynamoDbEnhancedClient enhancedClient) {
        String TABLE_NAME = System.getenv("Bookings");  // Make sure to set this environment variable
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Booking.class));
    }

    // New provider for Location table
    @Provides
    @Singleton
    DynamoDbTable<Location> provideLocationTable(DynamoDbEnhancedClient enhancedClient) {
        String TABLE_NAME = System.getenv("Locations") != null ? System.getenv("Locations") : "Locations";
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Location.class));
    }

    // Provide BookingService
    @Provides
    @Singleton
    BookingService provideBookingService(DynamoDbTable<Booking> bookingTable, DynamoDbTable<Car> carTable, DynamoDbClient dynamoDbClient) {
        return new BookingService(bookingTable, carTable, dynamoDbClient);
    }

    // Provide LocationService
    @Provides
    @Singleton
    LocationService provideLocationService(DynamoDbTable<Location> locationTable) {
        return new LocationService(locationTable);
    }
    @Provides
    @Singleton
    public PopularCarsService providePopularCarsService(DynamoDbClient dynamoDbClient) {
        return new PopularCarsService(dynamoDbClient);
    }
}