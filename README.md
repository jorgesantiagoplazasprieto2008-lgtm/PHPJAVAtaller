# Sistema de Votaciones y Encuestas Institucionales Seguras (SVIS)

**Taller de Desarrollo de Software &bull; SENA ADSO**  
**Instructor / Diseñador Curricular:** Osman Alonso Aranguren Escobar  
**Eje Temático:** Desacoplamiento de Servicios REST, Integridad Transaccional ACID, Voto Único por OTP y Control de Concurrencia.

---

## 1. Arquitectura de la Solución

El sistema implementa una arquitectura estrictamente desacoplada en tres capas independientes:

```
+------------------------------------+          HTTP cURL (JSON)          +--------------------------------------+
|            FRONTEND PHP            |   ==============================>   |          BACKEND JAVA WEB            |
|       (Cliente Desacoplado)        |   <==============================   |         (Maven + Servlets)           |
|                                    |          Respuestas REST            |  Apache Tomcat 11.0 / Jakarta EE 10  |
| - login.php (Gestión de Perfil)    |                                     |                                      |
| - admin/dashboard.php (Parámetros) |                                     | - EncuestasServlet (/api/encuestas)  |
| - admin/padron.php (Tokens OTP)    |                                     | - TokensServlet (/api/tokens)        |
| - admin/resultados.php (Métricas)  |                                     | - VotosServlet (/api/votos/emitir)   |
| - votar.php (Sufragio y Recibo)    |                                     | - AuthServlet (/api/auth)            |
+------------------------------------+                                     +-------------------+------------------+
        (Cero conexión a BD)                                                                   |
                                                                                    JDBC Transaccional
                                                                                   setAutoCommit(false)
                                                                                  SELECT ... FOR UPDATE
                                                                                               |
                                                                                               v
                                                                           +-------------------+------------------+
                                                                           |         BASE DE DATOS MYSQL          |
                                                                           |        Motor Transaccional InnoDB    |
                                                                           |                                      |
                                                                           | - usuarios (Padrón institucional)    |
                                                                           | - encuestas (Estado ACTIVA/CERRADA)  |
                                                                           | - opciones (Contador anónimo +1)     |
                                                                           | - tokens_otp (UNIQUE + Quema Atómica)|
                                                                           | - comprobantes_voto (Recibo SHA-256) |
                                                                           +--------------------------------------+
```

---

## 2. Cumplimiento de Reglas Críticas de Integridad

| Regla | Principio Técnico | Implementación en SVIS |
| :--- | :--- | :--- |
| **REGLA 1** | **Unicidad en Base de Datos** | Restricción `UNIQUE(encuesta_id, usuario_id)` en tabla `tokens_otp`. Imposibilita matemáticamente generar credenciales duplicadas a un mismo aprendiz. |
| **REGLA 2** | **Quema Atómica del OTP** | Todo el sufragio ocurre dentro de una transacción JDBC con `setAutoCommit(false)`. La actualización `DISPONIBLE -> USADO` y la suma `votos = votos + 1` se confirman juntas (`commit()`) o se descartan juntas (`rollback()`). |
| **REGLA 3** | **Aislamiento contra Race Conditions** | Bloqueo pesimista de fila: `SELECT ... FOR UPDATE` sobre la fila del token. Si dos pestañas o peticiones simultáneas intentan votar con el mismo token, una es serializada primero; la segunda lee el estado `USADO` y es rechazada con **`HTTP 409 Conflict`**. |
| **REGLA 4** | **Secreto Absoluto del Voto** | Ni la tabla `opciones` ni `comprobantes_voto` almacenan llaves foráneas a usuarios ni tokens. Solo se incrementa numéricamente el contador (+1) y se genera un hash anónimo SHA-256 como constancia de sufragio. |

---

## 3. Credenciales de Prueba del Padrón

| Rol | Documento | Contraseña | Nombre Completo |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `1001` | `admin123` | Administrador Electoral ADSO |
| **ESTUDIANTE** | `1002` | `aprendiz123` | Carlos Mario Restrepo |
| **ESTUDIANTE** | `1003` | `aprendiz123` | Valentina Gómez Peña |
| **ESTUDIANTE** | `1004` | `aprendiz123` | Andrés Felipe Morales |
| **ESTUDIANTE** | `1005` | `aprendiz123` | Daniela Sofía Vargas |
| **ESTUDIANTE** | `1006` | `aprendiz123` | Juan Camilo Quintero |

---

## 4. Estructura de Directorios

```
PHPJava/
├── database/
│   ├── init.sql                          # DDL: Creación de tablas, llaves e índices
│   └── seed.sql                          # Datos semilla iniciales del padrón
├── backend-java-maven/
│   ├── pom.xml                           # Descriptor Maven (Jakarta Servlet 6, MySQL, Gson)
│   ├── build_and_deploy.ps1              # Script de compilación y empaquetado WAR
│   └── src/main/
│       ├── java/com/svis/
│       │   ├── config/DatabaseConfig.java # Conexión JDBC local
│       │   ├── dao/                      # EncuestaDAO, TokenDAO, VotoDAO, UsuarioDAO
│       │   ├── filter/CorsFilter.java    # Filtro CORS y UTF-8
│       │   ├── model/                    # POJOs y DTOs
│       │   └── servlet/                  # Endpoints REST (Encuestas, Tokens, Votos, Auth)
│       └── webapp/WEB-INF/web.xml
├── frontend-php/
│   ├── config/config.php                 # Endpoint de API REST (http://localhost:8080/svis/api)
│   ├── services/ApiService.php           # Cliente cURL HTTP desacoplado
│   ├── includes/                         # header.php, footer.php, auth_check.php
│   ├── assets/                           # CSS moderno (style.css) y JS reactivo (app.js)
│   ├── admin/
│   │   ├── dashboard.php                 # Creación de consultas y apertura/cierre
│   │   ├── padron.php                    # Visualización de tokens OTP por aprendiz
│   │   └── resultados.php                # Escrutinio en tiempo real y porcentajes
│   ├── login.php                         # Autenticación de perfil ($_SESSION['rol'])
│   ├── logout.php                        # Destrucción segura de sesión
│   ├── index.php                         # Redirección contextual
│   └── votar.php                         # Portal de sufragio con bloqueo visual
└── tests-concurrencia/
    ├── test_race_condition.ps1           # Prueba de estrés en vivo (2 peticiones paralelas)
    ├── test_race_condition.sh            # Script Bash para entornos Linux/Mac
    └── auditoria_anonimato.sql           # Auditoría electoral de inalterabilidad y secreto
```

---

## 5. Instrucciones de Ejecución y Sustentación

### Paso 1: Inicializar la Base de Datos MySQL
```powershell
cmd.exe /c ' "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root --password=12345 < "c:\Users\Usuario\.antigravity-ide\PHPJava\database\init.sql" '
cmd.exe /c ' "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root --password=12345 < "c:\Users\Usuario\.antigravity-ide\PHPJava\database\seed.sql" '
```

### Paso 2: Compilar y Desplegar el Backend Java Web
```powershell
powershell.exe -ExecutionPolicy Bypass -File "c:\Users\Usuario\.antigravity-ide\PHPJava\backend-java-maven\build_and_deploy.ps1"
```
*El backend queda accesible en `http://localhost:8080/svis/api`.*

### Paso 3: Iniciar el Frontend PHP Desacoplado
```powershell
& "C:\xampp\php\php.exe" -S localhost:8000 -t "c:\Users\Usuario\.antigravity-ide\PHPJava\frontend-php"
```
*Acceso desde navegador: [http://localhost:8000/login.php](http://localhost:8000/login.php).*

---

## 6. Ejecución de Evidencias Evaluativas

### Evidencia 5.1: Prueba de Concurrencia y Doble Voto en Paralelo
Ejecute el script de estrés para verificar en tiempo real que el backend serializa la colisión respondiendo **`HTTP 200 OK`** y **`HTTP 409 Conflict`**:
```powershell
powershell.exe -ExecutionPolicy Bypass -File "c:\Users\Usuario\.antigravity-ide\PHPJava\tests-concurrencia\test_race_condition.ps1"
```

### Evidencia 5.2: Auditoría de Anonimato e Inalterabilidad en MySQL
Ejecute el script de inspección SQL para demostrar ante el instructor que no existe relación técnica entre el elector y la opción votada:
```powershell
cmd.exe /c ' "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root --password=12345 -t < "c:\Users\Usuario\.antigravity-ide\PHPJava\tests-concurrencia\auditoria_anonimato.sql" '
```
El resultado arrojará: `✓ CONCILIACIÓN MATEMÁTICA PERFECTA (100% AUDITABLE Y ANÓNIMO)`.
