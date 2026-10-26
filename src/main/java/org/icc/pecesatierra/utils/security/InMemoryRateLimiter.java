package org.icc.pecesatierra.utils.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de peticiones en memoria con ventana fija (H-18).
 *
 * <p>Sin dependencias nuevas: una {@link ConcurrentHashMap} de ventanas y un
 * {@link Clock} inyectable. Cada clave mantiene su propia ventana; al expirar se
 * reinicia el contador. El numero de claves vivas esta acotado: cuando se alcanza
 * el maximo se purgan las ventanas expiradas y, si aun asi no hay sitio, se
 * descarta la mas antigua. Asi un atacante que rote usuarios o IPs no puede hacer
 * crecer el mapa sin limite.</p>
 *
 * <p>El estado se pierde al reiniciar el proceso y no se comparte entre
 * instancias. Es suficiente como defensa en profundidad contra fuerza bruta
 * online; si algun dia hay varias instancias hay que mover el contador a un
 * almacen compartido.</p>
 */
public class InMemoryRateLimiter {

    private static final int DEFAULT_MAX_ENTRIES = 10_000;

    private final int maxRequests;
    private final long windowMillis;
    private final int maxEntries;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int maxRequests, Duration window) {
        this(maxRequests, window, DEFAULT_MAX_ENTRIES, Clock.systemUTC());
    }

    public InMemoryRateLimiter(int maxRequests, Duration window, int maxEntries, Clock clock) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("maxRequests debe ser mayor que cero");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("La ventana debe ser positiva");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
        this.maxEntries = Math.max(1, maxEntries);
        this.clock = clock;
    }

    /**
     * Registra una peticion para la clave indicada.
     *
     * @return {@code true} si la peticion esta dentro del limite de la ventana
     */
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        evictIfNeeded(now);

        boolean[] allowed = new boolean[1];
        windows.compute(key, (ignored, current) -> {
            Window window = current;
            if (window == null || window.isExpired(now, windowMillis)) {
                window = new Window(now);
            }
            allowed[0] = window.tryConsume(maxRequests);
            return window;
        });
        return allowed[0];
    }

    /** Claves actualmente en memoria; solo para pruebas y diagnostico. */
    public int trackedKeys() {
        return windows.size();
    }

    private void evictIfNeeded(long now) {
        if (windows.size() < maxEntries) {
            return;
        }

        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMillis));

        if (windows.size() >= maxEntries) {
            windows.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().startsAt()))
                    .map(Map.Entry::getKey)
                    .ifPresent(windows::remove);
        }
    }

    /** Ventana fija: instante de inicio y peticiones consumidas. */
    private static final class Window {

        private final long startsAt;
        private int count;

        private Window(long startsAt) {
            this.startsAt = startsAt;
        }

        private boolean isExpired(long now, long windowMillis) {
            return now - startsAt >= windowMillis;
        }

        private boolean tryConsume(int maxRequests) {
            count++;
            return count <= maxRequests;
        }

        private long startsAt() {
            return startsAt;
        }
    }
}
