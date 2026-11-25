package com.mycompany.proyectofinaldesarollosoft.servicio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private static final BigDecimal TASA_FALLBACK = new BigDecimal("4000.00");
    private static final URI URL_TASA = URI.create("https://open.er-api.com/v6/latest/USD");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final BigDecimal tasaDefecto;
    private final Supplier<BigDecimal> consultaTasa;
    private volatile ResultadoTasa ultimaConsulta;

    public ServicioTasaCambio() {
        this(leerTasaDefecto(), ServicioTasaCambio::consultarApi);
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

    private static BigDecimal leerTasaDefecto() {
        String valor = System.getenv("TASA_DEFECTO");
        if (valor != null) {
            try {
                BigDecimal tasa = new BigDecimal(valor.trim());
                if (tasa.signum() > 0) {
                    return tasa;
                }
            } catch (NumberFormatException ignored) {
                // Una variable inválida usa la tasa de respaldo documentada.
            }
        }
        return TASA_FALLBACK;
    }

    private static BigDecimal consultarApi() {
        HttpRequest request = HttpRequest.newBuilder(URL_TASA)
                .timeout(Duration.ofSeconds(3))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("La API de tasa respondió con HTTP " + response.statusCode());
            }
            JsonObject respuesta = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!respuesta.has("result") || !"success".equals(respuesta.get("result").getAsString())) {
                throw new IllegalStateException("La API de tasa indicó una consulta fallida.");
            }
            JsonObject rates = respuesta.getAsJsonObject("rates");
            if (rates == null || !rates.has("COP") || rates.get("COP").isJsonNull()) {
                throw new IllegalStateException("La respuesta de la API no contiene la tasa COP.");
            }
            BigDecimal tasa = rates.get("COP").getAsBigDecimal();
            if (tasa.signum() <= 0) {
                throw new IllegalStateException("La API devolvió una tasa COP inválida.");
            }
            return tasa;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La consulta de tasa fue interrumpida.", exception);
        } catch (java.io.IOException | com.google.gson.JsonParseException exception) {
            throw new IllegalStateException("No se pudo consultar la tasa de cambio.", exception);
        }
    }
}
