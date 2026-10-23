package com.automatas.expendedora.gui;

import com.automatas.expendedora.maquina.MaquinaExpendedora;
import com.automatas.expendedora.maquina.Producto;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/** Representación visual de la máquina: vitrina de productos, pantalla, monedas, botones y bandejas. */
class PanelMaquina extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int ANCHO_CONTROLES = 160;

    /** Lo que hace cada control de la máquina; la ventana principal lo ejecuta y reporta. */
    interface Acciones {
        void moneda(int valor);

        void seleccionar(String codigo);

        void comprar();

        void cancelar();
    }

    private final transient MaquinaExpendedora maquina;
    private final transient Acciones acciones;
    private final VistaVitrina vitrina = new VistaVitrina();
    private final JLabel lcdSaldo = new JLabel();
    private final JLabel lcdEstado = new JLabel();
    private final JLabel lcdMensaje = new JLabel();
    private final JLabel lblSeleccion = new JLabel();
    private final JLabel entregaValor = new JLabel();
    private final JLabel entregaNota = new JLabel();
    private final JLabel cambioValor = new JLabel();
    private final JLabel cambioNota = new JLabel();
    private final JPanel bandejaEntrega;
    private final JPanel bandejaCambio;
    private final transient List<JButton> controles = new ArrayList<>();
    private boolean habilitado = true;

    PanelMaquina(MaquinaExpendedora maquina, Acciones acciones) {
        this.maquina = maquina;
        this.acciones = acciones;
        setLayout(new BorderLayout(12, 12));
        setBackground(Estilo.CUERPO);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 14, 14));

        JLabel titulo = new JLabel("MÁQUINA EXPENDEDORA", SwingConstants.CENTER);
        titulo.setForeground(Color.WHITE);
        titulo.setFont(Estilo.fuente(Font.BOLD, 18));
        add(titulo, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(vitrina);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.CUERPO_OSCURO, 5));
        scroll.getViewport().setBackground(Estilo.VITRINA);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        add(crearControles(), BorderLayout.EAST);

        JPanel bandejas = new JPanel(new GridLayout(1, 2, 10, 0));
        bandejas.setOpaque(false);
        bandejaEntrega = crearBandeja("BANDEJA DE ENTREGA", entregaValor, entregaNota);
        bandejaCambio = crearBandeja("CAMBIO / DEVOLUCIÓN", cambioValor, cambioNota);
        bandejas.add(bandejaEntrega);
        bandejas.add(bandejaCambio);
        add(bandejas, BorderLayout.SOUTH);

        actualizar();
    }

    private JComponent crearControles() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(ANCHO_CONTROLES, 10));

        JPanel lcd = new JPanel(new BorderLayout(0, 4));
        lcd.setBackground(Estilo.LCD_FONDO);
        lcd.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.CUERPO_OSCURO, 4),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        lcdSaldo.setFont(Estilo.mono(Font.BOLD, 22));
        lcdEstado.setFont(Estilo.mono(Font.PLAIN, 12));
        lcdMensaje.setFont(Estilo.mono(Font.PLAIN, 11));
        lcdMensaje.setVerticalAlignment(SwingConstants.TOP);
        for (JLabel etiqueta : new JLabel[] {lcdSaldo, lcdEstado, lcdMensaje}) {
            etiqueta.setForeground(Estilo.LCD_TEXTO);
        }
        lcd.add(lcdSaldo, BorderLayout.NORTH);
        lcd.add(lcdMensaje, BorderLayout.CENTER);
        lcd.add(lcdEstado, BorderLayout.SOUTH);
        agregar(panel, lcd, 118);
        panel.add(Box.createVerticalStrut(12));

        agregar(panel, etiqueta("Monedas"), 18);
        JPanel monedas = new JPanel(new GridLayout(1, 3, 6, 0));
        monedas.setOpaque(false);
        for (int valor : MaquinaExpendedora.MONEDAS_PERMITIDAS) {
            BotonMaquina boton = new BotonMaquina("Q" + valor, Estilo.MONEDA);
            boton.setToolTipText("MONEDA(" + valor + ");");
            boton.addActionListener(e -> acciones.moneda(valor));
            controles.add(boton);
            monedas.add(boton);
        }
        agregar(panel, monedas, 40);
        panel.add(Box.createVerticalStrut(12));

        lblSeleccion.setForeground(Color.WHITE);
        lblSeleccion.setFont(Estilo.fuente(Font.PLAIN, 12));
        agregar(panel, lblSeleccion, 52);
        panel.add(Box.createVerticalStrut(8));

        BotonMaquina comprar = new BotonMaquina("COMPRAR", Estilo.COMPRAR);
        comprar.setToolTipText("COMPRAR();");
        comprar.addActionListener(e -> acciones.comprar());
        controles.add(comprar);
        agregar(panel, comprar, 42);
        panel.add(Box.createVerticalStrut(8));

        BotonMaquina cancelar = new BotonMaquina("CANCELAR", Estilo.CANCELAR);
        cancelar.setToolTipText("CANCELAR();");
        cancelar.addActionListener(e -> acciones.cancelar());
        controles.add(cancelar);
        agregar(panel, cancelar, 42);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static void agregar(JPanel panel, JComponent componente, int alto) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        componente.setPreferredSize(new Dimension(ANCHO_CONTROLES, alto));
        panel.add(componente);
    }

    private static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(new Color(0xD6EAF8));
        etiqueta.setFont(Estilo.fuente(Font.BOLD, 12));
        return etiqueta;
    }

    private static JPanel crearBandeja(String titulo, JLabel valor, JLabel nota) {
        JPanel bandeja = new JPanel(new BorderLayout(0, 2));
        bandeja.setBackground(Estilo.BANDEJA);
        bandeja.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.CUERPO_OSCURO, 3),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        bandeja.setPreferredSize(new Dimension(10, 84));
        JLabel encabezado = etiqueta(titulo);
        encabezado.setFont(Estilo.fuente(Font.BOLD, 11));
        valor.setFont(Estilo.fuente(Font.BOLD, 20));
        valor.setHorizontalAlignment(SwingConstants.CENTER);
        nota.setFont(Estilo.fuente(Font.PLAIN, 11));
        nota.setHorizontalAlignment(SwingConstants.CENTER);
        bandeja.add(encabezado, BorderLayout.NORTH);
        bandeja.add(valor, BorderLayout.CENTER);
        bandeja.add(nota, BorderLayout.SOUTH);
        return bandeja;
    }

    /** Vuelve a pintar todo a partir del estado actual de la máquina. */
    void actualizar() {
        vitrina.removeAll();
        if (maquina.getProductos().isEmpty()) {
            vitrina.setLayout(new BorderLayout());
            JLabel vacio = new JLabel("<html><center>Sin productos.<br>Registre productos con PRODUCTO(...)"
                    + "<br>o importe un archivo JSON.</center></html>", SwingConstants.CENTER);
            vacio.setForeground(Estilo.INFO);
            vacio.setBorder(BorderFactory.createEmptyBorder(40, 10, 40, 10));
            vitrina.add(vacio, BorderLayout.CENTER);
        } else {
            vitrina.setLayout(new GridLayout(0, 3, 8, 8));
            for (Producto producto : maquina.getProductos()) {
                vitrina.add(crearRanura(producto));
            }
        }
        vitrina.revalidate();
        vitrina.repaint();

        lcdSaldo.setText("SALDO Q" + maquina.getSaldo());
        lcdEstado.setText("ESTADO " + maquina.getEstado());
        mostrarMensaje(mensajePorEstado());

        Producto seleccion = maquina.getSeleccion();
        lblSeleccion.setText(seleccion == null
                ? "<html>Selección:<br><b>ninguna</b></html>"
                : "<html>Selección:<br><b>" + seleccion.getCodigo() + " · " + seleccion.getNombre()
                        + " · Q" + seleccion.getPrecio() + "</b></html>");

        Producto entrega = maquina.getUltimaEntrega();
        int cambio = maquina.getUltimoCambio();
        pintarBandeja(bandejaEntrega, entregaValor, entregaNota, entrega != null,
                entrega == null ? "—" : entrega.getNombre(), entrega == null ? " " : "Producto despachado");
        String notaCambio = entrega != null ? "Cambio de la compra" : (cambio > 0 ? "Dinero devuelto" : " ");
        pintarBandeja(bandejaCambio, cambioValor, cambioNota, cambio > 0, "Q" + cambio, notaCambio);
    }

    /** Mensaje en la pantalla hasta la siguiente actualización (errores, avisos). */
    void mostrarMensaje(String texto) {
        lcdMensaje.setText("<html><div style='width:128px'>" + texto + "</div></html>");
    }

    void setControlesHabilitados(boolean habilitado) {
        this.habilitado = habilitado;
        for (JButton control : controles) {
            control.setEnabled(habilitado);
        }
        actualizar();
    }

    private String mensajePorEstado() {
        switch (maquina.getEstado()) {
            case E1:
                return "Elija un producto";
            case E2:
                return "Presione COMPRAR";
            default:
                return maquina.getUltimaEntrega() != null ? "Retire su producto" : "Inserte monedas";
        }
    }

    private BotonMaquina crearRanura(Producto producto) {
        boolean agotado = producto.getCantidad() == 0;
        String existencias = agotado
                ? "<font color='#B03A2E'><b>AGOTADO</b></font>"
                : "<font size='2'>" + producto.getCantidad() + (producto.getCantidad() == 1 ? " unidad" : " unidades") + "</font>";
        BotonMaquina ranura = new BotonMaquina("<html><center><font size='5'><b>" + producto.getCodigo()
                + "</b></font><br>" + producto.getNombre() + "<br><b>Q" + producto.getPrecio() + "</b><br>"
                + existencias + "</center></html>", agotado ? Estilo.RANURA_AGOTADA : Estilo.RANURA);
        ranura.setForeground(Estilo.TEXTO_RANURA);
        ranura.setFont(Estilo.fuente(Font.PLAIN, 12));
        ranura.setPreferredSize(new Dimension(100, 100));
        ranura.setToolTipText("SELECCIONAR(" + producto.getCodigo() + ");");
        if (producto == maquina.getSeleccion()) {
            ranura.setBorde(Estilo.SELECCION);
        }
        ranura.setEnabled(habilitado);
        ranura.addActionListener(e -> acciones.seleccionar(producto.getCodigo()));
        return ranura;
    }

    private static void pintarBandeja(JPanel bandeja, JLabel valor, JLabel nota, boolean llena, String texto, String detalle) {
        bandeja.setBackground(llena ? Estilo.BANDEJA_LLENA : Estilo.BANDEJA);
        Color color = llena ? Estilo.TEXTO_RANURA : new Color(0xAEB6BF);
        valor.setForeground(color);
        nota.setForeground(color);
        valor.setText(texto);
        nota.setText(detalle);
    }

    /** Panel de la vitrina: se ajusta al ancho visible y crece hacia abajo con la barra de desplazamiento. */
    private static class VistaVitrina extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        VistaVitrina() {
            setBackground(Estilo.VITRINA);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
