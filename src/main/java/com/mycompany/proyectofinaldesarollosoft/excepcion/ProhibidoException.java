package com.mycompany.proyectofinaldesarollosoft.excepcion;

public class ProhibidoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProhibidoException(String mensaje) {
        super(mensaje);
    }
}
