package com.automatas.expendedora.sintactico;

import com.automatas.expendedora.lexico.TipoToken;
import com.automatas.expendedora.lexico.Token;
import java.util.List;

/** Instrucción que pasó el análisis sintáctico. */
public record Instruccion(TipoToken tipo, List<Token> parametros, int linea, int columna) {

    public String parametro(int indice) {
        return parametros.get(indice).lexema();
    }

    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder(tipo.name()).append('(');
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) {
                texto.append(',');
            }
            texto.append(parametros.get(i).lexema());
        }
        return texto.append(");").toString();
    }
}
