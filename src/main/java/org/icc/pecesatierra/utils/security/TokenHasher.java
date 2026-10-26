package org.icc.pecesatierra.utils.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hash de los refresh tokens antes de persistirlos (H-17).
 *
 * <p>El valor que viaja al cliente tiene 128 bytes de {@link java.security.SecureRandom},
 * asi que no hace falta BCrypt: un SHA-256 con un "pepper" estatico de servidor
 * basta para que un volcado de la tabla no permita suplantar sesiones. El pepper
 * se configura con {@code security.refresh-token.pepper} y por defecto tiene un
 * valor fijo para no romper el arranque en desarrollo.</p>
 *
 * <p>Cambiar el pepper invalida todos los refresh tokens vigentes: los usuarios
 * tendran que volver a iniciar sesion.</p>
 */
@Component
public class TokenHasher {

    private final byte[] pepper;

    public TokenHasher(@Value("${security.refresh-token.pepper:peces-a-tierra-refresh-token-pepper}")
                       String pepper) {
        this.pepper = pepper == null
                ? new byte[0]
                : pepper.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Devuelve el hash SHA-256 (hex, 64 caracteres) del token en claro.
     *
     * @param rawToken valor entregado al cliente; si es nulo se devuelve cadena
     *                 vacia, que nunca coincide con una fila existente
     */
    public String hash(String rawToken) {
        if (rawToken == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(pepper);
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM; si falta, el despliegue esta roto.
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
