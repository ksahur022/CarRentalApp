package com.carrentalapp.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

@DynamoDbBean
public class ClientReview {
    private String review_id;
    private String author;
    private String author_image_url;
    private String car_id;
    private String date;
    private Double rental_experience;
    private String text;
    private String user_id;
    private String booking_id; // Added booking_id field

    public ClientReview() {
        // Default constructor required by DynamoDB
    }

    // Constructor for sample data
    public ClientReview(String author, String authorImageUrl, String date, Double rentalExperience, String text) {
        this.author = author;
        this.author_image_url = authorImageUrl;
        this.date = date;
        this.rental_experience = rentalExperience;
        this.text = text;
    }

    @DynamoDbPartitionKey
    public String getReviewId() {
        return review_id;
    }

    public void setReviewId(String review_id) {
        this.review_id = review_id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorImageUrl() {
        return author_image_url;
    }

    public void setAuthorImageUrl(String author_image_url) {
        this.author_image_url = author_image_url;
    }

    public String getCarId() {
        return car_id;
    }

    public void setCarId(String car_id) {
        this.car_id = car_id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Double getRentalExperience() {
        return rental_experience;
    }

    public void setRentalExperience(Double rental_experience) {
        this.rental_experience = rental_experience;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUserId() {
        return user_id;
    }

    public void setUserId(String user_id) {
        this.user_id = user_id;
    }

    // New getter and setter for booking_id
    public String getBookingId() {
        return booking_id;
    }

    public void setBookingId(String booking_id) {
        this.booking_id = booking_id;
    }

    // Static helper method to convert DynamoDB item to ClientReview object
    public static ClientReview fromDynamoDbItem(Map<String, AttributeValue> item) {
        ClientReview review = new ClientReview();
        review.setReviewId(item.containsKey("review_id") ? item.get("review_id").s() : "");
        review.setAuthor(item.containsKey("author") ? item.get("author").s() : "");
        review.setAuthorImageUrl(item.containsKey("author_image_url") ? item.get("author_image_url").s() : "");
        review.setCarId(item.containsKey("car_id") ? item.get("car_id").s() : "");
        review.setDate(item.containsKey("date") ? item.get("date").s() : "");

        // Handle rental_experience as a number
        if (item.containsKey("rental_experience")) {
            try {
                if (item.get("rental_experience").n() != null) {
                    review.setRentalExperience(Double.parseDouble(item.get("rental_experience").n()));
                }
            } catch (Exception e) {
                // Fallback to string if parsing fails
                try {
                    if (item.get("rental_experience").s() != null) {
                        review.setRentalExperience(Double.parseDouble(item.get("rental_experience").s()));
                    }
                } catch (Exception ex) {
                    review.setRentalExperience(0.0);
                }
            }
        }

        review.setText(item.containsKey("text") ? item.get("text").s() : "");
        review.setUserId(item.containsKey("user_id") ? item.get("user_id").s() : "");
        review.setBookingId(item.containsKey("booking_id") ? item.get("booking_id").s() : ""); // Added booking_id

        return review;
    }

    // Convert ClientReview object to DynamoDB item
    public Map<String, AttributeValue> toDynamoDbItem() {
        Map<String, AttributeValue> item = new HashMap<>();

        if (review_id != null) item.put("review_id", AttributeValue.builder().s(review_id).build());
        if (author != null) item.put("author", AttributeValue.builder().s(author).build());
        if (author_image_url != null) item.put("author_image_url", AttributeValue.builder().s(author_image_url).build());
        if (car_id != null) item.put("car_id", AttributeValue.builder().s(car_id).build());
        if (date != null) item.put("date", AttributeValue.builder().s(date).build());
        if (rental_experience != null) item.put("rental_experience", AttributeValue.builder().n(rental_experience.toString()).build());
        if (text != null) item.put("text", AttributeValue.builder().s(text).build());
        if (user_id != null) item.put("user_id", AttributeValue.builder().s(user_id).build());
        if (booking_id != null) item.put("booking_id", AttributeValue.builder().s(booking_id).build()); // Added booking_id

        return item;
    }
}