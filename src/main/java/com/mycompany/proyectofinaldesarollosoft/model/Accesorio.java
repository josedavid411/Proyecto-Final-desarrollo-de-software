package com.mycompany.proyectofinaldesarollosoft.model;

import java.math.BigDecimal;

public class Accesorio extends Producto {
    private final String tipo;
    private final String descripcion;

    public Accesorio(String id, String nombre, BigDecimal precioCOP, String tipo, String descripcion) {
        super(id, nombre, precioCOP);
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de accesorio es obligatorio.");
        }
        this.tipo = tipo.trim();
        this.descripcion = descripcion == null ? "" : descripcion.trim();
    }

    public String consultarInformacion() {
        return tipo + ": " + descripcion;
    }

    public String getTipo() {
        return tipo;
    }
}
