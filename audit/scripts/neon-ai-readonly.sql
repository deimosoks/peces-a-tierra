-- =============================================================================
-- Neon: rol de solo lectura para el endpoint de IA
-- =============================================================================
-- Ejecutar en el SQL Editor de Neon, en la rama de producción, conectado como
-- `neondb_owner` (o el owner de la base).
--
-- IMPORTANTE: NO crear este rol con el botón "Add role" de la consola. Los roles
-- creados desde consola/CLI/API reciben membresía en `neon_superuser`, que
-- incluye CREATEDB, CREATEROLE, BYPASSRLS, pg_read_all_data y pg_write_all_data.
-- Este script lo crea por SQL para que sea un rol normal de Postgres.
--
-- ACCESO TEMPORAL: este script da SELECT sobre TODAS las tablas de `public`.
-- Eso incluye users.password_hash, refresh_token y PII. Es un riesgo aceptado
-- temporalmente; ver audit/TODO.md (H-01) para la migración a vistas seguras.
-- =============================================================================

-- 1. Reemplazar <PASSWORD> por una contraseña de al menos 12 caracteres y 60 bits
--    de entropía (mayúsculas, minúsculas, números y símbolos). No commitearla.
--    Si el rol ya existe, usar el ALTER ROLE de la sección 2.
CREATE ROLE peces_ai_readonly
    WITH LOGIN PASSWORD '<PASSWORD>'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;

-- 2. Si el rol ya existía (por ejemplo, creado con "Add role"):
-- ALTER ROLE peces_ai_readonly
--     WITH LOGIN PASSWORD '<PASSWORD>'
--     NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
-- REVOKE neon_superuser FROM peces_ai_readonly;

-- 3. Cortar privilegios heredados y dejar solo conexión + lectura.
--    Cambiar `neondb` si la base del proyecto tiene otro nombre.
REVOKE ALL ON DATABASE neondb FROM peces_ai_readonly;
GRANT CONNECT ON DATABASE neondb TO peces_ai_readonly;

REVOKE ALL ON SCHEMA public FROM peces_ai_readonly;
GRANT USAGE ON SCHEMA public TO peces_ai_readonly;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO peces_ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO peces_ai_readonly;

-- 4. Verificación de atributos: todo debe ser false salvo rolcanlogin.
SELECT rolname,
       rolsuper,
       rolcreatedb,
       rolcreaterole,
       rolbypassrls,
       rolcanlogin
FROM pg_roles
WHERE rolname = 'peces_ai_readonly';

-- 5. Verificación de grants: debe aparecer SELECT sobre las tablas de public.
SELECT table_schema,
       COUNT(*) AS tablas_con_select
FROM information_schema.role_table_grants
WHERE grantee = 'peces_ai_readonly'
  AND privilege_type = 'SELECT'
GROUP BY table_schema;

-- 6. Prueba desde psql con las credenciales del rol (no desde el SQL Editor,
--    que corre como owner). Cadena de conexión:
--    postgresql://peces_ai_readonly:<PASSWORD>@<HOST>/neondb?sslmode=require
--
--    SELECT COUNT(*) FROM users;   -- funciona con el acceso temporal
--    INSERT INTO ...;              -- debe fallar: permission denied
--
--    Para comprobar que no puede escribir:
--    DELETE FROM members WHERE false;  -- permission denied
--
-- 7. Cuando se migre a vistas seguras (audit/TODO.md, H-01):
--    REVOKE SELECT ON ALL TABLES IN SCHEMA public FROM peces_ai_readonly;
--    ALTER DEFAULT PRIVILEGES IN SCHEMA public
--        REVOKE SELECT ON TABLES FROM peces_ai_readonly;
--    REVOKE USAGE ON SCHEMA public FROM peces_ai_readonly;
--    -- y crear/otorgar ai_read:
--    CREATE SCHEMA ai_read;
--    GRANT USAGE ON SCHEMA ai_read TO peces_ai_readonly;
--    GRANT SELECT ON ALL TABLES IN SCHEMA ai_read TO peces_ai_readonly;
--    ALTER DEFAULT PRIVILEGES IN SCHEMA ai_read
--        GRANT SELECT ON TABLES TO peces_ai_readonly;
--
-- 8. Rollback completo:
--    REVOKE ALL ON ALL TABLES IN SCHEMA public FROM peces_ai_readonly;
--    REVOKE ALL ON SCHEMA public FROM peces_ai_readonly;
--    REVOKE ALL ON DATABASE neondb FROM peces_ai_readonly;
--    DROP ROLE peces_ai_readonly;
--
-- NOTA: los roles en Neon son por rama. Si se usan ramas hijas, hay que crear el
-- rol y aplicar los grants en cada rama donde se use.
