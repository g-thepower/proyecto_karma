package com.jinyami.karma.client.render;

public class KarmaAnimationManager {
    private static float progress = 0.0f;      // Animación de aparición (0.0 a 1.0)
    private static float colorProgress = 0.0f; // Transición de blanco a negro (0.0 a 1.0)
    private static final float SPEED = 0.033f; // Velocidad de la animación

    public static void update(boolean isActive) {
        if (isActive) {
            // Primero completamos la animación de aparición de abajo a arriba
            if (progress < 1.0f) {
                progress = Math.min(1.0f, progress + SPEED);
            } else {
                // Una vez completada, iniciamos el cambio gradual de color de blanco a negro
                if (colorProgress < 1.0f) {
                    colorProgress = Math.min(1.0f, colorProgress + (SPEED * 0.5f)); // Puedes ajustar la velocidad del cambio aquí
                }
            }
        } else {
            // Al desactivar, revertimos primero el color y luego la animación de desaparición
            if (colorProgress > 0.0f) {
                colorProgress = Math.max(0.0f, colorProgress - SPEED);
            } else if (progress > 0.0f) {
                progress = Math.max(0.0f, progress - SPEED);
            }
        }
    }

    public static float getProgress() {
        return progress;
    }

    public static float getColorProgress() {
        return colorProgress;
    }
}