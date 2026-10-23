package com.automatas.expendedora.errores;

public record ErrorAnalisis(Tipo tipo, int linea, int columna, String descripcion) {

    public enum Tipo {
        LEXICO("ERROR LEXICO"),
        SINTACTICO("ERROR SINTACTICO"),
        SEMANTICO("ERROR SEMANTICO");

        private final String etiqueta;

        Tipo(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    /** Formato de los ejemplos del enunciado: "[ERROR SEMANTICO] Linea 10:" y la descripción debajo. */
    @Override
    public String toString() {
        return "[" + tipo.getEtiqueta() + "] Linea " + linea + ":\n" + descripcion;
    }
}
