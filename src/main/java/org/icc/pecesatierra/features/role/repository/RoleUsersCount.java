package org.icc.pecesatierra.features.role.repository;

/**
 * H-39: proyeccion del conteo de usuarios por rol. Permite resolver el
 * {@code totalUsers} de toda la lista de roles con una sola consulta agregada
 * en lugar de una consulta por rol.
 */
public interface RoleUsersCount {

    String getRoleId();

    long getTotal();
}
