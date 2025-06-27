package com.carrentalapp.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.models.Location;
import com.carrentalapp.models.LocationsResponse;
import com.carrentalapp.services.LocationService;
import com.google.gson.Gson;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class GetLocationsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final LocationService locationService;
    private final Gson gson;

    @Inject // Add this annotation
    public GetLocationsHandler(LocationService locationService) {
        this.locationService = locationService;
        this.gson = new Gson();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            List<Location> locations = locationService.getAllLocations();
            LocationsResponse response = new LocationsResponse(locations);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(gson.toJson(response));
        } catch (Exception e) {
            context.getLogger().log("Error retrieving locations: " + e.getMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"message\":\"Error retrieving locations\"}");
        }
    }
}