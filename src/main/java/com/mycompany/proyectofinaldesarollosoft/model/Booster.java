package com.mycompany.proyectofinaldesarollosoft.model;

import java.math.BigDecimal;
import java.util.List;

public class Booster extends Producto {
    public static final int CANTIDAD_CARTAS = 10;

    private final List<Carta> cartas;
    private boolean abierto;
    private String idComprador;

    public Booster(String id, String nombre, BigDecimal precioCOP, List<Carta> cartas) {
        super(id, nombre, precioCOP);
        if (cartas == null || cartas.size() != CANTIDAD_CARTAS || cartas.contains(null)) {
            throw new IllegalArgumentException("Un booster debe contener exactamente diez cartas.");
        }
        this.cartas = List.copyOf(cartas);
    }

    public void venderA(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        if (!estaDisponible()) {
            throw new IllegalStateException("El booster ya fue vendido.");
        }
        idComprador = clienteId;
        marcarNoDisponible();
    }

    public List<Carta> abrir(String clienteId) {
        if (estaDisponible() || !idComprador.equals(clienteId)) {
            throw new IllegalStateException("Solo el comprador puede abrir este booster.");
        }
        if (abierto) {
            throw new IllegalStateException("El booster ya fue abierto.");
        }
        abierto = true;
        return cartas;
    }

    public boolean estaAbierto() {
        return abierto;
    }

    public boolean estaVendido() {
        return !estaDisponible();
    }

    public String getIdComprador() {
        return idComprador;
    }

    public List<Carta> getCartas() {
        if (!abierto) {
            throw new IllegalStateException("El contenido solo se muestra al abrir el booster.");
        }
        return cartas;
    }
}
