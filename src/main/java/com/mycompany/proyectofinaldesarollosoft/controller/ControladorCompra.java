package com.mycompany.proyectofinaldesarollosoft.controller;

import com.mycompany.proyectofinaldesarollosoft.excepcion.ProhibidoException;
import com.mycompany.proyectofinaldesarollosoft.model.Cliente;
import com.mycompany.proyectofinaldesarollosoft.model.Compra;
import com.mycompany.proyectofinaldesarollosoft.model.Tienda;
import com.mycompany.proyectofinaldesarollosoft.model.Usuario;

import java.util.Objects;

public class ControladorCompra {
    private final Tienda tienda;

    public ControladorCompra(Tienda tienda) {
        this.tienda = Objects.requireNonNull(tienda, "tienda");
    }

    public Compra procesarCompra(Usuario actor, String productoId) {
        if (!(actor instanceof Cliente)) {
            throw new ProhibidoException("Solo un Cliente autenticado puede realizar compras.");
        }
        return tienda.comprarProducto((Cliente) actor, productoId);
    }
}
