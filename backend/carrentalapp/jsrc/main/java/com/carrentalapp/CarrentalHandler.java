package com.carrentalapp;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.carrentalapp.handler.*;
import com.carrentalapp.models.RouteKey;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

//import static com.carrentalapp.services.AboutUsService.ABOUT_US_JSON;
import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_CLIENT_ID;
import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_USER_POOL_ID;

@LambdaHandler(
		lambdaName = "carrental_handler",
		roleName = "carrental_handler-role",
		isPublishVersion = true,
		aliasName = "${lambdas_alias_name}",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DependsOn(resourceType = ResourceType.COGNITO_USER_POOL, name = "${pool_name}")
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "REGION", value = "${region}"),
		@EnvironmentVariable(key = "COGNITO_ID", value = "${pool_name}", valueTransformer = USER_POOL_NAME_TO_USER_POOL_ID),
		@EnvironmentVariable(key = "CLIENT_ID", value = "${pool_name}", valueTransformer = USER_POOL_NAME_TO_CLIENT_ID),
		@EnvironmentVariable(key = "CLIENT_DB", value = "${client_db}"),
		@EnvironmentVariable(key = "Cars", value = "Cars"),
		@EnvironmentVariable(key = "Bookings", value = "Bookings"),
		@EnvironmentVariable(key = "Locations", value = "Locations"),
		@EnvironmentVariable(key = "Reviews", value = "Reviews")
})
public class CarrentalHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private static final Logger LOGGER = Logger.getLogger(CarrentalHandler.class.getName());
	private static final String ABOUT_US_JSON = "{\n" +
			"  \"content\": [\n" +
			"    {\n" +
			"      \"description\": \"in car rentals highlights a steadfast commitment to excellence, marked by a track record of trust and satisfaction among thousands of clients worldwide\",\n" +
			"      \"numericValue\": \"15\",\n" +
			"      \"title\": \"years\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"description\": \"we make car rentals accessible and convenient for customers no matter where their travels take them, ensuring quality service and easy access\",\n" +
			"      \"numericValue\": \"6\",\n" +
			"      \"title\": \"locations\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"description\": \"we cater to every kind of traveler, from business professionals to families and adventure seekers, ensuring the perfect vehicle is always available\",\n" +
			"      \"numericValue\": \"25\",\n" +
			"      \"title\": \"car brands\"\n" +
			"    },\n" +
			"    {\n" +
			"      \"description\": \"we cater to every kind of traveler, from business professionals to families and adventure seekers, ensuring the perfect vehicle is always available\",\n" +
			"      \"numericValue\": \"100+\",\n" +
			"      \"title\": \"cars\"\n" +
			"    }\n" +
			"  ]\n" +
			"}";

	private final Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> handlersByRouteKey;
	private final Map<String, String> headersForCORS;
	private final RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> routeNotImplementedHandler;

	public CarrentalHandler() {
		this.handlersByRouteKey = initHandlers();
		this.headersForCORS = initHeadersForCORS();
		this.routeNotImplementedHandler = new RouteNotImplementedHandler();
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
		return getHandler(requestEvent)
				.handleRequest(requestEvent, context)
				.withHeaders(headersForCORS);
	}

	private RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getHandler(APIGatewayProxyRequestEvent requestEvent) {
		return handlersByRouteKey.getOrDefault(getRouteKey(requestEvent), routeNotImplementedHandler);
	}

	private RouteKey getRouteKey(APIGatewayProxyRequestEvent requestEvent) {
		// Use the resource path from API Gateway if available
		String resourcePath = requestEvent.getResource();
		if (resourcePath != null && !resourcePath.isEmpty()) {
			return new RouteKey(requestEvent.getHttpMethod(), resourcePath);
		}
		String path = requestEvent.getPath();
		if (path.endsWith("/") && path.length() > 1) {
			path = path.substring(0, path.length() - 1);
		}
		// Fallback to the actual path
		return new RouteKey(requestEvent.getHttpMethod(), path);
	}

	private Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> initHandlers() {
		Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> handlers = new HashMap<>();

		// Existing routes
		handlers.put(new RouteKey("POST", "/auth/sign-up"), SignUpHandlerFactory.getHandler());
		handlers.put(new RouteKey("POST", "/auth/sign-in"), SigninHandlerFactory.getHandler());
		handlers.put(new RouteKey("GET", "/cars"), CarSelectionHandlerFactory.getHandler());
		handlers.put(new RouteKey("GET", "/cars/{car_id}"), CarDetailsHandlerFactory.getHandler());
		handlers.put(new RouteKey("GET", "/home/about-us"), AboutUsHandlerFactory.getHandler());

		// Existing booking routes
		handlers.put(new RouteKey("POST", "/bookings"), CarBookingHandlerFactory.getInstance());
		handlers.put(new RouteKey("GET", "/bookings/{userId}"), GetClientBookingsHandlerFactory.getInstance());

		// New booking management routes
		handlers.put(new RouteKey("GET", "/bookings/details/{bookingId}"), GetBookingDetailsHandlerFactory.getInstance());

		// Updated routes to avoid API Gateway path parameter conflict
		handlers.put(new RouteKey("POST", "/bookings/modify/{bookingId}"), ModifyBookingHandlerFactory.getInstance());
		handlers.put(new RouteKey("POST", "/bookings/cancel/{bookingId}"), CancelBookingHandlerFactory.getInstance());

		// Add this line to the initHandlers() method in CarrentalHandler.java
		handlers.put(new RouteKey("GET", "/locations"), GetLocationsHandlerFactory.getHandler());

		handlers.put(new RouteKey("GET", "/api/support/bookings"), GetAllBookingsHandlerFactory.createHandler());

		handlers.put(new RouteKey("GET", "/cars/{car_id}/client-review"), ClientReviewHandlerFactory.getHandler());
		handlers.put(new RouteKey("POST", "/reviews"), ClientReviewSubmitHandlerFactory.getHandler());
		handlers.put(new RouteKey("POST", "/feedbacks"), ClientReviewSubmitHandlerFactory.getHandler());
		// Add route for car booked days
		handlers.put(new RouteKey("GET", "/cars/{car_id}/booked-days"), CarBookedDaysHandlerFactory.getHandler());
		handlers.put(new RouteKey("GET", "/cars/popular"), PopularCarsHandlerFactory.getHandler());

		// Add the new route for recent feedbacks - using ClientReviewHandler
		handlers.put(new RouteKey("GET", "/feedbacks/recent"), ClientReviewHandlerFactory.getHandler());

		return handlers;
	}

	/**
	 * Handles requests to the About Us endpoint.
	 *
	 * @param request The API Gateway request event
	 * @param context The Lambda execution context
	 * @return API Gateway response with About Us information
	 */
	private APIGatewayProxyResponseEvent handleAboutUs(APIGatewayProxyRequestEvent request, Context context) {
//		LOGGER.info("Handling About Us route...");

		return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withBody(ABOUT_US_JSON)
				.withHeaders(Collections.singletonMap("Content-Type", "application/json"));
	}

	/**
	 * To allow all origins, all methods, and common headers
	 * <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/how-to-cors.html">Using cross-origin resource sharing (CORS)</a>
	 */
	private Map<String, String> initHeadersForCORS() {
		return Map.of(
				"Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
				"Access-Control-Allow-Origin", "*",
				"Access-Control-Allow-Methods", "*",
				"Accept-Version", "*"
		);
	}
}