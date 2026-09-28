package dev.thanly.tpa;

import java.util.UUID;

/**
 * Una solicitud pendiente.
 *
 * @param sender   quien la envio
 * @param target   quien la tiene que aceptar
 * @param type     THERE = el que envia va hacia el otro (/tpa); HERE = el otro viene (/tpahere)
 * @param expiresAt momento en que vence (milisegundos)
 */
public record TpaRequest(UUID sender, UUID target, Type type, long expiresAt) {

    public enum Type {
        THERE,
        HERE
    }

    public boolean expired(long now) {
        return now >= expiresAt;
    }

    public long secondsLeft(long now) {
        return Math.max(0, (expiresAt - now + 999) / 1000);
    }
}
