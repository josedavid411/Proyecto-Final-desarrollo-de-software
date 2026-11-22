package com.mycompany.proyectofinaldesarollosoft.servicio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Supplier;

public class ServicioTasaCambio {
    public enum Fuente {
        API,
        DEFECTO
    }

    public record ResultadoTasa(BigDecimal tasa, Fuente fuente) {
        public ResultadoTasa {
            if (tasa == null || tasa.signum() <= 0) {
                throw new IllegalArgumentException("La tasa debe ser positiva.");
            }
            Objects.requireNonNull(fuente, "fuente");
        }
    }

    private static final BigDecimal TASA_SIMULADA = new BigDecimal("4000.00");

    private final BigDecimal tasaDefecto;
    private final Supplier<BigDecimal> consultaTasa;
    private ResultadoTasa ultimaConsulta;

    public ServicioTasaCambio() {
        this(new BigDecimal("4000.00"), () -> TASA_SIMULADA);
    }

    public ServicioTasaCambio(BigDecimal tasaDefecto, Supplier<BigDecimal> consultaTasa) {
        if (tasaDefecto == null || tasaDefecto.signum() <= 0) {
            throw new IllegalArgumentException("La tasa por defecto debe ser positiva.");
        }
        this.tasaDefecto = tasaDefecto;
        this.consultaTasa = Objects.requireNonNull(consultaTasa, "consultaTasa");
    }

    public ResultadoTasa consultarTasaUSDACOP() {
        try {
            BigDecimal tasa = consultaTasa.get();
            if (tasa != null && tasa.signum() > 0) {
                ultimaConsulta = new ResultadoTasa(tasa, Fuente.API);
                return ultimaConsulta;
            }
        } catch (RuntimeException ignored) {
            // Una falla de la consulta se representa mediante la tasa de respaldo.
        }
        System.out.println("La API no respondió, por lo que se toma un valor por defecto de " + tasaDefecto);
        ultimaConsulta = new ResultadoTasa(tasaDefecto, Fuente.DEFECTO);
        return ultimaConsulta;
    }

    public BigDecimal obtenerTasaUSDACOP() {
        return consultarTasaUSDACOP().tasa();
    }

    public BigDecimal getTasaDefecto() {
        return tasaDefecto;
    }

    public Fuente getFuenteUltimaConsulta() {
        return ultimaConsulta == null ? null : ultimaConsulta.fuente();
    }
}
