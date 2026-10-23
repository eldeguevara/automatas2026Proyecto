package com.automatas.expendedora;

import com.automatas.expendedora.gui.VentanaPrincipal;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
                // Si falla, Swing usa su apariencia por defecto.
            }
            new VentanaPrincipal().setVisible(true);
        });
    }
}
