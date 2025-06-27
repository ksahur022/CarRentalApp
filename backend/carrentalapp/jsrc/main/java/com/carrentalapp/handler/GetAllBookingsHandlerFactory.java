package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.AppComponent;
import com.carrentalapp.di.DaggerAppComponent;

public class GetAllBookingsHandlerFactory {
    public static RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> createHandler() {
        AppComponent component = DaggerAppComponent.create();
        return component.getAllBookingsHandler();
    }
}