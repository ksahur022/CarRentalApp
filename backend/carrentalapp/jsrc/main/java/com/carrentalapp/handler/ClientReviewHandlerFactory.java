package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.DaggerAppComponent;

public class ClientReviewHandlerFactory {

    private static final ClientReviewHandler handlerInstance =
            DaggerAppComponent.create().buildClientReviewHandler();

    public static ClientReviewHandler getHandler() {
        if (handlerInstance == null) {
            throw new IllegalStateException("CarSelectionHandler is not initialized");
        }
        return handlerInstance;
    }
}