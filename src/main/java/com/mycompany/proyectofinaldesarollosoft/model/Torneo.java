package com.mycompany.proyectofinaldesarollosoft.model;

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

    public boolean inscribirCliente(Cliente cliente) {
        if (cliente == null || !hayCupos()
                || cliente.estaInscritoEn(id)
                || participantes.stream().anyMatch(actual -> actual.getId().equals(cliente.getId()))) {
            return false;
        }
        cliente.registrarInscripcion(id);
        participantes.add(cliente);
        return true;
    }

    public boolean cancelarInscripcion(String clienteId) {
        for (int i = 0; i < participantes.size(); i++) {
            Cliente cliente = participantes.get(i);
            if (cliente.getId().equals(clienteId)) {
                participantes.remove(i);
                cliente.cancelarInscripcion(id);
                return true;
            }
        }
        return false;
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
