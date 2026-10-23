package com.automatas.expendedora.gui;

import com.automatas.expendedora.errores.ErrorAnalisis;
import com.automatas.expendedora.lexico.Token;
import com.automatas.expendedora.maquina.Automata;
import com.automatas.expendedora.maquina.Entrada;
import com.automatas.expendedora.maquina.Estado;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Pestañas de resultados: salida de la ejecución, tabla de tokens, tabla de errores y tabla δ. */
class PanelReportes extends JTabbedPane {

    private static final long serialVersionUID = 1L;
    private static final int TAB_SALIDA = 0;
    private static final int TAB_TOKENS = 1;
    private static final int TAB_ERRORES = 2;

    private final JTextPane consola = new JTextPane();
    private final DefaultTableModel modeloTokens = modelo("#", "Lexema", "Token", "Línea", "Columna");
    private final DefaultTableModel modeloErrores = modelo("#", "Tipo", "Línea", "Columna", "Descripción");
    private final JTable tablaErrores = new JTable(modeloErrores);
    private final JTable tablaDelta;
    private final transient List<ErrorAnalisis> errores = new ArrayList<>();
    private final transient SimpleAttributeSet estiloOk = estilo(Estilo.OK, false);
    private final transient SimpleAttributeSet estiloError = estilo(Estilo.ERROR, true);
    private final transient SimpleAttributeSet estiloInfo = estilo(Estilo.INFO, false);
    private final transient SimpleAttributeSet estiloTitulo = estilo(new Color(0x1F3A5F), true);

    PanelReportes(Supplier<Estado> estadoMostrado, IntConsumer alElegirLinea) {
        setFont(Estilo.fuente(Font.PLAIN, 13));

        consola.setEditable(false);
        consola.setFont(Estilo.mono(Font.PLAIN, 14));
        addTab("Salida", new JScrollPane(consola));

        JTable tablaTokens = new JTable(modeloTokens);
        configurar(tablaTokens, 40, 140, 200, 60, 70);
        addTab("Tokens", new JScrollPane(tablaTokens));

        configurar(tablaErrores, 35, 90, 50, 65, 520);
        for (int columna = 0; columna < 4; columna++) {
            int ancho = tablaErrores.getColumnModel().getColumn(columna).getPreferredWidth();
            tablaErrores.getColumnModel().getColumn(columna).setMinWidth(ancho);
            tablaErrores.getColumnModel().getColumn(columna).setMaxWidth(ancho);
        }
        tablaErrores.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                    boolean foco, int fila, int columna) {
                JLabel celda = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccionado, foco, fila, columna);
                celda.setToolTipText(String.valueOf(valor));
                return celda;
            }
        });
        tablaErrores.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaErrores.getSelectedRow();
            if (!e.getValueIsAdjusting() && fila >= 0) {
                alElegirLinea.accept(errores.get(fila).linea());
            }
        });
        addTab("Errores", new JScrollPane(tablaErrores));

        tablaDelta = new JTable(modeloDelta());
        tablaDelta.setRowHeight(23);
        tablaDelta.setEnabled(false);
        tablaDelta.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                    boolean foco, int fila, int columna) {
                JLabel celda = (JLabel) super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
                boolean actual = columna > 0 && Estado.values()[columna - 1] == estadoMostrado.get();
                celda.setBackground(actual ? new Color(0xFDEBD0) : Color.WHITE);
                celda.setFont(celda.getFont().deriveFont(actual || columna == 0 ? Font.BOLD : Font.PLAIN));
                celda.setHorizontalAlignment(columna == 0 ? LEFT : CENTER);
                return celda;
            }
        });
        tablaDelta.getColumnModel().getColumn(0).setPreferredWidth(150);
        StringBuilder leyenda = new StringBuilder("<html>Función de transición δ(estado, entrada) = estado destino. "
                + "Columnas: estado actual; filas: entrada; \"—\" = transición no permitida. "
                + "La columna resaltada es el estado actual.<br>");
        for (Estado estado : Estado.values()) {
            leyenda.append("<b>").append(estado).append("</b> ").append(estado.getDescripcion()).append(" &nbsp; ");
        }
        JLabel etiquetaLeyenda = new JLabel(leyenda.append("</html>").toString());
        etiquetaLeyenda.setFont(Estilo.fuente(Font.PLAIN, 12));
        etiquetaLeyenda.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JPanel panelDelta = new JPanel(new BorderLayout());
        panelDelta.add(etiquetaLeyenda, BorderLayout.NORTH);
        panelDelta.add(new JScrollPane(tablaDelta), BorderLayout.CENTER);
        addTab("Autómata (δ)", panelDelta);
    }

    // ---------- Salida ----------

    void limpiarSalida() {
        consola.setText("");
    }

    void agregarTitulo(String texto) {
        escribir(texto, estiloTitulo);
    }

    void agregarInfo(String texto) {
        escribir(texto, estiloInfo);
    }

    /** Colorea cada línea según su prefijo: [OK] en verde, [ERROR ...] en rojo. */
    void agregarSalida(List<String> lineas) {
        for (String linea : lineas) {
            if (linea.startsWith("[ERROR")) {
                escribir(linea, estiloError);
            } else if (linea.startsWith("[OK]")) {
                escribir(linea, estiloOk);
            } else {
                escribir(linea, estiloInfo);
            }
        }
    }

    void verSalida() {
        setSelectedIndex(TAB_SALIDA);
    }

    void verErrores() {
        setSelectedIndex(TAB_ERRORES);
    }

    private void escribir(String texto, SimpleAttributeSet estilo) {
        StyledDocument documento = consola.getStyledDocument();
        try {
            documento.insertString(documento.getLength(), texto + "\n", estilo);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        consola.setCaretPosition(documento.getLength());
    }

    // ---------- Tablas ----------

    void mostrarTokens(List<Token> tokens) {
        modeloTokens.setRowCount(0);
        int numero = 1;
        for (Token token : tokens) {
            modeloTokens.addRow(new Object[] {numero++, token.lexema(), token.tipo().getNombre(), token.linea(), token.columna()});
        }
        setTitleAt(TAB_TOKENS, "Tokens (" + tokens.size() + ")");
    }

    void mostrarErrores(List<ErrorAnalisis> nuevos) {
        errores.clear();
        errores.addAll(nuevos);
        modeloErrores.setRowCount(0);
        int numero = 1;
        for (ErrorAnalisis error : nuevos) {
            modeloErrores.addRow(new Object[] {numero++, tipo(error.tipo()), error.linea(), error.columna(),
                error.descripcion().replace("\n", " · ")});
        }
        setTitleAt(TAB_ERRORES, "Errores (" + nuevos.size() + ")");
    }

    void refrescarDelta() {
        tablaDelta.repaint();
    }

    private static String tipo(ErrorAnalisis.Tipo tipo) {
        switch (tipo) {
            case LEXICO:
                return "Léxico";
            case SINTACTICO:
                return "Sintáctico";
            default:
                return "Semántico";
        }
    }

    private static DefaultTableModel modeloDelta() {
        Estado[] estados = Estado.values();
        String[] columnas = new String[estados.length + 1];
        columnas[0] = "Entrada \\ Estado";
        for (int i = 0; i < estados.length; i++) {
            columnas[i + 1] = estados[i].name();
        }
        DefaultTableModel modelo = modelo(columnas);
        for (Entrada entrada : Entrada.values()) {
            Object[] fila = new Object[columnas.length];
            fila[0] = entrada.getEtiqueta();
            for (int i = 0; i < estados.length; i++) {
                Estado destino = Automata.getDelta().get(estados[i]).get(entrada);
                fila[i + 1] = destino == null ? "—" : destino.name();
            }
            modelo.addRow(fila);
        }
        return modelo;
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    private static void configurar(JTable tabla, int... anchos) {
        tabla.setRowHeight(22);
        tabla.setFont(Estilo.fuente(Font.PLAIN, 13));
        tabla.getTableHeader().setFont(Estilo.fuente(Font.BOLD, 13));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(false);
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    private static SimpleAttributeSet estilo(Color color, boolean negrita) {
        SimpleAttributeSet estilo = new SimpleAttributeSet();
        StyleConstants.setForeground(estilo, color);
        StyleConstants.setBold(estilo, negrita);
        return estilo;
    }
}
