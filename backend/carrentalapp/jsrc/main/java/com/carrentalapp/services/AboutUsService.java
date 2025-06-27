package com.carrentalapp.services;

import javax.inject.Inject;

public class AboutUsService {

    public static final String ABOUT_US_JSON = "{\n" +
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

    @Inject
    public AboutUsService() {}

    public String getAboutUsInfo() {
        return ABOUT_US_JSON;
    }
}
