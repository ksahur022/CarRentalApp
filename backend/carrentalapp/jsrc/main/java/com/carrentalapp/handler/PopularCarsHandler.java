package com.carrentalapp.handler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.services.PopularCarsService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class PopularCarsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final PopularCarsService popularCarsService;
    private final Gson gson;

    @Inject
    public PopularCarsHandler(PopularCarsService popularCarsService, Gson gson) {
        this.popularCarsService = popularCarsService;
        this.gson = gson;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            Map<String, List<Map<String, String>>> popularCars = popularCarsService.getPopularCars();
//            Map<String, Object> responseBody = new HashMap<>();
//            responseBody.put("content", popularCars);

            String jsonBody = gson.toJson(popularCars);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(jsonBody)
                    .withHeaders(Map.of("Content-Type", "application/json"));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error fetching popular cars: " + e.getMessage());
        }
    }
}