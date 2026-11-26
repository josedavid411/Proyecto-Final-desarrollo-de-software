package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.excepcion.ConflictoException;
import com.mycompany.proyectofinaldesarollosoft.excepcion.NoEncontradoException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Torneo {
    private final String id;
    private final String nombre;
    private final LocalDate fecha;
    private final int maxParticipantes;
    private final List<Cliente> participantes = new ArrayList<>();

    public Torneo(String id, String nombre, LocalDate fecha, int maxParticipantes) {
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El id y el nombre son obligatorios.");
        }
        if (fecha == null || maxParticipantes <= 0) {
            throw new IllegalArgumentException("La fecha y un cupo positivo son obligatorios.");
        }
        this.id = id.trim();
        this.nombre = nombre.trim();
        this.fecha = fecha;
        this.maxParticipantes = maxParticipantes;
    }

    public void inscribirCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        if (cliente.estaInscritoEn(id)
                || participantes.stream().anyMatch(actual -> actual.getId().equals(cliente.getId()))) {
            throw new ConflictoException("El cliente ya está inscrito en este torneo.");
        }
        if (participantes.size() >= maxParticipantes) {
            throw new ConflictoException("El torneo no tiene cupos disponibles.");
        }
        if (LocalDate.now().isAfter(this.fecha)) {
            throw new ConflictoException("No se puede inscribir en un torneo cuya fecha ya pasó.");
        }
        cliente.registrarInscripcion(id);
        participantes.add(cliente);
    }

    public void cancelarInscripcion(String clienteId) {
        for (int i = 0; i < participantes.size(); i++) {
            Cliente cliente = participantes.get(i);
            if (cliente.getId().equals(clienteId)) {
                participantes.remove(i);
                cliente.cancelarInscripcion(id);
                return;
            }
        }
        throw new NoEncontradoException("El cliente no está inscrito en este torneo.");
    }

    public void restaurarParticipante(Cliente cliente) {
        if (cliente == null || participantes.stream().anyMatch(actual -> actual.getId().equals(cliente.getId()))) {
            return;
        }
        participantes.add(cliente);
        cliente.restaurarTorneoInscrito(id);
    }

    public boolean hayCupos() {
        return participantes.size() < maxParticipantes;
    }

    public List<Cliente> consultarParticipantes() {
        return List.copyOf(participantes);
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getMaxParticipantes() {
        return maxParticipantes;
    }
}
