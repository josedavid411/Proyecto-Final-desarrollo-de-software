package com.mycompany.proyectofinaldesarollosoft.controller;

import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
import com.mycompany.proyectofinaldesarollosoft.excepcion.NoAutorizadoException;
import com.mycompany.proyectofinaldesarollosoft.excepcion.ProhibidoException;
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
                .orElseThrow(() -> new NoAutorizadoException("Usuario o contraseña incorrectos."));
    }

    public Administrador loginAdministrador(String username, String password) {
        Usuario usuario = login(username, password);
        if (usuario.getRol() != Rol.ADMIN || !(usuario instanceof Administrador)) {
            throw new ProhibidoException("El usuario autenticado no tiene el rol Administrador.");
        }
        return (Administrador) usuario;
    }

    public Cliente loginCliente(String username, String password) {
        Usuario usuario = login(username, password);
        if (usuario.getRol() != Rol.CLIENTE || !(usuario instanceof Cliente)) {
            throw new ProhibidoException("El usuario autenticado no tiene el rol Cliente.");
        }
        return (Cliente) usuario;
    }
}
