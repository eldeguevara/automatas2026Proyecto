package com.automatas.expendedora.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector de JSON hecho a mano: analizador descendente recursivo sobre la gramática
 *   valor   ::= objeto | arreglo | cadena | numero | true | false | null
 *   objeto  ::= "{" [ cadena ":" valor { "," cadena ":" valor } ] "}"
 *   arreglo ::= "[" [ valor { "," valor } ] "]"
 * Se detiene en el primer error de sintaxis e indica su línea y columna.
 */
public class LectorJson {

    private final String texto;
    private int pos;
    private int linea = 1;
    private int columna = 1;

    private LectorJson(String texto) {
        this.texto = texto;
    }

    public static ValorJson leer(String texto) throws ErrorJson {
        LectorJson lector = new LectorJson(texto);
        lector.saltarEspacios();
        ValorJson valor = lector.valor();
        lector.saltarEspacios();
        if (!lector.fin()) {
            throw lector.error("Hay contenido después del final del JSON");
        }
        return valor;
    }

    private ValorJson valor() throws ErrorJson {
        if (fin()) {
            throw error("Se esperaba un valor, pero el archivo terminó");
        }
        int lineaInicio = linea;
        int columnaInicio = columna;
        char c = actual();
        switch (c) {
            case '{':
                return objeto();
            case '[':
                return arreglo();
            case '"':
                return new ValorJson(ValorJson.Tipo.CADENA, cadena(), lineaInicio, columnaInicio);
            case 't':
                palabra("true");
                return new ValorJson(ValorJson.Tipo.BOOLEANO, Boolean.TRUE, lineaInicio, columnaInicio);
            case 'f':
                palabra("false");
                return new ValorJson(ValorJson.Tipo.BOOLEANO, Boolean.FALSE, lineaInicio, columnaInicio);
            case 'n':
                palabra("null");
                return new ValorJson(ValorJson.Tipo.NULO, null, lineaInicio, columnaInicio);
            default:
                if (c == '-' || esDigito(c)) {
                    return new ValorJson(ValorJson.Tipo.NUMERO, numero(), lineaInicio, columnaInicio);
                }
                throw error("Valor no válido: '" + c + "'");
        }
    }

    private ValorJson objeto() throws ErrorJson {
        int lineaInicio = linea;
        int columnaInicio = columna;
        avanzar(); // {
        Map<String, ValorJson> campos = new LinkedHashMap<>();
        saltarEspacios();
        if (!fin() && actual() == '}') {
            avanzar();
            return new ValorJson(ValorJson.Tipo.OBJETO, campos, lineaInicio, columnaInicio);
        }
        while (true) {
            if (fin() || actual() != '"') {
                throw error("Se esperaba el nombre de un campo entre comillas");
            }
            int lineaClave = linea;
            int columnaClave = columna;
            String clave = cadena();
            if (campos.containsKey(clave)) {
                throw new ErrorJson(lineaClave, columnaClave, "El campo \"" + clave + "\" está repetido");
            }
            saltarEspacios();
            if (fin() || actual() != ':') {
                throw error("Se esperaba ':' después de \"" + clave + "\"");
            }
            avanzar();
            saltarEspacios();
            campos.put(clave, valor());
            saltarEspacios();
            if (!fin() && actual() == ',') {
                avanzar();
                saltarEspacios();
                if (!fin() && actual() == '}') {
                    throw error("Sobra la coma antes de '}'");
                }
            } else if (!fin() && actual() == '}') {
                avanzar();
                return new ValorJson(ValorJson.Tipo.OBJETO, campos, lineaInicio, columnaInicio);
            } else {
                throw error("Se esperaba ',' o '}'");
            }
        }
    }

    private ValorJson arreglo() throws ErrorJson {
        int lineaInicio = linea;
        int columnaInicio = columna;
        avanzar(); // [
        List<ValorJson> elementos = new ArrayList<>();
        saltarEspacios();
        if (!fin() && actual() == ']') {
            avanzar();
            return new ValorJson(ValorJson.Tipo.ARREGLO, elementos, lineaInicio, columnaInicio);
        }
        while (true) {
            elementos.add(valor());
            saltarEspacios();
            if (!fin() && actual() == ',') {
                avanzar();
                saltarEspacios();
                if (!fin() && actual() == ']') {
                    throw error("Sobra la coma antes de ']'");
                }
            } else if (!fin() && actual() == ']') {
                avanzar();
                return new ValorJson(ValorJson.Tipo.ARREGLO, elementos, lineaInicio, columnaInicio);
            } else {
                throw error("Se esperaba ',' o ']'");
            }
        }
    }

    private String cadena() throws ErrorJson {
        avanzar(); // "
        StringBuilder resultado = new StringBuilder();
        while (true) {
            if (fin() || actual() == '\n') {
                throw error("Falta cerrar la cadena con comillas");
            }
            char c = actual();
            if (c == '"') {
                avanzar();
                return resultado.toString();
            }
            if (c == '\\') {
                avanzar();
                if (fin()) {
                    throw error("Falta cerrar la cadena con comillas");
                }
                char escape = actual();
                switch (escape) {
                    case '"': resultado.append('"'); break;
                    case '\\': resultado.append('\\'); break;
                    case '/': resultado.append('/'); break;
                    case 'b': resultado.append('\b'); break;
                    case 'f': resultado.append('\f'); break;
                    case 'n': resultado.append('\n'); break;
                    case 'r': resultado.append('\r'); break;
                    case 't': resultado.append('\t'); break;
                    case 'u':
                        resultado.append(unicode());
                        continue;
                    default:
                        throw error("Secuencia de escape no válida: \\" + escape);
                }
                avanzar();
                continue;
            }
            if (c < 0x20) {
                throw error("Carácter de control dentro de la cadena");
            }
            resultado.append(c);
            avanzar();
        }
    }

    private char unicode() throws ErrorJson {
        avanzar(); // u
        int codigo = 0;
        for (int i = 0; i < 4; i++) {
            if (fin() || Character.digit(actual(), 16) < 0) {
                throw error("Se esperaban 4 dígitos hexadecimales después de \\u");
            }
            codigo = codigo * 16 + Character.digit(actual(), 16);
            avanzar();
        }
        return (char) codigo;
    }

    /** numero ::= ["-"] ("0" | 1-9 {dígito}) ["." dígito {dígito}] [("e"|"E") ["+"|"-"] dígito {dígito}] */
    private String numero() throws ErrorJson {
        int inicio = pos;
        if (actual() == '-') {
            avanzar();
        }
        if (!fin() && actual() == '0') {
            avanzar();
        } else if (!fin() && esDigito(actual())) {
            digitos();
        } else {
            throw error("Número no válido");
        }
        if (!fin() && actual() == '.') {
            avanzar();
            if (fin() || !esDigito(actual())) {
                throw error("Se esperaba un dígito después del punto decimal");
            }
            digitos();
        }
        if (!fin() && (actual() == 'e' || actual() == 'E')) {
            avanzar();
            if (!fin() && (actual() == '+' || actual() == '-')) {
                avanzar();
            }
            if (fin() || !esDigito(actual())) {
                throw error("Se esperaba un dígito en el exponente");
            }
            digitos();
        }
        return texto.substring(inicio, pos);
    }

    private void digitos() {
        while (!fin() && esDigito(actual())) {
            avanzar();
        }
    }

    private void palabra(String esperada) throws ErrorJson {
        for (int i = 0; i < esperada.length(); i++) {
            if (fin() || actual() != esperada.charAt(i)) {
                throw error("Valor no válido; ¿quiso escribir " + esperada + "?");
            }
            avanzar();
        }
    }

    private void saltarEspacios() {
        while (!fin()) {
            char c = actual();
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n' && c != '﻿') {
                return;
            }
            avanzar();
        }
    }

    private static boolean esDigito(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean fin() {
        return pos >= texto.length();
    }

    private char actual() {
        return texto.charAt(pos);
    }

    private void avanzar() {
        if (texto.charAt(pos) == '\n') {
            linea++;
            columna = 1;
        } else if (texto.charAt(pos) != '﻿') {
            columna++;
        }
        pos++;
    }

    private ErrorJson error(String mensaje) {
        return new ErrorJson(linea, columna, mensaje);
    }
}
