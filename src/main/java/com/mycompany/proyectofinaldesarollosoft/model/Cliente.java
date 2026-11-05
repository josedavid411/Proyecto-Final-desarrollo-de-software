package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
import java.util.ArrayList;
import java.util.List;

public class Cliente extends Usuario {
    private final List<Compra> historialCompras = new ArrayList<>();
    private final List<String> torneosInscritos = new ArrayList<>();

    public Cliente(String id, String nombre, String username, String password) {
        super(id, nombre, username, password, Rol.CLIENTE);
    }

    public void registrarCompra(Compra compra) {
        if (compra == null || !getId().equals(compra.getCliente().getId())) {
            throw new IllegalArgumentException("La compra debe pertenecer a este cliente.");
        }
        historialCompras.add(compra);
    }

    public void registrarInscripcion(String torneoId) {
        if (torneoId == null || torneoId.isBlank() || estaInscritoEn(torneoId)) {
            throw new IllegalArgumentException("El torneo es inválido o el cliente ya está inscrito.");
        }
        torneosInscritos.add(torneoId);
    }

    public boolean estaInscritoEn(String torneoId) {
        return torneosInscritos.contains(torneoId);
    }

    public void cancelarInscripcion(String torneoId) {
        torneosInscritos.remove(torneoId);
    }

    public List<Compra> getHistorialCompras() {
        return List.copyOf(historialCompras);
    }

    public List<String> getTorneosInscritos() {
        return List.copyOf(torneosInscritos);
    }
}
