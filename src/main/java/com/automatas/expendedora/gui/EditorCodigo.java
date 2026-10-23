package com.automatas.expendedora.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

/** Editor de instrucciones .vm con números de línea y resaltado de líneas. */
class EditorCodigo extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextArea area = new JTextArea();
    private final NumerosLinea numeros = new NumerosLinea();
    private final transient List<Object> marcasError = new ArrayList<>();
    private transient Object marcaEjecucion;

    EditorCodigo() {
        super(new BorderLayout());
        area.setFont(Estilo.mono(Font.PLAIN, 15));
        area.setTabSize(4);
        area.setMargin(new Insets(4, 6, 4, 6));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setRowHeaderView(numeros);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
        alCambiar(() -> {
            numeros.revalidate();
            numeros.repaint();
        });
    }

    String getTexto() {
        return area.getText();
    }

    void setTexto(String texto) {
        area.setText(texto);
        area.setCaretPosition(0);
    }

    void setEditable(boolean editable) {
        area.setEditable(editable);
    }

    /** Avisa cada vez que el usuario modifica el código. */
    final void alCambiar(Runnable accion) {
        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                accion.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                accion.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                accion.run();
            }
        });
    }

    void marcarError(int linea) {
        Object marca = resaltar(linea, Estilo.LINEA_ERROR);
        if (marca != null) {
            marcasError.add(marca);
        }
    }

    void limpiarErrores() {
        for (Object marca : marcasError) {
            area.getHighlighter().removeHighlight(marca);
        }
        marcasError.clear();
    }

    /** Resalta la instrucción que se está ejecutando (solo una a la vez). */
    void marcarEjecucion(int linea) {
        quitarEjecucion();
        marcaEjecucion = resaltar(linea, Estilo.LINEA_EJECUCION);
        irALinea(linea, false);
    }

    void quitarEjecucion() {
        if (marcaEjecucion != null) {
            area.getHighlighter().removeHighlight(marcaEjecucion);
            marcaEjecucion = null;
        }
    }

    void irALinea(int linea, boolean enfocar) {
        try {
            int inicio = area.getLineStartOffset(Math.min(linea, area.getLineCount()) - 1);
            area.setCaretPosition(inicio);
            Rectangle visible = area.modelToView2D(inicio).getBounds();
            visible.height *= 3;
            area.scrollRectToVisible(visible);
            if (enfocar) {
                area.requestFocusInWindow();
            }
        } catch (BadLocationException e) {
            // Línea fuera del texto: no hay nada que mostrar.
        }
    }

    private Object resaltar(int linea, Color color) {
        try {
            if (linea < 1 || linea > area.getLineCount()) {
                return null;
            }
            int inicio = area.getLineStartOffset(linea - 1);
            int fin = area.getLineEndOffset(linea - 1);
            Highlighter.HighlightPainter pintor = new DefaultHighlighter.DefaultHighlightPainter(color);
            return area.getHighlighter().addHighlight(inicio, fin, pintor);
        } catch (BadLocationException e) {
            return null;
        }
    }

    /** Columna de números de línea a la izquierda del texto. */
    private class NumerosLinea extends JComponent {

        private static final long serialVersionUID = 1L;

        NumerosLinea() {
            setFont(Estilo.mono(Font.PLAIN, 15));
            setForeground(new Color(0x85929E));
            setBackground(new Color(0xF2F4F4));
            setOpaque(true);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int digitos = Math.max(2, String.valueOf(area.getLineCount()).length());
            return new Dimension(fm.charWidth('0') * digitos + 16, area.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle zona = g.getClipBounds();
            g.setColor(getBackground());
            g.fillRect(zona.x, zona.y, zona.width, zona.height);
            g.setColor(getForeground());
            g.setFont(getFont());
            FontMetrics fm = area.getFontMetrics(area.getFont());
            int alto = fm.getHeight();
            int margen = area.getInsets().top;
            int primera = Math.max(0, (zona.y - margen) / alto);
            int ultima = Math.min(area.getLineCount() - 1, (zona.y + zona.height - margen) / alto);
            int ancho = getWidth();
            for (int i = primera; i <= ultima; i++) {
                String numero = String.valueOf(i + 1);
                g.drawString(numero, ancho - 8 - fm.stringWidth(numero), margen + i * alto + fm.getAscent());
            }
        }
    }
}
