package com.automatas.expendedora.lexico;

public enum TipoToken {
    PRODUCTO("Reservado_PRODUCTO"),
    CARGAR("Reservado_CARGAR"),
    MONEDA("Reservado_MONEDA"),
    SELECCIONAR("Reservado_SELECCIONAR"),
    COMPRAR("Reservado_COMPRAR"),
    CANCELAR("Reservado_CANCELAR"),
    MOSTRAR("Reservado_MOSTRAR"),
    IDENTIFICADOR("Identificador"),
    NUMERO("Numero"),
    PARENTESIS_IZQ("Simbolo_parentesis_izq"),
    PARENTESIS_DER("Simbolo_parentesis_dcho"),
    COMA("Simbolo_coma"),
    PUNTO_Y_COMA("Simbolo_punto_y_coma"),
    FIN("Fin_de_archivo");

    private final String nombre;

    TipoToken(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esReservada() {
        return ordinal() <= MOSTRAR.ordinal();
    }

    /** Devuelve la palabra reservada que coincide exactamente con el lexema, o null. */
    public static TipoToken buscarReservada(String lexema) {
        for (TipoToken tipo : values()) {
            if (tipo.esReservada() && tipo.name().equals(lexema)) {
                return tipo;
            }
        }
        return null;
    }
}
