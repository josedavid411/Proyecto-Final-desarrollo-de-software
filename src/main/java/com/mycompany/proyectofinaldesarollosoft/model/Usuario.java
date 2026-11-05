package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
import java.util.Objects;

public abstract class Usuario {
    private final String id;
    private final String nombre;
    private final String username;
    private final transient String password;
    private final Rol rol;

    protected Usuario(String id, String nombre, String username, String password, Rol rol) {
        this.id = textoRequerido(id, "id");
        this.nombre = textoRequerido(nombre, "nombre");
        this.username = textoRequerido(username, "username");
        this.password = textoRequerido(password, "password");
        this.rol = Objects.requireNonNull(rol, "rol");
    }

    private static String textoRequerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }
        return valor.trim();
    }

    public boolean autenticar(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsername() {
        return username;
    }

    public Rol getRol() {
        return rol;
    }
}
