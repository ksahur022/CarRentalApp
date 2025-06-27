package com.carrentalapp.models;

import java.util.List;

public class ClientReviewResponse {
    private List<ClientReview> content;
    private int currentPage;
    private int totalElements;
    private int totalPages;

    public ClientReviewResponse(List<ClientReview> content, int currentPage, int totalElements, int totalPages) {
        this.content = content;
        this.currentPage = currentPage;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    // Getters and setters
    public List<ClientReview> getContent() {
        return content;
    }

    public void setContent(List<ClientReview> content) {
        this.content = content;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}