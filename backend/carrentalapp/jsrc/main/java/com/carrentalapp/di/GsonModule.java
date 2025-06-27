package com.carrentalapp.di;

import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class GsonModule {
    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }
}