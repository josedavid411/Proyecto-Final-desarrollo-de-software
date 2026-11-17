package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.servicio.ServicioTasaCambio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Tienda {
    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Torneo> torneos = new ArrayList<>();
    private final Inventario inventario;
    private final ServicioTasaCambio servicioTasaCambio;

    public Tienda() {
        this(new ServicioTasaCambio());
    }

    public Tienda(ServicioTasaCambio servicioTasaCambio) {
        this.inventario = new Inventario();
        this.servicioTasaCambio = Objects.requireNonNull(servicioTasaCambio, "servicioTasaCambio");
    }

    public void registrarUsuario(Usuario usuario) {
        if (usuario == null || usuarios.stream().anyMatch(actual ->
                actual.getId().equals(usuario.getId())
                        || actual.getUsername().equalsIgnoreCase(usuario.getUsername()))) {
            throw new IllegalArgumentException("El usuario es nulo o ya existe por id o username.");
        }
        usuarios.add(usuario);
    }

    public void registrarProducto(Producto producto) {
        Objects.requireNonNull(producto, "producto");
        if (inventario.buscarProducto(producto.getId()).isPresent()) {
            throw new IllegalArgumentException("El producto ya está registrado en la tienda.");
        }
        BigDecimal tasa = servicioTasaCambio.obtenerTasaUSDACOP();
        producto.actualizarPrecio(producto.calcularPrecioCOP(tasa));
        inventario.agregarProducto(producto);
    }

    public boolean eliminarProducto(String productoId) {
        return inventario.eliminarProducto(productoId);
    }

    public Torneo crearTorneo(String id, String nombre, LocalDate fecha, int maxParticipantes) {
        if (torneos.stream().anyMatch(torneo -> torneo.getId().equals(id))) {
            throw new IllegalArgumentException("Ya existe un torneo con ese id.");
        }
        Torneo torneo = new Torneo(id, nombre, fecha, maxParticipantes);
        torneos.add(torneo);
        return torneo;
    }

    public Compra comprarProducto(Cliente cliente, String productoId) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        Cliente clienteRegistrado = usuarios.stream()
                .filter(Cliente.class::isInstance)
                .map(Cliente.class::cast)
                .filter(actual -> actual.getId().equals(cliente.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El cliente no está registrado en la tienda."));
        Producto producto = inventario.buscarProducto(productoId)
                .orElseThrow(() -> new IllegalArgumentException("El producto no está disponible en la tienda."));
        if (!producto.estaDisponible()) {
            throw new IllegalStateException("El producto no está disponible para la compra.");
        }
        Compra compra = Compra.crear(clienteRegistrado, List.of(producto));
        inventario.transferirProducto(productoId, clienteRegistrado.getInventario());
        clienteRegistrado.registrarCompra(compra);
        return compra;
    }

    public List<Usuario> getUsuarios() {
        return List.copyOf(usuarios);
    }

    public List<Torneo> getTorneos() {
        return List.copyOf(torneos);
    }

    public Inventario getInventario() {
        return inventario;
    }
}
