package com.mycompany.proyectofinaldesarollosoft.model;

import java.math.BigDecimal;

public abstract class Producto {
    private final String id;
    private final String nombre;
    private BigDecimal precioCOP;
    private boolean disponible = true;

    protected Producto(String id, String nombre, BigDecimal precioCOP) {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El id y el nombre son obligatorios.");
        }
        this.id = id.trim();
        this.nombre = nombre.trim();
        actualizarPrecio(precioCOP);
    }

    public void actualizarPrecio(BigDecimal precioCOP) {
        if (precioCOP == null || precioCOP.signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser mayor o igual a cero.");
        }
        this.precioCOP = precioCOP;
    }

    public void marcarNoDisponible() {
        disponible = false;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecioCOP() {
        return precioCOP;
    }

    public boolean estaDisponible() {
        return disponible;
    }
}
