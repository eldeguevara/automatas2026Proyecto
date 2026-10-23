package com.automatas.expendedora.maquina;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Autómata finito determinista que controla el proceso de compra.
 *
 * AFD = (Q, Σ, δ, q0, F)
 *   Q  = {E0, E1, E2, E3, E4, E5}        (enum Estado)
 *   Σ  = {MONEDA, SELECCIONAR, COMPRAR, CANCELAR, COMPRA_VALIDA, COMPRA_RECHAZADA, CON_CAMBIO, FINALIZAR}
 *   q0 = E0
 *   F  = {E0}                            (la operación termina al volver a esperar dinero)
 *   δ  = tabla DELTA; una entrada sin transición en el estado actual no está permitida.
 */
public class Automata {

    public record Transicion(Estado origen, Entrada entrada, Estado destino) {
    }

    public static final Estado INICIAL = Estado.E0;

    private static final Map<Estado, Map<Entrada, Estado>> DELTA = new EnumMap<>(Estado.class);

    static {
        for (Estado estado : Estado.values()) {
            DELTA.put(estado, new EnumMap<>(Entrada.class));
        }
        agregar(Estado.E0, Entrada.MONEDA, Estado.E1);
        agregar(Estado.E1, Entrada.MONEDA, Estado.E1);
        agregar(Estado.E1, Entrada.SELECCIONAR, Estado.E2);
        agregar(Estado.E1, Entrada.CANCELAR, Estado.E5);
        agregar(Estado.E2, Entrada.MONEDA, Estado.E2);
        agregar(Estado.E2, Entrada.SELECCIONAR, Estado.E2);
        agregar(Estado.E2, Entrada.COMPRAR, Estado.E3);
        agregar(Estado.E2, Entrada.CANCELAR, Estado.E5);
        agregar(Estado.E3, Entrada.COMPRA_VALIDA, Estado.E4);
        agregar(Estado.E3, Entrada.COMPRA_RECHAZADA, Estado.E2);
        agregar(Estado.E4, Entrada.CON_CAMBIO, Estado.E5);
        agregar(Estado.E4, Entrada.FINALIZAR, Estado.E0);
        agregar(Estado.E5, Entrada.FINALIZAR, Estado.E0);
    }

    private static void agregar(Estado origen, Entrada entrada, Estado destino) {
        DELTA.get(origen).put(entrada, destino);
    }

    private Estado actual = INICIAL;
    private Consumer<Transicion> observador;

    public Estado getEstado() {
        return actual;
    }

    public boolean permite(Entrada entrada) {
        return DELTA.get(actual).containsKey(entrada);
    }

    /** Aplica δ(actual, entrada). La máquina siempre consulta permite() antes de llamar. */
    public void transitar(Entrada entrada) {
        Estado destino = DELTA.get(actual).get(entrada);
        if (destino == null) {
            throw new IllegalStateException("δ(" + actual + ", " + entrada + ") no está definida");
        }
        Transicion transicion = new Transicion(actual, entrada, destino);
        actual = destino;
        if (observador != null) {
            observador.accept(transicion);
        }
    }

    /** Para la interfaz: recibe cada transición en el momento en que ocurre. */
    public void setObservador(Consumer<Transicion> observador) {
        this.observador = observador;
    }

    void setEstado(Estado estado) {
        this.actual = estado;
    }

    /** La tabla δ completa, para mostrarla en la interfaz. */
    public static Map<Estado, Map<Entrada, Estado>> getDelta() {
        return Collections.unmodifiableMap(DELTA);
    }
}
