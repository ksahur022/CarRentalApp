package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.DaggerAppComponent;

public class CarBookedDaysHandlerFactory {

    private static final CarBookedDaysHandler handlerInstance =
            DaggerAppComponent.create().buildCarBookedDaysHandler();

    public static CarBookedDaysHandler getHandler() {
        if (handlerInstance == null) {
            throw new IllegalStateException("CarBookedDaysHandler is not initialized");
        }
        return handlerInstance;
    }
}
