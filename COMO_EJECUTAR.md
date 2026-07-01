# BFA Espacial Dimensional — Guia de Ejecucion

## Opcion 1: Docker Compose (Recomendado)

Solo necesitas [Docker](https://www.docker.com/products/docker-desktop/) instalado.

```bash
git clone https://github.com/FLADavidentor/IQTest_BFA_Espacial_Dimensional.git
cd IQTest_BFA_Espacial_Dimensional
docker compose up
```

Abre: **http://localhost:8080/login**

Para detener: `Ctrl+C`
Para borrar datos: `docker compose down -v`

---

## Opcion 2: Script Nativo

### Requisitos
- **Java 17+** ([Adoptium](https://adoptium.net))
- **PostgreSQL 14+** ([postgresql.org](https://www.postgresql.org/download/))

### Windows (PowerShell)
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
.\setup.ps1
```

### Linux / macOS
```bash
chmod +x setup.sh
./setup.sh
```

El script:
1. Detecta Java y PostgreSQL
2. Inicia PostgreSQL si esta detenido
3. Crea la base de datos `bfa` (usuario: `bfa`, password: `bfa`)
4. Compila el proyecto
5. Inicia el servidor en http://localhost:8080
6. Al presionar `Ctrl+C`, detiene todo y limpia

---

## Opcion 3: Manual

### 1. Iniciar PostgreSQL y crear la base de datos
```sql
CREATE USER bfa WITH PASSWORD 'bfa';
CREATE DATABASE bfa OWNER bfa;
GRANT ALL PRIVILEGES ON DATABASE bfa TO bfa;
```

### 2. Compilar y ejecutar
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

### 3. Abrir en el navegador
http://localhost:8080/login

---

## Credenciales

| Rol         | Usuario      | Password        | Redirige a            |
|-------------|--------------|-----------------|------------------------|
| Admin       | `admin`      | `admin123`      | `/admin/reactivos`     |
| Evaluador   | `evaluador`  | `evaluador123`  | `/resultados`          |
| Estudiante  | `estudiante` | `estudiante123` | `/evaluacion/inicio`   |

---

## Capturas Automaticas (Opcional)

Requiere [Node.js](https://nodejs.org).

```bash
npm install
node automate.js
```

Genera 25 capturas en `screenshots/` cubriendo todos los flujos y verificaciones de seguridad.

---

## Estructura de Roles

```
Admin        → /admin/** (reactivos, baremos, usuarios, versiones, imagenes)
             → /resultados/** (historial, monitoreo, reportes, respuestas)

Evaluador    → /resultados/** (solo lectura: historial, monitoreo, reportes)
             ✗ /admin/** → 403 Forbidden

Estudiante   → /evaluacion/** (consignas, subtest, completado)
             ✗ /admin/** → 403 Forbidden
             ✗ /resultados/** → 403 Forbidden
```
