package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rareza;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Inventario {
    private final List<Producto> productos = new ArrayList<>();

    public void agregarProducto(Producto producto) {
        if (producto == null || buscarProducto(producto.getId()).isPresent()) {
            throw new IllegalArgumentException("El producto es nulo o su id ya está registrado.");
        }
        productos.add(producto);
    }

    public boolean eliminarProducto(String id) {
        return productos.removeIf(producto -> producto.getId().equals(id));
    }

    public Optional<Producto> buscarProducto(String id) {
        return productos.stream().filter(producto -> producto.getId().equals(id)).findFirst();
    }

    public List<Producto> listarProductos() {
        return List.copyOf(productos);
    }

    public List<Producto> listarDisponibles() {
        return productos.stream().filter(Producto::estaDisponible).toList();
    }

    public List<Carta> buscarCartasPorRareza(Rareza rareza) {
        return productos.stream()
                .filter(Carta.class::isInstance)
                .map(Carta.class::cast)
                .filter(carta -> carta.getRareza() == rareza)
                .toList();
    }
}
