package com.mycompany.proyectofinaldesarollosoft.controller;

import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import com.mycompany.proyectofinaldesarollosoft.model.Accesorio;
import com.mycompany.proyectofinaldesarollosoft.model.Carta;
import com.mycompany.proyectofinaldesarollosoft.model.Producto;
import com.mycompany.proyectofinaldesarollosoft.model.ProductoSellado;
import com.mycompany.proyectofinaldesarollosoft.model.Tienda;
import com.mycompany.proyectofinaldesarollosoft.model.Usuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ControladorProducto {
    private final Tienda tienda;

    public ControladorProducto(Tienda tienda) {
        this.tienda = Objects.requireNonNull(tienda, "tienda");
    }

    public Carta registrarCarta(Usuario actor, String id, String nombre, Rareza rareza, BigDecimal precioBaseUSD) {
        tienda.validarAdministrador(actor);
        Carta carta = new Carta(id, nombre, rareza, precioBaseUSD);
        tienda.registrarProducto(actor, carta);
        return carta;
    }

    public ProductoSellado registrarProductoSellado(
            Usuario actor, String id, String nombre, BigDecimal precioBaseUSD, String descripcion) {
        tienda.validarAdministrador(actor);
        ProductoSellado producto = new ProductoSellado(id, nombre, precioBaseUSD, descripcion);
        tienda.registrarProducto(actor, producto);
        return producto;
    }

    public Accesorio registrarAccesorio(
            Usuario actor,
            String id,
            String nombre,
            BigDecimal precioBaseUSD,
            String tipo,
            String descripcion) {
        tienda.validarAdministrador(actor);
        Accesorio accesorio = new Accesorio(id, nombre, precioBaseUSD, tipo, descripcion);
        tienda.registrarProducto(actor, accesorio);
        return accesorio;
    }

    public List<Producto> listarInventarioDisponible() {
        return tienda.getInventario().listarDisponibles();
    }
}
