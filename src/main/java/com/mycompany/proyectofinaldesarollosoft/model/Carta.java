package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Carta extends Producto {
    private final Rareza rareza;
    private final BigDecimal precioBaseUSD;

    public Carta(String id, String nombre, Rareza rareza, BigDecimal precioBaseUSD) {
        super(id, nombre, BigDecimal.ZERO);
        this.rareza = Objects.requireNonNull(rareza, "rareza");
        if (precioBaseUSD == null || precioBaseUSD.signum() < 0) {
            throw new IllegalArgumentException("El precio base en USD debe ser no negativo.");
        }
        this.precioBaseUSD = precioBaseUSD;
    }

    public BigDecimal calcularPrecioCOP(BigDecimal tasaUSD) {
        if (tasaUSD == null || tasaUSD.signum() <= 0) {
            throw new IllegalArgumentException("La tasa de cambio debe ser positiva.");
        }
        return precioBaseUSD.multiply(tasaUSD).setScale(2, RoundingMode.HALF_UP);
    }

    public Rareza getRareza() {
        return rareza;
    }

    public BigDecimal getPrecioBaseUSD() {
        return precioBaseUSD;
    }
}
