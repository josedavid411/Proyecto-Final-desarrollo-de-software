package com.mycompany.proyectofinaldesarollosoft;

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
import com.mycompany.proyectofinaldesarollosoft.servicio.PersistenciaJson;
import com.mycompany.proyectofinaldesarollosoft.servicio.ServicioTasaCambio;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String[] OPCIONES_RAREZA = {
        "NORMAL", "POCO_COMUN", "RARO", "SUPER_RARO", "ULTRA_RARO"
    };

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        boolean modoConsola = args.length > 0 && "--cli".equals(args[0]);
        String[] argumentos = modoConsola ? java.util.Arrays.copyOfRange(args, 1, args.length) : args;
        Path archivo = argumentos.length == 0 ? Path.of("data", "tienda.json") : Path.of(argumentos[0]);
        ServicioTasaCambio servicioTasa = new ServicioTasaCambio();
        Tienda tienda = new Tienda(servicioTasa, new PersistenciaJson(archivo));

        ControladorAutenticacion controladorAutenticacion = new ControladorAutenticacion(tienda);
        ControladorUsuario controladorUsuario = new ControladorUsuario(tienda);
        ControladorProducto controladorProducto = new ControladorProducto(tienda);
        ControladorBooster controladorBooster = new ControladorBooster(tienda);
        ControladorCompra controladorCompra = new ControladorCompra(tienda);
        ControladorTorneo controladorTorneo = new ControladorTorneo(tienda);

        if (!modoConsola) {
            new ServidorApi(
                    tienda,
                    servicioTasa,
                    controladorAutenticacion,
                    controladorUsuario,
                    controladorProducto,
                    controladorBooster,
                    controladorCompra,
                    controladorTorneo).iniciar();
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            Usuario usuarioActual = null;
            boolean ejecutando = true;
            while (ejecutando && scanner.hasNextLine()) {
                try {
                    if (usuarioActual == null) {
                        mostrarMenuPrincipal();
                        Integer opcion = leerEntero(scanner, "Seleccione una opción: ");
                        if (opcion == null || opcion == 0) {
                            ejecutando = false;
                        } else if (opcion == 1) {
                            usuarioActual = iniciarSesion(scanner, controladorAutenticacion);
                        } else {
                            System.out.println("Opción no válida.");
                        }
                    } else if (usuarioActual.getRol() == Rol.ADMIN) {
                        mostrarMenuAdministrador();
                        Integer opcion = leerEntero(scanner, "Seleccione una opción: ");
                        if (opcion == null) {
                            ejecutando = false;
                        } else if (opcion == 8) {
                            usuarioActual = null;
                            System.out.println("Sesión cerrada.");
                        } else {
                            ejecutarOpcionAdministrador(
                                    opcion,
                                    scanner,
                                    usuarioActual,
                                    tienda,
                                    servicioTasa,
                                    controladorUsuario,
                                    controladorProducto,
                                    controladorBooster,
                                    controladorTorneo);
                        }
                    } else if (usuarioActual instanceof Cliente cliente) {
                        mostrarMenuCliente();
                        Integer opcion = leerEntero(scanner, "Seleccione una opción: ");
                        if (opcion == null) {
                            ejecutando = false;
                        } else if (opcion == 6) {
                            usuarioActual = null;
                            System.out.println("Sesión cerrada.");
                        } else {
                            ejecutarOpcionCliente(
                                    opcion,
                                    scanner,
                                    cliente,
                                    tienda,
                                    servicioTasa,
                                    controladorProducto,
                                    controladorBooster,
                                    controladorCompra,
                                    controladorTorneo,
                                    controladorUsuario);
                        }
                    }
                } catch (FinEntradaException exception) {
                    ejecutando = false;
                }
            }
        }
        System.out.println("Programa finalizado.");
    }

    private static void mostrarMenuPrincipal() {
        System.out.println("\n=== Menú principal ===");
        System.out.println("1. Iniciar sesión");
        System.out.println("0. Salir");
        System.out.println("Administrador por defecto: admin / admin123");
    }

    private static void mostrarMenuAdministrador() {
        System.out.println("\n=== Menú de Administrador ===");
        System.out.println("1. Registrar nuevo Cliente");
        System.out.println("2. Registrar Carta");
        System.out.println("3. Registrar Producto Sellado");
        System.out.println("4. Registrar Accesorio");
        System.out.println("5. Crear Booster");
        System.out.println("6. Crear Torneo");
        System.out.println("7. Consultar Inventario de la Tienda");
        System.out.println("8. Cerrar sesión");
    }

    private static void mostrarMenuCliente() {
        System.out.println("\n=== Menú de Cliente ===");
        System.out.println("1. Consultar Productos disponibles en la Tienda");
        System.out.println("2. Comprar Producto o Booster");
        System.out.println("3. Abrir un Booster de mi inventario");
        System.out.println("4. Consultar mi Historial de Compras e Inventario personal");
        System.out.println("5. Ver Torneos e Inscribirme a un Torneo");
        System.out.println("6. Cerrar sesión");
    }

    private static Usuario iniciarSesion(
            Scanner scanner,
            ControladorAutenticacion controladorAutenticacion) {
        String username = leerTexto(scanner, "Username: ");
        String password = leerTexto(scanner, "Password: ");
        try {
            Usuario usuario = controladorAutenticacion.login(username, password);
            System.out.println("Sesión iniciada como " + usuario.getNombre() + " (" + usuario.getRol() + ").");
            return usuario;
        } catch (RuntimeException exception) {
            mostrarError(exception);
            return null;
        }
    }

    private static void ejecutarOpcionAdministrador(
            int opcion,
            Scanner scanner,
            Usuario administrador,
            Tienda tienda,
            ServicioTasaCambio servicioTasa,
            ControladorUsuario controladorUsuario,
            ControladorProducto controladorProducto,
            ControladorBooster controladorBooster,
            ControladorTorneo controladorTorneo) {
        switch (opcion) {
            case 1 -> registrarCliente(scanner, administrador, controladorUsuario);
            case 2 -> registrarCarta(scanner, administrador, controladorProducto);
            case 3 -> registrarProductoSellado(scanner, administrador, controladorProducto);
            case 4 -> registrarAccesorio(scanner, administrador, controladorProducto);
            case 5 -> crearBooster(scanner, administrador, controladorBooster);
            case 6 -> crearTorneo(scanner, administrador, controladorTorneo);
            case 7 -> listarInventarioTienda(tienda, servicioTasa, controladorProducto);
            default -> System.out.println("Opción no válida.");
        }
    }

    private static void registrarCliente(
            Scanner scanner, Usuario administrador, ControladorUsuario controladorUsuario) {
        String id = leerTexto(scanner, "ID del cliente: ");
        String nombre = leerTexto(scanner, "Nombre: ");
        String username = leerTexto(scanner, "Username: ");
        String password = leerTexto(scanner, "Password: ");
        try {
            Cliente cliente = controladorUsuario.registrarCliente(administrador, id, nombre, username, password);
            System.out.println("Cliente registrado: " + cliente.getNombre() + " (" + cliente.getId() + ").");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void registrarCarta(
            Scanner scanner, Usuario administrador, ControladorProducto controladorProducto) {
        String id = leerTexto(scanner, "ID de la carta: ");
        String nombre = leerTexto(scanner, "Nombre: ");
        Rareza rareza = leerRareza(scanner);
        BigDecimal precioUSD = leerBigDecimal(scanner, "Precio base USD: ");
        try {
            Carta carta = controladorProducto.registrarCarta(administrador, id, nombre, rareza, precioUSD);
            System.out.println("Carta registrada: " + carta.getNombre() + " (" + carta.getRareza() + ").");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void registrarProductoSellado(
            Scanner scanner, Usuario administrador, ControladorProducto controladorProducto) {
        String id = leerTexto(scanner, "ID del producto: ");
        String nombre = leerTexto(scanner, "Nombre: ");
        String descripcion = leerTexto(scanner, "Descripción: ");
        BigDecimal precioUSD = leerBigDecimal(scanner, "Precio USD: ");
        try {
            ProductoSellado producto = controladorProducto.registrarProductoSellado(
                    administrador, id, nombre, precioUSD, descripcion);
            System.out.println("Producto sellado registrado: " + producto.getNombre() + ".");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void registrarAccesorio(
            Scanner scanner, Usuario administrador, ControladorProducto controladorProducto) {
        String id = leerTexto(scanner, "ID del accesorio: ");
        String nombre = leerTexto(scanner, "Nombre: ");
        String tipo = leerTexto(scanner, "Tipo: ");
        String descripcion = leerTexto(scanner, "Descripción: ");
        BigDecimal precioUSD = leerBigDecimal(scanner, "Precio USD: ");
        try {
            Accesorio accesorio = controladorProducto.registrarAccesorio(
                    administrador, id, nombre, precioUSD, tipo, descripcion);
            System.out.println("Accesorio registrado: " + accesorio.getNombre() + ".");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void crearBooster(
            Scanner scanner, Usuario administrador, ControladorBooster controladorBooster) {
        String id = leerTexto(scanner, "ID del booster: ");
        String nombre = leerTexto(scanner, "Nombre: ");
        BigDecimal precioUSD = leerBigDecimal(scanner, "Precio base USD: ");
        try {
            Booster booster = controladorBooster.crearBooster(administrador, id, nombre, precioUSD);
            System.out.println("Booster creado: " + booster.getNombre()
                    + " con " + Booster.CANTIDAD_CARTAS + " cartas.");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void crearTorneo(
            Scanner scanner, Usuario administrador, ControladorTorneo controladorTorneo) {
        String id = leerTexto(scanner, "ID del torneo: ");
        String nombre = leerTexto(scanner, "Nombre: ");
        LocalDate fecha = leerFecha(scanner);
        Integer cupo = leerEntero(scanner, "Cupo máximo: ");
        if (cupo == null) {
            return;
        }
        try {
            Torneo torneo = controladorTorneo.crearTorneo(administrador, id, nombre, fecha, cupo);
            System.out.println("Torneo creado: " + torneo.getNombre() + " (" + torneo.getFecha() + ").");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void listarInventarioTienda(
            Tienda tienda,
            ServicioTasaCambio servicioTasa,
            ControladorProducto controladorProducto) {
        try {
            List<Producto> productos = controladorProducto.listarInventarioDisponible();
            mostrarProductos(productos, servicioTasa, "Inventario de la tienda");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void ejecutarOpcionCliente(
            int opcion,
            Scanner scanner,
            Cliente cliente,
            Tienda tienda,
            ServicioTasaCambio servicioTasa,
            ControladorProducto controladorProducto,
            ControladorBooster controladorBooster,
            ControladorCompra controladorCompra,
            ControladorTorneo controladorTorneo,
            ControladorUsuario controladorUsuario) {
        switch (opcion) {
            case 1 -> listarProductosTienda(servicioTasa, controladorProducto);
            case 2 -> comprarProducto(scanner, cliente, controladorCompra);
            case 3 -> abrirBooster(scanner, cliente, controladorBooster);
            case 4 -> mostrarHistorialEInventario(
                    cliente, servicioTasa, controladorUsuario);
            case 5 -> mostrarTorneosEInscribir(scanner, cliente, tienda, controladorTorneo);
            default -> System.out.println("Opción no válida.");
        }
    }

    private static void listarProductosTienda(
            ServicioTasaCambio servicioTasa, ControladorProducto controladorProducto) {
        try {
            List<Producto> productos = controladorProducto.listarInventarioDisponible();
            mostrarProductos(productos, servicioTasa, "Productos disponibles en la tienda");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void comprarProducto(
            Scanner scanner, Cliente cliente, ControladorCompra controladorCompra) {
        String productoId = leerTexto(scanner, "ID del producto que desea comprar: ");
        try {
            Compra compra = controladorCompra.procesarCompra(cliente, productoId);
            System.out.println("Compra realizada. Total: " + compra.calcularTotal() + " COP.");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void abrirBooster(
            Scanner scanner, Cliente cliente, ControladorBooster controladorBooster) {
        String boosterId = leerTexto(scanner, "ID del booster que desea abrir: ");
        try {
            List<Carta> cartas = controladorBooster.abrirBooster(cliente, boosterId);
            System.out.println("Cartas obtenidas:");
            for (Carta carta : cartas) {
                System.out.println("- " + carta.getNombre() + " [" + carta.getRareza() + "]");
            }
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void mostrarHistorialEInventario(
            Cliente cliente,
            ServicioTasaCambio servicioTasa,
            ControladorUsuario controladorUsuario) {
        try {
            List<Compra> compras = controladorUsuario.consultarHistorialCompras(cliente, cliente.getId());
            System.out.println("\nHistorial de compras:");
            if (compras.isEmpty()) {
                System.out.println("(sin compras)");
            }
            for (Compra compra : compras) {
                System.out.println("- " + compra.getFecha() + " | " + compra.getId()
                        + " | " + compra.calcularTotal() + " COP");
                compra.getProductos().forEach(producto ->
                        System.out.println("  Producto: " + producto.getNombre() + " [" + producto.getId() + "]"));
            }

            var inventario = cliente.getInventario().listarProductos();
            var tasa = servicioTasa.consultarTasaUSDACOP();
            System.out.println("\nInventario personal | Fuente de tasa: " + tasa.fuente());
            if (inventario.isEmpty()) {
                System.out.println("(vacío)");
            }
            for (Producto producto : inventario) {
                imprimirProducto(producto, tasa.tasa());
                if (producto instanceof Booster booster && booster.estaAbierto()) {
                    for (Carta carta : booster.getCartas()) {
                        System.out.println("  - " + carta.getNombre() + " [" + carta.getRareza() + "]");
                    }
                }
            }
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void mostrarTorneosEInscribir(
            Scanner scanner,
            Cliente cliente,
            Tienda tienda,
            ControladorTorneo controladorTorneo) {
        List<Torneo> torneos = tienda.getTorneos();
        System.out.println("\nTorneos disponibles:");
        if (torneos.isEmpty()) {
            System.out.println("(no hay torneos registrados)");
            return;
        }
        for (Torneo torneo : torneos) {
            System.out.println("- " + torneo.getId() + " | " + torneo.getNombre()
                    + " | " + torneo.getFecha() + " | cupos disponibles: "
                    + (torneo.getMaxParticipantes() - torneo.consultarParticipantes().size())
                    + "/" + torneo.getMaxParticipantes()
                    + (cliente.estaInscritoEn(torneo.getId()) ? " | ya inscrito" : ""));
        }
        String torneoId = leerTexto(scanner, "ID del torneo para inscribirse (Enter para volver): ");
        if (torneoId.isBlank()) {
            return;
        }
        try {
            boolean inscrito = controladorTorneo.inscribirCliente(cliente, torneoId);
            System.out.println(inscrito ? "Inscripción realizada." : "No se pudo completar la inscripción.");
        } catch (RuntimeException exception) {
            mostrarError(exception);
        }
    }

    private static void mostrarProductos(
            List<Producto> productos,
            ServicioTasaCambio servicioTasa,
            String titulo) {
        var tasa = servicioTasa.consultarTasaUSDACOP();
        System.out.println("\n" + titulo + " | Fuente de tasa: " + tasa.fuente()
                + " | USD/COP: " + tasa.tasa());
        if (productos.isEmpty()) {
            System.out.println("(sin productos disponibles)");
            return;
        }
        for (Producto producto : productos) {
            imprimirProducto(producto, tasa.tasa());
        }
    }

    private static void imprimirProducto(Producto producto, BigDecimal tasa) {
        String tipo;
        if (producto instanceof Carta carta) {
            tipo = "Carta " + carta.getRareza();
        } else if (producto instanceof Booster booster) {
            tipo = "Booster (" + (booster.estaAbierto() ? "abierto" : "cerrado") + ")";
        } else if (producto instanceof ProductoSellado) {
            tipo = "Producto sellado";
        } else if (producto instanceof Accesorio) {
            tipo = "Accesorio";
        } else {
            tipo = producto.getClass().getSimpleName();
        }
        BigDecimal precioCOP = producto.calcularPrecioCOP(tasa);
        System.out.println("- " + producto.getId() + " | " + producto.getNombre()
                + " | " + tipo + " | USD " + producto.getPrecioBaseUSD()
                + " | COP " + precioCOP);
    }

    private static Rareza leerRareza(Scanner scanner) {
        while (true) {
            System.out.println("Rareza: " + String.join(", ", OPCIONES_RAREZA));
            String valor = leerTexto(scanner, "Rareza: ").trim().toUpperCase();
            try {
                return Rareza.valueOf(valor);
            } catch (IllegalArgumentException exception) {
                System.out.println("Rareza no válida; seleccione una de las opciones indicadas.");
            }
        }
    }

    private static LocalDate leerFecha(Scanner scanner) {
        while (true) {
            String valor = leerTexto(scanner, "Fecha (YYYY-MM-DD): ");
            try {
                return LocalDate.parse(valor);
            } catch (DateTimeParseException exception) {
                System.out.println("Fecha no válida. Use el formato YYYY-MM-DD.");
            }
        }
    }

    private static BigDecimal leerBigDecimal(Scanner scanner, String mensaje) {
        while (true) {
            String valor = leerTexto(scanner, mensaje);
            try {
                return new BigDecimal(valor);
            } catch (NumberFormatException exception) {
                System.out.println("Ingrese un número válido, por ejemplo 12.50.");
            }
        }
    }

    private static Integer leerEntero(Scanner scanner, String mensaje) {
        while (scanner.hasNextLine()) {
            System.out.print(mensaje);
            String valor = scanner.nextLine().trim();
            try {
                return Integer.valueOf(valor);
            } catch (NumberFormatException exception) {
                System.out.println("Ingrese una opción numérica válida.");
            }
        }
        return null;
    }

    private static String leerTexto(Scanner scanner, String mensaje) {
        System.out.print(mensaje);
        if (!scanner.hasNextLine()) {
            throw new FinEntradaException();
        }
        return scanner.nextLine().trim();
    }

    private static void mostrarError(RuntimeException exception) {
        String mensaje = exception.getMessage();
        System.out.println("Error: " + (mensaje == null || mensaje.isBlank()
                ? exception.getClass().getSimpleName()
                : mensaje));
    }

    private static final class FinEntradaException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
