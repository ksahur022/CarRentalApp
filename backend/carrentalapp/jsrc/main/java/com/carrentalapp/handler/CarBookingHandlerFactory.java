package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.DaggerAppComponent;

public class CarBookingHandlerFactory {

    private static final CarBookingHandler handlerInstance =
            DaggerAppComponent.create().buildCarBookingHandler();

    public static CarBookingHandler getHandler() {
        if (handlerInstance == null) {
            throw new IllegalStateException("CarDetailsHandler is not initialized");
        }
        return handlerInstance;
    }

    // Added missing method
    public static CarBookingHandler getInstance() {
        return getHandler();
    }
}