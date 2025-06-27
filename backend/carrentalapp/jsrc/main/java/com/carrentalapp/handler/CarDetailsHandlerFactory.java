package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.DaggerAppComponent;

public class CarDetailsHandlerFactory {

    private static final CarDetailsHandler handlerInstance =
            DaggerAppComponent.create().buildCarDetailsHandler();

    public static CarDetailsHandler getHandler() {
        if (handlerInstance == null) {
            throw new IllegalStateException("CarDetailsHandler is not initialized");
        }
        return handlerInstance;
    }
}


