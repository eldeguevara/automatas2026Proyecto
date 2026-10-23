package com.automatas.expendedora.sintactico;

import com.automatas.expendedora.errores.ErrorAnalisis;
import com.automatas.expendedora.lexico.TipoToken;
import com.automatas.expendedora.lexico.Token;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Analizador sintáctico descendente recursivo (predictivo LL(1)).
 *
 * Gramática (BNF):
 *   <programa>    ::= <instruccion> <programa> | λ
 *   <instruccion> ::= PRODUCTO "(" ID "," ID "," NUM "," NUM ")" ";"
 *                   | CARGAR "(" ID "," NUM ")" ";"
 *                   | MONEDA "(" NUM ")" ";"
 *                   | SELECCIONAR "(" ID ")" ";"
 *                   | COMPRAR "(" ")" ";"
 *                   | CANCELAR "(" ")" ";"
 *                   | MOSTRAR "(" ")" ";"
 *
 * Cada alternativa de <instruccion> empieza con una palabra reservada distinta, así que un solo
 * token de anticipación decide la producción (la tabla LL(1) no tiene conflictos).
 *
 * Recuperación en modo pánico: ante un error se descartan tokens hasta el ";" o hasta la
 * siguiente palabra reservada, y el análisis sigue con la siguiente instrucción.
 */
public class AnalizadorSintactico {

    private record Parametro(String nombre, TipoToken tipo) {
    }

    /** Señal interna para abandonar la instrucción actual y pasar a la recuperación. */
    private static class ErrorSintactico extends Exception {
        private static final long serialVersionUID = 1L;
    }

    // Parámetros de cada producción de <instruccion>, en orden.
    private static final Map<TipoToken, Parametro[]> FIRMAS = new EnumMap<>(TipoToken.class);

    static {
        Parametro codigo = new Parametro("codigo", TipoToken.IDENTIFICADOR);
        FIRMAS.put(TipoToken.PRODUCTO, new Parametro[] {
            codigo,
            new Parametro("nombre", TipoToken.IDENTIFICADOR),
            new Parametro("precio", TipoToken.NUMERO),
            new Parametro("cantidad", TipoToken.NUMERO)
        });
        FIRMAS.put(TipoToken.CARGAR, new Parametro[] {codigo, new Parametro("cantidad", TipoToken.NUMERO)});
        FIRMAS.put(TipoToken.MONEDA, new Parametro[] {new Parametro("valor", TipoToken.NUMERO)});
        FIRMAS.put(TipoToken.SELECCIONAR, new Parametro[] {codigo});
        FIRMAS.put(TipoToken.COMPRAR, new Parametro[0]);
        FIRMAS.put(TipoToken.CANCELAR, new Parametro[0]);
        FIRMAS.put(TipoToken.MOSTRAR, new Parametro[0]);
    }

    private final List<Token> tokens;
    private int pos;
    private final List<Instruccion> instrucciones = new ArrayList<>();
    private final List<ErrorAnalisis> errores = new ArrayList<>();

    public AnalizadorSintactico(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** <programa> ::= <instruccion> <programa> | λ   (la recursión por la derecha se recorre como ciclo). */
    public List<Instruccion> analizar() {
        while (actual().tipo() != TipoToken.FIN) {
            int inicio = pos;
            int lineaInstruccion = actual().linea();
            try {
                instrucciones.add(instruccion());
            } catch (ErrorSintactico e) {
                recuperar(lineaInstruccion, pos == inicio);
            }
        }
        return instrucciones;
    }

    /** Solo las instrucciones sin errores léxicos ni sintácticos. */
    public List<Instruccion> getInstrucciones() {
        return instrucciones;
    }

    public List<ErrorAnalisis> getErrores() {
        return errores;
    }

    private Instruccion instruccion() throws ErrorSintactico {
        Token palabra = actual();
        Parametro[] firma = FIRMAS.get(palabra.tipo());
        if (firma == null) {
            throw errorEn(palabra, instruccionNoReconocida(palabra));
        }
        avanzar();
        String nombre = palabra.lexema();

        esperar(TipoToken.PARENTESIS_IZQ, "Se esperaba '(' después de " + nombre);
        List<Token> parametros = new ArrayList<>();
        for (int i = 0; i < firma.length; i++) {
            if (i > 0) {
                if (actual().tipo() == TipoToken.PARENTESIS_DER) {
                    throw errorFaltante("Falta un parámetro: " + descripcionFirma(nombre, firma));
                }
                esperar(TipoToken.COMA, "Se esperaba ',' antes del parámetro " + firma[i].nombre());
            }
            parametros.add(parametro(nombre, firma, i));
        }
        TipoToken siguiente = actual().tipo();
        if (siguiente == TipoToken.COMA
                || (firma.length == 0 && (siguiente == TipoToken.IDENTIFICADOR || siguiente == TipoToken.NUMERO))) {
            throw errorEn(actual(), "Sobran parámetros: " + descripcionFirma(nombre, firma));
        }
        esperar(TipoToken.PARENTESIS_DER, "Falta el paréntesis de cierre ')'");
        esperar(TipoToken.PUNTO_Y_COMA, "Falta el punto y coma ';' al final de la instrucción");
        return new Instruccion(palabra.tipo(), parametros, palabra.linea(), palabra.columna());
    }

    private Token parametro(String instruccion, Parametro[] firma, int indice) throws ErrorSintactico {
        Parametro esperado = firma[indice];
        Token token = actual();
        if (token.tipo() == esperado.tipo()) {
            avanzar();
            return token;
        }
        if (token.tipo() == TipoToken.ERROR) {
            throw new ErrorSintactico();
        }
        if (token.tipo() == TipoToken.PARENTESIS_DER || token.tipo() == TipoToken.COMA) {
            throw errorFaltante("Falta un parámetro: " + descripcionFirma(instruccion, firma));
        }
        String clase = esperado.tipo() == TipoToken.NUMERO ? "un número entero" : "un identificador";
        throw errorEn(token, "El parámetro " + esperado.nombre() + " de " + instruccion + " debe ser "
                + clase + ", se encontró '" + token.lexema() + "'");
    }

    private void esperar(TipoToken tipo, String mensaje) throws ErrorSintactico {
        if (actual().tipo() == tipo) {
            avanzar();
            return;
        }
        if (actual().tipo() == TipoToken.ERROR) {
            // Ya se reportó como error léxico: solo se descarta la instrucción.
            throw new ErrorSintactico();
        }
        throw errorFaltante(mensaje);
    }

    /**
     * Si el error fue en el primer token (no empezaba con una instrucción), basta llegar a la
     * siguiente palabra reservada. Si fue a media instrucción, la palabra reservada debe estar en
     * otra línea; así un parámetro mal escrito como SELECCIONAR(MOSTRAR); no genera errores en cascada.
     */
    private void recuperar(int lineaInstruccion, boolean desdeElInicio) {
        while (actual().tipo() != TipoToken.FIN) {
            Token token = actual();
            if (token.tipo() == TipoToken.PUNTO_Y_COMA) {
                avanzar();
                return;
            }
            if (token.tipo().esReservada() && (desdeElInicio || token.linea() != lineaInstruccion)) {
                return;
            }
            avanzar();
        }
    }

    private ErrorSintactico errorEn(Token token, String mensaje) {
        if (token.tipo() != TipoToken.ERROR) {
            errores.add(new ErrorAnalisis(ErrorAnalisis.Tipo.SINTACTICO, token.linea(), token.columna(), mensaje));
        }
        return new ErrorSintactico();
    }

    /** Para lo que falta: si el siguiente token ya está en otra línea, el error es de la línea anterior. */
    private ErrorSintactico errorFaltante(String mensaje) {
        Token token = actual();
        Token previo = tokens.get(pos - 1);
        if (token.linea() > previo.linea()) {
            errores.add(new ErrorAnalisis(ErrorAnalisis.Tipo.SINTACTICO, previo.linea(),
                    previo.columna() + previo.lexema().length(), mensaje));
            return new ErrorSintactico();
        }
        return errorEn(token, mensaje);
    }

    private static String instruccionNoReconocida(Token token) {
        if (token.tipo() == TipoToken.IDENTIFICADOR) {
            TipoToken parecida = TipoToken.buscarReservada(token.lexema().toUpperCase());
            if (parecida != null) {
                return "Instrucción no reconocida: '" + token.lexema()
                        + "'. Las instrucciones van en mayúsculas: " + parecida.name();
            }
            return "Instrucción no reconocida: '" + token.lexema() + "'";
        }
        return "Se esperaba una instrucción y se encontró '" + token.lexema() + "'";
    }

    private static String descripcionFirma(String instruccion, Parametro[] firma) {
        if (firma.length == 0) {
            return instruccion + " no recibe parámetros";
        }
        StringBuilder nombres = new StringBuilder();
        for (int i = 0; i < firma.length; i++) {
            if (i > 0) {
                nombres.append(", ");
            }
            nombres.append(firma[i].nombre());
        }
        return instruccion + " requiere " + firma.length + (firma.length == 1 ? " parámetro (" : " parámetros (")
                + nombres + ")";
    }

    private Token actual() {
        return tokens.get(pos);
    }

    private void avanzar() {
        if (pos < tokens.size() - 1) {
            pos++;
        }
    }
}
