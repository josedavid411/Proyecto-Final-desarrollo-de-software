package com.mycompany.proyectofinaldesarollosoft.servicio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServicioTasaCambio {
    private static final Logger LOGGER = Logger.getLogger(ServicioTasaCambio.class.getName());
    private static final URI API_URI = URI.create("https://open.er-api.com/v6/latest/USD");
    private static final BigDecimal TASA_POR_DEFECTO = new BigDecimal("4000.00");

    private final HttpClient clienteHttp = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public BigDecimal obtenerTasaUSDACOP() {
        HttpRequest solicitud = HttpRequest.newBuilder(API_URI)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> respuesta = clienteHttp.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                return usarTasaPorDefecto("La API respondió con estado " + respuesta.statusCode() + ".");
            }
            return leerTasa(respuesta.body());
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "No se pudo consultar la tasa de cambio; se usará la tasa de respaldo.", exception);
            return TASA_POR_DEFECTO;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "La consulta de tasa de cambio fue interrumpida; se usará la tasa de respaldo.", exception);
            return TASA_POR_DEFECTO;
        }
    }

    private BigDecimal leerTasa(String cuerpo) {
        try {
            JsonElement respuesta = JsonParser.parseString(cuerpo);
            if (!respuesta.isJsonObject()) {
                return usarTasaPorDefecto("La respuesta de la API no es un objeto JSON.");
            }
            JsonObject objetoRespuesta = respuesta.getAsJsonObject();
            JsonElement tasas = objetoRespuesta.get("rates");
            if (tasas == null || !tasas.isJsonObject()) {
                return usarTasaPorDefecto("La respuesta de la API no contiene las tasas de cambio.");
            }
            JsonElement cop = tasas.getAsJsonObject().get("COP");
            if (cop == null || !cop.isJsonPrimitive() || !cop.getAsJsonPrimitive().isNumber()) {
                return usarTasaPorDefecto("La respuesta de la API no contiene una tasa COP válida.");
            }
            BigDecimal tasa = cop.getAsBigDecimal();
            if (tasa.signum() <= 0) {
                return usarTasaPorDefecto("La tasa COP recibida no es positiva.");
            }
            return tasa;
        } catch (JsonParseException | NumberFormatException exception) {
            LOGGER.log(Level.WARNING, "La respuesta de la API de tasa de cambio no es válida; se usará la tasa de respaldo.", exception);
            return TASA_POR_DEFECTO;
        }
    }

    private BigDecimal usarTasaPorDefecto(String motivo) {
        LOGGER.warning(motivo + " Se usará la tasa de cambio por defecto.");
        return TASA_POR_DEFECTO;
    }
}
