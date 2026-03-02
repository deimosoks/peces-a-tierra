# 🐟 Peces a Tierra - Sistema de Gestión Eclesiástica

**Peces a Tierra** es una plataforma integral diseñada para la administración eficiente de recursos humanos, membresía y control de asistencias en iglesias. El sistema automatiza la generación de reportes y gestiona la seguridad mediante un robusto sistema de Roles y Permisos (RBAC).

## 🚀 Características Principales

*   **👥 Gestión de Integrantes:** 
    *   Administración completa de perfiles (datos personales, contacto, ubicación).
    *   Gestión dinámica de **Categorías y Subcategorías** (sin valores hardcodeados).
*   **⚙️ Motor de Reglas de Categoría:**
    *   Sistema inteligente para definir reglas de pertenencia a categorías basadas en **edad y género**.
    *   Validación automática de integridad entre categorías y subcategorías.
*   **📅 Control de Asistencias:** 
    *   Registro masivo de asistencias por eventos/servicios.
    *   Validación de reglas de negocio (horarios, estado del miembro).
    *   Sistema de invalidación de asistencias con justificación y auditoría.
*   **🔒 Seguridad Avanzada:**
    *   Autenticación mediante **JWT (JSON Web Tokens)**.
    *   Autorización basada en Roles y Permisos dinámicos (RBAC).
    *   Protección de endpoints a nivel de método (`@PreAuthorize`).
*   **📊 Reportes y Dashboard:** Generación automática de estadísticas y reportes de actividad.

## 🛠️ Stack Tecnológico

El proyecto está construido utilizando las últimas tecnologías y mejores prácticas de desarrollo en Java:

*   **Lenguaje:** [Java 21](https://openjdk.org/projects/jdk/21/)
*   **Framework:** [Spring Boot 3.5.9](https://spring.io/projects/spring-boot)
*   **Base de Datos:** PostgreSQL
*   **Seguridad:** Spring Security 6 + JWT
*   **Persistencia:** Spring Data JPA (Hibernate)
*   **Herramientas de Build:** Gradle
*   **Utilidades:**
    *   **Lombok:** Para reducción de código boilerplate.
    *   **MapStruct:** Para mapeo eficiente entre Entidades y DTOs.
    *   **Apache Tika:** Para validación y manejo de archivos.
    *   **JPA Specifications:** Para filtrado dinámico de datos.

## 🏗️ Arquitectura

El proyecto sigue una arquitectura en capas limpia y escalable:

1.  **Web/Controllers:** Manejo de peticiones HTTP y validación de entrada.
2.  **Services:** Lógica de negocio pura, transaccionalidad y validaciones complejas.
3.  **Repositories:** Acceso a datos y consultas dinámicas mediante `JpaSpecificationExecutor`.
4.  **Domain/Entities:** Modelado de datos relacional.
5.  **Exceptions:** Manejo centralizado de errores con `GlobalExceptionHandler` y respuestas estandarizadas (`ApiException`).

## ⚙️ Configuración e Instalación

### Prerrequisitos

*   JDK 21 instalado.
*   PostgreSQL instalado y ejecutándose.

### Pasos para ejecutar

1.  **Clonar el repositorio:**
    ```bash
    git clone <url-del-repositorio>
    cd peces-a-tierra
    ```

2.  **Configurar la Base de Datos:**
    Asegúrate de crear una base de datos en PostgreSQL y configurar las credenciales en `src/main/resources/application.properties` (o mediante variables de entorno):

    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/peces_a_tierra_db
    spring.datasource.username=tu_usuario
    spring.datasource.password=tu_contraseña
    ```

3.  **Ejecutar la aplicación:**
    ```bash
    ./gradlew bootRun
    ```

4.  **Acceso:**
    La API estará disponible en `http://localhost:8080`.

## 📄 Licencia

Este proyecto está bajo la Licencia [MIT](LICENSE).
