package com.automatas.expendedora.semantico;

import com.automatas.expendedora.errores.ErrorAnalisis;
import com.automatas.expendedora.lexico.TipoToken;
import com.automatas.expendedora.maquina.ErrorSemantico;
import com.automatas.expendedora.maquina.MaquinaExpendedora;
import com.automatas.expendedora.sintactico.Instruccion;
import java.util.ArrayList;
import java.util.List;

/**
 * Análisis semántico y ejecución: aplica una por una las instrucciones válidas a la máquina.
 * La máquina valida cada operación; aquí cada rechazo se convierte en un error semántico con
 * su número de línea y se arma la salida con el formato de los ejemplos del enunciado.
 * Sobre una copia de la máquina sirve para analizar sin ejecutar.
 */
public class Ejecutor {

    private static final int MAX_DIGITOS = 9;

    private final MaquinaExpendedora maquina;
    private final List<Instruccion> instrucciones;
    private final List<ErrorAnalisis> errores = new ArrayList<>();
    private int siguiente;

    public Ejecutor(MaquinaExpendedora maquina, List<Instruccion> instrucciones) {
        this.maquina = maquina;
        this.instrucciones = instrucciones;
    }

    public boolean hayMas() {
        return siguiente < instrucciones.size();
    }

    /** La instrucción que se ejecutará en el próximo paso, o null si ya no hay. */
    public Instruccion getSiguienteInstruccion() {
        return hayMas() ? instrucciones.get(siguiente) : null;
    }

    /** Ejecuta la siguiente instrucción y devuelve sus líneas de salida. */
    public List<String> ejecutarSiguiente() {
        Instruccion instruccion = instrucciones.get(siguiente++);
        List<String> salida = new ArrayList<>();
        try {
            for (String mensaje : aplicar(instruccion)) {
                salida.add("[OK] " + mensaje);
            }
        } catch (ErrorSemantico e) {
            ErrorAnalisis error = new ErrorAnalisis(ErrorAnalisis.Tipo.SEMANTICO,
                    instruccion.linea(), instruccion.columna(), e.getMessage());
            errores.add(error);
            salida.add(error.toString());
        }
        return salida;
    }

    public List<String> ejecutarTodo() {
        List<String> salida = new ArrayList<>();
        while (hayMas()) {
            salida.addAll(ejecutarSiguiente());
        }
        return salida;
    }

    public List<ErrorAnalisis> getErrores() {
        return errores;
    }

    private List<String> aplicar(Instruccion instruccion) throws ErrorSemantico {
        switch (instruccion.tipo()) {
            case PRODUCTO:
                return maquina.registrarProducto(instruccion.parametro(0), instruccion.parametro(1),
                        numero(instruccion, 2), numero(instruccion, 3));
            case CARGAR:
                return maquina.recargar(instruccion.parametro(0), numero(instruccion, 1));
            case MONEDA: {
                List<String> mensajes = new ArrayList<>(maquina.insertarMoneda(numero(instruccion, 0)));
                // Como en los ejemplos: el saldo se informa al terminar una serie de monedas válidas seguidas.
                if (!siguienteEsMonedaValida()) {
                    mensajes.add("Saldo disponible: Q" + maquina.getSaldo());
                }
                return mensajes;
            }
            case SELECCIONAR:
                return maquina.seleccionar(instruccion.parametro(0));
            case COMPRAR:
                return maquina.comprar();
            case CANCELAR:
                return maquina.cancelar();
            case MOSTRAR:
                return maquina.mostrar();
            default:
                throw new IllegalStateException("Instrucción desconocida: " + instruccion.tipo());
        }
    }

    private boolean siguienteEsMonedaValida() {
        Instruccion proxima = getSiguienteInstruccion();
        if (proxima == null || proxima.tipo() != TipoToken.MONEDA) {
            return false;
        }
        String valor = proxima.parametro(0);
        return valor.length() <= MAX_DIGITOS && MaquinaExpendedora.esMonedaPermitida(Integer.parseInt(valor));
    }

    private static int numero(Instruccion instruccion, int indice) throws ErrorSemantico {
        String lexema = instruccion.parametro(indice);
        if (lexema.length() > MAX_DIGITOS) {
            throw new ErrorSemantico("El valor " + lexema + " está fuera del rango permitido.");
        }
        return Integer.parseInt(lexema);
    }
}
