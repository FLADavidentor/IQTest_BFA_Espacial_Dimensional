# ============================================================
#  BFA Espacial Dimensional — Setup from scratch (Windows)
#  Assumes: only the cloned repository exists.
#  Run in PowerShell as Administrator:
#    Set-ExecutionPolicy Bypass -Scope Process -Force
#    .\setup.ps1
# ============================================================
$ErrorActionPreference = "Stop"

function Ok($msg)   { Write-Host "[OK] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[!]  $msg" -ForegroundColor Yellow }
function Fail($msg) { Write-Host "[X]  $msg" -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BFA Espacial Dimensional - Setup"      -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── Check Administrator ──
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Warn "Se recomienda ejecutar como Administrador para instalar software."
    Warn "Si ya tienes Java 17+, PostgreSQL y Node.js, puedes continuar."
    $continue = Read-Host "Continuar de todos modos? (S/n)"
    if ($continue -eq "n") { exit 0 }
}

# ── 1. Java 17+ ──
Write-Host "-- Verificando Java..." -ForegroundColor White
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    $javaVerOutput = & java -version 2>&1 | Select-Object -First 1
    if ($javaVerOutput -match '"(\d+)') {
        $javaMajor = [int]$Matches[1]
        if ($javaMajor -ge 17) {
            Ok "Java $javaMajor detectado."
        } else {
            Warn "Java $javaMajor detectado pero se requiere 17+."
            Warn "Instalando OpenJDK 17 via winget..."
            winget install --id Microsoft.OpenJDK.17 --accept-source-agreements --accept-package-agreements --silent
            # Refresh PATH
            $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
        }
    }
} else {
    Warn "Java no encontrado. Instalando OpenJDK 17 via winget..."
    $wingetCmd = Get-Command winget -ErrorAction SilentlyContinue
    if (-not $wingetCmd) {
        Fail "winget no disponible. Instala Java 17+ manualmente desde https://adoptium.net"
    }
    winget install --id Microsoft.OpenJDK.17 --accept-source-agreements --accept-package-agreements --silent
    # Refresh PATH
    $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
}
Ok "Java listo: $(java -version 2>&1 | Select-Object -First 1)"

# ── 2. PostgreSQL ──
Write-Host ""
Write-Host "-- Verificando PostgreSQL..." -ForegroundColor White
$psqlCmd = Get-Command psql -ErrorAction SilentlyContinue
if ($psqlCmd) {
    Ok "PostgreSQL detectado: $(psql --version)"
} else {
    Warn "PostgreSQL no encontrado. Instalando via winget..."
    winget install --id PostgreSQL.PostgreSQL.16 --accept-source-agreements --accept-package-agreements --silent
    # Refresh PATH
    $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH", "User")
    # Also add common PostgreSQL paths
    $pgPaths = @(
        "C:\Program Files\PostgreSQL\16\bin",
        "C:\Program Files\PostgreSQL\17\bin",
        "C:\Program Files\PostgreSQL\15\bin"
    )
    foreach ($p in $pgPaths) {
        if (Test-Path $p) { $env:PATH += ";$p" }
    }
}

# Start PostgreSQL service
Write-Host "-- Iniciando servicio PostgreSQL..." -ForegroundColor White
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($pgService) {
    if ($pgService.Status -ne "Running") {
        Start-Service $pgService.Name -ErrorAction SilentlyContinue
    }
    Ok "Servicio PostgreSQL en ejecucion: $($pgService.Name)"
} else {
    Warn "No se encontro servicio PostgreSQL. Asegurate de que este en ejecucion."
}

# Create DB and user
Write-Host "-- Creando base de datos 'bfa' y usuario 'bfa'..." -ForegroundColor White
try {
    # Try with default postgres superuser
    $env:PGPASSWORD = "postgres"
    
    # Check if user exists
    $userExists = & psql -U postgres -h localhost -tc "SELECT 1 FROM pg_roles WHERE rolname='bfa'" 2>$null
    if ($userExists -notmatch "1") {
        & psql -U postgres -h localhost -c "CREATE USER bfa WITH PASSWORD 'bfa';" 2>$null
        Ok "Usuario 'bfa' creado."
    } else {
        Ok "Usuario 'bfa' ya existe."
    }
    
    # Check if DB exists
    $dbExists = & psql -U postgres -h localhost -tc "SELECT 1 FROM pg_database WHERE datname='bfa'" 2>$null
    if ($dbExists -notmatch "1") {
        & psql -U postgres -h localhost -c "CREATE DATABASE bfa OWNER bfa;" 2>$null
        Ok "Base de datos 'bfa' creada."
    } else {
        Ok "Base de datos 'bfa' ya existe."
    }
    
    & psql -U postgres -h localhost -c "GRANT ALL PRIVILEGES ON DATABASE bfa TO bfa;" 2>$null
    Ok "Base de datos 'bfa' lista."
} catch {
    Warn "No se pudo crear la BD automaticamente."
    Write-Host ""
    Write-Host "  Crea la BD manualmente con estos comandos:" -ForegroundColor Yellow
    Write-Host "    psql -U postgres" -ForegroundColor Gray
    Write-Host "    CREATE USER bfa WITH PASSWORD 'bfa';" -ForegroundColor Gray
    Write-Host "    CREATE DATABASE bfa OWNER bfa;" -ForegroundColor Gray
    Write-Host "    GRANT ALL PRIVILEGES ON DATABASE bfa TO bfa;" -ForegroundColor Gray
    Write-Host ""
}

$env:PGPASSWORD = ""

# ── 3. Node.js (opcional) ──
Write-Host ""
Write-Host "-- Verificando Node.js (opcional, para automatizacion)..." -ForegroundColor White
$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
if ($nodeCmd) {
    Ok "Node.js detectado: $(node --version)"
    if (Test-Path "package.json") {
        Write-Host "   Instalando dependencias npm..."
        npm install --silent 2>$null
    }
} else {
    Warn "Node.js no encontrado. Solo es necesario para automate.js (capturas)."
    Warn "Instalalo desde https://nodejs.org si quieres usar la automatizacion."
}

# ── 4. Build ──
Write-Host ""
Write-Host "-- Compilando el proyecto..." -ForegroundColor White
& .\mvnw.cmd compile -q -DskipTests
Ok "Proyecto compilado exitosamente."

# ── 5. Done ──
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Setup completado!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Para iniciar el servidor:" -ForegroundColor White
Write-Host "    .\mvnw.cmd spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "  Luego abre:  http://localhost:8080/login" -ForegroundColor White
Write-Host ""
Write-Host "  Credenciales:" -ForegroundColor White
Write-Host "    Admin:      admin / admin123" -ForegroundColor Gray
Write-Host "    Evaluador:  evaluador / evaluador123" -ForegroundColor Gray
Write-Host "    Estudiante: estudiante / estudiante123" -ForegroundColor Gray
Write-Host ""
Write-Host "  (Opcional) Para capturas automaticas:" -ForegroundColor White
Write-Host "    node automate.js" -ForegroundColor Gray
Write-Host ""
