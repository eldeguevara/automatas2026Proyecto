package com.automatas.expendedora;

import com.automatas.expendedora.errores.ErrorAnalisis;
import com.automatas.expendedora.lexico.AnalizadorLexico;
import com.automatas.expendedora.lexico.TipoToken;
import com.automatas.expendedora.lexico.Token;
import com.automatas.expendedora.maquina.MaquinaExpendedora;
import com.automatas.expendedora.semantico.Ejecutor;
import com.automatas.expendedora.sintactico.AnalizadorSintactico;
import com.automatas.expendedora.sintactico.Instruccion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resultado de pasar un programa .vm por las tres fases: léxica, sintáctica y semántica.
 * La fase semántica se simula sobre una copia de la máquina, así que analizar no la modifica.
 */
public record Analisis(List<Token> tokens, List<Instruccion> instrucciones, List<ErrorAnalisis> errores) {

    public static Analisis de(String fuente, MaquinaExpendedora base) {
        AnalizadorLexico lexico = new AnalizadorLexico(fuente);
        List<Token> todos = lexico.analizar();
        AnalizadorSintactico sintactico = new AnalizadorSintactico(todos);
        List<Instruccion> instrucciones = sintactico.analizar();
        Ejecutor simulacion = new Ejecutor(base.copia(), instrucciones);
        simulacion.ejecutarTodo();

        List<ErrorAnalisis> errores = new ArrayList<>(lexico.getErrores());
        errores.addAll(sintactico.getErrores());
        errores.addAll(simulacion.getErrores());
        errores.sort(Comparator.comparingInt(ErrorAnalisis::linea).thenComparingInt(ErrorAnalisis::columna));

        // La tabla de tokens muestra solo los lexemas válidos; los inválidos están en los errores.
        List<Token> tokens = new ArrayList<>();
        for (Token token : todos) {
            if (token.tipo() != TipoToken.ERROR && token.tipo() != TipoToken.FIN) {
                tokens.add(token);
            }
        }
        return new Analisis(tokens, instrucciones, errores);
    }

    public long contar(ErrorAnalisis.Tipo tipo) {
        return errores.stream().filter(error -> error.tipo() == tipo).count();
    }

    /** Errores léxicos y sintácticos: los semánticos se reportan al ejecutar cada instrucción. */
    public List<ErrorAnalisis> erroresDeEscritura() {
        List<ErrorAnalisis> resultado = new ArrayList<>();
        for (ErrorAnalisis error : errores) {
            if (error.tipo() != ErrorAnalisis.Tipo.SEMANTICO) {
                resultado.add(error);
            }
        }
        return resultado;
    }
}
