package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.di.AppComponent;
import com.carrentalapp.di.DaggerAppComponent;

public class ModifyBookingHandlerFactory implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ModifyBookingHandlerFactory INSTANCE = new ModifyBookingHandlerFactory();

    public static ModifyBookingHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        AppComponent appComponent = DaggerAppComponent.create();
        ModifyBookingHandler handler = appComponent.modifyBookingHandler();
        return handler.handleRequest(input, context);
    }
}