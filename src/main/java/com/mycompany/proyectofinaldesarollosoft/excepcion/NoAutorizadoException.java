package com.mycompany.proyectofinaldesarollosoft.excepcion;

public class NoAutorizadoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
