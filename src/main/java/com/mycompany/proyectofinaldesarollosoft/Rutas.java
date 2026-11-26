package com.mycompany.proyectofinaldesarollosoft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorAutenticacion;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorBooster;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorCompra;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorProducto;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorTorneo;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorUsuario;
import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
import com.mycompany.proyectofinaldesarollosoft.excepcion.ConflictoException;
import com.mycompany.proyectofinaldesarollosoft.excepcion.NoAutorizadoException;
import com.mycompany.proyectofinaldesarollosoft.excepcion.NoEncontradoException;
import com.mycompany.proyectofinaldesarollosoft.excepcion.ProhibidoException;
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
import com.mycompany.proyectofinaldesarollosoft.servicio.ServicioTasaCambio;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Rutas {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String ATRIBUTO_USUARIO = "usuarioAutenticado";

    private final Tienda tienda;
    private final ControladorAutenticacion controladorAutenticacion;
    private final ControladorUsuario controladorUsuario;
    private final ControladorProducto controladorProducto;
    private final ControladorBooster controladorBooster;
    private final ControladorCompra controladorCompra;
    private final ControladorTorneo controladorTorneo;

    public Rutas(Tienda tienda) {
        this.tienda = tienda;
        this.controladorAutenticacion = new ControladorAutenticacion(tienda);
        this.controladorUsuario = new ControladorUsuario(tienda);
        this.controladorProducto = new ControladorProducto(tienda);
        this.controladorBooster = new ControladorBooster(tienda);
        this.controladorCompra = new ControladorCompra(tienda);
        this.controladorTorneo = new ControladorTorneo(tienda);
    }

    public void iniciar(int puerto) {
        Spark.port(puerto);
        Spark.before((request, response) -> {
            response.type("application/json");
            if (!request.pathInfo().equals("/salud")) {
                autenticar(request);
            }
        });

        registrarRutas();
        registrarErrores();
        Spark.notFound((request, response) -> {
            response.type("application/json");
            response.status(404);
            return json(Map.of("error", "La ruta solicitada no existe."));
        });
        Spark.awaitInitialization();
    }

    private void registrarRutas() {
        Spark.get("/salud", (request, response) -> json(Map.of("estado", "ok")));
        Spark.post("/clientes", (request, response) -> registrarCliente(request, response));
        Spark.get("/clientes", (request, response) -> listarClientes(request));
        Spark.post("/cartas", (request, response) -> registrarCarta(request, response));
        Spark.post("/sellados", (request, response) -> registrarSellado(request, response));
        Spark.post("/accesorios", (request, response) -> registrarAccesorio(request, response));
        Spark.get("/productos", (request, response) -> listarProductos());
        Spark.post("/boosters", (request, response) -> crearBooster(request, response));
        Spark.get("/boosters", (request, response) -> listarBoosters());
        Spark.post("/compras", (request, response) -> comprar(request, response));
        Spark.get("/compras/historial", (request, response) -> historialPropio(request));
        Spark.get("/clientes/:id/compras", (request, response) -> comprasCliente(request));
        Spark.post("/boosters/:id/abrir", (request, response) -> abrirBooster(request));
        Spark.post("/torneos", (request, response) -> crearTorneo(request, response));
        Spark.get("/torneos", (request, response) -> listarTorneos());
        Spark.post("/torneos/:id/inscripciones", (request, response) -> inscribirEnTorneo(request, response));
    }

    private void registrarErrores() {
        Spark.exception(NoAutorizadoException.class, (exception, request, response) ->
                responderError(response, 401, exception.getMessage()));
        Spark.exception(ProhibidoException.class, (exception, request, response) ->
                responderError(response, 403, exception.getMessage()));
        Spark.exception(NoEncontradoException.class, (exception, request, response) ->
                responderError(response, 404, exception.getMessage()));
        Spark.exception(ConflictoException.class, (exception, request, response) ->
                responderError(response, 409, exception.getMessage()));
        Spark.exception(JsonSyntaxException.class, (exception, request, response) ->
                responderError(response, 400, "El cuerpo no contiene JSON válido."));
        Spark.exception(DateTimeParseException.class, (exception, request, response) ->
                responderError(response, 400, "La fecha debe tener el formato YYYY-MM-DD."));
        Spark.exception(IllegalArgumentException.class, (exception, request, response) ->
                responderError(response, 400, mensaje(exception, "La solicitud contiene datos inválidos.")));
        Spark.exception(Exception.class, (exception, request, response) -> {
            System.err.println("Error inesperado en la API: " + exception.getMessage());
            responderError(response, 500, "Error interno del servidor.");
        });
    }

    private void autenticar(Request request) {
        String autorizacion = request.headers("Authorization");
        if (autorizacion == null || !autorizacion.regionMatches(true, 0, "Basic ", 0, 6)) {
            throw new NoAutorizadoException("Se requieren credenciales HTTP Basic.");
        }

        String textoCredenciales;
        try {
            byte[] credenciales = Base64.getDecoder().decode(autorizacion.substring(6).trim());
            textoCredenciales = new String(credenciales, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new NoAutorizadoException("Las credenciales HTTP Basic no son válidas.");
        }

        int separador = textoCredenciales.indexOf(':');
        if (separador <= 0 || separador == textoCredenciales.length() - 1) {
            throw new NoAutorizadoException("Las credenciales HTTP Basic no son válidas.");
        }

        String username = textoCredenciales.substring(0, separador);
        String password = textoCredenciales.substring(separador + 1);
        Usuario usuario;
        try {
            usuario = controladorAutenticacion.login(username, password);
        } catch (IllegalArgumentException exception) {
            throw new NoAutorizadoException("Usuario o contraseña incorrectos.");
        }
        request.attribute(ATRIBUTO_USUARIO, usuario);
    }

    private synchronized String registrarCliente(Request request, Response response) {
        Usuario administrador = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        Cliente cliente = controladorUsuario.registrarCliente(
                administrador,
                texto(datos, "nombre"),
                texto(datos, "username"),
                texto(datos, "password"));
        response.status(201);
        return json(usuarioJson(cliente));
    }

    private synchronized String listarClientes(Request request) {
        exigirAdmin(request);
        List<Map<String, Object>> clientes = new ArrayList<>();
        for (Usuario usuario : tienda.getUsuarios()) {
            if (usuario instanceof Cliente) {
                clientes.add(usuarioJson(usuario));
            }
        }
        return json(Map.of("clientes", clientes));
    }

    private synchronized String registrarCarta(Request request, Response response) {
        Usuario administrador = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        Carta carta = controladorProducto.registrarCarta(
                administrador,
                UUID.randomUUID().toString(),
                texto(datos, "nombre"),
                rareza(texto(datos, "rareza")),
                decimal(datos, "precioUSD"));
        response.status(201);
        return json(productoJson(carta, null, false));
    }

    private synchronized String registrarSellado(Request request, Response response) {
        Usuario administrador = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        ProductoSellado producto = controladorProducto.registrarProductoSellado(
                administrador,
                UUID.randomUUID().toString(),
                texto(datos, "nombre"),
                decimal(datos, "precioUSD"),
                textoOpcional(datos, "descripcion"));
        response.status(201);
        return json(productoJson(producto, null, false));
    }

    private synchronized String registrarAccesorio(Request request, Response response) {
        Usuario administrador = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        Accesorio accesorio = controladorProducto.registrarAccesorio(
                administrador,
                UUID.randomUUID().toString(),
                texto(datos, "nombre"),
                decimal(datos, "precioUSD"),
                texto(datos, "tipo"),
                textoOpcional(datos, "descripcion"));
        response.status(201);
        return json(productoJson(accesorio, null, false));
    }

    private synchronized String listarProductos() {
        ServicioTasaCambio.ResultadoTasa resultado = tienda.consultarTasaCambio();
        List<Map<String, Object>> productos = new ArrayList<>();
        for (Producto producto : tienda.getInventario().listarProductos()) {
            productos.add(productoJson(producto, resultado, false));
        }
        return json(Map.of("productos", productos));
    }

    private synchronized String crearBooster(Request request, Response response) {
        Usuario administrador = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        Booster booster = controladorBooster.crearBooster(
                administrador,
                UUID.randomUUID().toString(),
                texto(datos, "nombre"),
                decimal(datos, "precioUSD"));
        response.status(201);
        return json(productoJson(booster, null, false));
    }

    private synchronized String listarBoosters() {
        List<Map<String, Object>> boosters = new ArrayList<>();
        for (Producto producto : tienda.getInventario().listarDisponibles()) {
            if (producto instanceof Booster) {
                boosters.add(productoJson(producto, null, false));
            }
        }
        return json(Map.of("boosters", boosters));
    }

    private synchronized String comprar(Request request, Response response) {
        Cliente cliente = exigirCliente(request);
        JsonObject datos = cuerpoJson(request);
        Compra compra = controladorCompra.procesarCompra(cliente, texto(datos, "productoId"));
        response.status(201);
        return json(compraJson(compra, false));
    }

    private synchronized String historialPropio(Request request) {
        Cliente cliente = exigirCliente(request);
        return json(historialJson(cliente.getId(), cliente.getHistorialCompras()));
    }

    private synchronized String comprasCliente(Request request) {
        Usuario administrador = exigirAdmin(request);
        String clienteId = request.params(":id");
        List<Compra> compras = controladorUsuario.consultarHistorialCompras(administrador, clienteId);
        return json(historialJson(clienteId, compras));
    }

    private synchronized String abrirBooster(Request request) {
        Cliente cliente = exigirCliente(request);
        String boosterId = request.params(":id");
        List<Carta> cartas = controladorBooster.abrirBooster(cliente, boosterId);
        List<Map<String, Object>> contenido = new ArrayList<>();
        for (Carta carta : cartas) {
            contenido.add(productoJson(carta, null, false));
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("boosterId", boosterId);
        respuesta.put("abierto", true);
        respuesta.put("cartas", contenido);
        return json(respuesta);
    }

    private synchronized String crearTorneo(Request request, Response response) {
        Usuario administrador = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        LocalDate fecha = LocalDate.parse(texto(datos, "fecha"));
        Torneo torneo = controladorTorneo.crearTorneo(
                administrador,
                UUID.randomUUID().toString(),
                texto(datos, "nombre"),
                fecha,
                entero(datos, "maxParticipantes"));
        response.status(201);
        return json(torneoJson(torneo));
    }

    private synchronized String listarTorneos() {
        List<Map<String, Object>> torneos = new ArrayList<>();
        for (Torneo torneo : tienda.getTorneos()) {
            torneos.add(torneoJson(torneo));
        }
        return json(Map.of("torneos", torneos));
    }

    private synchronized String inscribirEnTorneo(Request request, Response response) {
        Cliente cliente = exigirCliente(request);
        String torneoId = request.params(":id");
        controladorTorneo.inscribirCliente(cliente, torneoId);
        response.status(201);
        return json(Map.of("torneoId", torneoId, "clienteId", cliente.getId()));
    }

    private Usuario exigirAdmin(Request request) {
        Usuario usuario = usuarioActual(request);
        if (usuario.getRol() != Rol.ADMIN) {
            throw new ProhibidoException("La operación requiere un Administrador.");
        }
        tienda.validarAdministrador(usuario);
        return usuario;
    }

    private Cliente exigirCliente(Request request) {
        Usuario usuario = usuarioActual(request);
        if (!(usuario instanceof Cliente)) {
            throw new ProhibidoException("La operación solo está disponible para clientes.");
        }
        return (Cliente) usuario;
    }

    private Usuario usuarioActual(Request request) {
        Usuario usuario = request.attribute(ATRIBUTO_USUARIO);
        if (usuario == null) {
            throw new NoAutorizadoException("Se requieren credenciales HTTP Basic.");
        }
        return usuario;
    }

    private Map<String, Object> usuarioJson(Usuario usuario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", usuario.getId());
        datos.put("nombre", usuario.getNombre());
        datos.put("username", usuario.getUsername());
        datos.put("rol", usuario.getRol().name());
        return datos;
    }

    private Map<String, Object> productoJson(
            Producto producto, ServicioTasaCambio.ResultadoTasa resultado, boolean mostrarCartasAbiertas) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", producto.getId());
        datos.put("nombre", producto.getNombre());
        datos.put("tipo", tipoProducto(producto));
        datos.put("precioUSD", producto.getPrecioBaseUSD());
        if (resultado == null) {
            datos.put("precioCOP", producto.getPrecioCOP());
            datos.put("fuenteTasa", producto.getFuenteTasa());
        } else {
            datos.put("precioCOP", producto.calcularPrecioCOP(resultado.tasa()));
            datos.put("fuenteTasa", resultado.fuente().name());
        }
        datos.put("disponible", producto.estaDisponible());

        if (producto instanceof Carta) {
            Carta carta = (Carta) producto;
            datos.put("rareza", carta.getRareza().getCodigo());
        } else if (producto instanceof Booster) {
            Booster booster = (Booster) producto;
            datos.put("abierto", booster.estaAbierto());
            if (mostrarCartasAbiertas && booster.estaAbierto()) {
                List<Map<String, Object>> cartas = new ArrayList<>();
                for (Carta carta : booster.getCartas()) {
                    cartas.add(productoJson(carta, null, false));
                }
                datos.put("cartas", cartas);
            }
        } else if (producto instanceof ProductoSellado) {
            datos.put("descripcion", ((ProductoSellado) producto).getDescripcion());
        } else if (producto instanceof Accesorio) {
            Accesorio accesorio = (Accesorio) producto;
            datos.put("tipoAccesorio", accesorio.getTipo());
            datos.put("descripcion", accesorio.getDescripcion());
        }
        return datos;
    }

    private String tipoProducto(Producto producto) {
        if (producto instanceof Carta) {
            return "CARTA";
        }
        if (producto instanceof Booster) {
            return "BOOSTER";
        }
        if (producto instanceof ProductoSellado) {
            return "SELLADO";
        }
        if (producto instanceof Accesorio) {
            return "ACCESORIO";
        }
        return producto.getClass().getSimpleName();
    }

    private Map<String, Object> compraJson(Compra compra, boolean mostrarCartasAbiertas) {
        List<Map<String, Object>> productos = new ArrayList<>();
        for (Producto producto : compra.getProductos()) {
            productos.add(productoJson(producto, null, mostrarCartasAbiertas));
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", compra.getId());
        datos.put("fecha", compra.getFecha().toString());
        datos.put("clienteId", compra.getCliente().getId());
        datos.put("productos", productos);
        datos.put("totalCOP", compra.calcularTotal());
        return datos;
    }

    private Map<String, Object> historialJson(String clienteId, List<Compra> compras) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Compra compra : compras) {
            resultado.add(compraJson(compra, true));
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("clienteId", clienteId);
        datos.put("compras", resultado);
        return datos;
    }

    private Map<String, Object> torneoJson(Torneo torneo) {
        List<String> participantes = new ArrayList<>();
        for (Cliente cliente : torneo.consultarParticipantes()) {
            participantes.add(cliente.getId());
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", torneo.getId());
        datos.put("nombre", torneo.getNombre());
        datos.put("fecha", torneo.getFecha().toString());
        datos.put("maxParticipantes", torneo.getMaxParticipantes());
        datos.put("hayCupos", torneo.hayCupos());
        datos.put("participantes", participantes);
        return datos;
    }

    private Rareza rareza(String codigo) {
        for (Rareza rareza : Rareza.values()) {
            if (rareza.getCodigo().equalsIgnoreCase(codigo)) {
                return rareza;
            }
        }
        throw new IllegalArgumentException("La rareza debe ser N, UC, R, SR o UR.");
    }

    private JsonObject cuerpoJson(Request request) {
        JsonElement elemento = JsonParser.parseString(request.body());
        if (!elemento.isJsonObject()) {
            throw new IllegalArgumentException("El cuerpo debe ser un objeto JSON.");
        }
        return elemento.getAsJsonObject();
    }

    private String texto(JsonObject datos, String campo) {
        String valor = textoOpcional(datos, campo);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio.");
        }
        return valor;
    }

    private String textoOpcional(JsonObject datos, String campo) {
        JsonElement elemento = datos.get(campo);
        if (elemento == null || elemento.isJsonNull()) {
            return null;
        }
        if (!elemento.isJsonPrimitive() || !elemento.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser texto.");
        }
        return elemento.getAsString();
    }

    private BigDecimal decimal(JsonObject datos, String campo) {
        JsonElement elemento = datos.get(campo);
        if (elemento == null || !elemento.isJsonPrimitive()) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser un número.");
        }
        JsonPrimitive primitiva = elemento.getAsJsonPrimitive();
        if (!primitiva.isNumber()) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser un número.");
        }
        try {
            return primitiva.getAsBigDecimal();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser un número válido.");
        }
    }

    private int entero(JsonObject datos, String campo) {
        try {
            return decimal(datos, campo).intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser un número entero.");
        }
    }

    private String json(Object contenido) {
        return GSON.toJson(contenido);
    }

    private void responderError(Response response, int estado, String mensaje) {
        response.type("application/json");
        response.status(estado);
        response.body(json(Map.of("error", mensaje == null ? "Error en la solicitud." : mensaje)));
    }

    private String mensaje(Exception exception, String predeterminado) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return predeterminado;
        }
        return exception.getMessage();
    }
}
