package com.automatas.expendedora.maquina;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado de la máquina (inventario, saldo, selección) y validaciones semánticas.
 * Cada operación devuelve sus mensajes [OK] (sin el prefijo) o lanza ErrorSemantico.
 * Las operaciones de compra solo avanzan si el autómata tiene la transición.
 */
public class MaquinaExpendedora {

    public static final int[] MONEDAS_PERMITIDAS = {1, 5, 10};

    private final Map<String, Producto> productos = new LinkedHashMap<>();
    private final Automata automata = new Automata();
    private int saldo;
    private Producto seleccion;
    private boolean compraFinalizada;
    private boolean huboOperaciones;
    private Producto ultimaEntrega;
    private int ultimoCambio;

    // ---------- Instrucciones de configuración ----------

    public List<String> registrarProducto(String codigo, String nombre, int precio, int cantidad)
            throws ErrorSemantico {
        if (productos.containsKey(codigo)) {
            throw new ErrorSemantico("Código de producto duplicado: " + codigo + ". Se rechaza el registro.");
        }
        if (precio == 0) {
            throw new ErrorSemantico("El precio del producto " + codigo + " no puede ser Q0. Se rechaza el producto.");
        }
        productos.put(codigo, new Producto(codigo, nombre, precio, cantidad));
        return List.of("Producto " + codigo + " registrado: " + nombre);
    }

    public List<String> recargar(String codigo, int cantidad) throws ErrorSemantico {
        Producto producto = productos.get(codigo);
        if (producto == null) {
            throw new ErrorSemantico("No se puede recargar: el producto " + codigo + " no existe.");
        }
        if (cantidad == 0) {
            throw new ErrorSemantico("La cantidad a recargar debe ser mayor que cero.");
        }
        int antes = producto.getCantidad();
        if (cantidad > Integer.MAX_VALUE - antes) {
            throw new ErrorSemantico("La recarga excede el máximo de existencias permitido.");
        }
        producto.setCantidad(antes + cantidad);
        return List.of("Recarga " + codigo + ": " + antes + " + " + cantidad + " = " + producto.getCantidad() + " unidades");
    }

    // ---------- Instrucciones de operación ----------

    public List<String> insertarMoneda(int valor) throws ErrorSemantico {
        huboOperaciones = true;
        if (!esMonedaPermitida(valor)) {
            throw new ErrorSemantico("Moneda no permitida: Q" + valor + ". Solo se aceptan monedas de Q1, Q5 y Q10.");
        }
        automata.transitar(Entrada.MONEDA);
        saldo += valor;
        compraFinalizada = false;
        return List.of("Moneda recibida: Q" + valor);
    }

    public List<String> seleccionar(String codigo) throws ErrorSemantico {
        huboOperaciones = true;
        if (!automata.permite(Entrada.SELECCIONAR)) {
            throw new ErrorSemantico("Debe ingresar dinero antes de seleccionar un producto.");
        }
        Producto producto = productos.get(codigo);
        if (producto == null) {
            throw new ErrorSemantico("El producto " + codigo + " no existe.");
        }
        if (producto.getCantidad() == 0) {
            throw new ErrorSemantico("El producto " + codigo + " no tiene existencias.\nExistencias disponibles: 0");
        }
        automata.transitar(Entrada.SELECCIONAR);
        seleccion = producto;
        return List.of(
                "Producto seleccionado: " + codigo,
                "Producto disponible: " + producto.getNombre(),
                "Precio: Q" + producto.getPrecio());
    }

    public List<String> comprar() throws ErrorSemantico {
        huboOperaciones = true;
        if (!automata.permite(Entrada.COMPRAR)) {
            if (compraFinalizada) {
                throw new ErrorSemantico("La compra ya fue confirmada. No se puede entregar el producto dos veces.");
            }
            throw new ErrorSemantico("No existe un producto seleccionado.");
        }
        automata.transitar(Entrada.COMPRAR);

        Producto producto = seleccion;
        if (producto.getCantidad() == 0) {
            automata.transitar(Entrada.COMPRA_RECHAZADA);
            throw new ErrorSemantico("El producto " + producto.getCodigo() + " no tiene existencias.\nExistencias disponibles: 0");
        }
        if (saldo < producto.getPrecio()) {
            automata.transitar(Entrada.COMPRA_RECHAZADA);
            throw new ErrorSemantico("Dinero insuficiente para realizar la compra."
                    + "\nSaldo disponible: Q" + saldo
                    + "\nPrecio del producto: Q" + producto.getPrecio()
                    + "\nDinero faltante: Q" + (producto.getPrecio() - saldo));
        }

        List<String> mensajes = new ArrayList<>();
        automata.transitar(Entrada.COMPRA_VALIDA);
        mensajes.add("Compra autorizada.");
        producto.setCantidad(producto.getCantidad() - 1);
        ultimaEntrega = producto;
        mensajes.add("Producto despachado: " + producto.getNombre());

        ultimoCambio = saldo - producto.getPrecio();
        if (ultimoCambio > 0) {
            automata.transitar(Entrada.CON_CAMBIO);
        }
        mensajes.add("Cambio devuelto: Q" + ultimoCambio);
        mensajes.add("Existencias restantes " + producto.getCodigo() + ": " + producto.getCantidad());
        automata.transitar(Entrada.FINALIZAR);
        saldo = 0;
        seleccion = null;
        compraFinalizada = true;
        mensajes.add("Operación finalizada.");
        mensajes.add("Estado actual: " + automata.getEstado());
        mensajes.add("Saldo actual: Q" + saldo);
        return mensajes;
    }

    public List<String> cancelar() throws ErrorSemantico {
        huboOperaciones = true;
        if (!automata.permite(Entrada.CANCELAR)) {
            throw new ErrorSemantico("No hay una operación en curso para cancelar.");
        }
        automata.transitar(Entrada.CANCELAR);
        int devuelto = saldo;
        ultimoCambio = devuelto;
        ultimaEntrega = null;
        saldo = 0;
        seleccion = null;
        compraFinalizada = false;
        automata.transitar(Entrada.FINALIZAR);
        return List.of(
                "Operación cancelada.",
                "Dinero devuelto: Q" + devuelto,
                "Estado actual: " + automata.getEstado(),
                "Saldo actual: Q" + saldo);
    }

    public List<String> mostrar() {
        return List.of(huboOperaciones ? "Inventario actualizado mostrado." : "Inventario mostrado.");
    }

    // ---------- Consultas ----------

    public static boolean esMonedaPermitida(int valor) {
        for (int moneda : MONEDAS_PERMITIDAS) {
            if (moneda == valor) {
                return true;
            }
        }
        return false;
    }

    public Collection<Producto> getProductos() {
        return Collections.unmodifiableCollection(productos.values());
    }

    public Producto getProducto(String codigo) {
        return productos.get(codigo);
    }

    public int getSaldo() {
        return saldo;
    }

    public Estado getEstado() {
        return automata.getEstado();
    }

    public Automata getAutomata() {
        return automata;
    }

    /** Producto seleccionado en la operación actual, o null. */
    public Producto getSeleccion() {
        return seleccion;
    }

    /** Último producto entregado (null si la última operación fue una cancelación). */
    public Producto getUltimaEntrega() {
        return ultimaEntrega;
    }

    /** Dinero devuelto en la última compra o cancelación. */
    public int getUltimoCambio() {
        return ultimoCambio;
    }

    /**
     * Reemplaza inventario y estado con los de un archivo JSON ya validado.
     * Se conserva el mismo autómata para que la interfaz siga recibiendo sus transiciones.
     */
    public void cargarEstado(Collection<Producto> nuevosProductos, Estado estado, int nuevoSaldo, String codigoSeleccion) {
        productos.clear();
        for (Producto producto : nuevosProductos) {
            productos.put(producto.getCodigo(), producto);
        }
        automata.setEstado(estado);
        saldo = nuevoSaldo;
        seleccion = codigoSeleccion == null ? null : productos.get(codigoSeleccion);
        compraFinalizada = false;
        huboOperaciones = false;
        ultimaEntrega = null;
        ultimoCambio = 0;
    }

    /** Copia independiente (sin observador) para simular un programa sin tocar la máquina real. */
    public MaquinaExpendedora copia() {
        MaquinaExpendedora copia = new MaquinaExpendedora();
        for (Producto producto : productos.values()) {
            copia.productos.put(producto.getCodigo(), producto.copia());
        }
        copia.automata.setEstado(automata.getEstado());
        copia.saldo = saldo;
        copia.seleccion = seleccion == null ? null : copia.productos.get(seleccion.getCodigo());
        copia.compraFinalizada = compraFinalizada;
        copia.huboOperaciones = huboOperaciones;
        return copia;
    }
}
