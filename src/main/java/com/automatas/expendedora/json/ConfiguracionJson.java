package com.automatas.expendedora.json;

import com.automatas.expendedora.lexico.AnalizadorLexico;
import com.automatas.expendedora.lexico.TipoToken;
import com.automatas.expendedora.lexico.Token;
import com.automatas.expendedora.maquina.Estado;
import com.automatas.expendedora.maquina.MaquinaExpendedora;
import com.automatas.expendedora.maquina.Producto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda y recupera la configuración (productos) y el estado de la máquina en JSON.
 *
 * Formato:
 * {
 *   "productos": [ {"codigo": "A1", "nombre": "AGUA", "precio": 5, "cantidad": 10}, ... ],
 *   "estado": {"estadoActual": "E1", "saldo": 10, "seleccion": null}
 * }
 * "estado" es opcional: si falta, la máquina queda en E0 con saldo Q0.
 */
public final class ConfiguracionJson {

    private static final List<String> CAMPOS_RAIZ = List.of("productos", "estado");
    private static final List<String> CAMPOS_PRODUCTO = List.of("codigo", "nombre", "precio", "cantidad");
    private static final List<String> CAMPOS_ESTADO = List.of("estadoActual", "saldo", "seleccion");

    private ConfiguracionJson() {
    }

    public static String exportar(MaquinaExpendedora maquina) {
        StringBuilder json = new StringBuilder("{\n  \"productos\": [");
        int i = 0;
        for (Producto producto : maquina.getProductos()) {
            json.append(i++ == 0 ? "\n" : ",\n")
                    .append("    {\"codigo\": ").append(cadena(producto.getCodigo()))
                    .append(", \"nombre\": ").append(cadena(producto.getNombre()))
                    .append(", \"precio\": ").append(producto.getPrecio())
                    .append(", \"cantidad\": ").append(producto.getCantidad())
                    .append('}');
        }
        json.append(i == 0 ? "],\n" : "\n  ],\n");
        Producto seleccion = maquina.getSeleccion();
        json.append("  \"estado\": {\n")
                .append("    \"estadoActual\": \"").append(maquina.getEstado()).append("\",\n")
                .append("    \"saldo\": ").append(maquina.getSaldo()).append(",\n")
                .append("    \"seleccion\": ").append(seleccion == null ? "null" : cadena(seleccion.getCodigo())).append('\n')
                .append("  }\n}\n");
        return json.toString();
    }

    /** Valida todo el archivo; solo si no hay errores reemplaza el contenido de la máquina. */
    public static void importar(String texto, MaquinaExpendedora destino) throws ErrorImportacion {
        ValorJson raiz;
        try {
            raiz = LectorJson.leer(texto);
        } catch (ErrorJson e) {
            throw new ErrorImportacion(List.of("JSON mal formado. " + e.getMessage()));
        }
        List<String> errores = new ArrayList<>();
        if (raiz.tipo() != ValorJson.Tipo.OBJETO) {
            throw new ErrorImportacion(List.of(en(raiz) + "el archivo debe ser un objeto { ... }."));
        }
        Map<String, ValorJson> campos = raiz.comoObjeto();
        revisarCampos(raiz, CAMPOS_RAIZ, List.of("productos"), "el archivo", errores);

        Map<String, Producto> productos = new LinkedHashMap<>();
        ValorJson lista = campos.get("productos");
        if (lista != null) {
            if (lista.tipo() != ValorJson.Tipo.ARREGLO) {
                errores.add(en(lista) + "\"productos\" debe ser una lista [ ... ].");
            } else {
                for (ValorJson elemento : lista.comoArreglo()) {
                    Producto producto = leerProducto(elemento, errores);
                    if (producto == null) {
                        continue;
                    }
                    if (productos.containsKey(producto.getCodigo())) {
                        errores.add(en(elemento) + "código de producto duplicado: " + producto.getCodigo() + ".");
                    } else {
                        productos.put(producto.getCodigo(), producto);
                    }
                }
            }
        }

        Estado estado = Estado.E0;
        int saldo = 0;
        String seleccion = null;
        ValorJson nodoEstado = campos.get("estado");
        if (nodoEstado != null) {
            if (nodoEstado.tipo() != ValorJson.Tipo.OBJETO) {
                errores.add(en(nodoEstado) + "\"estado\" debe ser un objeto { ... }.");
            } else {
                int erroresAntes = errores.size();
                revisarCampos(nodoEstado, CAMPOS_ESTADO, List.of("estadoActual", "saldo"), "\"estado\"", errores);
                Map<String, ValorJson> datos = nodoEstado.comoObjeto();
                Estado leido = leerEstado(datos.get("estadoActual"), errores);
                Integer saldoLeido = datos.containsKey("saldo") ? entero(datos.get("saldo"), "saldo", 0, errores) : null;
                ValorJson nodoSeleccion = datos.get("seleccion");
                if (nodoSeleccion != null && nodoSeleccion.tipo() != ValorJson.Tipo.NULO
                        && nodoSeleccion.tipo() != ValorJson.Tipo.CADENA) {
                    errores.add(en(nodoSeleccion) + "\"seleccion\" debe ser el código de un producto o null.");
                }
                if (errores.size() == erroresAntes && leido != null && saldoLeido != null) {
                    estado = leido;
                    saldo = saldoLeido;
                    seleccion = nodoSeleccion == null || nodoSeleccion.tipo() == ValorJson.Tipo.NULO
                            ? null : nodoSeleccion.comoTexto();
                    revisarCoherencia(nodoEstado, estado, saldo, seleccion, productos, errores);
                }
            }
        }

        if (!errores.isEmpty()) {
            throw new ErrorImportacion(errores);
        }
        destino.cargarEstado(productos.values(), estado, saldo, seleccion);
    }

    private static Producto leerProducto(ValorJson nodo, List<String> errores) {
        if (nodo.tipo() != ValorJson.Tipo.OBJETO) {
            errores.add(en(nodo) + "cada producto debe ser un objeto { ... }.");
            return null;
        }
        int erroresAntes = errores.size();
        revisarCampos(nodo, CAMPOS_PRODUCTO, CAMPOS_PRODUCTO, "el producto", errores);
        Map<String, ValorJson> datos = nodo.comoObjeto();
        String codigo = identificador(datos.get("codigo"), "codigo", errores);
        String nombre = identificador(datos.get("nombre"), "nombre", errores);
        Integer precio = datos.containsKey("precio") ? entero(datos.get("precio"), "precio", 1, errores) : null;
        Integer cantidad = datos.containsKey("cantidad") ? entero(datos.get("cantidad"), "cantidad", 0, errores) : null;
        if (errores.size() > erroresAntes) {
            return null;
        }
        return new Producto(codigo, nombre, precio, cantidad);
    }

    private static Estado leerEstado(ValorJson nodo, List<String> errores) {
        if (nodo == null) {
            return null;
        }
        if (nodo.tipo() == ValorJson.Tipo.CADENA) {
            for (Estado estado : Estado.values()) {
                if (estado.name().equals(nodo.comoTexto())) {
                    return estado;
                }
            }
        }
        errores.add(en(nodo) + "\"estadoActual\" debe ser uno de los estados E0 a E5.");
        return null;
    }

    /** El estado guardado debe ser uno al que el autómata llega entre instrucciones. */
    private static void revisarCoherencia(ValorJson nodo, Estado estado, int saldo, String seleccion,
            Map<String, Producto> productos, List<String> errores) {
        switch (estado) {
            case E0:
                if (saldo != 0 || seleccion != null) {
                    errores.add(en(nodo) + "en E0 (Esperando dinero) el saldo debe ser 0 y no puede haber producto seleccionado.");
                }
                break;
            case E1:
                if (saldo == 0 || seleccion != null) {
                    errores.add(en(nodo) + "en E1 (Dinero ingresado) el saldo debe ser mayor que 0 y no puede haber producto seleccionado.");
                }
                break;
            case E2:
                if (saldo == 0 || seleccion == null) {
                    errores.add(en(nodo) + "en E2 (Producto seleccionado) el saldo debe ser mayor que 0 y debe haber un producto seleccionado.");
                } else if (!productos.containsKey(seleccion)) {
                    errores.add(en(nodo) + "el producto seleccionado " + seleccion + " no existe en la lista de productos.");
                } else if (productos.get(seleccion).getCantidad() == 0) {
                    errores.add(en(nodo) + "el producto seleccionado " + seleccion + " no tiene existencias.");
                }
                break;
            default:
                errores.add(en(nodo) + estado + " (" + estado.getDescripcion()
                        + ") es un estado de paso; solo se puede guardar E0, E1 o E2.");
        }
    }

    private static void revisarCampos(ValorJson nodo, List<String> permitidos, List<String> obligatorios,
            String dueno, List<String> errores) {
        Map<String, ValorJson> campos = nodo.comoObjeto();
        for (Map.Entry<String, ValorJson> campo : campos.entrySet()) {
            if (!permitidos.contains(campo.getKey())) {
                errores.add(en(campo.getValue()) + "campo desconocido \"" + campo.getKey() + "\" en " + dueno + ".");
            }
        }
        for (String obligatorio : obligatorios) {
            if (!campos.containsKey(obligatorio)) {
                errores.add(en(nodo) + "falta el campo \"" + obligatorio + "\" en " + dueno + ".");
            }
        }
    }

    /** Código y nombre siguen el patrón Identificador del analizador léxico, para poder usarlos en un .vm. */
    private static String identificador(ValorJson nodo, String campo, List<String> errores) {
        if (nodo == null) {
            return null;
        }
        if (nodo.tipo() == ValorJson.Tipo.CADENA) {
            List<Token> tokens = new AnalizadorLexico(nodo.comoTexto()).analizar();
            Token primero = tokens.get(0);
            if (tokens.size() == 2 && primero.tipo() == TipoToken.IDENTIFICADOR
                    && primero.lexema().equals(nodo.comoTexto())) {
                return nodo.comoTexto();
            }
        }
        errores.add(en(nodo) + "\"" + campo + "\" debe ser un texto que empiece con letra y siga con letras o dígitos"
                + " (no puede ser una palabra reservada).");
        return null;
    }

    private static Integer entero(ValorJson nodo, String campo, int minimo, List<String> errores) {
        if (nodo.tipo() == ValorJson.Tipo.NUMERO) {
            String texto = nodo.comoTexto();
            boolean esEntero = texto.indexOf('.') < 0 && texto.indexOf('e') < 0 && texto.indexOf('E') < 0;
            if (esEntero && texto.length() <= 10) {
                long valor = Long.parseLong(texto);
                if (valor >= minimo && valor <= Integer.MAX_VALUE) {
                    return (int) valor;
                }
            }
        }
        errores.add(en(nodo) + "\"" + campo + "\" debe ser un número entero "
                + (minimo == 0 ? "mayor o igual a 0." : "mayor que 0."));
        return null;
    }

    private static String en(ValorJson nodo) {
        return "Linea " + nodo.linea() + ": ";
    }

    private static String cadena(String texto) {
        StringBuilder resultado = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            if (c == '"' || c == '\\') {
                resultado.append('\\');
            }
            resultado.append(c);
        }
        return resultado.append('"').toString();
    }
}
