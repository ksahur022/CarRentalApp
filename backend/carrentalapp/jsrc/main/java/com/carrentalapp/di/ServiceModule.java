package com.carrentalapp.di;

import com.carrentalapp.services.ClientReviewService;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Singleton;

@Module
public class ServiceModule {

    @Provides
    @Singleton
    public ClientReviewService provideClientReviewService(DynamoDbClient dynamoDbClient) {
        return new ClientReviewService(dynamoDbClient);
    }
}