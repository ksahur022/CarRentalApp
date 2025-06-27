package com.carrentalapp.di;

import com.carrentalapp.handler.*;
import com.carrentalapp.services.ClientReviewService;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {CognitoModule.class, DynamoDBModule.class, UserModule.class,GsonModule.class })
public interface AppComponent {
    SignUpHandler buildSignUpHandler();
    SigninHandler buildLoginHandler();
    CarSelectionHandler buildCarSelectionHandler();
    CarDetailsHandler buildCarDetailsHandler();
    AboutUsHandler buildAboutUsHandler();
    PopularCarsHandler buildPopularCarsHandler();
    CarBookingHandler buildCarBookingHandler();
    GetClientBookingsHandler buildGetClientBookingsHandler();
    CarBookedDaysHandler buildCarBookedDaysHandler();
    ClientReviewHandler clientReviewHandler();

    // New handlers for booking management
    GetBookingDetailsHandler getBookingDetailsHandler();
    ModifyBookingHandler modifyBookingHandler();
    CancelBookingHandler cancelBookingHandler();

    // New handler for locations
    GetLocationsHandler getLocationsHandler();
    GetAllBookingsHandler getAllBookingsHandler();
    ClientReviewHandler buildClientReviewHandler();
    ClientReviewService clientReviewService();

}