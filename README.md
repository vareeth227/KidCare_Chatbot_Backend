# KidCare — Microservicio de Chatbot

Microservicio encargado de almacenar las interacciones registradas sobre un menor, tanto las generadas mediante el chatbot guiado por Claude como las ingresadas manualmente en modo fallback.

---

## Tecnologías

- Java 21
- Spring Boot 3.5.14
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data MongoDB
- Lombok
- Maven

---

## Puerto

```
8083
```

---

## Estructura del proyecto

```
src/main/java/com/kidcare/chatbot_service/
│
├── model/
│   └── Interaccion.java → Documento MongoDB que almacena cada interacción registrada sobre un menor
│
├── repository/
│   └── InteraccionRepository.java → Acceso a datos de la colección Interaccion (búsqueda por menor e historial)
│
├── dto/
│   ├── InteraccionRequestDTO.java  → Datos para registrar una interacción
│   └── InteraccionResponseDTO.java → Datos de respuesta de una interacción registrada
│
├── security/
│   ├── JwtUtil.java        → Genera, valida y extrae datos de tokens JWT
│   ├── JwtFilter.java      → Intercepta cada request y valida el token JWT del header
│   └── SecurityConfig.java → Configura rutas protegidas y política de sesión
│
├── service/
│   └── InteraccionService.java → Lógica de registro, edición, listado y eliminación de interacciones
│
├── controller/
│   └── InteraccionController.java → Endpoints CRUD de /api/interacciones
│
└── exception/
    └── GlobalExceptionHandler.java → Maneja errores de validación y excepciones de negocio
```

---

## Endpoints

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| POST | `/api/interacciones` | Autenticado | Registra una nueva interacción |
| GET | `/api/interacciones/menor/{idMenor}` | Autenticado | Lista todas las interacciones de un menor |
| PUT | `/api/interacciones/{id}` | Autenticado | Edita una interacción existente |
| DELETE | `/api/interacciones/{id}` | Autenticado | Elimina una interacción |

---

## Lógica de interacciones

- Toda interacción — tanto las generadas con chatbot como las manuales — se almacenan en MongoDB.
- El campo `fallback` indica si fue registrada en modo manual por fallo de la API de Claude.
- El chat en sí solo vive en memoria del teléfono y **no se persiste** en ninguna base de datos.
- Toda interacción es anonimizada por Claude antes de almacenarse.
- Solo el tutor puede eliminar interacciones; el delegado solo puede registrar y visualizar.

---

## Requisitos previos

- Java 21 instalado
- Maven instalado
- MongoDB Atlas o MongoDB local corriendo (cuando se conecte la BD)
- VS Code con Extension Pack for Java y Spring Boot Extension Pack

---

## Cómo iniciar el proyecto

### 1. Clonar el repositorio

```bash
git clone https://github.com/vareeth227/KidCare_Chatbot_Backend.git
cd KidCare_Chatbot_Backend
```

### 2. Configurar variables de entorno

Edita el archivo `src/main/resources/application.properties` con tu connection string de MongoDB cuando tengas la base de datos lista:

```properties
server.port=8083
spring.application.name=chatbot-service
spring.data.mongodb.uri=mongodb+srv://TU_USUARIO:TU_PASSWORD@cluster.mongodb.net/db_chatbot
jwt.secret=kidcare-secret-key-2024-segura-32chars
jwt.expiration=86400000
```

### 3. Compilar el proyecto

```bash
mvn clean install -DskipTests
```

### 4. Ejecutar el proyecto

```bash
mvn spring-boot:run
```

El microservicio estará disponible en `http://localhost:8083`

---

## Notas importantes

- El token JWT debe enviarse en el header `Authorization: Bearer <token>` en todas las rutas.
- La clave `jwt.secret` debe ser la misma en todos los microservicios de KidCare.
- Este microservicio usa **MongoDB** a diferencia de los otros que usan MySQL.
- Por ahora MongoDB está desactivado en `application.properties`. Cuando se conecte Docker hay que eliminar la línea `spring.autoconfigure.exclude`.

---

## Integrantes

| Nombre | Rol |
|--------|-----|
| Génesis Rojas | Líder de Proyecto / DBA / Analista Funcional |
| Francisco Monsalve | Frontend Mobile / QA |
| Benjamín Peña | Backend / Integración IA / DevOps |
