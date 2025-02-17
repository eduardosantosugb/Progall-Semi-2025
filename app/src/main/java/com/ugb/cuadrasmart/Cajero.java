package com.ugb.cuadrasmart;

public class Cajero {
    private int id;
    private String nombre;

    public Cajero(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}