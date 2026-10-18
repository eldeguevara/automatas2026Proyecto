package com.automatas.expendedora;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("Simulador de Máquina Expendedora");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setSize(1200, 800);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
