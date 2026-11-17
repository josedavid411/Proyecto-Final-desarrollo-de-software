package com.mycompany.proyectofinaldesarollosoft.controller;

import com.mycompany.proyectofinaldesarollosoft.model.Booster;
import com.mycompany.proyectofinaldesarollosoft.model.Carta;
import com.mycompany.proyectofinaldesarollosoft.model.Cliente;
import com.mycompany.proyectofinaldesarollosoft.model.Tienda;
import com.mycompany.proyectofinaldesarollosoft.model.Usuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ControladorBooster {
    private final Tienda tienda;

    public ControladorBooster(Tienda tienda) {
        this.tienda = Objects.requireNonNull(tienda, "tienda");
    }

    public Booster crearBooster(Usuario actor, String id, String nombre, BigDecimal precioBaseUSD) {
        tienda.validarAdministrador(actor);
        return tienda.crearBooster(actor, id, nombre, precioBaseUSD);
    }

    public List<Carta> abrirBooster(Cliente cliente, String boosterId) {
        return tienda.abrirBooster(cliente, boosterId);
    }
}
