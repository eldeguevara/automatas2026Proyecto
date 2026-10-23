package com.automatas.expendedora.maquina;

/** Operación bien escrita pero inválida para el estado actual de la máquina. */
public class ErrorSemantico extends Exception {

    private static final long serialVersionUID = 1L;

    public ErrorSemantico(String mensaje) {
        super(mensaje);
    }
}
