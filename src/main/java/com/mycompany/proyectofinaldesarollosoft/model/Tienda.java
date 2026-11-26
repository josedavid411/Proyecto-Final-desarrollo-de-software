package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
import com.mycompany.proyectofinaldesarollosoft.excepcion.ConflictoException;
import com.mycompany.proyectofinaldesarollosoft.excepcion.NoEncontradoException;
import com.mycompany.proyectofinaldesarollosoft.excepcion.ProhibidoException;
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
        boolean existeAdministradorPorDefecto = false;
        for (Usuario usuario : usuarios) {
            if (usuario instanceof Administrador
                    && usuario.getId().equals(ADMIN_ID_POR_DEFECTO)) {
                existeAdministradorPorDefecto = true;
                break;
            }
        }
        if (!existeAdministradorPorDefecto) {
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
        ServicioTasaCambio.ResultadoTasa resultado = servicioTasaCambio.consultarTasaUSDACOP();
        producto.actualizarPrecio(
                producto.calcularPrecioCOP(resultado.tasa()),
                resultado.fuente().name());
        inventario.agregarProducto(producto);
        guardarEstado();
    }

    public Booster crearBooster(Usuario actor, String id, String nombre, BigDecimal precioBaseUSD) {
        validarAdministrador(actor);
        List<Carta> cartasDisponibles = new ArrayList<>();
        for (Producto producto : inventario.listarProductos()) {
            if (producto instanceof Carta) {
                cartasDisponibles.add((Carta) producto);
            }
        }
        if (cartasDisponibles.isEmpty()) {
            throw new ConflictoException("No hay cartas registradas para crear el booster.");
        }
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
        for (Torneo torneo : torneos) {
            if (torneo.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe un torneo con ese id.");
            }
        }
        Torneo torneo = new Torneo(id, nombre, fecha, maxParticipantes);
        torneos.add(torneo);
        guardarEstado();
        return torneo;
    }

    public Compra comprarProducto(Cliente cliente, String productoId) {
        Cliente clienteRegistrado = buscarClienteRegistrado(cliente);
        Producto producto = inventario.buscarProducto(productoId).orElse(null);
        if (producto == null) {
            if (buscarProductoEnInventariosClientes(productoId) instanceof Booster) {
                throw new ConflictoException("El booster ya fue vendido.");
            }
            throw new NoEncontradoException("El producto no existe en la tienda.");
        }
        if (!producto.estaDisponible()) {
            throw new ConflictoException("El producto no está disponible para la compra.");
        }
        Compra compra = Compra.crear(clienteRegistrado, List.of(producto));
        Producto transferido = inventario.transferirProducto(productoId, clienteRegistrado.getInventario());
        if (transferido instanceof Booster) {
            ((Booster) transferido).asignarPropietario(clienteRegistrado.getId());
        }
        clienteRegistrado.registrarCompra(compra);
        guardarEstado();
        return compra;
    }

    public List<Carta> abrirBooster(Cliente cliente, String boosterId) {
        Cliente clienteRegistrado = buscarClienteRegistrado(cliente);
        Producto producto = clienteRegistrado.getInventario().buscarProducto(boosterId).orElse(null);
        if (producto == null) {
            producto = buscarProductoEnInventariosClientes(boosterId);
            if (producto == null) {
                producto = inventario.buscarProducto(boosterId).orElse(null);
            }
        }
        if (producto == null) {
            throw new NoEncontradoException("El booster no existe.");
        }
        if (!(producto instanceof Booster)) {
            throw new IllegalArgumentException("El producto solicitado no es un booster.");
        }
        Booster booster = (Booster) producto;
        if (!clienteRegistrado.getId().equals(booster.getPropietarioId())) {
            throw new ProhibidoException("El booster pertenece a otro cliente o no ha sido comprado.");
        }
        List<Carta> cartas = booster.abrir(clienteRegistrado.getId());
        guardarEstado();
        return cartas;
    }

    public void inscribirCliente(Cliente cliente, String torneoId) {
        Cliente clienteRegistrado = buscarClienteRegistrado(cliente);
        Torneo torneo = buscarTorneo(torneoId);
        torneo.inscribirCliente(clienteRegistrado);
        guardarEstado();
    }

    public void cancelarInscripcion(Cliente cliente, String torneoId) {
        Cliente clienteRegistrado = buscarClienteRegistrado(cliente);
        Torneo torneo = buscarTorneo(torneoId);
        torneo.cancelarInscripcion(clienteRegistrado.getId());
        guardarEstado();
    }

    public void validarAdministrador(Usuario actor) {
        boolean registrado = false;
        for (Usuario usuario : usuarios) {
            if (usuario == actor) {
                registrado = true;
                break;
            }
        }
        if (!registrado || actor.getRol() != Rol.ADMIN) {
            throw new ProhibidoException("La operación requiere un Administrador registrado.");
        }
    }

    private Cliente buscarClienteRegistrado(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        for (Usuario usuario : usuarios) {
            if (usuario instanceof Cliente && usuario == cliente) {
                return (Cliente) usuario;
            }
        }
        throw new NoEncontradoException("El cliente no está registrado en la tienda.");
    }

    private Torneo buscarTorneo(String torneoId) {
        for (Torneo torneo : torneos) {
            if (torneo.getId().equals(torneoId)) {
                return torneo;
            }
        }
        throw new NoEncontradoException("El torneo no existe.");
    }

    private Producto buscarProductoEnInventariosClientes(String productoId) {
        for (Usuario usuario : usuarios) {
            if (usuario instanceof Cliente) {
                Cliente cliente = (Cliente) usuario;
                Producto producto = cliente.getInventario().buscarProducto(productoId).orElse(null);
                if (producto != null) {
                    return producto;
                }
            }
        }
        return null;
    }

    private void registrarUsuarioInterno(Usuario usuario) {
        for (Usuario actual : usuarios) {
            if (actual.getId().equals(usuario.getId())
                    || actual.getUsername().equalsIgnoreCase(usuario.getUsername())) {
                throw new ConflictoException("Ya existe un usuario con ese id o username.");
            }
        }
        usuarios.add(usuario);
    }

    private void restaurar(PersistenciaJson.EstadoTienda estado) {
        Map<String, Usuario> usuariosPorId = new HashMap<>();
        for (PersistenciaJson.UsuarioJson datos : estado.usuarios) {
            if (datos == null || datos.rol == null) {
                throw new IllegalArgumentException("Hay un usuario inválido en el archivo JSON.");
            }
            Usuario usuario;
            if (Rol.valueOf(datos.rol) == Rol.ADMIN) {
                usuario = Administrador.restaurar(datos.id, datos.nombre, datos.username, datos.credencial);
            } else {
                usuario = Cliente.restaurar(datos.id, datos.nombre, datos.username, datos.credencial);
            }
            registrarUsuarioInterno(usuario);
            usuariosPorId.put(usuario.getId(), usuario);
        }

        for (PersistenciaJson.ProductoJson datos : estado.productosTienda) {
            inventario.agregarProducto(PersistenciaJson.restaurarProducto(datos));
        }

        Map<String, Map<String, Producto>> productosClientes = new HashMap<>();
        for (PersistenciaJson.InventarioClienteJson datos : estado.inventariosClientes) {
            Usuario usuario = usuariosPorId.get(datos.clienteId);
            if (!(usuario instanceof Cliente)) {
                throw new IllegalArgumentException("El inventario JSON hace referencia a un cliente inexistente.");
            }
            Cliente cliente = (Cliente) usuario;
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
            if (!(usuario instanceof Cliente)) {
                throw new IllegalArgumentException("La compra JSON hace referencia a un cliente inexistente.");
            }
            Cliente cliente = (Cliente) usuario;
            Map<String, Producto> productosPorId = productosClientes.getOrDefault(cliente.getId(), Map.of());
            List<Producto> productos = new ArrayList<>();
            for (String productoId : datos.productoIds) {
                Producto producto = productosPorId.get(productoId);
                if (producto == null) {
                    throw new IllegalArgumentException("La compra JSON hace referencia a un producto inexistente.");
                }
                productos.add(producto);
            }
            cliente.registrarCompra(new Compra(datos.id, LocalDate.parse(datos.fecha), cliente, productos));
        }

        for (PersistenciaJson.TorneoJson datos : estado.torneos) {
            Torneo torneo = new Torneo(
                    datos.id, datos.nombre, LocalDate.parse(datos.fecha), datos.maxParticipantes);
            for (String participanteId : datos.participantesIds) {
                Usuario participante = usuariosPorId.get(participanteId);
                if (!(participante instanceof Cliente)) {
                    throw new IllegalArgumentException("El torneo JSON hace referencia a un cliente inexistente.");
                }
                torneo.restaurarParticipante((Cliente) participante);
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
        for (Usuario usuario : usuarios) {
            if (usuario instanceof Administrador
                    && usuario.getId().equals(ADMIN_ID_POR_DEFECTO)) {
                return (Administrador) usuario;
            }
        }
        throw new IllegalStateException("No existe el administrador por defecto.");
    }

    public List<Usuario> getUsuarios() {
        return List.copyOf(usuarios);
    }

    public List<Torneo> getTorneos() {
        return List.copyOf(torneos);
    }

    public List<Compra> getCompras() {
        List<Compra> compras = new ArrayList<>();
        for (Usuario usuario : usuarios) {
            if (usuario instanceof Cliente) {
                Cliente cliente = (Cliente) usuario;
                compras.addAll(cliente.getHistorialCompras());
            }
        }
        return compras;
    }

    public Inventario getInventario() {
        return inventario;
    }

    public ServicioTasaCambio.ResultadoTasa consultarTasaCambio() {
        return servicioTasaCambio.consultarTasaUSDACOP();
    }

    public PersistenciaJson getPersistencia() {
        return persistencia;
    }
}
