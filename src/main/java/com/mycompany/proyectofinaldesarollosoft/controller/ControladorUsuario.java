package com.mycompany.proyectofinaldesarollosoft.controller;

import com.mycompany.proyectofinaldesarollosoft.model.Cliente;
import com.mycompany.proyectofinaldesarollosoft.model.Compra;
import com.mycompany.proyectofinaldesarollosoft.model.Tienda;
import com.mycompany.proyectofinaldesarollosoft.model.Usuario;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ControladorUsuario {
    private final Tienda tienda;

    public ControladorUsuario(Tienda tienda) {
        this.tienda = Objects.requireNonNull(tienda, "tienda");
    }

    public Cliente registrarCliente(
            Usuario actor,
            String id,
            String nombre,
            String username,
            String password) {
        tienda.validarAdministrador(actor);
        Cliente cliente = new Cliente(id, nombre, username, password);
        tienda.registrarCliente(actor, cliente);
        return cliente;
    }

    public Cliente registrarCliente(Usuario actor, String nombre, String username, String password) {
        return registrarCliente(actor, UUID.randomUUID().toString(), nombre, username, password);
    }

    public List<Compra> consultarHistorialCompras(Usuario actor, String clienteId) {
        Objects.requireNonNull(actor, "actor");
        Cliente cliente = tienda.getUsuarios().stream()
                .filter(Cliente.class::isInstance)
                .map(Cliente.class::cast)
                .filter(actual -> actual.getId().equals(clienteId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
        if (actor != cliente) {
            tienda.validarAdministrador(actor);
        }
        return cliente.getHistorialCompras();
    }
}
