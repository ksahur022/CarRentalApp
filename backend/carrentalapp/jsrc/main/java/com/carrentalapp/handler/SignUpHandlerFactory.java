package com.carrentalapp.handler;

import com.carrentalapp.di.DaggerAppComponent;

public class SignUpHandlerFactory {

    private static final SignUpHandler handlerInstance = DaggerAppComponent.create().buildSignUpHandler();

    public static SignUpHandler getHandler() {
        if (handlerInstance == null) {
            throw new IllegalStateException("SignUpHandler is not initialized");
        }
        return handlerInstance;
    }

    // Optional: Uncomment for testing
    // public static void main(String[] args) {
    //     SignUpHandlerFactory.getHandler();
    // }
}
