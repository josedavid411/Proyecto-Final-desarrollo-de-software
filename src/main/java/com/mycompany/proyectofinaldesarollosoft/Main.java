package com.mycompany.proyectofinaldesarollosoft;

import com.mycompany.proyectofinaldesarollosoft.model.Tienda;

public class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Tienda tienda = new Tienda();
        new Rutas(tienda).iniciar(leerPuerto());
    }

    private static int leerPuerto() {
        String valor = System.getenv("PORT");
        if (valor != null) {
            try {
                int puerto = Integer.parseInt(valor);
                if (puerto > 0 && puerto <= 65535) {
                    return puerto;
                }
            } catch (NumberFormatException ignored) {
                // Si PORT no es válido, se usa el puerto 4567.
            }
        }
        return 4567;
    }
}
