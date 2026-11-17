package com.mycompany.proyectofinaldesarollosoft.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Producto {
    private final String id;
    private final String nombre;
    private final BigDecimal precioBaseUSD;
    private BigDecimal precioCOP;
    private boolean disponible = true;

    protected Producto(String id, String nombre, BigDecimal precioBaseUSD) {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El id y el nombre son obligatorios.");
        }
        if (precioBaseUSD == null || precioBaseUSD.signum() < 0) {
            throw new IllegalArgumentException("El precio base en USD debe ser no negativo.");
        }
        this.id = id.trim();
        this.nombre = nombre.trim();
        this.precioBaseUSD = precioBaseUSD;
        this.precioCOP = BigDecimal.ZERO.setScale(2);
    }

    public void actualizarPrecio(BigDecimal precioCOP) {
        if (precioCOP == null || precioCOP.signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser mayor o igual a cero.");
        }
        this.precioCOP = precioCOP;
    }

    public BigDecimal calcularPrecioCOP(BigDecimal tasa) {
        if (tasa == null || tasa.signum() <= 0) {
            throw new IllegalArgumentException("La tasa de cambio debe ser positiva.");
        }
        return precioBaseUSD.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
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

    public BigDecimal getPrecioBaseUSD() {
        return precioBaseUSD;
    }

    public BigDecimal getPrecioCOP() {
        return precioCOP;
    }

    public boolean estaDisponible() {
        return disponible;
    }
}
