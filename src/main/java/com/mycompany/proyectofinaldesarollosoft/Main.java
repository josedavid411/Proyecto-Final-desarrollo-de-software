package com.mycompany.proyectofinaldesarollosoft;

import com.mycompany.proyectofinaldesarollosoft.controller.ControladorAutenticacion;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorBooster;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorCompra;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorProducto;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorTorneo;
import com.mycompany.proyectofinaldesarollosoft.controller.ControladorUsuario;
import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import com.mycompany.proyectofinaldesarollosoft.model.Accesorio;
import com.mycompany.proyectofinaldesarollosoft.model.Administrador;
import com.mycompany.proyectofinaldesarollosoft.model.Booster;
import com.mycompany.proyectofinaldesarollosoft.model.Carta;
import com.mycompany.proyectofinaldesarollosoft.model.Cliente;
import com.mycompany.proyectofinaldesarollosoft.model.Compra;
import com.mycompany.proyectofinaldesarollosoft.model.Tienda;
import com.mycompany.proyectofinaldesarollosoft.model.Torneo;
import com.mycompany.proyectofinaldesarollosoft.servicio.PersistenciaJson;
import com.mycompany.proyectofinaldesarollosoft.servicio.ServicioTasaCambio;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

public class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path archivo = args.length == 0 ? Path.of("data", "demostracion-tienda.json") : Path.of(args[0]);
        ServicioTasaCambio servicioTasa = new ServicioTasaCambio();
        Tienda tienda = new Tienda(servicioTasa, new PersistenciaJson(archivo));

        ControladorAutenticacion controladorAutenticacion = new ControladorAutenticacion(tienda);
        ControladorUsuario controladorUsuario = new ControladorUsuario(tienda);
        ControladorProducto controladorProducto = new ControladorProducto(tienda);
        ControladorBooster controladorBooster = new ControladorBooster(tienda);
        ControladorCompra controladorCompra = new ControladorCompra(tienda);
        ControladorTorneo controladorTorneo = new ControladorTorneo(tienda);

        Administrador admin = controladorAutenticacion.loginAdministrador(
                Tienda.ADMIN_USERNAME_POR_DEFECTO,
                Tienda.ADMIN_PASSWORD_POR_DEFECTO);
        System.out.println("Login de administrador: " + admin.getUsername());

        String sufijo = UUID.randomUUID().toString();
        Carta rara = controladorProducto.registrarCarta(
                admin, "carta-rara-" + sufijo, "Carta Rara", Rareza.RARO, new BigDecimal("1.25"));
        Carta ultraRara = controladorProducto.registrarCarta(
                admin, "carta-ultra-" + sufijo, "Carta Ultra Rara", Rareza.ULTRA_RARO, new BigDecimal("4.50"));
        System.out.println("Cartas registradas: " + rara.getRareza() + ", " + ultraRara.getRareza());

        controladorProducto.registrarProductoSellado(
                admin, "caja-" + sufijo, "Caja sellada", new BigDecimal("12.75"), "Producto de demostración");
        Accesorio accesorio = controladorProducto.registrarAccesorio(
                admin, "accesorio-" + sufijo, "Fundas", new BigDecimal("3.20"),
                "Protección", "Fundas para cartas");
        System.out.println("También se registró el accesorio: " + accesorio.getNombre());
        System.out.println("Fuente de tasa usada: " + servicioTasa.getFuenteUltimaConsulta());
        String boosterId = "booster-" + sufijo;
        Booster booster = controladorBooster.crearBooster(
                admin, boosterId, "Booster de demostración", new BigDecimal("8.50"));
        System.out.println(booster.getNombre() + " creado con " + Booster.CANTIDAD_CARTAS
                + " cartas del inventario; las cartas catalogadas siguen disponibles.");

        Cliente cliente = controladorUsuario.registrarCliente(
                admin, "Cliente de demostración", "cliente-" + sufijo, "clave-demo");
        Compra compra = controladorCompra.procesarCompra(cliente, boosterId);
        System.out.println("Compra registrada: " + compra.getId()
                + " por " + compra.calcularTotal() + " COP.");

        var cartasAbiertas = controladorBooster.abrirBooster(cliente, boosterId);
        System.out.println("El cliente abrió su booster: " + cartasAbiertas.size() + " cartas.");
        System.out.println("Raridades obtenidas: "
                + cartasAbiertas.stream().map(carta -> carta.getRareza().name()).toList());

        Torneo torneo = controladorTorneo.crearTorneo(
                admin, "torneo-" + sufijo, "Torneo de demostración", LocalDate.now().plusDays(7), 16);
        controladorTorneo.inscribirCliente(cliente, torneo.getId());
        System.out.println("Cliente inscrito en el torneo: " + torneo.getNombre());

        ServicioTasaCambio servicioFallback = new ServicioTasaCambio(
                new BigDecimal("4000.00"),
                () -> {
                    throw new IllegalStateException("Falla simulada de API");
                });
        ServicioTasaCambio.ResultadoTasa resultadoFallback = servicioFallback.consultarTasaUSDACOP();
        System.out.println("Fuente de tasa alternativa: " + resultadoFallback.fuente());

        tienda.getPersistencia().guardar(tienda);
        System.out.println("Estado guardado en: " + tienda.getPersistencia().getArchivo());
    }
}
