package com.automatas.expendedora.json;

import java.util.List;

/** El archivo JSON no se pudo cargar; trae todos los problemas encontrados. */
public class ErrorImportacion extends Exception {

    private static final long serialVersionUID = 1L;

    private final transient List<String> errores;

    public ErrorImportacion(List<String> errores) {
        super(String.join("\n", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
