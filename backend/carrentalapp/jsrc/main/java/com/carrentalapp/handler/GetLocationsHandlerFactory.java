package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.DaggerAppComponent;

public class GetLocationsHandlerFactory {
    private static GetLocationsHandler instance;

    public static synchronized RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getHandler() {
        if (instance == null) {
            instance = DaggerAppComponent.create().getLocationsHandler();
        }
        return instance;
    }
}