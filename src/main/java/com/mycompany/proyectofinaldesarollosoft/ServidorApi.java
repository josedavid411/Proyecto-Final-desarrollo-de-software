package com.mycompany.proyectofinaldesarollosoft;

import static spark.Spark.after;
import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.port;
import static spark.Spark.post;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorAutenticacion;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorBooster;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorCompra;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorProducto;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorTorneo;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorUsuario;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import spark.Request;
import spark.Response;

public final class ServidorApi {
    private static final Gson GSON = new Gson();
    private final Map<String, Usuario> sesiones = new ConcurrentHashMap<>();
    private final Tienda tienda;
    private final ControladorAutenticacion autenticacion;
    private final ControladorUsuario usuarios;
    private final ControladorProducto productos;
    private final ControladorBooster boosters;
    private final ControladorCompra compras;
    private final ControladorTorneo torneos;

    public ServidorApi() {
        this(new Tienda());
    }

    public ServidorApi(Tienda tienda) {
        this.tienda = tienda;
        this.autenticacion = new ControladorAutenticacion(tienda);
        this.usuarios = new ControladorUsuario(tienda);
        this.productos = new ControladorProducto(tienda);
        this.boosters = new ControladorBooster(tienda);
        this.compras = new ControladorCompra(tienda);
        this.torneos = new ControladorTorneo(tienda);
    }

    public void iniciar() {
        port(leerPuerto());
        before((request, response) -> response.type("application/json"));
        after((request, response) -> response.type("application/json"));
        registrarRutas();
        exception(Exception.class, (exception, request, response) -> responderError(response, exception));
        notFound((request, response) -> {
            response.status(404);
            return GSON.toJson(json("error", "La ruta solicitada no existe."));
        });
        awaitInitialization();
    }

    private void registrarRutas() {
        post("/login", (request, response) -> {
            JsonObject body = leerBody(request);
            String username = campoOpcional(body, "username");
            String password = campoOpcional(body, "password");
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new ApiException(401, "Usuario o contraseña incorrectos.");
            }
            Usuario usuario;
            try {
                usuario = autenticacion.login(username, password);
            } catch (SecurityException exception) {
                throw new ApiException(401, "Usuario o contraseña incorrectos.");
            }
            String token = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            sesiones.put(token, usuario);
            return responder(response, 200, json(
                    "token", token,
                    "usuario", dtoUsuario(usuario)));
        });

        post("/clientes", (request, response) -> {
            Usuario actor = exigirAdmin(request);
            JsonObject body = leerBody(request);
            String id = campoOpcional(body, "id");
            String nombre = campoObligatorio(body, "nombre");
            String username = campoObligatorio(body, "username");
            String password = campoObligatorio(body, "password");
            Cliente cliente = id == null || id.isBlank()
                    ? usuarios.registrarCliente(actor, nombre, username, password)
                    : usuarios.registrarCliente(actor, id, nombre, username, password);
            return responder(response, 201, dtoUsuario(cliente));
        });

        get("/inventario", (request, response) -> {
            ServicioTasaCambio.ResultadoTasa resultado = tienda.getServicioTasaCambio().consultarTasaUSDACOP();
            List<Map<String, Object>> disponibles = productos.listarInventarioDisponible().stream()
                    .map(producto -> dtoProducto(producto, resultado.tasa(), resultado.fuente()))
                    .toList();
            return responder(response, 200, json(
                    "productos", disponibles,
                    "fuenteTasa", resultado.fuente().name()));
        });

        post("/productos/carta", (request, response) -> {
            Usuario actor = exigirAdmin(request);
            JsonObject body = leerBody(request);
            Carta carta = productos.registrarCarta(
                    actor,
                    campoObligatorio(body, "id"),
                    campoObligatorio(body, "nombre"),
                    leerRareza(campoObligatorio(body, "rareza")),
                    leerDecimal(body, "precioBaseUSD"));
            return responder(response, 201, dtoProducto(carta, null, tienda.getServicioTasaCambio().getFuenteUltimaConsulta()));
        });

        post("/productos/sellado", (request, response) -> {
            Usuario actor = exigirAdmin(request);
            JsonObject body = leerBody(request);
            ProductoSellado sellado = productos.registrarProductoSellado(
                    actor,
                    campoObligatorio(body, "id"),
                    campoObligatorio(body, "nombre"),
                    leerDecimal(body, "precioBaseUSD"),
                    campoOpcional(body, "descripcion"));
            return responder(response, 201, dtoProducto(sellado, null, tienda.getServicioTasaCambio().getFuenteUltimaConsulta()));
        });

        post("/productos/accesorio", (request, response) -> {
            Usuario actor = exigirAdmin(request);
            JsonObject body = leerBody(request);
            Accesorio accesorio = productos.registrarAccesorio(
                    actor,
                    campoObligatorio(body, "id"),
                    campoObligatorio(body, "nombre"),
                    leerDecimal(body, "precioBaseUSD"),
                    campoObligatorio(body, "tipo"),
                    campoOpcional(body, "descripcion"));
            return responder(response, 201, dtoProducto(accesorio, null, tienda.getServicioTasaCambio().getFuenteUltimaConsulta()));
        });

        post("/boosters", (request, response) -> {
            Usuario actor = exigirAdmin(request);
            if (tienda.getInventario().listarProductos().stream().noneMatch(Carta.class::isInstance)) {
                throw new ApiException(400, "No hay cartas registradas para crear un booster.");
            }
            JsonObject body = leerBody(request);
            Booster booster = boosters.crearBooster(
                    actor,
                    campoObligatorio(body, "id"),
                    campoObligatorio(body, "nombre"),
                    leerDecimal(body, "precioBaseUSD"));
            return responder(response, 201, dtoProducto(booster, null, tienda.getServicioTasaCambio().getFuenteUltimaConsulta()));
        });

        post("/boosters/:id/abrir", (request, response) -> {
            Cliente cliente = exigirCliente(request);
            String id = request.params(":id");
            Producto propio = cliente.getInventario().buscarProducto(id).orElse(null);
            if (!(propio instanceof Booster)) {
                boolean existeAjeno = tienda.getUsuarios().stream()
                        .filter(Cliente.class::isInstance)
                        .map(Cliente.class::cast)
                        .filter(otro -> otro != cliente)
                        .anyMatch(otro -> otro.getInventario().buscarProducto(id)
                                .filter(Booster.class::isInstance)
                                .isPresent());
                boolean enVenta = tienda.getInventario().buscarProducto(id)
                        .filter(Booster.class::isInstance)
                        .isPresent();
                if (existeAjeno || enVenta) {
                    throw new ApiException(403, "El booster no pertenece al cliente autenticado.");
                }
                throw new ApiException(404, "El booster no existe.");
            }
            List<Map<String, Object>> cartas = boosters.abrirBooster(cliente, id).stream()
                    .map(carta -> dtoProducto(carta, null, null))
                    .toList();
            return responder(response, 200, json("boosterId", id, "cartas", cartas));
        });

        post("/compras", (request, response) -> {
            Cliente cliente = exigirCliente(request);
            JsonObject body = leerBody(request);
            String productoId = campoOpcional(body, "productoId");
            if (productoId == null || productoId.isBlank()) {
                productoId = campoOpcional(body, "boosterId");
            }
            if (productoId == null || productoId.isBlank()) {
                throw new ApiException(400, "Debe indicar productoId o boosterId.");
            }
            final String idProductoComprado = productoId;
            Producto disponible = tienda.getInventario().buscarProducto(idProductoComprado).orElse(null);
            if (disponible == null) {
                boolean boosterVendido = tienda.getUsuarios().stream()
                        .filter(Cliente.class::isInstance)
                        .map(Cliente.class::cast)
                        .anyMatch(actual -> actual.getInventario().buscarProducto(idProductoComprado)
                                .filter(Booster.class::isInstance)
                                .isPresent());
                if (boosterVendido) {
                    throw new ApiException(409, "El booster ya fue vendido.");
                }
                throw new ApiException(404, "El producto no existe.");
            }
            if (!disponible.estaDisponible()) {
                throw new ApiException(409, "El producto ya fue vendido.");
            }
            Compra compra = compras.procesarCompra(cliente, idProductoComprado);
            return responder(response, 201, dtoCompra(compra));
        });

        get("/clientes/:id/historial", (request, response) -> {
            Usuario actor = exigirUsuario(request);
            String clienteId = request.params(":id");
            Cliente cliente = buscarCliente(clienteId);
            if (actor != cliente && actor.getRol() != Rol.ADMIN) {
                throw new ApiException(403, "No tiene permiso para consultar este historial.");
            }
            List<Map<String, Object>> historial = usuarios.consultarHistorialCompras(actor, clienteId).stream()
                    .map(this::dtoCompra)
                    .toList();
            return responder(response, 200, json("clienteId", clienteId, "compras", historial));
        });

        post("/torneos", (request, response) -> {
            Usuario actor = exigirAdmin(request);
            JsonObject body = leerBody(request);
            Torneo torneo = torneos.crearTorneo(
                    actor,
                    campoObligatorio(body, "id"),
                    campoObligatorio(body, "nombre"),
                    leerFecha(campoObligatorio(body, "fecha")),
                    leerEntero(body, "maxParticipantes"));
            return responder(response, 201, dtoTorneo(torneo));
        });

        get("/torneos", (request, response) -> responder(
                response, 200, json("torneos", tienda.getTorneos().stream().map(this::dtoTorneo).toList())));

        post("/torneos/:id/inscribir", (request, response) -> {
            Cliente cliente = exigirCliente(request);
            String id = request.params(":id");
            if (tienda.getTorneos().stream().noneMatch(torneo -> torneo.getId().equals(id))) {
                throw new ApiException(404, "El torneo no existe.");
            }
            torneos.inscribirCliente(cliente, id);
            Torneo torneo = tienda.getTorneos().stream()
                    .filter(actual -> actual.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(404, "El torneo no existe."));
            return responder(response, 201, dtoTorneo(torneo));
        });
    }

    private Usuario exigirUsuario(Request request) {
        String header = request.headers("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Se requiere autenticación Bearer.");
        }
        Usuario usuario = sesiones.get(header.substring("Bearer ".length()).trim());
        if (usuario == null) {
            throw new ApiException(401, "El token de autenticación no es válido.");
        }
        return usuario;
    }

    private Usuario exigirAdmin(Request request) {
        Usuario usuario = exigirUsuario(request);
        if (usuario.getRol() != Rol.ADMIN) {
            throw new ApiException(403, "Esta operación requiere permisos de administrador.");
        }
        return usuario;
    }

    private Cliente exigirCliente(Request request) {
        Usuario usuario = exigirUsuario(request);
        if (!(usuario instanceof Cliente cliente)) {
            throw new ApiException(403, "Esta operación requiere una cuenta de cliente.");
        }
        return cliente;
    }

    private Cliente buscarCliente(String id) {
        return tienda.getUsuarios().stream()
                .filter(Cliente.class::isInstance)
                .map(Cliente.class::cast)
                .filter(cliente -> cliente.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "El cliente no existe."));
    }

    private JsonObject leerBody(Request request) {
        try {
            JsonElement elemento = JsonParser.parseString(request.body());
            if (!elemento.isJsonObject()) {
                throw new ApiException(400, "El cuerpo debe ser un objeto JSON.");
            }
            return elemento.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException exception) {
            throw new ApiException(400, "El cuerpo JSON no es válido.");
        }
    }

    private static String campoObligatorio(JsonObject body, String campo) {
        String valor = campoOpcional(body, campo);
        if (valor == null || valor.isBlank()) {
            throw new ApiException(400, "El campo " + campo + " es obligatorio.");
        }
        return valor.trim();
    }

    private static String campoOpcional(JsonObject body, String campo) {
        JsonElement elemento = body.get(campo);
        if (elemento == null || elemento.isJsonNull()) {
            return null;
        }
        if (!elemento.isJsonPrimitive() || !elemento.getAsJsonPrimitive().isString()) {
            throw new ApiException(400, "El campo " + campo + " debe ser texto.");
        }
        return elemento.getAsString();
    }

    private static BigDecimal leerDecimal(JsonObject body, String campo) {
        JsonElement elemento = body.get(campo);
        if (elemento == null || elemento.isJsonNull() || !elemento.isJsonPrimitive()
                || !elemento.getAsJsonPrimitive().isNumber()) {
            throw new ApiException(400, "El campo " + campo + " debe ser numérico.");
        }
        try {
            return elemento.getAsBigDecimal();
        } catch (NumberFormatException exception) {
            throw new ApiException(400, "El campo " + campo + " no es un número válido.");
        }
    }

    private static int leerEntero(JsonObject body, String campo) {
        try {
            return leerDecimal(body, campo).intValueExact();
        } catch (ArithmeticException exception) {
            throw new ApiException(400, "El campo " + campo + " debe ser un número entero.");
        }
    }

    private static com.mycompany.proyectofinaldesarollosoft.enums.Rareza leerRareza(String valor) {
        try {
            return com.mycompany.proyectofinaldesarollosoft.enums.Rareza.valueOf(valor);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(400, "La rareza indicada no es válida.");
        }
    }

    private static LocalDate leerFecha(String valor) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException exception) {
            throw new ApiException(400, "La fecha debe tener el formato AAAA-MM-DD.");
        }
    }

    private Map<String, Object> dtoProducto(
            Producto producto, BigDecimal tasa, ServicioTasaCambio.Fuente fuente) {
        BigDecimal precioCOP = tasa == null ? producto.getPrecioCOP() : producto.calcularPrecioCOP(tasa);
        Map<String, Object> dto = json(
                "id", producto.getId(),
                "nombre", producto.getNombre(),
                "precioBaseUSD", producto.getPrecioBaseUSD(),
                "precioCOP", precioCOP,
                "disponible", producto.estaDisponible());
        if (producto instanceof Carta carta) {
            dto.put("tipo", "CARTA");
            dto.put("rareza", carta.getRareza().name());
        } else if (producto instanceof Booster booster) {
            dto.put("tipo", "BOOSTER");
            dto.put("abierto", booster.estaAbierto());
            if (booster.estaAbierto()) {
                dto.put("cartas", booster.getCartas().stream()
                        .map(carta -> dtoProducto(carta, tasa, fuente))
                        .toList());
            }
        } else if (producto instanceof ProductoSellado sellado) {
            dto.put("tipo", "SELLADO");
            dto.put("descripcion", sellado.getDescripcion());
        } else if (producto instanceof Accesorio accesorio) {
            dto.put("tipo", "ACCESORIO");
            dto.put("tipoAccesorio", accesorio.getTipo());
            dto.put("descripcion", accesorio.getDescripcion());
        }
        if (fuente != null) {
            dto.put("fuenteTasa", fuente.name());
        }
        return dto;
    }

    private Map<String, Object> dtoUsuario(Usuario usuario) {
        return json(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "username", usuario.getUsername(),
                "rol", usuario.getRol().name());
    }

    private Map<String, Object> dtoCompra(Compra compra) {
        List<Map<String, Object>> detalle = new ArrayList<>();
        for (Producto producto : compra.getProductos()) {
            detalle.add(dtoProducto(producto, null, null));
        }
        return json(
                "id", compra.getId(),
                "fecha", compra.getFecha().toString(),
                "clienteId", compra.getCliente().getId(),
                "productos", detalle,
                "totalCOP", compra.calcularTotal());
    }

    private Map<String, Object> dtoTorneo(Torneo torneo) {
        return json(
                "id", torneo.getId(),
                "nombre", torneo.getNombre(),
                "fecha", torneo.getFecha().toString(),
                "maxParticipantes", torneo.getMaxParticipantes(),
                "participantes", torneo.consultarParticipantes().stream().map(Cliente::getId).toList(),
                "cuposDisponibles", torneo.hayCupos());
    }

    private static Map<String, Object> json(Object... valores) {
        Map<String, Object> objeto = new LinkedHashMap<>();
        for (int i = 0; i < valores.length; i += 2) {
            objeto.put((String) valores[i], valores[i + 1]);
        }
        return objeto;
    }

    private static Object responder(Response response, int status, Object cuerpo) {
        response.status(status);
        response.type("application/json");
        return GSON.toJson(cuerpo);
    }

    private static void responderError(Response response, Exception exception) {
        int status = 500;
        String descripcion = "Error interno del servidor.";
        if (exception instanceof ApiException apiException) {
            status = apiException.status;
            descripcion = apiException.getMessage();
        } else if (exception instanceof IllegalArgumentException) {
            status = 400;
            descripcion = mensaje(exception, "La solicitud contiene datos inválidos.");
        } else if (exception instanceof IllegalStateException) {
            status = 409;
            descripcion = mensaje(exception, "La operación no puede realizarse en el estado actual.");
        } else if (exception instanceof SecurityException) {
            status = 403;
            descripcion = mensaje(exception, "La operación no está autorizada.");
        }
        response.status(status);
        response.type("application/json");
        response.body(GSON.toJson(json("error", descripcion)));
    }

    private static String mensaje(Exception exception, String predeterminado) {
        String mensaje = exception.getMessage();
        return mensaje == null || mensaje.isBlank() ? predeterminado : mensaje;
    }

    private static int leerPuerto() {
        String valor = System.getenv("PORT");
        if (valor == null || valor.isBlank()) {
            return 4567;
        }
        try {
            int puerto = Integer.parseInt(valor);
            if (puerto < 1 || puerto > 65535) {
                throw new NumberFormatException("Fuera de rango");
            }
            return puerto;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("La variable PORT debe ser un puerto válido.", exception);
        }
    }

    private static final class ApiException extends RuntimeException {
        private final int status;

        private ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
