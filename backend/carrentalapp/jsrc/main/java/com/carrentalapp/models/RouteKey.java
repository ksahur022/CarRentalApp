package com.carrentalapp.models;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Objects;

public class RouteKey {

    private final String httpMethod;
    private final String path;

    public RouteKey(String httpMethod, String path) {
        this.httpMethod = httpMethod;
        this.path = path;
    }

//    public String getMethod() {
//        return method;
//    }
    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteKey routeKey = (RouteKey) o;
        return Objects.equals(httpMethod, routeKey.httpMethod) &&
                Objects.equals(path, routeKey.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpMethod, path);
    }

    @Override
    public String toString() {
        return "RouteKey{" +
                "method='" + httpMethod + '\'' +
                ", path='" + path + '\'' +
                '}';
    }

}

