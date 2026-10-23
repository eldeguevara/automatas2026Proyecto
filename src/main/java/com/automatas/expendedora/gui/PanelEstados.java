package com.automatas.expendedora.gui;

import com.automatas.expendedora.maquina.Automata;
import com.automatas.expendedora.maquina.Automata.Transicion;
import com.automatas.expendedora.maquina.Entrada;
import com.automatas.expendedora.maquina.Estado;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

/**
 * Indicador de estados: dibuja el autómata de compra, resalta el estado actual y anima cada
 * transición que hace la máquina, una por una, para que se vea el recorrido (p. ej. E2→E3→E4→E5→E0).
 */
class PanelEstados extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int PASO_MS = 450;

    private static final Color[] COLOR_ESTADO = {
        new Color(0xDCEBFF), new Color(0xDFF7E6), new Color(0xFFF3C4),
        new Color(0xECE6FF), new Color(0xD5F5EC), new Color(0xFDE2EC)
    };

    private final Diagrama diagrama = new Diagrama();
    private final JLabel lblActual = new JLabel();
    private final DefaultListModel<String> historial = new DefaultListModel<>();
    private final JList<String> listaHistorial = new JList<>(historial);
    private final transient Deque<Transicion> pendientes = new ArrayDeque<>();
    private final Timer temporizador;
    private transient Runnable alCambiar = () -> { };
    private Estado mostrado = Automata.INICIAL;
    private transient Transicion activa;

    PanelEstados() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 8, 8, 8));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        JLabel titulo = new JLabel("Autómata de compra");
        titulo.setFont(Estilo.fuente(Font.BOLD, 15));
        lblActual.setFont(Estilo.fuente(Font.PLAIN, 12));
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(lblActual, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);
        add(diagrama, BorderLayout.CENTER);

        listaHistorial.setFont(Estilo.mono(Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(listaHistorial);
        scroll.setBorder(BorderFactory.createTitledBorder("Transiciones realizadas"));
        scroll.setPreferredSize(new Dimension(10, 150));
        add(scroll, BorderLayout.SOUTH);

        temporizador = new Timer(PASO_MS, e -> mostrarSiguiente());
        actualizarEtiqueta();
    }

    /** Se llama cada vez que cambia el estado mostrado (para refrescar la tabla δ). */
    void setAlCambiar(Runnable alCambiar) {
        this.alCambiar = alCambiar;
    }

    Estado getEstadoMostrado() {
        return mostrado;
    }

    /** true mientras quedan transiciones por animar. */
    boolean estaAnimando() {
        return !pendientes.isEmpty();
    }

    /** Coloca el diagrama en un estado sin animación (máquina reiniciada o importada). */
    void sincronizar(Estado estado, String motivo) {
        pendientes.clear();
        temporizador.stop();
        mostrado = estado;
        activa = null;
        historial.addElement("— " + motivo + ": " + estado);
        listaHistorial.ensureIndexIsVisible(historial.size() - 1);
        actualizarEtiqueta();
        diagrama.repaint();
        alCambiar.run();
    }

    /** Recibe una transición del autómata y la anima cuando le toque. */
    void encolar(Transicion transicion) {
        pendientes.add(transicion);
        if (!temporizador.isRunning()) {
            mostrarSiguiente();
            temporizador.start();
        }
    }

    private void mostrarSiguiente() {
        Transicion transicion = pendientes.poll();
        if (transicion == null) {
            temporizador.stop();
            return;
        }
        activa = transicion;
        mostrado = transicion.destino();
        historial.addElement("δ(" + transicion.origen() + ", " + transicion.entrada().getEtiqueta() + ") = "
                + transicion.destino());
        listaHistorial.ensureIndexIsVisible(historial.size() - 1);
        actualizarEtiqueta();
        diagrama.repaint();
        alCambiar.run();
    }

    private void actualizarEtiqueta() {
        lblActual.setText("<html>Estado actual: <b>" + mostrado + " · " + mostrado.getDescripcion() + "</b></html>");
    }

    /** Dibujo del autómata: E0 a E5 en columna, con las transiciones de la tabla δ. */
    private class Diagrama extends JComponent {

        private static final long serialVersionUID = 1L;

        private static final int X = 40;
        private static final int ANCHO = 172;
        private static final int ALTO = 44;
        private static final int SEPARACION = 72;
        private static final int Y0 = 14;
        private static final int CARRIL_IZQ = 20;
        private static final int CARRIL_DER1 = X + ANCHO + 16;
        private static final int CARRIL_DER2 = X + ANCHO + 36;

        Diagrama() {
            setPreferredSize(new Dimension(CARRIL_DER2 + 24, Y0 + 5 * SEPARACION + ALTO + 10));
        }

        private int y(Estado estado) {
            return Y0 + estado.ordinal() * SEPARACION;
        }

        private int medio(Estado estado) {
            return y(estado) + ALTO / 2;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Primero las transiciones normales y al final la activa, para que quede encima.
            for (Map.Entry<Estado, Map<Entrada, Estado>> fila : Automata.getDelta().entrySet()) {
                for (Map.Entry<Entrada, Estado> celda : fila.getValue().entrySet()) {
                    if (!esActiva(fila.getKey(), celda.getKey())) {
                        dibujarTransicion(g2, fila.getKey(), celda.getKey(), celda.getValue(), false);
                    }
                }
            }
            if (activa != null) {
                dibujarTransicion(g2, activa.origen(), activa.entrada(), activa.destino(), true);
            }
            for (Estado estado : Estado.values()) {
                dibujarEstado(g2, estado);
            }
            dibujarEtiquetasCarriles(g2);
            g2.dispose();
        }

        private boolean esActiva(Estado origen, Entrada entrada) {
            return activa != null && activa.origen() == origen && activa.entrada() == entrada;
        }

        private void dibujarEstado(Graphics2D g2, Estado estado) {
            boolean actual = estado == mostrado;
            int y = y(estado);
            Color base = COLOR_ESTADO[estado.ordinal()];
            g2.setColor(actual ? base.darker() : base);
            g2.fillRoundRect(X, y, ANCHO, ALTO, 16, 16);
            g2.setColor(actual ? Estilo.ACENTO : new Color(0x5D6D7E));
            g2.setStroke(new BasicStroke(actual ? 3.5f : 1.2f));
            g2.drawRoundRect(X, y, ANCHO, ALTO, 16, 16);

            g2.setColor(new Color(0x1B2631));
            g2.setFont(Estilo.fuente(Font.BOLD, 12));
            String texto = estado + " · " + estado.getDescripcion();
            String lazo = lazo(estado);
            FontMetrics fm = g2.getFontMetrics();
            int ytexto = lazo == null ? y + (ALTO + fm.getAscent()) / 2 - 2 : y + 18;
            g2.drawString(texto, X + (ANCHO - fm.stringWidth(texto)) / 2, ytexto);
            if (lazo != null) {
                boolean lazoActivo = activa != null && activa.origen() == estado && activa.destino() == estado;
                g2.setFont(Estilo.fuente(Font.PLAIN, 10));
                g2.setColor(lazoActivo ? Estilo.ACENTO.darker() : new Color(0x34495E));
                fm = g2.getFontMetrics();
                g2.drawString(lazo, X + (ANCHO - fm.stringWidth(lazo)) / 2, y + 34);
            }
        }

        /** Transiciones que vuelven al mismo estado; se escriben dentro de la caja. */
        private String lazo(Estado estado) {
            StringBuilder texto = new StringBuilder();
            for (Map.Entry<Entrada, Estado> celda : Automata.getDelta().get(estado).entrySet()) {
                if (celda.getValue() == estado) {
                    texto.append(texto.length() == 0 ? "lazo: " : ", ").append(celda.getKey().getEtiqueta());
                }
            }
            return texto.length() == 0 ? null : texto.toString();
        }

        private void dibujarTransicion(Graphics2D g2, Estado origen, Entrada entrada, Estado destino, boolean resaltada) {
            if (origen == destino) {
                return;
            }
            Point[] ruta;
            String etiqueta = null;
            boolean punteada = true;
            int centro = X + ANCHO / 2;
            if (destino.ordinal() == origen.ordinal() + 1) {
                ruta = new Point[] {new Point(centro, y(origen) + ALTO), new Point(centro, y(destino))};
                etiqueta = entrada.getEtiqueta();
                punteada = false;
            } else if (entrada == Entrada.CANCELAR) {
                ruta = new Point[] {new Point(X, medio(origen)), new Point(CARRIL_IZQ, medio(origen)),
                    new Point(CARRIL_IZQ, medio(destino)), new Point(X, medio(destino))};
            } else if (entrada == Entrada.COMPRA_RECHAZADA) {
                ruta = new Point[] {new Point(X + ANCHO, medio(origen)), new Point(CARRIL_DER1, medio(origen)),
                    new Point(CARRIL_DER1, medio(destino)), new Point(X + ANCHO, medio(destino))};
            } else {
                ruta = new Point[] {new Point(X + ANCHO, medio(origen)), new Point(CARRIL_DER2, medio(origen)),
                    new Point(CARRIL_DER2, medio(destino)), new Point(X + ANCHO, medio(destino))};
            }

            Stroke trazo = resaltada
                    ? new BasicStroke(3.5f)
                    : punteada
                            ? new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f)
                            : new BasicStroke(1.6f);
            g2.setStroke(trazo);
            g2.setColor(resaltada ? Estilo.ACENTO : new Color(0x5D6D7E));
            for (int i = 0; i < ruta.length - 1; i++) {
                g2.drawLine(ruta[i].x, ruta[i].y, ruta[i + 1].x, ruta[i + 1].y);
            }
            flecha(g2, ruta[ruta.length - 2], ruta[ruta.length - 1]);

            if (etiqueta != null) {
                g2.setFont(Estilo.fuente(resaltada ? Font.BOLD : Font.PLAIN, 11));
                g2.setColor(resaltada ? Estilo.ACENTO.darker() : new Color(0x2C3E50));
                g2.drawString(etiqueta, centro + 7, (ruta[0].y + ruta[1].y) / 2 + 4);
            }
        }

        private void flecha(Graphics2D g2, Point desde, Point hasta) {
            double angulo = Math.atan2(hasta.y - desde.y, hasta.x - desde.x);
            int largo = 9;
            int[] xs = {hasta.x,
                (int) Math.round(hasta.x - largo * Math.cos(angulo - Math.PI / 7)),
                (int) Math.round(hasta.x - largo * Math.cos(angulo + Math.PI / 7))};
            int[] ys = {hasta.y,
                (int) Math.round(hasta.y - largo * Math.sin(angulo - Math.PI / 7)),
                (int) Math.round(hasta.y - largo * Math.sin(angulo + Math.PI / 7))};
            g2.fillPolygon(xs, ys, 3);
        }

        private void dibujarEtiquetasCarriles(Graphics2D g2) {
            g2.setFont(Estilo.fuente(Font.PLAIN, 10));
            g2.setColor(new Color(0x2C3E50));
            textoVertical(g2, "CANCELAR", CARRIL_IZQ - 6, (medio(Estado.E2) + medio(Estado.E5)) / 2);
            textoVertical(g2, "rechazada", X + ANCHO + 10, (medio(Estado.E2) + medio(Estado.E3)) / 2);
            textoVertical(g2, "Finalizar / reiniciar", CARRIL_DER2 + 12, (medio(Estado.E0) + medio(Estado.E4)) / 2);
        }

        private void textoVertical(Graphics2D g2, String texto, int x, int yCentro) {
            AffineTransform original = g2.getTransform();
            int ancho = g2.getFontMetrics().stringWidth(texto);
            g2.rotate(-Math.PI / 2, x, yCentro);
            g2.drawString(texto, x - ancho / 2, yCentro);
            g2.setTransform(original);
        }
    }
}
