package com.automatas.expendedora.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JButton;

/** Botón redondeado que se pinta igual en cualquier look and feel. */
class BotonMaquina extends JButton {

    private static final long serialVersionUID = 1L;
    private static final Color DESHABILITADO = new Color(0x9AA3AD);

    private Color color;
    private Color borde;

    BotonMaquina(String texto, Color color) {
        super(texto);
        this.color = color;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setMargin(new Insets(0, 0, 0, 0));
        setForeground(Color.WHITE);
        setFont(Estilo.fuente(java.awt.Font.BOLD, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    void setColor(Color color) {
        this.color = color;
        repaint();
    }

    /** Borde de resaltado (por ejemplo, el producto seleccionado); null para quitarlo. */
    void setBorde(Color borde) {
        this.borde = borde;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fondo = color;
        if (!isEnabled()) {
            fondo = DESHABILITADO;
        } else if (getModel().isPressed()) {
            fondo = color.darker();
        } else if (getModel().isRollover()) {
            fondo = mezclar(color, Color.WHITE, 0.15f);
        }
        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        if (borde != null) {
            g2.setColor(borde);
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    private static Color mezclar(Color a, Color b, float proporcion) {
        return new Color(
                Math.round(a.getRed() * (1 - proporcion) + b.getRed() * proporcion),
                Math.round(a.getGreen() * (1 - proporcion) + b.getGreen() * proporcion),
                Math.round(a.getBlue() * (1 - proporcion) + b.getBlue() * proporcion));
    }
}
