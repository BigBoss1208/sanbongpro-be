package com.example.becicd.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import org.springframework.web.multipart.MultipartFile;

public class ProductRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    private String description;

    @NotNull(message = "price is required")
    @PositiveOrZero(message = "price must be >= 0")
    private Double price;

    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity must be >= 0")
    private Integer quantity;

    private MultipartFile image;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}
