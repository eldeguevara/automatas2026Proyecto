package com.automatas.expendedora.maquina;

/**
 * Σ: entradas del autómata de compra. Las cuatro primeras vienen de las instrucciones del
 * usuario; las demás las genera la máquina al procesar la compra o devolver el dinero.
 */
public enum Entrada {
    MONEDA("MONEDA"),
    SELECCIONAR("SELECCIONAR"),
    COMPRAR("COMPRAR"),
    CANCELAR("CANCELAR"),
    COMPRA_VALIDA("Compra válida"),
    COMPRA_RECHAZADA("Compra rechazada"),
    CON_CAMBIO("Con cambio"),
    FINALIZAR("Finalizar / reiniciar");

    private final String etiqueta;

    Entrada(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
