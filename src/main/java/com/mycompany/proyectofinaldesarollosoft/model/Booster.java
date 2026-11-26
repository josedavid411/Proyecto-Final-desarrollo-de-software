package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import com.mycompany.proyectofinaldesarollosoft.excepcion.ConflictoException;
import com.mycompany.proyectofinaldesarollosoft.excepcion.ProhibidoException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Booster extends Producto {
    public static final int CANTIDAD_CARTAS = 10;

    private final List<Carta> cartas;
    private boolean abierto;
    private String propietarioId;

    public Booster(String id, String nombre, BigDecimal precioBaseUSD, List<Carta> cartasDisponibles) {
        super(id, nombre, precioBaseUSD);
        this.cartas = generarContenido(cartasDisponibles, Rareza::sortear, new Random());
    }

    private Booster(
            String id,
            String nombre,
            BigDecimal precioBaseUSD,
            List<Carta> cartas,
            boolean abierto,
            String propietarioId,
            BigDecimal precioCOP,
            boolean disponible) {
        super(id, nombre, precioBaseUSD);
        if (cartas == null || cartas.size() != CANTIDAD_CARTAS || cartas.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Un booster debe contener exactamente diez cartas.");
        }
        this.cartas = List.copyOf(cartas);
        this.abierto = abierto;
        this.propietarioId = propietarioId;
        actualizarPrecio(precioCOP);
        if (!disponible) {
            marcarNoDisponible();
        }
    }

    public static Booster restaurar(
            String id,
            String nombre,
            BigDecimal precioBaseUSD,
            List<Carta> cartas,
            boolean abierto,
            String propietarioId,
            BigDecimal precioCOP,
            boolean disponible) {
        return new Booster(id, nombre, precioBaseUSD, cartas, abierto, propietarioId, precioCOP, disponible);
    }

    private static List<Carta> generarContenido(
            List<Carta> cartasDisponibles,
            java.util.function.Supplier<Rareza> sorteoRareza,
            Random random) {
        if (cartasDisponibles == null || cartasDisponibles.isEmpty()) {
            throw new ConflictoException("No hay cartas registradas para crear el booster.");
        }
        if (cartasDisponibles.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("La lista de cartas no puede contener valores nulos.");
        }

        List<Carta> contenido = new ArrayList<>(CANTIDAD_CARTAS);
        for (int i = 0; i < CANTIDAD_CARTAS; i++) {
            Rareza sorteada = sorteoRareza.get();
            List<Carta> candidatas = cartasDisponibles.stream()
                    .filter(carta -> carta.getRareza() == sorteada)
                    .toList();
            if (candidatas.isEmpty()) {
                candidatas = buscarCartasCercanasInferiores(cartasDisponibles, sorteada);
            }
            if (candidatas.isEmpty()) {
                candidatas = buscarCartasCercanasSuperiores(cartasDisponibles, sorteada);
            }
            if (candidatas.isEmpty()) {
                throw new IllegalStateException("No fue posible seleccionar una carta para el booster.");
            }
            contenido.add(candidatas.get(random.nextInt(candidatas.size())));
        }
        if (!tieneCartaRara(contenido)) {
            Rareza rarezaAlta = sortearRarezaAltaDisponible(cartasDisponibles, random);
            if (rarezaAlta != null) {
                List<Carta> cartasRaras = filtrarRareza(cartasDisponibles, rarezaAlta);
                contenido.set(CANTIDAD_CARTAS - 1, cartasRaras.get(random.nextInt(cartasRaras.size())));
            }
        }
        return List.copyOf(contenido);
    }

    private static boolean tieneCartaRara(List<Carta> cartas) {
        for (Carta carta : cartas) {
            if (carta.getRareza().getOrden() >= Rareza.RARO.getOrden()) {
                return true;
            }
        }
        return false;
    }

    private static Rareza sortearRarezaAltaDisponible(List<Carta> cartas, Random random) {
        int pesoTotal = 0;
        for (Rareza rareza : Rareza.values()) {
            if (rareza.getOrden() >= Rareza.RARO.getOrden() && hayCartaDeRareza(cartas, rareza)) {
                pesoTotal += rareza.getProbabilidad();
            }
        }
        if (pesoTotal == 0) {
            return null;
        }

        int valor = random.nextInt(pesoTotal);
        int pesoAcumulado = 0;
        for (Rareza rareza : Rareza.values()) {
            if (rareza.getOrden() >= Rareza.RARO.getOrden() && hayCartaDeRareza(cartas, rareza)) {
                pesoAcumulado += rareza.getProbabilidad();
                if (valor < pesoAcumulado) {
                    return rareza;
                }
            }
        }
        return null;
    }

    private static boolean hayCartaDeRareza(List<Carta> cartas, Rareza rareza) {
        for (Carta carta : cartas) {
            if (carta.getRareza() == rareza) {
                return true;
            }
        }
        return false;
    }

    private static List<Carta> buscarCartasCercanasInferiores(List<Carta> cartas, Rareza rareza) {
        for (Rareza inferior = rareza.getRarezaInferiorMasCercana();
                inferior != null;
                inferior = inferior.getRarezaInferiorMasCercana()) {
            List<Carta> candidatas = filtrarRareza(cartas, inferior);
            if (!candidatas.isEmpty()) {
                return candidatas;
            }
        }
        return List.of();
    }

    private static List<Carta> buscarCartasCercanasSuperiores(List<Carta> cartas, Rareza rareza) {
        for (Rareza superior = rareza.getRarezaSuperiorMasCercana();
                superior != null;
                superior = superior.getRarezaSuperiorMasCercana()) {
            List<Carta> candidatas = filtrarRareza(cartas, superior);
            if (!candidatas.isEmpty()) {
                return candidatas;
            }
        }
        return List.of();
    }

    private static List<Carta> filtrarRareza(List<Carta> cartas, Rareza rareza) {
        return cartas.stream()
                .filter(carta -> carta.getRareza() == rareza)
                .sorted(Comparator.comparing(Carta::getId))
                .toList();
    }

    List<Carta> abrir(String clienteId) {
        if (propietarioId == null || !propietarioId.equals(clienteId)) {
            throw new ProhibidoException("Solo el cliente propietario puede abrir este booster.");
        }
        if (abierto) {
            throw new ConflictoException("El booster ya fue abierto.");
        }
        abierto = true;
        return cartas;
    }

    public boolean estaAbierto() {
        return abierto;
    }

    public List<Carta> getCartas() {
        if (!abierto) {
            throw new IllegalStateException("El contenido solo se muestra al abrir el booster.");
        }
        return cartas;
    }

    public List<Carta> getCartasParaPersistencia() {
        return cartas;
    }

    public String getPropietarioId() {
        return propietarioId;
    }

    public void asignarPropietario(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El id del propietario es obligatorio.");
        }
        if (propietarioId != null && !propietarioId.equals(clienteId)) {
            throw new ConflictoException("El booster ya fue vendido a otro cliente.");
        }
        propietarioId = clienteId;
    }
}
