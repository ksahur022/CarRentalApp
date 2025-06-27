
package com.carrentalapp.handler;

import com.carrentalapp.di.DaggerAppComponent;

public class SigninHandlerFactory {
    // Use volatile for thread safety with lazy initialization
    private static volatile SigninHandler handlerInstance;

    public static SigninHandler getHandler() {
        // Double-checked locking for thread-safe lazy initialization
        if (handlerInstance == null) {
            synchronized (SigninHandlerFactory.class) {
                if (handlerInstance == null) {
                    handlerInstance = DaggerAppComponent.create().buildLoginHandler();
                }
            }
        }

        if (handlerInstance == null) {
            throw new IllegalStateException("LoginHandler is not initialized");
        }
        return handlerInstance;
    }
}



