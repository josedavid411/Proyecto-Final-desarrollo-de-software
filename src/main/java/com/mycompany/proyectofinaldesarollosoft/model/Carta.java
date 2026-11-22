package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import java.math.BigDecimal;
import java.util.Objects;

public class Carta extends Producto {
    private final Rareza rareza;

    public Carta(String id, String nombre, Rareza rareza, BigDecimal precioBaseUSD) {
        super(id, nombre, precioBaseUSD);
        this.rareza = Objects.requireNonNull(rareza, "rareza");
    }

    public Rareza getRareza() {
        return rareza;
    }
}
