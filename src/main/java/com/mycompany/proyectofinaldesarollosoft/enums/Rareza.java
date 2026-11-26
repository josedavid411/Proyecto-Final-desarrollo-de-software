package com.mycompany.proyectofinaldesarollosoft.enums;

import java.util.concurrent.ThreadLocalRandom;

public enum Rareza {
    NORMAL("N", 50, 0),
    POCO_COMUN("UC", 25, 1),
    RARO("R", 15, 2),
    SUPER_RARO("SR", 7, 3),
    ULTRA_RARO("UR", 3, 4);

    private final String codigo;
    private final int probabilidad;
    private final int orden;

    Rareza(String codigo, int probabilidad, int orden) {
        this.codigo = codigo;
        this.probabilidad = probabilidad;
        this.orden = orden;
    }

    public String getCodigo() {
        return codigo;
    }

    public int getProbabilidad() {
        return probabilidad;
    }

    public int getOrden() {
        return orden;
    }

    public Rareza getRarezaInferiorMasCercana() {
        return desdeOrden(orden - 1, -1);
    }

    public Rareza getRarezaSuperiorMasCercana() {
        return desdeOrden(orden + 1, 1);
    }

    private static Rareza desdeOrden(int ordenInicial, int incremento) {
        for (int ordenBuscado = ordenInicial;
                ordenBuscado >= 0 && ordenBuscado < values().length;
                ordenBuscado += incremento) {
            for (Rareza rareza : values()) {
                if (rareza.orden == ordenBuscado) {
                    return rareza;
                }
            }
        }
        return null;
    }

    public static Rareza sortear() {
        int valor = ThreadLocalRandom.current().nextInt(100);
        int acumulado = 0;
        for (Rareza rareza : values()) {
            acumulado += rareza.probabilidad;
            if (valor < acumulado) {
                return rareza;
            }
        }
        throw new IllegalStateException("Las probabilidades de rareza deben sumar 100.");
    }
}
