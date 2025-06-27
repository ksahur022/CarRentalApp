package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.DaggerAppComponent;

public class GetClientBookingsHandlerFactory {

    private static final GetClientBookingsHandler handlerInstance =
            DaggerAppComponent.create().buildGetClientBookingsHandler();

    public static GetClientBookingsHandler getHandler() {
        if (handlerInstance == null) {
            throw new IllegalStateException("GetClientBookingsHandler is not initialized");
        }
        return handlerInstance;
    }

    // Added missing method
    public static GetClientBookingsHandler getInstance() {
        return getHandler();
    }
}