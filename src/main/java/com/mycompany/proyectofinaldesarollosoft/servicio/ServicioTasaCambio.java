package com.mycompany.proyectofinaldesarollosoft.servicio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.IOException;
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

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private final BigDecimal tasaDefecto;
    private final Supplier<BigDecimal> consultaTasa;
    private volatile ResultadoTasa ultimaConsulta;

    public ServicioTasaCambio() {
        this(leerTasaDefecto(), ServicioTasaCambio::consultarTasaReal);
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
        } catch (RuntimeException exception) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
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

    private static BigDecimal leerTasaDefecto() {
        String valor = System.getenv("TASA_DEFECTO");
        if (valor != null) {
            try {
                BigDecimal tasa = new BigDecimal(valor.trim());
                if (tasa.signum() > 0) {
                    return tasa;
                }
            } catch (NumberFormatException ignored) {
                // Un valor ausente o inválido utiliza la tasa solicitada por defecto.
            }
        }
        return new BigDecimal("4000.00");
    }

    private static BigDecimal consultarTasaReal() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://open.er-api.com/v6/latest/USD"))
                .timeout(TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("La API de tasa de cambio respondió HTTP " + response.statusCode());
            }
            JsonObject contenido = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!contenido.has("rates") || !contenido.getAsJsonObject("rates").has("COP")) {
                throw new IllegalStateException("La respuesta de tasa de cambio no contiene COP.");
            }
            return contenido.getAsJsonObject("rates").get("COP").getAsBigDecimal();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo consultar la tasa de cambio.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La consulta de tasa de cambio fue interrumpida.", exception);
        }
    }
}
