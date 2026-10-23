package com.automatas.expendedora.gui;

import java.awt.Color;
import java.awt.Font;

/** Colores y fuentes de la interfaz. */
final class Estilo {

    static final Color CUERPO = new Color(0x1F3A5F);
    static final Color CUERPO_OSCURO = new Color(0x132640);
    static final Color VITRINA = new Color(0xDCE8F2);
    static final Color RANURA = new Color(0xFFFFFF);
    static final Color RANURA_AGOTADA = new Color(0xC9CED4);
    static final Color TEXTO_RANURA = new Color(0x1B2631);
    static final Color SELECCION = new Color(0xF5B041);
    static final Color LCD_FONDO = new Color(0x0E2F1C);
    static final Color LCD_TEXTO = new Color(0x7CFC9A);
    static final Color MONEDA = new Color(0xB7950B);
    static final Color COMPRAR = new Color(0x1E8449);
    static final Color CANCELAR = new Color(0xB03A2E);
    static final Color BANDEJA = new Color(0x0B1A2C);
    static final Color BANDEJA_LLENA = new Color(0xF8C471);
    static final Color OK = new Color(0x1E7B34);
    static final Color ERROR = new Color(0xC0392B);
    static final Color INFO = new Color(0x5D6D7E);
    static final Color ACENTO = new Color(0xE67E22);
    static final Color LINEA_EJECUCION = new Color(0xFFF176);
    static final Color LINEA_ERROR = new Color(0xF5B7B1);

    private Estilo() {
    }

    static Font fuente(int estilo, int tamano) {
        return new Font("Segoe UI", estilo, tamano);
    }

    static Font mono(int estilo, int tamano) {
        return new Font("Consolas", estilo, tamano);
    }
}
