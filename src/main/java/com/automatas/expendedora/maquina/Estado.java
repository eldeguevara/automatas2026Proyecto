package com.automatas.expendedora.maquina;

/** Q: estados del autómata de compra (numeral 5.1 del enunciado). */
public enum Estado {
    E0("Esperando dinero"),
    E1("Dinero ingresado"),
    E2("Producto seleccionado"),
    E3("Procesando compra"),
    E4("Entregando producto"),
    E5("Devolviendo dinero");

    private final String descripcion;

    Estado(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
