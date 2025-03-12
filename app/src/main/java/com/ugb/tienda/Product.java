package com.ugb.tienda;

public class Product {
    private int id;
    private String code;
    private String description;
    private String brand;
    private String presentation;
    private double price;
    private String imagePath; // Ruta o nombre del archivo de la imagen

    // Constructor vacío
    public Product() { }

    // Constructor completo
    public Product(int id, String code, String description, String brand, String presentation, double price, String imagePath) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.brand = brand;
        this.presentation = presentation;
        this.price = price;
        this.imagePath = imagePath;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getPresentation() {
        return presentation;
    }
    public void setPresentation(String presentation) {
        this.presentation = presentation;
    }

    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }

    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
