package com.mycompany.proyectofinaldesarollosoft.controller;

import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
import com.mycompany.proyectofinaldesarollosoft.model.Administrador;
import com.mycompany.proyectofinaldesarollosoft.model.Cliente;
import com.mycompany.proyectofinaldesarollosoft.model.Tienda;
import com.mycompany.proyectofinaldesarollosoft.model.Usuario;
import java.util.Objects;

public class ControladorAutenticacion {
    private final Tienda tienda;

    public ControladorAutenticacion(Tienda tienda) {
        this.tienda = Objects.requireNonNull(tienda, "tienda");
    }

    public Usuario login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("El username y la contraseña son obligatorios.");
        }
        return tienda.getUsuarios().stream()
                .filter(usuario -> usuario.autenticar(username, password))
                .findFirst()
                .orElseThrow(() -> new SecurityException("Usuario o contraseña incorrectos."));
    }

    public Administrador loginAdministrador(String username, String password) {
        Usuario usuario = login(username, password);
        if (usuario.getRol() != Rol.ADMIN || !(usuario instanceof Administrador administrador)) {
            throw new SecurityException("El usuario autenticado no tiene el rol Administrador.");
        }
        return administrador;
    }

    public Cliente loginCliente(String username, String password) {
        Usuario usuario = login(username, password);
        if (usuario.getRol() != Rol.CLIENTE || !(usuario instanceof Cliente cliente)) {
            throw new SecurityException("El usuario autenticado no tiene el rol Cliente.");
        }
        return cliente;
    }
}
