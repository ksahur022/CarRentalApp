package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.services.AboutUsService;

import javax.inject.Inject;
import java.util.Collections;

public class AboutUsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AboutUsService aboutUsService;

    @Inject
    public AboutUsHandler(AboutUsService aboutUsService) {
        this.aboutUsService = aboutUsService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String aboutUsJson = aboutUsService.getAboutUsInfo();
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(aboutUsJson)
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
    }
}

