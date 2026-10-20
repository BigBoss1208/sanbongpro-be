package com.example.becicd.dto;

public class VNPayResponseDTO {

    private String paymentUrl;

    public VNPayResponseDTO() {}

    public VNPayResponseDTO(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
}