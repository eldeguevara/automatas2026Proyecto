package com.automatas.expendedora.json;

import java.util.List;
import java.util.Map;

/** Valor leído de un JSON, con la posición donde empieza para reportar errores. */
public record ValorJson(Tipo tipo, Object valor, int linea, int columna) {

    public enum Tipo {
        OBJETO, ARREGLO, CADENA, NUMERO, BOOLEANO, NULO
    }

    @SuppressWarnings("unchecked")
    public Map<String, ValorJson> comoObjeto() {
        return (Map<String, ValorJson>) valor;
    }

    @SuppressWarnings("unchecked")
    public List<ValorJson> comoArreglo() {
        return (List<ValorJson>) valor;
    }

    /** Texto de una cadena, o el número tal como se escribió. */
    public String comoTexto() {
        return (String) valor;
    }
}
