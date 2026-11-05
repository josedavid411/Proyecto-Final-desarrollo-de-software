package com.mycompany.proyectofinaldesarollosoft.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Compra {
    private final String id;
    private final LocalDate fecha;
    private final Cliente cliente;
    private final List<Producto> productos = new ArrayList<>();

    public Compra(String id, LocalDate fecha, Cliente cliente, List<Producto> productos) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de compra es obligatorio.");
        }
        this.id = id.trim();
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        if (productos == null || productos.isEmpty() || productos.contains(null)) {
            throw new IllegalArgumentException("La compra debe incluir productos.");
        }
        this.productos.addAll(productos);
    }

    public static Compra crear(Cliente cliente, List<Producto> productos) {
        return new Compra(UUID.randomUUID().toString(), LocalDate.now(), cliente, productos);
    }

    public BigDecimal calcularTotal() {
        return productos.stream()
                .map(Producto::getPrecioCOP)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public List<Producto> getProductos() {
        return List.copyOf(productos);
    }
}
