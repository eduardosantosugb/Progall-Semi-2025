package com.ugb.tiendacouchdb;

import java.io.Serializable;

public class Product implements Serializable {
    private int id;
    private String codigo;
    private String descripcion;
    private String marca;
    private String presentacion;
    private double precio;
    private String imagen;
    private double costo;
    private double ganancia; // % de ganancia calculado

    public Product() {}

    public Product(int id, String codigo, String descripcion, String marca,
                   String presentacion, double precio, double costo, double ganancia, String imagen) {
        this.id = id;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.marca = marca;
        this.presentacion = presentacion;
        this.precio = precio;
        this.costo = costo;
        this.ganancia = ganancia;
        this.imagen = imagen;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMarca() {
        return marca;
    }
    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getPresentacion() {
        return presentacion;
    }
    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }

    public double getPrecio() {
        return precio;
    }
    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public double getCosto() {
        return costo;
    }
    public void setCosto(double costo) {
        this.costo = costo;
    }

    public double getGanancia() {
        return ganancia;
    }
    public void setGanancia(double ganancia) {
        this.ganancia = ganancia;
    }

    public String getImagen() {
        return imagen;
    }
    public void setImagen(String imagen) {
        this.imagen = imagen;
    }
}
