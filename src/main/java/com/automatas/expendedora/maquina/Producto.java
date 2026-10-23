package com.automatas.expendedora.maquina;

public class Producto {

    private final String codigo;
    private final String nombre;
    private final int precio;
    private int cantidad;

    public Producto(String codigo, String nombre, int precio, int cantidad) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPrecio() {
        return precio;
    }

    public int getCantidad() {
        return cantidad;
    }

    void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    Producto copia() {
        return new Producto(codigo, nombre, precio, cantidad);
    }
}
