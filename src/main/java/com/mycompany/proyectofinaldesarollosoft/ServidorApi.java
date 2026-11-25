package com.mycompany.proyectofinaldesarollosoft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorAutenticacion;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorBooster;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorCompra;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorProducto;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorTorneo;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorUsuario;
import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import spark.Request;
import spark.Response;
import spark.Spark;

public final class ServidorApi {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Tienda tienda;
    private final ServicioTasaCambio servicioTasa;
    private final ControladorAutenticacion controladorAutenticacion;
    private final ControladorUsuario controladorUsuario;
    private final ControladorProducto controladorProducto;
    private final ControladorBooster controladorBooster;
    private final ControladorCompra controladorCompra;
    private final ControladorTorneo controladorTorneo;
    private final Map<String, Usuario> sesiones = new ConcurrentHashMap<>();

    public ServidorApi(
            Tienda tienda,
            ServicioTasaCambio servicioTasa,
            ControladorAutenticacion controladorAutenticacion,
            ControladorUsuario controladorUsuario,
            ControladorProducto controladorProducto,
            ControladorBooster controladorBooster,
            ControladorCompra controladorCompra,
            ControladorTorneo controladorTorneo) {
        this.tienda = tienda;
        this.servicioTasa = servicioTasa;
        this.controladorAutenticacion = controladorAutenticacion;
        this.controladorUsuario = controladorUsuario;
        this.controladorProducto = controladorProducto;
        this.controladorBooster = controladorBooster;
        this.controladorCompra = controladorCompra;
        this.controladorTorneo = controladorTorneo;
    }

    public void iniciar() {
        Spark.port(leerPuerto());
        Spark.before((request, response) -> response.type("application/json; charset=utf-8"));
        registrarRutas();
        Spark.notFound((request, response) -> {
            response.type("application/json; charset=utf-8");
            response.status(404);
            return GSON.toJson(Map.of("error", "La ruta solicitada no existe."));
        });
        Spark.exception(Exception.class, (exception, request, response) -> {
            response.type("application/json; charset=utf-8");
            response.status(500);
            response.body(GSON.toJson(Map.of("error", "Error interno del servidor.")));
            System.err.println("Error no controlado en Spark: " + exception.getMessage());
        });
        Spark.awaitInitialization();
    }

    private void registrarRutas() {
        Spark.post("/login", (request, response) -> ejecutar(response, () -> iniciarSesion(request)));
        Spark.post("/clientes", (request, response) -> ejecutar(response, () -> registrarCliente(request)));
        Spark.get("/inventario", (request, response) -> ejecutar(response, this::listarInventario));
        Spark.post("/productos/carta", (request, response) -> ejecutar(response, () -> registrarCarta(request)));
        Spark.post("/productos/sellado", (request, response) -> ejecutar(response, () -> registrarSellado(request)));
        Spark.post("/productos/accesorio", (request, response) -> ejecutar(response, () -> registrarAccesorio(request)));
        Spark.post("/boosters", (request, response) -> ejecutar(response, () -> crearBooster(request)));
        Spark.post("/boosters/:id/abrir", (request, response) -> ejecutar(response, () -> abrirBooster(request)));
        Spark.post("/compras", (request, response) -> ejecutar(response, () -> comprar(request)));
        Spark.get("/clientes/:id/historial", (request, response) -> ejecutar(response, () -> consultarHistorial(request)));
        Spark.post("/torneos", (request, response) -> ejecutar(response, () -> crearTorneo(request)));
        Spark.get("/torneos", (request, response) -> ejecutar(response, this::listarTorneos));
        Spark.post("/torneos/:id/inscribir", (request, response) -> ejecutar(response, () -> inscribirEnTorneo(request)));
    }

    private Object iniciarSesion(Request request) {
        JsonObject datos = cuerpoJson(request);
        Usuario usuario;
        try {
            usuario = controladorAutenticacion.login(texto(datos, "username"), texto(datos, "password"));
        } catch (SecurityException exception) {
            throw new ApiException(401, "Usuario o contraseña incorrectos.");
        }
        String token = UUID.randomUUID().toString();
        sesiones.put(token, usuario);
        return Map.of("token", token, "usuario", usuarioJson(usuario));
    }

    private Object registrarCliente(Request request) {
        Usuario actor = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        String id = textoOpcional(datos, "id");
        Cliente cliente = id == null || id.isBlank()
                ? controladorUsuario.registrarCliente(
                        actor, texto(datos, "nombre"), texto(datos, "username"), texto(datos, "password"))
                : controladorUsuario.registrarCliente(
                        actor, id, texto(datos, "nombre"), texto(datos, "username"), texto(datos, "password"));
        return creado(Map.of("cliente", usuarioJson(cliente)));
    }

    private Object listarInventario() {
        ServicioTasaCambio.ResultadoTasa resultado = servicioTasa.consultarTasaUSDACOP();
        List<Map<String, Object>> productos = controladorProducto.listarInventarioDisponible().stream()
                .map(producto -> productoJson(producto, resultado.tasa(), true))
                .toList();
        return Map.of(
                "productos", productos,
                "tasaUSDACOP", resultado.tasa(),
                "fuenteTasa", resultado.fuente().name());
    }

    private Object registrarCarta(Request request) {
        Usuario actor = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        Rareza rareza;
        try {
            rareza = Rareza.valueOf(texto(datos, "rareza").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(400, "La rareza indicada no es válida.");
        }
        Carta carta = controladorProducto.registrarCarta(
                actor, texto(datos, "id"), texto(datos, "nombre"), rareza, decimal(datos, "precioBaseUSD"));
        return creado(productoCreado(carta));
    }

    private Object registrarSellado(Request request) {
        Usuario actor = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        ProductoSellado producto = controladorProducto.registrarProductoSellado(
                actor,
                texto(datos, "id"),
                texto(datos, "nombre"),
                decimal(datos, "precioBaseUSD"),
                textoOpcional(datos, "descripcion"));
        return creado(productoCreado(producto));
    }

    private Object registrarAccesorio(Request request) {
        Usuario actor = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        Accesorio accesorio = controladorProducto.registrarAccesorio(
                actor,
                texto(datos, "id"),
                texto(datos, "nombre"),
                decimal(datos, "precioBaseUSD"),
                texto(datos, "tipo"),
                textoOpcional(datos, "descripcion"));
        return creado(productoCreado(accesorio));
    }

    private Object crearBooster(Request request) {
        Usuario actor = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        boolean hayCartas = tienda.getInventario().listarProductos().stream().anyMatch(Carta.class::isInstance);
        if (!hayCartas) {
            throw new ApiException(400, "No hay cartas registradas para crear el booster.");
        }
        Booster booster = controladorBooster.crearBooster(
                actor, texto(datos, "id"), texto(datos, "nombre"), decimal(datos, "precioBaseUSD"));
        return creado(productoCreado(booster));
    }

    private Object abrirBooster(Request request) {
        Cliente cliente = exigirCliente(request);
        String id = request.params(":id");
        Producto encontrado = buscarProductoEnTiendaOClientes(id);
        if (!(encontrado instanceof Booster booster)) {
            throw new ApiException(404, "El booster solicitado no existe.");
        }
        if (!cliente.getId().equals(booster.getPropietarioId())) {
            throw new ApiException(403, "El booster pertenece a otro cliente o no ha sido comprado.");
        }
        if (booster.estaAbierto()) {
            throw new ApiException(409, "El booster ya fue abierto.");
        }
        List<Carta> cartas = controladorBooster.abrirBooster(cliente, id);
        return Map.of("boosterId", id, "abierto", true, "cartas", cartas.stream()
                .map(carta -> productoJson(carta, null, false))
                .toList());
    }

    private Object comprar(Request request) {
        Cliente cliente = exigirCliente(request);
        JsonObject datos = cuerpoJson(request);
        String id = texto(datos, "productoId");
        Producto enTienda = tienda.getInventario().buscarProducto(id).orElse(null);
        if (enTienda == null) {
            Producto yaVendido = buscarProductoEnInventariosClientes(id);
            if (yaVendido == null) {
                throw new ApiException(404, "El producto no existe.");
            }
            if (yaVendido instanceof Booster) {
                throw new ApiException(409, "El booster ya fue vendido.");
            }
            throw new ApiException(404, "El producto no está disponible en la tienda.");
        }
        if (!enTienda.estaDisponible()) {
            throw new ApiException(409, "El producto no está disponible para la compra.");
        }
        Compra compra = controladorCompra.procesarCompra(cliente, id);
        return creado(compraJson(compra));
    }

    private Object consultarHistorial(Request request) {
        Usuario actor = exigirUsuario(request);
        String clienteId = request.params(":id");
        try {
            List<Compra> compras = controladorUsuario.consultarHistorialCompras(actor, clienteId);
            return Map.of("clienteId", clienteId, "compras", compras.stream()
                    .map(this::compraJson)
                    .toList());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(404, "El cliente no existe.");
        }
    }

    private Object crearTorneo(Request request) {
        Usuario actor = exigirAdmin(request);
        JsonObject datos = cuerpoJson(request);
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(texto(datos, "fecha"));
        } catch (DateTimeParseException exception) {
            throw new ApiException(400, "La fecha debe tener el formato YYYY-MM-DD.");
        }
        Torneo torneo = controladorTorneo.crearTorneo(
                actor,
                texto(datos, "id"),
                texto(datos, "nombre"),
                fecha,
                entero(datos, "maxParticipantes"));
        return creado(torneoJson(torneo));
    }

    private Object listarTorneos() {
        return Map.of("torneos", tienda.getTorneos().stream().map(this::torneoJson).toList());
    }

    private Object inscribirEnTorneo(Request request) {
        Cliente cliente = exigirCliente(request);
        String id = request.params(":id");
        Torneo torneo = tienda.getTorneos().stream()
                .filter(actual -> actual.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "El torneo no existe."));
        try {
            controladorTorneo.inscribirCliente(cliente, id);
        } catch (IllegalStateException exception) {
            throw new ApiException(409, exception.getMessage());
        }
        return creado(Map.of("torneo", torneoJson(torneo), "clienteId", cliente.getId()));
    }

    private Usuario exigirAdmin(Request request) {
        Usuario usuario = exigirUsuario(request);
        if (usuario.getRol() != Rol.ADMIN) {
            throw new ApiException(403, "La operación requiere un Administrador.");
        }
        return usuario;
    }

    private Cliente exigirCliente(Request request) {
        Usuario usuario = exigirUsuario(request);
        if (!(usuario instanceof Cliente cliente)) {
            throw new ApiException(403, "La operación solo está disponible para clientes.");
        }
        return cliente;
    }

    private Usuario exigirUsuario(Request request) {
        String autorizacion = request.headers("Authorization");
        if (autorizacion == null || !autorizacion.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ApiException(401, "Se requiere autenticación mediante Bearer token.");
        }
        Usuario usuario = sesiones.get(autorizacion.substring(7).trim());
        if (usuario == null) {
            throw new ApiException(401, "La sesión no es válida.");
        }
        return usuario;
    }

    private Producto buscarProductoEnTiendaOClientes(String id) {
        Producto producto = tienda.getInventario().buscarProducto(id).orElse(null);
        return producto != null ? producto : buscarProductoEnInventariosClientes(id);
    }

    private Producto buscarProductoEnInventariosClientes(String id) {
        return tienda.getUsuarios().stream()
                .filter(Cliente.class::isInstance)
                .map(Cliente.class::cast)
                .map(cliente -> cliente.getInventario().buscarProducto(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> productoCreado(Producto producto) {
        Map<String, Object> respuesta = productoJson(producto, null, false);
        respuesta.put("fuenteTasa", String.valueOf(servicioTasa.getFuenteUltimaConsulta()));
        return respuesta;
    }

    private Map<String, Object> productoJson(Producto producto, BigDecimal tasa, boolean incluirFuente) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", producto.getId());
        datos.put("nombre", producto.getNombre());
        datos.put("tipo", tipoProducto(producto));
        datos.put("precioBaseUSD", producto.getPrecioBaseUSD());
        datos.put("precioCOP", tasa == null ? producto.getPrecioCOP() : producto.calcularPrecioCOP(tasa));
        datos.put("disponible", producto.estaDisponible());
        if (producto instanceof Carta carta) {
            datos.put("rareza", carta.getRareza().name());
        } else if (producto instanceof Booster booster) {
            datos.put("abierto", booster.estaAbierto());
            if (booster.estaAbierto()) {
                datos.put("cartas", booster.getCartas().stream()
                        .map(carta -> productoJson(carta, tasa, false))
                        .toList());
            }
        } else if (producto instanceof ProductoSellado sellado) {
            datos.put("descripcion", sellado.getDescripcion());
        } else if (producto instanceof Accesorio accesorio) {
            datos.put("tipoAccesorio", accesorio.getTipo());
            datos.put("descripcion", accesorio.getDescripcion());
        }
        if (incluirFuente) {
            datos.put("fuenteTasa", servicioTasa.getFuenteUltimaConsulta().name());
        }
        return datos;
    }

    private Map<String, Object> compraJson(Compra compra) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", compra.getId());
        datos.put("fecha", compra.getFecha().toString());
        datos.put("clienteId", compra.getCliente().getId());
        datos.put("productos", compra.getProductos().stream()
                .map(producto -> productoJson(producto, null, false))
                .toList());
        datos.put("totalCOP", compra.calcularTotal());
        return datos;
    }

    private Map<String, Object> torneoJson(Torneo torneo) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", torneo.getId());
        datos.put("nombre", torneo.getNombre());
        datos.put("fecha", torneo.getFecha().toString());
        datos.put("maxParticipantes", torneo.getMaxParticipantes());
        datos.put("participantes", torneo.consultarParticipantes().stream().map(Cliente::getId).toList());
        datos.put("cuposDisponibles", torneo.getMaxParticipantes() - torneo.consultarParticipantes().size());
        return datos;
    }

    private Map<String, Object> usuarioJson(Usuario usuario) {
        return Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "username", usuario.getUsername(),
                "rol", usuario.getRol().name());
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

    private Map<String, Object> creado(Object contenido) {
        if (contenido instanceof Map<?, ?> mapa) {
            return Map.of("__status", 201, "__body", mapa);
        }
        return Map.of("__status", 201, "__body", Map.of("resultado", contenido));
    }

    private String ejecutar(Response response, Supplier<Object> accion) {
        response.type("application/json; charset=utf-8");
        try {
            Object salida;
            synchronized (tienda) {
                salida = accion.get();
            }
            if (salida instanceof Map<?, ?> mapa && mapa.containsKey("__status")) {
                response.status((Integer) mapa.get("__status"));
                salida = mapa.get("__body");
            }
            return GSON.toJson(salida);
        } catch (ApiException exception) {
            response.status(exception.status);
            return GSON.toJson(Map.of("error", exception.getMessage()));
        } catch (JsonParseException | DateTimeParseException exception) {
            response.status(400);
            return GSON.toJson(Map.of("error", "La solicitud contiene datos JSON inválidos."));
        } catch (SecurityException exception) {
            response.status(403);
            return GSON.toJson(Map.of("error", mensaje(exception, "La operación no está autorizada.")));
        } catch (IllegalArgumentException exception) {
            response.status(400);
            return GSON.toJson(Map.of("error", mensaje(exception, "La solicitud contiene datos inválidos.")));
        } catch (IllegalStateException exception) {
            if (mensaje(exception, "").startsWith("No se pudo guardar")
                    || mensaje(exception, "").startsWith("No se pudo cargar")) {
                response.status(500);
                return GSON.toJson(Map.of("error", "No se pudo guardar el estado de la tienda."));
            }
            response.status(409);
            return GSON.toJson(Map.of("error", mensaje(exception, "La operación no se puede completar.")));
        } catch (RuntimeException exception) {
            System.err.println("Error inesperado en la API: " + exception.getMessage());
            response.status(500);
            return GSON.toJson(Map.of("error", "Error interno del servidor."));
        }
    }

    private JsonObject cuerpoJson(Request request) {
        try {
            JsonElement elemento = JsonParser.parseString(request.body());
            if (!elemento.isJsonObject()) {
                throw new ApiException(400, "El cuerpo debe ser un objeto JSON.");
            }
            return elemento.getAsJsonObject();
        } catch (JsonParseException exception) {
            throw new ApiException(400, "El cuerpo no contiene JSON válido.");
        }
    }

    private String texto(JsonObject datos, String campo) {
        String valor = textoOpcional(datos, campo);
        if (valor == null || valor.isBlank()) {
            throw new ApiException(400, "El campo '" + campo + "' es obligatorio.");
        }
        return valor;
    }

    private String textoOpcional(JsonObject datos, String campo) {
        JsonElement valor = datos.get(campo);
        if (valor == null || valor.isJsonNull()) {
            return null;
        }
        if (!valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isString()) {
            throw new ApiException(400, "El campo '" + campo + "' debe ser texto.");
        }
        return valor.getAsString();
    }

    private BigDecimal decimal(JsonObject datos, String campo) {
        try {
            JsonElement valor = datos.get(campo);
            if (valor == null || valor.isJsonNull()) {
                throw new NumberFormatException();
            }
            return valor.getAsBigDecimal();
        } catch (NumberFormatException | ClassCastException exception) {
            throw new ApiException(400, "El campo '" + campo + "' debe ser un número válido.");
        }
    }

    private int entero(JsonObject datos, String campo) {
        try {
            JsonElement valor = datos.get(campo);
            if (valor == null || valor.isJsonNull()) {
                throw new NumberFormatException();
            }
            return valor.getAsBigDecimal().intValueExact();
        } catch (NumberFormatException | ArithmeticException | ClassCastException exception) {
            throw new ApiException(400, "El campo '" + campo + "' debe ser un número entero.");
        }
    }

    private int leerPuerto() {
        String valor = System.getenv("PORT");
        if (valor != null) {
            try {
                int puerto = Integer.parseInt(valor);
                if (puerto > 0 && puerto <= 65535) {
                    return puerto;
                }
            } catch (NumberFormatException ignored) {
                // Un puerto de entorno inválido usa el puerto estándar de Spark.
            }
        }
        return 4567;
    }

    private String mensaje(RuntimeException exception, String predeterminado) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? predeterminado
                : exception.getMessage();
    }

    private static final class ApiException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final int status;

        private ApiException(int status, String mensaje) {
            super(mensaje);
            this.status = status;
        }
    }
}
