package com.mycompany.proyectofinaldesarollosoft.controller;

import com.mycompany.proyectofinaldesarollosoft.model.Cliente;
import com.mycompany.proyectofinaldesarollosoft.model.Tienda;
import com.mycompany.proyectofinaldesarollosoft.model.Torneo;
import com.mycompany.proyectofinaldesarollosoft.model.Usuario;

import java.time.LocalDate;
import java.util.Objects;

public class ControladorTorneo {
    private final Tienda tienda;

    public ControladorTorneo(Tienda tienda) {
        this.tienda = Objects.requireNonNull(tienda, "tienda");
    }

    public Torneo crearTorneo(
            Usuario actor, String id, String nombre, LocalDate fecha, int maxParticipantes) {
        tienda.validarAdministrador(actor);
        return tienda.crearTorneo(actor, id, nombre, fecha, maxParticipantes);
    }

    public void inscribirCliente(Cliente cliente, String torneoId) {
        tienda.inscribirCliente(cliente, torneoId);
    }

    public void cancelarInscripcion(Cliente cliente, String torneoId) {
        tienda.cancelarInscripcion(cliente, torneoId);
    }
}
