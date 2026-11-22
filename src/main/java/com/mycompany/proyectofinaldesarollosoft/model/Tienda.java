package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
import com.mycompany.proyectofinaldesarollosoft.servicio.PersistenciaJson;
import com.mycompany.proyectofinaldesarollosoft.servicio.ServicioTasaCambio;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Tienda {
    public static final String ADMIN_ID_POR_DEFECTO = "admin-default";
    public static final String ADMIN_USERNAME_POR_DEFECTO = "admin";
    public static final String ADMIN_PASSWORD_POR_DEFECTO = "admin123";

    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Torneo> torneos = new ArrayList<>();
    private final Inventario inventario = new Inventario();
    private final ServicioTasaCambio servicioTasaCambio;
    private final PersistenciaJson persistencia;

    public Tienda() {
        this(new ServicioTasaCambio(), new PersistenciaJson(Path.of("data", "tienda.json")));
    }

    public Tienda(ServicioTasaCambio servicioTasaCambio) {
        this(servicioTasaCambio, new PersistenciaJson(Path.of("data", "tienda.json")));
    }

    public Tienda(ServicioTasaCambio servicioTasaCambio, PersistenciaJson persistencia) {
        this.servicioTasaCambio = Objects.requireNonNull(servicioTasaCambio, "servicioTasaCambio");
        this.persistencia = Objects.requireNonNull(persistencia, "persistencia");
        try {
            restaurar(persistencia.cargar());
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("No se pudo cargar el estado de la tienda.", exception);
        }
        if (usuarios.stream().noneMatch(usuario -> usuario instanceof Administrador
                && usuario.getId().equals(ADMIN_ID_POR_DEFECTO))) {
            registrarUsuarioInterno(new Administrador(
                    ADMIN_ID_POR_DEFECTO,
                    "Administrador",
                    ADMIN_USERNAME_POR_DEFECTO,
                    ADMIN_PASSWORD_POR_DEFECTO));
            guardarEstado();
        }
    }

    public void registrarCliente(Usuario actor, Cliente cliente) {
        validarAdministrador(actor);
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        registrarUsuarioInterno(cliente);
        guardarEstado();
    }

    public void registrarProducto(Usuario actor, Producto producto) {
        validarAdministrador(actor);
        Objects.requireNonNull(producto, "producto");
        if (inventario.buscarProducto(producto.getId()).isPresent()) {
            throw new IllegalArgumentException("El producto ya está registrado en la tienda.");
        }
        BigDecimal tasa = servicioTasaCambio.obtenerTasaUSDACOP();
        producto.actualizarPrecio(producto.calcularPrecioCOP(tasa));
        inventario.agregarProducto(producto);
        guardarEstado();
    }

    public Booster crearBooster(Usuario actor, String id, String nombre, BigDecimal precioBaseUSD) {
        validarAdministrador(actor);
        List<Carta> cartasDisponibles = inventario.listarProductos().stream()
                .filter(Carta.class::isInstance)
                .map(Carta.class::cast)
                .toList();
        Booster booster = new Booster(id, nombre, precioBaseUSD, cartasDisponibles);
        registrarProducto(actor, booster);
        return booster;
    }

    public boolean eliminarProducto(Usuario actor, String productoId) {
        validarAdministrador(actor);
        boolean eliminado = inventario.eliminarProducto(productoId);
        if (eliminado) {
            guardarEstado();
        }
        return eliminado;
    }

    public Torneo crearTorneo(Usuario actor, String id, String nombre, LocalDate fecha, int maxParticipantes) {
        validarAdministrador(actor);
        if (torneos.stream().anyMatch(torneo -> torneo.getId().equals(id))) {
            throw new IllegalArgumentException("Ya existe un torneo con ese id.");
        }
        Torneo torneo = new Torneo(id, nombre, fecha, maxParticipantes);
        torneos.add(torneo);
        guardarEstado();
        return torneo;
    }

    public Compra comprarProducto(Cliente cliente, String productoId) {
        Cliente clienteRegistrado = buscarClienteRegistrado(cliente);
        Producto producto = inventario.buscarProducto(productoId)
                .orElseThrow(() -> new IllegalArgumentException("El producto no está disponible en la tienda."));
        if (!producto.estaDisponible()) {
            throw new IllegalStateException("El producto no está disponible para la compra.");
        }
        Compra compra = Compra.crear(clienteRegistrado, List.of(producto));
        Producto transferido = inventario.transferirProducto(productoId, clienteRegistrado.getInventario());
        if (transferido instanceof Booster booster) {
            booster.asignarPropietario(clienteRegistrado.getId());
        }
        clienteRegistrado.registrarCompra(compra);
        guardarEstado();
        return compra;
    }

    public List<Carta> abrirBooster(Cliente cliente, String boosterId) {
        Cliente clienteRegistrado = buscarClienteRegistrado(cliente);
        Producto producto = clienteRegistrado.getInventario().buscarProducto(boosterId)
                .orElseThrow(() -> new IllegalArgumentException("El booster no pertenece al inventario del cliente."));
        if (!(producto instanceof Booster booster)) {
            throw new IllegalArgumentException("El producto solicitado no es un booster.");
        }
        List<Carta> cartas = booster.abrir(clienteRegistrado.getId());
        guardarEstado();
        return cartas;
    }

    public boolean inscribirCliente(Cliente cliente, String torneoId) {
        Cliente clienteRegistrado = buscarClienteRegistrado(cliente);
        Torneo torneo = buscarTorneo(torneoId);
        boolean inscrito = torneo.inscribirCliente(clienteRegistrado);
        if (inscrito) {
            guardarEstado();
        }
        return inscrito;
    }

    public boolean cancelarInscripcion(Cliente cliente, String torneoId) {
        Cliente clienteRegistrado = buscarClienteRegistrado(cliente);
        Torneo torneo = buscarTorneo(torneoId);
        boolean cancelada = torneo.cancelarInscripcion(clienteRegistrado.getId());
        if (cancelada) {
            guardarEstado();
        }
        return cancelada;
    }

    public void validarAdministrador(Usuario actor) {
        boolean registrado = usuarios.stream().anyMatch(usuario -> usuario == actor);
        if (!registrado || actor.getRol() != Rol.ADMIN) {
            throw new SecurityException("La operación requiere un Administrador registrado.");
        }
    }

    private Cliente buscarClienteRegistrado(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        return usuarios.stream()
                .filter(Cliente.class::isInstance)
                .map(Cliente.class::cast)
                .filter(actual -> actual == cliente)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El cliente no está registrado en la tienda."));
    }

    private Torneo buscarTorneo(String torneoId) {
        return torneos.stream()
                .filter(actual -> actual.getId().equals(torneoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe."));
    }

    private void registrarUsuarioInterno(Usuario usuario) {
        if (usuarios.stream().anyMatch(actual ->
                actual.getId().equals(usuario.getId())
                        || actual.getUsername().equalsIgnoreCase(usuario.getUsername()))) {
            throw new IllegalArgumentException("Ya existe un usuario con ese id o username.");
        }
        usuarios.add(usuario);
    }

    private void restaurar(PersistenciaJson.EstadoTienda estado) {
        Map<String, Usuario> usuariosPorId = new HashMap<>();
        for (PersistenciaJson.UsuarioJson datos : estado.usuarios) {
            if (datos == null || datos.rol == null) {
                throw new IllegalArgumentException("Hay un usuario inválido en el archivo JSON.");
            }
            Usuario usuario = switch (Rol.valueOf(datos.rol)) {
                case ADMIN -> Administrador.restaurar(datos.id, datos.nombre, datos.username, datos.credencial);
                case CLIENTE -> Cliente.restaurar(datos.id, datos.nombre, datos.username, datos.credencial);
            };
            registrarUsuarioInterno(usuario);
            usuariosPorId.put(usuario.getId(), usuario);
        }

        for (PersistenciaJson.ProductoJson datos : estado.productosTienda) {
            inventario.agregarProducto(PersistenciaJson.restaurarProducto(datos));
        }

        Map<String, Map<String, Producto>> productosClientes = new HashMap<>();
        for (PersistenciaJson.InventarioClienteJson datos : estado.inventariosClientes) {
            Usuario usuario = usuariosPorId.get(datos.clienteId);
            if (!(usuario instanceof Cliente cliente)) {
                throw new IllegalArgumentException("El inventario JSON hace referencia a un cliente inexistente.");
            }
            Map<String, Producto> productosPorId = new HashMap<>();
            for (PersistenciaJson.ProductoJson productoJson : datos.productos) {
                Producto producto = PersistenciaJson.restaurarProducto(productoJson);
                cliente.getInventario().agregarProducto(producto);
                productosPorId.put(producto.getId(), producto);
            }
            productosClientes.put(cliente.getId(), productosPorId);
        }

        for (PersistenciaJson.CompraJson datos : estado.compras) {
            Usuario usuario = usuariosPorId.get(datos.clienteId);
            if (!(usuario instanceof Cliente cliente)) {
                throw new IllegalArgumentException("La compra JSON hace referencia a un cliente inexistente.");
            }
            Map<String, Producto> productosPorId = productosClientes.getOrDefault(cliente.getId(), Map.of());
            List<Producto> productos = datos.productoIds.stream()
                    .map(id -> {
                        Producto producto = productosPorId.get(id);
                        if (producto == null) {
                            throw new IllegalArgumentException("La compra JSON hace referencia a un producto inexistente.");
                        }
                        return producto;
                    })
                    .toList();
            cliente.registrarCompra(new Compra(datos.id, LocalDate.parse(datos.fecha), cliente, productos));
        }

        for (PersistenciaJson.TorneoJson datos : estado.torneos) {
            Torneo torneo = new Torneo(
                    datos.id, datos.nombre, LocalDate.parse(datos.fecha), datos.maxParticipantes);
            for (String participanteId : datos.participantesIds) {
                Usuario participante = usuariosPorId.get(participanteId);
                if (!(participante instanceof Cliente cliente)) {
                    throw new IllegalArgumentException("El torneo JSON hace referencia a un cliente inexistente.");
                }
                torneo.restaurarParticipante(cliente);
            }
            torneos.add(torneo);
        }
    }

    private void guardarEstado() {
        try {
            persistencia.guardar(this);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar el estado JSON de la tienda.", exception);
        }
    }

    public Administrador getAdministradorPorDefecto() {
        return usuarios.stream()
                .filter(usuario -> usuario.getId().equals(ADMIN_ID_POR_DEFECTO))
                .filter(Administrador.class::isInstance)
                .map(Administrador.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el administrador por defecto."));
    }

    public List<Usuario> getUsuarios() {
        return List.copyOf(usuarios);
    }

    public List<Torneo> getTorneos() {
        return List.copyOf(torneos);
    }

    public List<Compra> getCompras() {
        return usuarios.stream()
                .filter(Cliente.class::isInstance)
                .map(Cliente.class::cast)
                .flatMap(cliente -> cliente.getHistorialCompras().stream())
                .toList();
    }

    public Inventario getInventario() {
        return inventario;
    }

    public PersistenciaJson getPersistencia() {
        return persistencia;
    }
}
