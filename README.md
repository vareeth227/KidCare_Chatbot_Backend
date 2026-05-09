# KidCare — Microservicio de Chatbot

Microservicio encargado de almacenar las interacciones registradas sobre un menor, tanto las generadas mediante el chatbot guiado por Claude como las ingresadas manualmente en modo fallback.

---

## Equipo

| Nombre | Rol |
|---|---|
| Génesis Rojas | Líder de Proyecto / DBA / Analista Funcional |
| Francisco Monsalve | Frontend Mobile / QA |
| Benjamín Peña | Backend / Integración IA / DevOps |

---

## Tecnologías

- Java 21
- Spring Boot 3.5.14
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data MongoDB
- Lombok
- Maven

**Puerto:** `8083`

---

## Estructura del proyecto

```
src/main/java/com/kidcare/chatbot_service/
│
├── model/
│   └── Interaccion.java → Documento MongoDB con cada interacción registrada sobre un menor
│
├── repository/
│   └── InteraccionRepository.java → Búsqueda por menor e historial en MongoDB
│
├── dto/
│   ├── InteraccionRequestDTO.java  → Datos para registrar una interacción
│   └── InteraccionResponseDTO.java → Datos de respuesta de una interacción registrada
│
├── security/
│   ├── JwtUtil.java        → Genera, valida y extrae datos de tokens JWT
│   ├── JwtFilter.java      → Intercepta requests y valida el JWT del header
│   └── SecurityConfig.java → Rutas protegidas y política de sesión stateless
│
├── service/
│   └── InteraccionService.java → Registro, edición, listado y eliminación de interacciones
│
├── controller/
│   └── InteraccionController.java → CRUD /api/interacciones
│
└── exception/
    └── GlobalExceptionHandler.java → Errores de validación → 400 Bad Request
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

## Cómo iniciar en otro equipo

### Prerrequisitos

| Herramienta | Versión mínima | Descarga |
|---|---|---|
| Java JDK | 21 | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |
| Docker Desktop | 4.x | https://www.docker.com/products/docker-desktop |
| Git | cualquiera | https://git-scm.com |

Verifica la instalación:
```bash
java -version    # debe decir openjdk 21
mvn -version     # debe decir Apache Maven 3.9.x
docker --version # debe decir Docker version 24.x o superior
```

---

### Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/vareeth227/KidCare_Chatbot_Backend.git
cd KidCare_Chatbot_Backend
```

---

### Paso 2 — Iniciar MongoDB con Docker

Crea el archivo `docker-compose.yml` en la carpeta raíz del proyecto:

```yaml
services:
  mongodb:
    image: mongo:7.0
    container_name: kidcare-mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db

volumes:
  mongo_data:
```

Inicia el contenedor:

```bash
docker compose up -d
```

Espera 10–15 segundos y verifica:

```bash
docker ps
```

Debes ver `kidcare-mongodb` con estado `Up`.

> **Nota:** MongoDB crea la base de datos automáticamente al insertar el primer documento. No es necesario crearla manualmente.

---

### Paso 3 — Revisar application.properties

El archivo `src/main/resources/application.properties` ya está configurado para conectarse a MongoDB en localhost. No necesitas cambiar nada para desarrollo local.

---

### Paso 4 — Compilar

```bash
mvn clean install -DskipTests
```

Espera a que aparezca `BUILD SUCCESS`.

---

### Paso 5 — Ejecutar

```bash
mvn spring-boot:run
```

Espera a que aparezca:

```
Started ChatbotServiceApplication in X.XXX seconds
```

El servicio queda disponible en `http://localhost:8083`.

---

### Paso 6 — Verificar

Necesitas un token JWT válido del usuario-service (puerto 8081). Con ese token:

**PowerShell:**
```powershell
$token = "eyJ..."  # pega tu token JWT aquí
Invoke-RestMethod -Uri "http://localhost:8083/api/interacciones/menor/1" -Method GET -Headers @{Authorization="Bearer $token"}
```

Respuesta esperada: lista vacía `[]` — confirma que el JWT fue validado correctamente y MongoDB responde.

---

## Notas importantes

- El token JWT debe enviarse en el header `Authorization: Bearer <token>` en todas las rutas.
- La clave `jwt.secret` debe ser la misma en todos los microservicios de KidCare: `kidcare-secret-key-2024-segura-32chars`
- Este microservicio usa **MongoDB** a diferencia de los otros que usan MySQL.
- La colección de MongoDB se crea automáticamente al registrar la primera interacción.
