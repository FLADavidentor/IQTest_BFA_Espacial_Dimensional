#!/usr/bin/env bash
# ============================================================
#  BFA Espacial Dimensional — Setup, Run & Cleanup (Linux/macOS)
#  Assumes: only the cloned repository exists.
#  Run:  chmod +x setup.sh && ./setup.sh
#  Ctrl+C to stop — cleans up server and DB automatically.
# ============================================================
set -euo pipefail

SPRING_PID=""
PG_STARTED_BY_US=false

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
fail() { echo -e "${RED}[✗]${NC} $1"; exit 1; }

# ── Cleanup on Ctrl+C or exit ──
cleanup() {
    echo ""
    echo -e "${YELLOW}── Deteniendo servicios...${NC}"

    if [ -n "$SPRING_PID" ] && kill -0 "$SPRING_PID" 2>/dev/null; then
        echo "   Deteniendo Spring Boot (PID $SPRING_PID)..."
        kill "$SPRING_PID" 2>/dev/null || true
        wait "$SPRING_PID" 2>/dev/null || true
        ok "Spring Boot detenido."
    fi

    if $PG_STARTED_BY_US; then
        echo "   Deteniendo PostgreSQL (iniciado por este script)..."
        if command -v systemctl &>/dev/null; then
            sudo systemctl stop postgresql 2>/dev/null || true
        elif command -v brew &>/dev/null; then
            brew services stop postgresql@16 2>/dev/null || brew services stop postgresql 2>/dev/null || true
        elif command -v pg_ctl &>/dev/null; then
            pg_ctl stop -D /var/lib/postgresql/data 2>/dev/null || true
        fi
        ok "PostgreSQL detenido."
    else
        echo "   PostgreSQL no fue iniciado por este script, se deja en ejecución."
    fi

    echo ""
    echo -e "${GREEN}── Limpieza completada. ¡Hasta luego!${NC}"
    echo ""
    exit 0
}

trap cleanup SIGINT SIGTERM EXIT

# ── Start ──
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  BFA Espacial Dimensional — Setup${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# ── 1. Java 17+ ──
echo "── Verificando Java..."
if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -1 | awk -F'"' '{print $2}' | cut -d. -f1)
    if [ "$JAVA_VER" -ge 17 ] 2>/dev/null; then
        ok "Java $JAVA_VER detectado."
    else
        warn "Java $JAVA_VER detectado pero se requiere 17+. Instalando..."
        if command -v apt-get &>/dev/null; then
            sudo apt-get update -qq && sudo apt-get install -y openjdk-17-jdk -qq
        elif command -v dnf &>/dev/null; then
            sudo dnf install -y java-17-openjdk-devel
        elif command -v brew &>/dev/null; then
            brew install openjdk@17
        fi
    fi
else
    warn "Java no encontrado. Instalando OpenJDK 17..."
    if command -v apt-get &>/dev/null; then
        sudo apt-get update -qq && sudo apt-get install -y openjdk-17-jdk -qq
    elif command -v dnf &>/dev/null; then
        sudo dnf install -y java-17-openjdk-devel
    elif command -v brew &>/dev/null; then
        brew install openjdk@17
    else
        fail "No se pudo instalar Java. Instala OpenJDK 17+ manualmente."
    fi
fi
java -version 2>&1 | head -1 | xargs -I{} echo -e "${GREEN}[✓]${NC} {}"

# ── 2. PostgreSQL ──
echo ""
echo "── Verificando PostgreSQL..."
if ! command -v psql &>/dev/null; then
    warn "PostgreSQL no encontrado. Instalando..."
    if command -v apt-get &>/dev/null; then
        sudo apt-get install -y postgresql postgresql-client -qq
    elif command -v dnf &>/dev/null; then
        sudo dnf install -y postgresql-server postgresql
        sudo postgresql-setup --initdb 2>/dev/null || true
    elif command -v brew &>/dev/null; then
        brew install postgresql@16
    else
        fail "No se pudo instalar PostgreSQL. Instálalo manualmente."
    fi
fi
ok "PostgreSQL instalado: $(psql --version)"

# Start PostgreSQL if not running
echo "── Verificando si PostgreSQL está en ejecución..."
PG_RUNNING=false
if pg_isready -q 2>/dev/null; then
    PG_RUNNING=true
    ok "PostgreSQL ya está en ejecución."
fi

if ! $PG_RUNNING; then
    warn "PostgreSQL no está en ejecución. Iniciando..."
    PG_STARTED_BY_US=true
    if command -v systemctl &>/dev/null; then
        sudo systemctl start postgresql
    elif command -v brew &>/dev/null; then
        brew services start postgresql@16 2>/dev/null || brew services start postgresql
    fi
    sleep 2
    if pg_isready -q 2>/dev/null; then
        ok "PostgreSQL iniciado por este script."
    else
        fail "No se pudo iniciar PostgreSQL."
    fi
fi

# ── 3. Create DB + User ──
echo ""
echo "── Configurando base de datos..."

# Create user if not exists
if sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='bfa'" 2>/dev/null | grep -q 1; then
    ok "Usuario 'bfa' ya existe."
else
    sudo -u postgres psql -c "CREATE USER bfa WITH PASSWORD 'bfa';" 2>/dev/null
    ok "Usuario 'bfa' creado."
fi

# Create database if not exists
if sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='bfa'" 2>/dev/null | grep -q 1; then
    ok "Base de datos 'bfa' ya existe."
else
    sudo -u postgres psql -c "CREATE DATABASE bfa OWNER bfa;" 2>/dev/null
    ok "Base de datos 'bfa' creada."
fi

sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE bfa TO bfa;" 2>/dev/null || true

# Verify connection
echo "── Verificando conexión a la base de datos..."
if PGPASSWORD=bfa psql -U bfa -h localhost -d bfa -c "SELECT 1;" &>/dev/null; then
    ok "Conexión a 'bfa' verificada exitosamente."
else
    # Try with pg_hba.conf trust for localhost
    warn "No se pudo conectar. Puede que pg_hba.conf requiera ajustes."
    warn "Asegúrate de que pg_hba.conf permita conexión md5/scram para localhost."
    warn "Intentando continuar de todas formas..."
fi

# ── 4. Node.js (opcional) ──
echo ""
echo "── Verificando Node.js (opcional, para automatización)..."
if command -v node &>/dev/null; then
    ok "Node.js detectado: $(node --version)"
    if [ -f "package.json" ]; then
        npm install --silent 2>/dev/null || true
        ok "Dependencias npm instaladas."
    fi
else
    warn "Node.js no encontrado. Solo es necesario para automate.js."
fi

# ── 5. Compile ──
echo ""
echo "── Compilando el proyecto..."
chmod +x ./mvnw 2>/dev/null || true
./mvnw compile -q -DskipTests
ok "Proyecto compilado."

# ── 6. Run ──
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  ¡Iniciando BFA Espacial Dimensional!${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""
echo "  Credenciales:"
echo "    Admin:      admin / admin123"
echo "    Evaluador:  evaluador / evaluador123"
echo "    Estudiante: estudiante / estudiante123"
echo ""
echo -e "  ${YELLOW}Presiona Ctrl+C para detener y limpiar.${NC}"
echo ""

./mvnw spring-boot:run -q &
SPRING_PID=$!
ok "Spring Boot iniciado (PID $SPRING_PID). Esperando a que arranque..."

# Wait for Tomcat
for i in $(seq 1 30); do
    if curl -s -o /dev/null http://localhost:8080/login 2>/dev/null; then
        echo ""
        ok "¡Servidor listo! Abre: http://localhost:8080/login"
        echo ""
        break
    fi
    sleep 2
done

# Keep alive until Ctrl+C
wait "$SPRING_PID"
