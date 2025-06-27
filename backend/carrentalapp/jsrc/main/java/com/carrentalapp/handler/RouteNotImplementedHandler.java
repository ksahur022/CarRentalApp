package com.carrentalapp.handler;

//package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;

public class RouteNotImplementedHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        System.out.println("Received requestEvent: " + requestEvent);
        System.out.println("HTTP Method: " + requestEvent.getHttpMethod());
        System.out.println("Path: " + requestEvent.getPath());

        String message = String.format(
                "Handler for the %s method on the %s path is not implemented.",
                requestEvent.getHttpMethod(),
                requestEvent.getPath()
        );

        JSONObject responseBody = new JSONObject().put("message", message);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(501)
                .withBody(responseBody.toString());
    }
}

