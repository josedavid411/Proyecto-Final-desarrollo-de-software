package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Booster extends Producto {
    public static final int CANTIDAD_CARTAS = 10;

    private final List<Carta> cartas;
    private boolean abierto;

    public Booster(String id, String nombre, BigDecimal precioBaseUSD) {
        super(id, nombre, precioBaseUSD);
        this.cartas = generarContenido();
    }

    private List<Carta> generarContenido() {
        List<Carta> contenido = new ArrayList<>(CANTIDAD_CARTAS);
        for (int i = 0; i < CANTIDAD_CARTAS; i++) {
            contenido.add(new Carta(
                    UUID.randomUUID().toString(),
                    "Carta " + (i + 1),
                    Rareza.sortear(),
                    BigDecimal.ZERO));
        }
        return List.copyOf(contenido);
    }

    public List<Carta> abrir() {
        if (abierto) {
            throw new IllegalStateException("El booster ya fue abierto.");
        }
        abierto = true;
        return cartas;
    }

    public boolean estaAbierto() {
        return abierto;
    }

    public List<Carta> getCartas() {
        if (!abierto) {
            throw new IllegalStateException("El contenido solo se muestra al abrir el booster.");
        }
        return cartas;
    }
}
