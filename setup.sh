#!/usr/bin/env bash
# ============================================================
#  BFA Espacial Dimensional — Setup from scratch (Linux/macOS)
#  Assumes: only the cloned repository exists.
#  Run:  chmod +x setup.sh && ./setup.sh
# ============================================================
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
fail() { echo -e "${RED}[✗]${NC} $1"; exit 1; }

echo "========================================"
echo "  BFA Espacial Dimensional — Setup"
echo "========================================"
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
ok "Java listo: $(java -version 2>&1 | head -1)"

# ── 2. PostgreSQL ──
echo ""
echo "── Verificando PostgreSQL..."
if command -v psql &>/dev/null; then
    ok "PostgreSQL detectado: $(psql --version)"
else
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

# Start PostgreSQL
if command -v systemctl &>/dev/null; then
    sudo systemctl start postgresql 2>/dev/null || true
    sudo systemctl enable postgresql 2>/dev/null || true
elif command -v brew &>/dev/null; then
    brew services start postgresql@16 2>/dev/null || brew services start postgresql 2>/dev/null || true
fi
ok "PostgreSQL en ejecución."

# Create DB and user
echo "── Creando base de datos 'bfa' y usuario 'bfa'..."
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='bfa'" | grep -q 1 || \
    sudo -u postgres psql -c "CREATE USER bfa WITH PASSWORD 'bfa';"
sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='bfa'" | grep -q 1 || \
    sudo -u postgres psql -c "CREATE DATABASE bfa OWNER bfa;"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE bfa TO bfa;" 2>/dev/null || true
ok "Base de datos 'bfa' lista."

# ── 3. Node.js (opcional, solo para automate.js) ──
echo ""
echo "── Verificando Node.js (opcional, para automatización)..."
if command -v node &>/dev/null; then
    ok "Node.js detectado: $(node --version)"
    if [ -f "package.json" ]; then
        echo "   Instalando dependencias npm..."
        npm install --silent 2>/dev/null || true
    fi
else
    warn "Node.js no encontrado. Solo es necesario para automate.js (capturas automáticas)."
    warn "Instálalo desde https://nodejs.org si quieres usar la automatización."
fi

# ── 4. Build ──
echo ""
echo "── Compilando el proyecto..."
chmod +x ./mvnw 2>/dev/null || true
./mvnw compile -q -DskipTests
ok "Proyecto compilado."

# ── 5. Done ──
echo ""
echo "========================================"
echo -e "${GREEN}  ¡Setup completado!${NC}"
echo "========================================"
echo ""
echo "  Para iniciar el servidor:"
echo "    ./mvnw spring-boot:run"
echo ""
echo "  Luego abre:  http://localhost:8080/login"
echo ""
echo "  Credenciales:"
echo "    Admin:      admin / admin123"
echo "    Evaluador:  evaluador / evaluador123"
echo "    Estudiante: estudiante / estudiante123"
echo ""
echo "  (Opcional) Para capturas automáticas:"
echo "    node automate.js"
echo ""
