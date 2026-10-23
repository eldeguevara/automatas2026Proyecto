package com.automatas.expendedora.json;

/** Error de sintaxis en un archivo JSON. */
public class ErrorJson extends Exception {

    private static final long serialVersionUID = 1L;

    public ErrorJson(int linea, int columna, String mensaje) {
        super("Linea " + linea + ", columna " + columna + ": " + mensaje);
    }
}
