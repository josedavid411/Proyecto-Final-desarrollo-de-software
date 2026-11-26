package com.mycompany.proyectofinaldesarollosoft.servicio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import com.mycompany.proyectofinaldesarollosoft.model.Accesorio;
import com.mycompany.proyectofinaldesarollosoft.model.Booster;
import com.mycompany.proyectofinaldesarollosoft.model.Carta;
import com.mycompany.proyectofinaldesarollosoft.model.Cliente;
import com.mycompany.proyectofinaldesarollosoft.model.Compra;
import com.mycompany.proyectofinaldesarollosoft.model.Producto;
import com.mycompany.proyectofinaldesarollosoft.model.ProductoSellado;
import com.mycompany.proyectofinaldesarollosoft.model.Tienda;
import com.mycompany.proyectofinaldesarollosoft.model.Torneo;
import com.mycompany.proyectofinaldesarollosoft.model.Usuario;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PersistenciaJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path archivo;

    public PersistenciaJson(Path archivo) {
        if (archivo == null) {
            throw new IllegalArgumentException("La ruta del archivo JSON es obligatoria.");
        }
        this.archivo = archivo.toAbsolutePath().normalize();
    }

    public Path getArchivo() {
        return archivo;
    }

    public EstadoTienda cargar() throws IOException {
        if (!Files.exists(archivo)) {
            return new EstadoTienda();
        }
        try {
            EstadoTienda estado = GSON.fromJson(Files.readString(archivo, StandardCharsets.UTF_8), EstadoTienda.class);
            if (estado == null) {
                throw new IOException("El archivo de persistencia JSON está vacío.");
            }
            estado.validarColecciones();
            return estado;
        } catch (JsonParseException exception) {
            throw new IOException("No se pudo interpretar el archivo JSON: " + archivo, exception);
        }
    }

    public void guardar(Tienda tienda) throws IOException {
        EstadoTienda estado = new EstadoTienda();
        for (Usuario usuario : tienda.getUsuarios()) {
            estado.usuarios.add(new UsuarioJson(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getUsername(),
                    usuario.getCredencialPersistida(),
                    usuario.getRol().name()));
            if (usuario instanceof Cliente cliente) {
                InventarioClienteJson inventarioCliente = new InventarioClienteJson(cliente.getId());
                cliente.getInventario().listarProductos().stream()
                        .map(PersistenciaJson::serializarProducto)
                        .forEach(inventarioCliente.productos::add);
                estado.inventariosClientes.add(inventarioCliente);
            }
        }
        tienda.getInventario().listarProductos().stream()
                .map(PersistenciaJson::serializarProducto)
                .forEach(estado.productosTienda::add);

        for (Compra compra : tienda.getCompras()) {
            estado.compras.add(new CompraJson(
                    compra.getId(),
                    compra.getFecha().toString(),
                    compra.getCliente().getId(),
                    compra.getProductos().stream().map(Producto::getId).toList()));
        }
        for (Torneo torneo : tienda.getTorneos()) {
            estado.torneos.add(new TorneoJson(
                    torneo.getId(),
                    torneo.getNombre(),
                    torneo.getFecha().toString(),
                    torneo.getMaxParticipantes(),
                    torneo.consultarParticipantes().stream().map(Cliente::getId).toList()));
        }

        Path directorio = archivo.getParent();
        if (directorio != null) {
            Files.createDirectories(directorio);
        }
        Path temporal = archivo.resolveSibling(archivo.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.writeString(temporal, GSON.toJson(estado), StandardCharsets.UTF_8);
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public static Producto restaurarProducto(ProductoJson datos) {
        if (datos == null || datos.id == null || datos.nombre == null
                || datos.tipo == null || datos.precioBaseUSD == null || datos.precioCOP == null) {
            throw new IllegalArgumentException("El producto guardado contiene datos incompletos.");
        }
        Producto producto = switch (datos.tipo) {
            case "CARTA" -> new Carta(datos.id, datos.nombre, Rareza.valueOf(datos.rareza), datos.precioBaseUSD);
            case "SELLADO" -> new ProductoSellado(
                    datos.id, datos.nombre, datos.precioBaseUSD, datos.descripcion);
            case "ACCESORIO" -> new Accesorio(
                    datos.id, datos.nombre, datos.precioBaseUSD, datos.tipoAccesorio, datos.descripcion);
            case "BOOSTER" -> Booster.restaurar(
                    datos.id,
                    datos.nombre,
                    datos.precioBaseUSD,
                    restaurarCartas(datos.cartas),
                    datos.abierto,
                    datos.propietarioId,
                    datos.precioCOP,
                    datos.disponible);
            default -> throw new IllegalArgumentException("Tipo de producto guardado desconocido: " + datos.tipo);
        };
        if (datos.fuenteTasa != null) {
            producto.actualizarPrecio(datos.precioCOP, datos.fuenteTasa);
        } else {
            producto.actualizarPrecio(datos.precioCOP);
        }
        if (!datos.disponible) {
            producto.marcarNoDisponible();
        }
        return producto;
    }

    private static ProductoJson serializarProducto(Producto producto) {
        ProductoJson datos = new ProductoJson();
        datos.id = producto.getId();
        datos.nombre = producto.getNombre();
        datos.precioBaseUSD = producto.getPrecioBaseUSD();
        datos.precioCOP = producto.getPrecioCOP();
        datos.fuenteTasa = producto.getFuenteTasa();
        datos.disponible = producto.estaDisponible();
        if (producto instanceof Carta carta) {
            datos.tipo = "CARTA";
            datos.rareza = carta.getRareza().name();
        } else if (producto instanceof Booster booster) {
            datos.tipo = "BOOSTER";
            datos.abierto = booster.estaAbierto();
            datos.propietarioId = booster.getPropietarioId();
            datos.cartas = booster.getCartasParaPersistencia().stream()
                    .map(PersistenciaJson::serializarProducto)
                    .toList();
        } else if (producto instanceof ProductoSellado sellado) {
            datos.tipo = "SELLADO";
            datos.descripcion = sellado.getDescripcion();
        } else if (producto instanceof Accesorio accesorio) {
            datos.tipo = "ACCESORIO";
            datos.tipoAccesorio = accesorio.getTipo();
            datos.descripcion = accesorio.getDescripcion();
        } else {
            throw new IllegalArgumentException("Subtipo de producto no persistible: " + producto.getClass().getName());
        }
        return datos;
    }

    private static List<Carta> restaurarCartas(List<ProductoJson> datosCartas) {
        if (datosCartas == null) {
            throw new IllegalArgumentException("El contenido guardado del booster no existe.");
        }
        List<Carta> cartas = new ArrayList<>(datosCartas.size());
        for (ProductoJson datos : datosCartas) {
            Producto producto = restaurarProducto(datos);
            if (!(producto instanceof Carta carta)) {
                throw new IllegalArgumentException("El contenido del booster debe contener únicamente cartas.");
            }
            cartas.add(carta);
        }
        return cartas;
    }

    public static class EstadoTienda {
        public int version = 1;
        public List<UsuarioJson> usuarios = new ArrayList<>();
        public List<ProductoJson> productosTienda = new ArrayList<>();
        public List<InventarioClienteJson> inventariosClientes = new ArrayList<>();
        public List<CompraJson> compras = new ArrayList<>();
        public List<TorneoJson> torneos = new ArrayList<>();

        private void validarColecciones() throws IOException {
            if (usuarios == null || productosTienda == null || inventariosClientes == null
                    || compras == null || torneos == null) {
                throw new IOException("El archivo JSON no contiene todas las colecciones requeridas.");
            }
        }
    }

    public static class UsuarioJson {
        public String id;
        public String nombre;
        public String username;
        public String credencial;
        public String rol;

        public UsuarioJson() {
        }

        public UsuarioJson(String id, String nombre, String username, String credencial, String rol) {
            this.id = id;
            this.nombre = nombre;
            this.username = username;
            this.credencial = credencial;
            this.rol = rol;
        }
    }

    public static class ProductoJson {
        public String tipo;
        public String id;
        public String nombre;
        public BigDecimal precioBaseUSD;
        public BigDecimal precioCOP;
        public String fuenteTasa;
        public boolean disponible = true;
        public String rareza;
        public String descripcion;
        public String tipoAccesorio;
        public boolean abierto;
        public String propietarioId;
        public List<ProductoJson> cartas;
    }

    public static class InventarioClienteJson {
        public String clienteId;
        public List<ProductoJson> productos = new ArrayList<>();

        public InventarioClienteJson() {
        }

        public InventarioClienteJson(String clienteId) {
            this.clienteId = clienteId;
        }
    }

    public static class CompraJson {
        public String id;
        public String fecha;
        public String clienteId;
        public List<String> productoIds;

        public CompraJson() {
        }

        public CompraJson(String id, String fecha, String clienteId, List<String> productoIds) {
            this.id = id;
            this.fecha = fecha;
            this.clienteId = clienteId;
            this.productoIds = productoIds;
        }
    }

    public static class TorneoJson {
        public String id;
        public String nombre;
        public String fecha;
        public int maxParticipantes;
        public List<String> participantesIds;

        public TorneoJson() {
        }

        public TorneoJson(String id, String nombre, String fecha, int maxParticipantes, List<String> participantesIds) {
            this.id = id;
            this.nombre = nombre;
            this.fecha = fecha;
            this.maxParticipantes = maxParticipantes;
            this.participantesIds = participantesIds;
        }
    }
}
