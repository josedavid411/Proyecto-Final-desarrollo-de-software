package com.mycompany.proyectofinaldesarollosoft.enums;

import java.util.concurrent.ThreadLocalRandom;

public enum Rareza {
    NORMAL("N", 0.50, 0),
    POCO_COMUN("UC", 0.25, 1),
    RARO("R", 0.15, 2),
    SUPER_RARO("SR", 0.07, 3),
    ULTRA_RARO("UR", 0.03, 4);

    private final String codigo;
    private final double probabilidad;
    private final int orden;

    Rareza(String codigo, double probabilidad, int orden) {
        this.codigo = codigo;
        this.probabilidad = probabilidad;
        this.orden = orden;
    }

    public String getCodigo() {
        return codigo;
    }

    public double getProbabilidad() {
        return probabilidad;
    }

    public int getOrden() {
        return orden;
    }

    public static Rareza sortear() {
        double valor = ThreadLocalRandom.current().nextDouble();
        double acumulado = 0;
        for (Rareza rareza : values()) {
            acumulado += rareza.probabilidad;
            if (valor < acumulado) {
                return rareza;
            }
        }
        throw new IllegalStateException("Las probabilidades de rareza deben sumar 1.");
    }
}
