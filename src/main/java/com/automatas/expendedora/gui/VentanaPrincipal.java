package com.automatas.expendedora.gui;

import com.automatas.expendedora.Analisis;
import com.automatas.expendedora.errores.ErrorAnalisis;
import com.automatas.expendedora.json.ConfiguracionJson;
import com.automatas.expendedora.json.ErrorImportacion;
import com.automatas.expendedora.lexico.TipoToken;
import com.automatas.expendedora.maquina.ErrorSemantico;
import com.automatas.expendedora.maquina.Estado;
import com.automatas.expendedora.maquina.MaquinaExpendedora;
import com.automatas.expendedora.maquina.Producto;
import com.automatas.expendedora.semantico.Ejecutor;
import com.automatas.expendedora.sintactico.Instruccion;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Ventana del simulador: máquina, indicador de estados, editor de instrucciones y reportes. */
public final class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int PASO_EJECUCION_MS = 450;
    private static final String TITULO = "Simulador de Máquina Expendedora";

    /** Operación de un botón de la máquina. */
    private interface Operacion {
        List<String> ejecutar() throws ErrorSemantico;
    }

    private final transient MaquinaExpendedora maquina = new MaquinaExpendedora();
    private final PanelEstados panelEstados = new PanelEstados(maquina.getAutomata());
    private final PanelMaquina panelMaquina;
    private final EditorCodigo editor = new EditorCodigo();
    private final PanelReportes reportes;
    private final JLabel barraEstado = new JLabel(" ");
    private final JCheckBox chkMaquinaVacia = new JCheckBox("Máquina vacía al ejecutar", true);
    private final transient List<JComponent> controlesEdicion = new ArrayList<>();
    private final JButton btnDetener;
    private final Timer temporizador = new Timer(PASO_EJECUCION_MS, e -> pasoAutomatico());

    private File archivoActual;
    private boolean modificado;
    private boolean automatico;
    private transient Ejecutor ejecutor;
    private int totalInstrucciones;
    private int instruccionesHechas;

    public VentanaPrincipal() {
        super(TITULO);
        panelMaquina = new PanelMaquina(maquina, new PanelMaquina.Acciones() {
            @Override
            public void moneda(int valor) {
                accionDeBoton("MONEDA(" + valor + ");", () -> {
                    List<String> mensajes = new ArrayList<>(maquina.insertarMoneda(valor));
                    mensajes.add("Saldo disponible: Q" + maquina.getSaldo());
                    return mensajes;
                });
            }

            @Override
            public void seleccionar(String codigo) {
                accionDeBoton("SELECCIONAR(" + codigo + ");", () -> maquina.seleccionar(codigo));
            }

            @Override
            public void comprar() {
                accionDeBoton("COMPRAR();", maquina::comprar);
            }

            @Override
            public void cancelar() {
                accionDeBoton("CANCELAR();", maquina::cancelar);
            }
        });
        reportes = new PanelReportes(panelEstados::getEstadoMostrado, linea -> editor.irALinea(linea, true));
        panelEstados.setAlCambiar(reportes::refrescarDelta);
        editor.alCambiar(this::codigoModificado);

        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        barra.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        barra.add(boton("Nuevo", this::nuevo));
        barra.add(boton("Abrir .vm", this::abrir));
        barra.add(boton("Guardar", this::guardar));
        barra.addSeparator();
        barra.add(boton("Analizar", this::analizar));
        barra.add(boton("Ejecutar", this::ejecutar));
        barra.add(boton("Paso a paso", this::pasoAPaso));
        btnDetener = boton("Detener", this::detener);
        controlesEdicion.remove(btnDetener);
        btnDetener.setEnabled(false);
        barra.add(btnDetener);
        barra.addSeparator();
        chkMaquinaVacia.setFont(Estilo.fuente(Font.PLAIN, 13));
        chkMaquinaVacia.setToolTipText("Si está marcado, Analizar y Ejecutar empiezan con una máquina sin productos, en E0 y con Q0.");
        controlesEdicion.add(chkMaquinaVacia);
        barra.add(chkMaquinaVacia);
        barra.addSeparator();
        barra.add(boton("Importar JSON", this::importarJson));
        barra.add(boton("Exportar JSON", this::exportarJson));

        panelMaquina.setPreferredSize(new Dimension(500, 600));
        panelEstados.setPreferredSize(new Dimension(290, 600));
        JSplitPane izquierda = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelMaquina, panelEstados);
        izquierda.setResizeWeight(0.7);
        izquierda.setBorder(null);

        JPanel panelEditor = new JPanel(new BorderLayout());
        panelEditor.setBorder(BorderFactory.createTitledBorder("Editor de instrucciones (.vm)"));
        panelEditor.add(editor, BorderLayout.CENTER);
        panelEditor.setPreferredSize(new Dimension(520, 320));
        reportes.setPreferredSize(new Dimension(520, 320));
        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelEditor, reportes);
        derecha.setResizeWeight(0.45);
        derecha.setBorder(null);

        JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        principal.setResizeWeight(0.3);

        barraEstado.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        barraEstado.setFont(Estilo.fuente(Font.PLAIN, 12));

        add(barra, BorderLayout.NORTH);
        add(principal, BorderLayout.CENTER);
        add(barraEstado, BorderLayout.SOUTH);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salir();
            }
        });
        setSize(1366, 768);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);
        actualizarTitulo();
        reportes.agregarInfo("Escriba o abra un archivo .vm y presione Analizar, Ejecutar o Paso a paso.");
        reportes.agregarInfo("Los botones de la máquina también operan sobre ella y su resultado aparece aquí.");
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setFont(Estilo.fuente(Font.PLAIN, 13));
        boton.setFocusable(false);
        boton.addActionListener(e -> accion.run());
        controlesEdicion.add(boton);
        return boton;
    }

    // ---------- Archivos ----------

    private void nuevo() {
        if (!confirmarDescartar()) {
            return;
        }
        editor.setTexto("");
        archivoActual = null;
        modificado = false;
        actualizarTitulo();
    }

    private void abrir() {
        if (!confirmarDescartar()) {
            return;
        }
        JFileChooser selector = selector("Instrucciones de la máquina (*.vm)", "vm");
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        try {
            editor.setTexto(leer(archivo));
            archivoActual = archivo;
            modificado = false;
            actualizarTitulo();
            reportes.agregarInfo("Archivo abierto: " + archivo.getName());
            barraEstado.setText("Archivo abierto: " + archivo.getAbsolutePath());
        } catch (IOException e) {
            mostrarError("No se pudo abrir el archivo:\n" + e.getMessage());
        }
    }

    private boolean guardar() {
        File destino = archivoActual;
        if (destino == null) {
            destino = elegirDestino("Instrucciones de la máquina (*.vm)", "vm", "programa.vm");
            if (destino == null) {
                return false;
            }
        }
        try {
            Files.writeString(destino.toPath(), editor.getTexto(), StandardCharsets.UTF_8);
            archivoActual = destino;
            modificado = false;
            actualizarTitulo();
            barraEstado.setText("Guardado: " + destino.getAbsolutePath());
            return true;
        } catch (IOException e) {
            mostrarError("No se pudo guardar el archivo:\n" + e.getMessage());
            return false;
        }
    }

    private void codigoModificado() {
        if (!modificado) {
            modificado = true;
            actualizarTitulo();
        }
        if (!automatico) {
            // El análisis y la ejecución paso a paso dejan de corresponder al texto.
            ejecutor = null;
            editor.limpiarErrores();
            editor.quitarEjecucion();
        }
    }

    // ---------- Análisis y ejecución ----------

    private MaquinaExpendedora maquinaBase() {
        return chkMaquinaVacia.isSelected() ? new MaquinaExpendedora() : maquina;
    }

    private void analizar() {
        Analisis analisis = Analisis.de(editor.getTexto(), maquinaBase());
        mostrarAnalisis(analisis);
        reportes.limpiarSalida();
        reportes.agregarTitulo("Análisis " + deArchivo());
        reportes.agregarInfo("Tokens reconocidos: " + analisis.tokens().size());
        reportes.agregarInfo("Errores léxicos: " + analisis.contar(ErrorAnalisis.Tipo.LEXICO));
        reportes.agregarInfo("Errores sintácticos: " + analisis.contar(ErrorAnalisis.Tipo.SINTACTICO));
        reportes.agregarInfo("Errores semánticos: " + analisis.contar(ErrorAnalisis.Tipo.SEMANTICO));
        reportes.agregarInfo("Instrucciones válidas para ejecutar: " + analisis.instrucciones().size());
        for (ErrorAnalisis error : analisis.errores()) {
            reportes.agregarSalida(List.of(error.toString()));
        }
        if (analisis.errores().isEmpty()) {
            reportes.agregarSalida(List.of("[OK] Análisis sin errores."));
            reportes.verSalida();
        } else {
            reportes.verErrores();
        }
        barraEstado.setText("Análisis terminado: " + analisis.errores().size() + " errores, "
                + analisis.instrucciones().size() + " instrucciones válidas.");
    }

    private void mostrarAnalisis(Analisis analisis) {
        reportes.mostrarTokens(analisis.tokens());
        reportes.mostrarErrores(analisis.errores());
        editor.limpiarErrores();
        for (ErrorAnalisis error : analisis.errores()) {
            editor.marcarError(error.linea());
        }
    }

    /** Analiza, prepara la máquina y deja listo el ejecutor. Devuelve false si no hay nada que ejecutar. */
    private boolean prepararEjecucion() {
        Analisis analisis = Analisis.de(editor.getTexto(), maquinaBase());
        mostrarAnalisis(analisis);
        reportes.limpiarSalida();
        reportes.verSalida();
        List<ErrorAnalisis> descartadas = analisis.erroresDeEscritura();
        for (ErrorAnalisis error : descartadas) {
            reportes.agregarSalida(List.of(error.toString()));
        }
        if (analisis.instrucciones().isEmpty()) {
            reportes.agregarInfo("No hay instrucciones válidas para ejecutar.");
            return false;
        }
        if (chkMaquinaVacia.isSelected()) {
            maquina.cargarEstado(List.of(), Estado.E0, 0, null);
            panelEstados.sincronizar(Estado.E0, "Máquina vacía");
            panelMaquina.actualizar();
        }
        if (!descartadas.isEmpty()) {
            reportes.agregarInfo("Las instrucciones con errores léxicos o sintácticos no se ejecutan.");
        }
        reportes.agregarTitulo("Ejecución " + deArchivo() + " (" + analisis.instrucciones().size()
                + " instrucciones válidas)");
        ejecutor = new Ejecutor(maquina, analisis.instrucciones());
        totalInstrucciones = analisis.instrucciones().size();
        instruccionesHechas = 0;
        return true;
    }

    private void ejecutar() {
        if (automatico || !prepararEjecucion()) {
            return;
        }
        automatico = true;
        habilitarControles(false);
        temporizador.start();
    }

    private void pasoAutomatico() {
        if (panelEstados.estaAnimando()) {
            return;
        }
        if (ejecutor == null || !ejecutor.hayMas()) {
            terminarAutomatico("Ejecución terminada.");
            return;
        }
        ejecutarPaso();
    }

    private void pasoAPaso() {
        if (automatico) {
            return;
        }
        if (ejecutor == null) {
            if (!prepararEjecucion()) {
                return;
            }
            reportes.agregarInfo("Modo paso a paso: cada clic en \"Paso a paso\" ejecuta una instrucción.");
        }
        if (panelEstados.estaAnimando()) {
            return;
        }
        ejecutarPaso();
        if (!ejecutor.hayMas()) {
            reportes.agregarInfo("Fin de la ejecución paso a paso.");
            ejecutor = null;
        }
    }

    private void ejecutarPaso() {
        Instruccion instruccion = ejecutor.getSiguienteInstruccion();
        editor.marcarEjecucion(instruccion.linea());
        List<String> salida = ejecutor.ejecutarSiguiente();
        instruccionesHechas++;
        reportes.agregarSalida(salida);
        if (instruccion.tipo() == TipoToken.MOSTRAR) {
            mostrarInventario();
        }
        panelMaquina.actualizar();
        avisarEnPantalla(salida);
        barraEstado.setText("Instrucción " + instruccionesHechas + " de " + totalInstrucciones
                + " (línea " + instruccion.linea() + "): " + instruccion);
    }

    private void detener() {
        if (automatico) {
            terminarAutomatico("Ejecución detenida por el usuario.");
        }
    }

    private void terminarAutomatico(String mensaje) {
        temporizador.stop();
        automatico = false;
        ejecutor = null;
        habilitarControles(true);
        editor.quitarEjecucion();
        reportes.agregarInfo(mensaje);
        barraEstado.setText(mensaje + " Instrucciones ejecutadas: " + instruccionesHechas + " de " + totalInstrucciones + ".");
    }

    private void habilitarControles(boolean habilitado) {
        for (JComponent control : controlesEdicion) {
            control.setEnabled(habilitado);
        }
        btnDetener.setEnabled(!habilitado);
        editor.setEditable(habilitado);
        panelMaquina.setControlesHabilitados(habilitado);
    }

    /** MOSTRAR(): además del mensaje [OK], lista el inventario en la salida. */
    private void mostrarInventario() {
        if (maquina.getProductos().isEmpty()) {
            reportes.agregarInfo("  (sin productos)");
            return;
        }
        reportes.agregarInfo(String.format("  %-6s %-10s %-7s %s", "Código", "Nombre", "Precio", "Existencias"));
        for (Producto producto : maquina.getProductos()) {
            reportes.agregarInfo(String.format("  %-6s %-10s %-7s %s", producto.getCodigo(), producto.getNombre(),
                    "Q" + producto.getPrecio(), producto.getCantidad() == 0 ? "AGOTADO" : producto.getCantidad()));
        }
    }

    /** Si la instrucción falló, la pantalla de la máquina muestra el motivo. */
    private void avisarEnPantalla(List<String> salida) {
        for (String linea : salida) {
            if (linea.startsWith("[ERROR")) {
                String[] partes = linea.split("\n");
                panelMaquina.mostrarMensaje(partes.length > 1 ? partes[1] : partes[0]);
                return;
            }
        }
    }

    // ---------- Botones de la máquina ----------

    private void accionDeBoton(String instruccion, Operacion operacion) {
        if (automatico) {
            return;
        }
        reportes.verSalida();
        reportes.agregarInfo("> " + instruccion + "  (botón de la máquina)");
        try {
            List<String> salida = new ArrayList<>();
            for (String mensaje : operacion.ejecutar()) {
                salida.add("[OK] " + mensaje);
            }
            reportes.agregarSalida(salida);
            panelMaquina.actualizar();
        } catch (ErrorSemantico e) {
            reportes.agregarSalida(List.of("[ERROR SEMANTICO] Botón de la máquina:\n" + e.getMessage()));
            panelMaquina.actualizar();
            panelMaquina.mostrarMensaje(e.getMessage().split("\n")[0]);
        }
        barraEstado.setText("Botón: " + instruccion + "  Estado: " + maquina.getEstado() + "  Saldo: Q" + maquina.getSaldo());
    }

    // ---------- JSON ----------

    private void exportarJson() {
        File destino = elegirDestino("Configuración de la máquina (*.json)", "json", "maquina.json");
        if (destino == null) {
            return;
        }
        try {
            Files.writeString(destino.toPath(), ConfiguracionJson.exportar(maquina), StandardCharsets.UTF_8);
            reportes.verSalida();
            reportes.agregarSalida(List.of("[OK] Configuración y estado exportados: " + destino.getName()));
            barraEstado.setText("Exportado: " + destino.getAbsolutePath());
        } catch (IOException e) {
            mostrarError("No se pudo guardar el archivo JSON:\n" + e.getMessage());
        }
    }

    private void importarJson() {
        JFileChooser selector = selector("Configuración de la máquina (*.json)", "json");
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        reportes.verSalida();
        try {
            ConfiguracionJson.importar(leer(archivo), maquina);
        } catch (IOException e) {
            mostrarError("No se pudo leer el archivo:\n" + e.getMessage());
            return;
        } catch (ErrorImportacion e) {
            List<String> salida = new ArrayList<>();
            salida.add("[ERROR JSON] No se importó " + archivo.getName() + "; la máquina no cambió.");
            for (String error : e.getErrores()) {
                salida.add("[ERROR JSON] " + error);
            }
            reportes.agregarSalida(salida);
            JTextArea lista = new JTextArea(String.join("\n", e.getErrores()));
            lista.setEditable(false);
            lista.setLineWrap(true);
            lista.setWrapStyleWord(true);
            lista.setFont(Estilo.mono(Font.PLAIN, 13));
            JScrollPane scroll = new JScrollPane(lista);
            scroll.setPreferredSize(new Dimension(620, 220));
            JOptionPane.showMessageDialog(this, scroll, "El archivo JSON tiene errores", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ejecutor = null;
        panelEstados.sincronizar(maquina.getEstado(), "Importado desde JSON");
        panelMaquina.actualizar();
        reportes.agregarSalida(List.of(
                "[OK] Configuración importada: " + archivo.getName(),
                "[OK] Productos cargados: " + maquina.getProductos().size(),
                "[OK] Estado actual: " + maquina.getEstado(),
                "[OK] Saldo actual: Q" + maquina.getSaldo()));
        if (chkMaquinaVacia.isSelected()) {
            chkMaquinaVacia.setSelected(false);
            reportes.agregarInfo("Se desmarcó \"Máquina vacía al ejecutar\" para que los programas usen la máquina importada.");
        }
        barraEstado.setText("Importado: " + archivo.getAbsolutePath());
    }

    // ---------- Utilidades ----------

    private JFileChooser selector(String descripcion, String extension) {
        File inicio = archivoActual != null ? archivoActual.getParentFile() : new File("ejemplos");
        JFileChooser selector = new JFileChooser(inicio.isDirectory() ? inicio : new File(System.getProperty("user.dir")));
        selector.setFileFilter(new FileNameExtensionFilter(descripcion, extension));
        return selector;
    }

    private File elegirDestino(String descripcion, String extension, String nombreSugerido) {
        JFileChooser selector = selector(descripcion, extension);
        selector.setSelectedFile(new File(selector.getCurrentDirectory(), nombreSugerido));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith("." + extension)) {
            destino = new File(destino.getParentFile(), destino.getName() + "." + extension);
        }
        if (destino.exists() && JOptionPane.showConfirmDialog(this, "El archivo " + destino.getName()
                + " ya existe. ¿Desea reemplazarlo?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return null;
        }
        return destino;
    }

    /** Lee en UTF-8; si el archivo se guardó en ANSI (Bloc de notas antiguo), usa windows-1252. */
    private static String leer(File archivo) throws IOException {
        byte[] datos = Files.readAllBytes(archivo.toPath());
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(datos)).toString();
        } catch (CharacterCodingException e) {
            return new String(datos, Charset.forName("windows-1252"));
        }
    }

    private boolean confirmarDescartar() {
        if (!modificado) {
            return true;
        }
        int opcion = JOptionPane.showConfirmDialog(this, "El código tiene cambios sin guardar. ¿Desea guardarlos?",
                TITULO, JOptionPane.YES_NO_CANCEL_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            return guardar();
        }
        return opcion == JOptionPane.NO_OPTION;
    }

    private void salir() {
        if (confirmarDescartar()) {
            dispose();
            System.exit(0);
        }
    }

    private String deArchivo() {
        return archivoActual == null ? "del código del editor" : "de " + archivoActual.getName();
    }

    private void actualizarTitulo() {
        String nombre = archivoActual == null ? "sin título" : archivoActual.getName();
        setTitle(TITULO + " — " + nombre + (modificado ? " *" : ""));
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, TITULO, JOptionPane.ERROR_MESSAGE);
    }
}
