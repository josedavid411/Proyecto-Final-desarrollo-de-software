package com.mycompany.proyectofinaldesarollosoft.model;

import java.math.BigDecimal;

public class ProductoSellado extends Producto {
    private final String descripcion;

    public ProductoSellado(String id, String nombre, BigDecimal precioCOP, String descripcion) {
        super(id, nombre, precioCOP);
        this.descripcion = descripcion == null ? "" : descripcion.trim();
    }

    public String consultarInformacion() {
        return descripcion;
    }
}
