package com.automatas.expendedora.lexico;

import com.automatas.expendedora.errores.ErrorAnalisis;
import java.util.ArrayList;
import java.util.List;

/**
 * Analizador léxico implementado como un AFD con tabla de transiciones.
 *
 * Patrones (expresiones regulares):
 *   Identificador = Letra (Letra | Digito)*      (si coincide con una palabra reservada, es reservada)
 *   Numero        = Digito Digito*
 *   Simbolos      = (  )  ,  ;
 * Delimitadores: espacio, tabulador y fin de línea.
 *
 * AFD = (Q, Σ, δ, q0, F)
 *   Q  = {Q0, Q1, Q2, Q3, Q4, Q5, Q6}
 *   Σ  = clases de caracteres {LETRA, DIGITO, (, ), ",", ;, OTRO}
 *   q0 = Q0
 *   F  = {Q1 identificador, Q2 numero, Q3 "(", Q4 ")", Q5 ",", Q6 ";"}
 *   δ  = tabla DELTA (-1 = sin transición)
 */
public class AnalizadorLexico {

    // Alfabeto del AFD: cada carácter se clasifica en una de estas columnas.
    private static final int LETRA = 0;
    private static final int DIGITO = 1;
    private static final int PAR_IZQ = 2;
    private static final int PAR_DER = 3;
    private static final int COMA = 4;
    private static final int PUNTO_COMA = 5;
    private static final int OTRO = 6;

    private static final int Q0 = 0;
    private static final int SIN_TRANSICION = -1;

    private static final int[][] DELTA = {
        //          LETRA  DIGITO   (    )    ,    ;   OTRO
        /* Q0 */ {    1,     2,     3,   4,   5,   6,   -1 },
        /* Q1 */ {    1,     1,    -1,  -1,  -1,  -1,   -1 },
        /* Q2 */ {   -1,     2,    -1,  -1,  -1,  -1,   -1 },
        /* Q3 */ {   -1,    -1,    -1,  -1,  -1,  -1,   -1 },
        /* Q4 */ {   -1,    -1,    -1,  -1,  -1,  -1,   -1 },
        /* Q5 */ {   -1,    -1,    -1,  -1,  -1,  -1,   -1 },
        /* Q6 */ {   -1,    -1,    -1,  -1,  -1,  -1,   -1 },
    };

    // Token que reconoce cada estado de aceptación (Q0 no es de aceptación).
    private static final TipoToken[] ACEPTACION = {
        null,
        TipoToken.IDENTIFICADOR,
        TipoToken.NUMERO,
        TipoToken.PARENTESIS_IZQ,
        TipoToken.PARENTESIS_DER,
        TipoToken.COMA,
        TipoToken.PUNTO_Y_COMA,
    };

    private final String fuente;
    private final List<Token> tokens = new ArrayList<>();
    private final List<ErrorAnalisis> errores = new ArrayList<>();

    public AnalizadorLexico(String fuente) {
        this.fuente = fuente;
    }

    /** Recorre la entrada y devuelve los tokens; el último siempre es FIN. */
    public List<Token> analizar() {
        int i = 0;
        int linea = 1;
        int columna = 1;

        while (i < fuente.length()) {
            char c = fuente.charAt(i);

            if (c == '\n') {
                linea++;
                columna = 1;
                i++;
                continue;
            }
            // '﻿' es la marca BOM que agrega el Bloc de notas al inicio del archivo; no ocupa columna.
            if (c == '﻿') {
                i++;
                continue;
            }
            if (c == ' ' || c == '\t' || c == '\r') {
                columna++;
                i++;
                continue;
            }

            // Avanza por el AFD mientras exista transición (se toma el lexema más largo).
            int estado = Q0;
            int inicio = i;
            int columnaInicio = columna;
            while (i < fuente.length()) {
                int siguiente = DELTA[estado][clasificar(fuente.charAt(i))];
                if (siguiente == SIN_TRANSICION) {
                    break;
                }
                estado = siguiente;
                i++;
                columna++;
            }

            if (estado == Q0) {
                errores.add(new ErrorAnalisis(ErrorAnalisis.Tipo.LEXICO, linea, columnaInicio,
                        "Caracter no reconocido: '" + c + "'"));
                i++;
                columna++;
            } else {
                String lexema = fuente.substring(inicio, i);
                TipoToken tipo = ACEPTACION[estado];
                if (tipo == TipoToken.IDENTIFICADOR) {
                    TipoToken reservada = TipoToken.buscarReservada(lexema);
                    if (reservada != null) {
                        tipo = reservada;
                    }
                }
                tokens.add(new Token(tipo, lexema, linea, columnaInicio));
            }
        }

        tokens.add(new Token(TipoToken.FIN, "", linea, columna));
        return tokens;
    }

    public List<ErrorAnalisis> getErrores() {
        return errores;
    }

    private static int clasificar(char c) {
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || "ÁÉÍÓÚÑáéíóúñ".indexOf(c) >= 0) {
            return LETRA;
        }
        if (c >= '0' && c <= '9') {
            return DIGITO;
        }
        switch (c) {
            case '(':
                return PAR_IZQ;
            case ')':
                return PAR_DER;
            case ',':
                return COMA;
            case ';':
                return PUNTO_COMA;
            default:
                return OTRO;
        }
    }
}
