package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class ClientReviewSubmitHandlerFactory {
    public static RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getHandler() {
        return new ClientReviewSubmitHandler();
    }
}