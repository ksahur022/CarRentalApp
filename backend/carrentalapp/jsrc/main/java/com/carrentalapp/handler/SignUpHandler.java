package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.carrentalapp.models.SignUpRequest;
import com.carrentalapp.services.UserService;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.Map;

public class SignUpHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final UserService userService;

    @Inject
    public SignUpHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            SignUpRequest signUpRequest = SignUpRequest.fromJson(request.getBody());

            Map<String, Object> response = userService.registerUser(signUpRequest);
            int statusCode = response.containsKey("statusCode") ? (Integer) response.get("statusCode") : 200;

            JSONObject responseBody = new JSONObject();
            for (Map.Entry<String, Object> entry : response.entrySet()) {
                if (!entry.getKey().equals("statusCode")) {
                    responseBody.put(entry.getKey(), entry.getValue());
                }
            }




            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withBody(responseBody.toString());

        } catch (IllegalArgumentException e) {
            // Create a simple JSON with only the message
            JSONObject responseBody = new JSONObject();
            responseBody.put("message", e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(responseBody.toString()); // Do NOT wrap this in another object
        }
        catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(new JSONObject().put("message", "Internal Server Error: " + e.getMessage()).toString());
        }
    }
}

