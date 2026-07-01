<#
.SYNOPSIS
  BFA Espacial Dimensional - Setup, Run and Cleanup (Windows)
.DESCRIPTION
  Fully automatic: starts PostgreSQL, verifies DB, compiles, runs.
  Ctrl+C stops everything and cleans up.
.NOTES
  Prerequisites: Java 17+ and PostgreSQL on PATH or in standard locations.
  Run: Set-ExecutionPolicy Bypass -Scope Process -Force; .\setup.ps1
#>

$ErrorActionPreference = "Continue"
$script:SPRING_PROCESS = $null
$script:PG_STARTED_BY_US = $false
$script:PG_DATA_DIR = $null
$script:PG_SERVICE_NAME = $null

function Show-Ok($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Show-Warn($msg) { Write-Host "  [!]  $msg" -ForegroundColor Yellow }
function Show-Fail($msg) { Write-Host "  [X]  $msg" -ForegroundColor Red; exit 1 }

function Invoke-Cleanup {
    Write-Host ""
    Write-Host "-- Deteniendo servicios..." -ForegroundColor Yellow
    if ($script:SPRING_PROCESS -ne $null -and -not $script:SPRING_PROCESS.HasExited) {
        Stop-Process -Id $script:SPRING_PROCESS.Id -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
        Show-Ok "Spring Boot detenido."
    }
    if ($script:PG_STARTED_BY_US) {
        if ($script:PG_SERVICE_NAME) {
            Stop-Service $script:PG_SERVICE_NAME -Force -ErrorAction SilentlyContinue
        } elseif ($script:PG_DATA_DIR) {
            & pg_ctl stop -D $script:PG_DATA_DIR -m fast 2>$null
        }
        Show-Ok "PostgreSQL detenido."
    }
    Write-Host "-- Limpieza completada." -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BFA Espacial Dimensional - Setup"      -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── 1. Java ──
Write-Host "-- [1/5] Java..." -ForegroundColor White
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) { Show-Fail "Java no encontrado. Instala JDK 17+ desde https://adoptium.net" }
$jvOut = & java -version 2>&1 | Select-Object -First 1
Show-Ok "$jvOut"

# ── 2. PostgreSQL binary ──
Write-Host "-- [2/5] PostgreSQL..." -ForegroundColor White
$pgBinDirs = @(
    "C:\Program Files\PostgreSQL\17\bin",
    "C:\Program Files\PostgreSQL\16\bin",
    "C:\Program Files\PostgreSQL\15\bin",
    "$env:USERPROFILE\pgsql\bin",
    "C:\pgsql\bin"
)
foreach ($p in $pgBinDirs) {
    if (Test-Path "$p\psql.exe") { $env:PATH += ";$p"; break }
}
if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    Show-Fail "PostgreSQL no encontrado. Instala desde https://www.postgresql.org/download/"
}
Show-Ok (& psql --version)

# ── 3. Start PostgreSQL ──
Write-Host "-- [3/5] Iniciando PostgreSQL..." -ForegroundColor White
$pgSvc = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($pgSvc) {
    $script:PG_SERVICE_NAME = $pgSvc.Name
    if ($pgSvc.Status -ne "Running") {
        $script:PG_STARTED_BY_US = $true
        Start-Service $pgSvc.Name; Start-Sleep 3
    }
    Show-Ok "Servicio $($pgSvc.Name) activo."
} else {
    # Portable pg_ctl
    $dataDirs = @("$env:USERPROFILE\pgsql\data","C:\pgsql\data") | Where-Object { Test-Path $_ }
    if ($dataDirs.Count -eq 0 -and $env:PGDATA -and (Test-Path $env:PGDATA)) { $dataDirs = @($env:PGDATA) }
    if ($dataDirs.Count -eq 0) { Show-Fail "No se encontro data directory de PostgreSQL." }
    $script:PG_DATA_DIR = $dataDirs[0]
    $st = & pg_ctl status -D $script:PG_DATA_DIR 2>$null
    if ("$st" -notmatch "running") {
        $script:PG_STARTED_BY_US = $true
        & pg_ctl start -D $script:PG_DATA_DIR -l "$($script:PG_DATA_DIR)\server.log" -w 2>$null | Out-Null
        # Wait until accepting connections (crash recovery can take time)
        for ($w = 0; $w -lt 15; $w++) {
            Start-Sleep 2
            $ready = & pg_isready -h 127.0.0.1 2>$null
            if ("$ready" -match "accepting") { break }
        }
    }
    Show-Ok "PostgreSQL activo (pg_ctl)."
}

# ── 4. Database ──
Write-Host "-- [4/5] Base de datos..." -ForegroundColor White

# First: check if bfa DB already works (most common case after first run)
$env:PGPASSWORD = "bfa"
$bfaOk = & psql -U bfa -h 127.0.0.1 -d bfa -tc "SELECT 'OK'" 2>$null
if ("$bfaOk" -match "OK") {
    Show-Ok "BD 'bfa' lista (ya existia)."
} else {
    # Need to create. Try superuser: postgres, current user, common names
    $created = $false
    $superUsers = @("postgres", $env:USERNAME, "admin", "root")
    foreach ($su in $superUsers) {
        $env:PGPASSWORD = ""
        $check = & psql -U $su -h 127.0.0.1 -d postgres -tc "SELECT 1" 2>$null
        if ("$check" -match "1") {
            Show-Ok "Superusuario: $su"
            & psql -U $su -h 127.0.0.1 -d postgres -c "CREATE USER bfa WITH PASSWORD 'bfa'" 2>$null
            & psql -U $su -h 127.0.0.1 -d postgres -c "CREATE DATABASE bfa OWNER bfa" 2>$null
            & psql -U $su -h 127.0.0.1 -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE bfa TO bfa" 2>$null
            $created = $true
            break
        }
    }
    if (-not $created) {
        # Try createdb/createuser (works with trust auth on some setups)
        & createuser -h 127.0.0.1 bfa 2>$null
        & createdb -h 127.0.0.1 -O bfa bfa 2>$null
        $env:PGPASSWORD = "bfa"
        & psql -U bfa -h 127.0.0.1 -d bfa -c "SELECT 1" 2>$null | Out-Null
    }
    # Final verify
    $env:PGPASSWORD = "bfa"
    $finalCheck = & psql -U bfa -h 127.0.0.1 -d bfa -tc "SELECT 'OK'" 2>$null
    if ("$finalCheck" -match "OK") {
        Show-Ok "BD 'bfa' creada y verificada."
    } else {
        Show-Fail "No se pudo crear la BD. Ejecuta manualmente: CREATE USER bfa PASSWORD 'bfa'; CREATE DATABASE bfa OWNER bfa;"
    }
}
$env:PGPASSWORD = ""

# ── 5. Compile ──
Write-Host "-- [5/5] Compilando..." -ForegroundColor White
& .\mvnw.cmd compile -q -DskipTests 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { Show-Fail "Error de compilacion." }
Show-Ok "Compilado."

# ── Run ──
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Servidor iniciando..."                  -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  http://localhost:8080/login"
Write-Host "  Admin:      admin / admin123"          -ForegroundColor Gray
Write-Host "  Evaluador:  evaluador / evaluador123"  -ForegroundColor Gray
Write-Host "  Estudiante: estudiante / estudiante123" -ForegroundColor Gray
Write-Host ""
Write-Host "  Ctrl+C para detener." -ForegroundColor Yellow
Write-Host ""

$script:SPRING_PROCESS = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList "spring-boot:run" -NoNewWindow -PassThru

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep 2
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/login" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($r.StatusCode -eq 200) { Show-Ok "Servidor listo!"; break }
    } catch {}
}

try { $script:SPRING_PROCESS.WaitForExit() } finally { Invoke-Cleanup }
