# ARRANQUE — Chatbot Service (puerto 8083)

Guía paso a paso para iniciar el microservicio de chatbot en un equipo nuevo.
Sigue los pasos en orden, sin saltarte ninguno.

> **Este servicio usa MongoDB**, no MySQL. Asegúrate de tener el contenedor de MongoDB corriendo, no el de MySQL.

---

## Antes de empezar — verifica que tienes todo instalado

Abre una terminal y ejecuta cada comando. Si alguno falla, instala la herramienta antes de continuar.

```bash
java -version
```
Debe decir `openjdk 21`. Si no lo tienes: https://adoptium.net → descarga **Temurin 21 LTS**.

```bash
mvn -version
```
Debe decir `Apache Maven 3.9.x`. Si no lo tienes: https://maven.apache.org/download.cgi

```bash
docker --version
```
Debe decir `Docker version 24.x` o superior. Si no lo tienes: https://www.docker.com/products/docker-desktop → instala Docker Desktop y ábrelo antes de continuar.

```bash
git --version
```
Cualquier versión sirve. Si no lo tienes: https://git-scm.com

> **Importante:** este servicio depende de que el **usuario-service (puerto 8081)** esté corriendo para validar los tokens JWT. Inicia primero el usuario-service siguiendo su propio `ARRANQUE.md`.

---

## Paso 1 — Obtener el código

Si ya tienes el repositorio clonado:

```bash
cd KidCare_Chatbot_Backend
git fetch origin
git checkout benja
git pull origin benja
```

Si es la primera vez:

```bash
git clone https://github.com/vareeth227/KidCare_Chatbot_Backend.git
cd KidCare_Chatbot_Backend
git checkout benja
```

---

## Paso 2 — Iniciar Docker Desktop

Abre Docker Desktop desde el menú de inicio y espera a que el ícono de la ballena deje de animarse.

Verifica que Docker esté corriendo:

```bash
docker ps
```

---

## Paso 3 — Iniciar MongoDB con Docker

> Si ya iniciaste MongoDB y el contenedor `kidcare-mongodb` está corriendo, salta al Paso 4.

Crea un archivo `docker-compose.yml` en la carpeta raíz del proyecto:

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

Ejecuta:

```bash
docker compose up -d
```

Espera 15 segundos y verifica:

```bash
docker ps
```

Debes ver `kidcare-mongodb` con estado `Up`.

> **Nota:** MongoDB crea la base de datos `db_chatbot` automáticamente cuando se inserta el primer documento. No es necesario crearla manualmente.

---

## Paso 4 — Revisar application.properties

Abre `src/main/resources/application.properties`. La URI de MongoDB ya está configurada para el Docker del Paso 3. No necesitas cambiar nada para desarrollo local.

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/db_chatbot
```

---

## Paso 5 — Compilar el proyecto

```bash
mvn clean install -DskipTests
```

Espera a que aparezca:

```
BUILD SUCCESS
```

---

## Paso 6 — Iniciar el servicio

```bash
mvn spring-boot:run
```

Espera a que aparezca:

```
Started ChatbotServiceApplication in X.XXX seconds
```

El servicio queda disponible en `http://localhost:8083`. **No cierres esta terminal.**

---

## Paso 7 — Verificar que funciona

Primero obtén un token JWT del usuario-service:

**Windows PowerShell:**
```powershell
$resp = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body '{"email":"test@kidcare.com","password":"Password123"}'
$token = $resp.token
```

**Mac / Linux:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@kidcare.com","password":"Password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
```

Prueba el endpoint de interacciones:

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/interacciones/menor/1" -Method GET -Headers @{Authorization="Bearer $token"}
```

**Mac / Linux:**
```bash
curl -s http://localhost:8083/api/interacciones/menor/1 -H "Authorization: Bearer $TOKEN"
```

Respuesta esperada: lista vacía `[]`. Eso confirma que MongoDB está conectado y el JWT fue validado.

---

## Solución de problemas frecuentes

### Error: "Connection refused" a MongoDB
MongoDB no está corriendo. Ejecuta `docker ps` y verifica que `kidcare-mongodb` aparece con estado `Up`. Si no aparece, repite el Paso 3.

### Error: "No server chosen by WriteConcern"
MongoDB arrancó pero no terminó de inicializarse. Espera 15–30 segundos más y vuelve a intentar.

### Error 401 al probar con token
Verifica que la clave `jwt.secret` en `application.properties` es exactamente: `kidcare-secret-key-2024-segura-32chars`

### Error: "Port 8083 already in use"
```powershell
netstat -ano | findstr :8083
taskkill /PID <numero> /F
```

### Error: "BUILD FAILURE"
Haz scroll hacia arriba para ver el error real. Verifica Java 21 y que MongoDB está corriendo.
