package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rol;

public class Administrador extends Usuario {

    public Administrador(String id, String nombre, String username, String password) {
        super(id, nombre, username, password, Rol.ADMIN);
    }
}
