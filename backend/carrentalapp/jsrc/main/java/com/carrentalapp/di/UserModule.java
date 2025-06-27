package com.carrentalapp.di;

import com.carrentalapp.services.CognitoService;
import com.carrentalapp.services.DynamoDBService;
import com.carrentalapp.services.UserService;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class UserModule {

    @Provides
    @Singleton
    public UserService provideUserService(CognitoService cognitoService, DynamoDBService dynamoDBService) {
        return new UserService(cognitoService, dynamoDBService);
    }
}
