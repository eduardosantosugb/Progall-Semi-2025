package com.ugb.cuadrasmart;

public class Tienda {
    private String nombre;
    private int iconResId;

    public Tienda(String nombre, int iconResId) {
        this.nombre = nombre;
        this.iconResId = iconResId;
    }

    public String getNombre() {
        return nombre;
    }

    public int getIconResId() {
        return iconResId;
    }
}

