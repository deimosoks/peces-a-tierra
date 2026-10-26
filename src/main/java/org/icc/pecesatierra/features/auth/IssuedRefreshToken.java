package org.icc.pecesatierra.features.auth;

import org.icc.pecesatierra.features.user.User;

import java.util.Date;

/**
 * Refresh token recien emitido (H-17).
 *
 * <p>Desde que la entidad {@link RefreshToken} guarda solo el hash, el valor en
 * claro no existe en ningun objeto persistido: viaja una sola vez, en este
 * registro, desde el servicio hasta la respuesta HTTP. No debe registrarse en
 * logs ni almacenarse en ningun otro sitio.</p>
 *
 * @param user      duenio de la sesion
 * @param token     valor en claro que recibe el cliente
 * @param expiresAt caducidad del token
 */
public record IssuedRefreshToken(User user, String token, Date expiresAt) {
}
