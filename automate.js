const puppeteer = require('puppeteer');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

async function main() {
    const artDir = path.join(__dirname, 'screenshots');
    if (!fs.existsSync(artDir)) {
        fs.mkdirSync(artDir);
    }
    
    console.log("0. Iniciando base de datos PostgreSQL...");
    const pgProcess = spawn('C:\\Users\\devuser\\pgsql\\bin\\pg_ctl.exe', [
        '-D', 'C:\\Users\\devuser\\pgsql\\data',
        '-l', 'C:\\Users\\devuser\\pgsql\\pg.log',
        'start'
    ]);
    
    // Wait 6 seconds for database startup
    await new Promise(r => setTimeout(r, 6000));
    
    console.log("0.1. Limpiando y recreando base de datos 'bfa'...");
    try {
        const drop = spawn('C:\\Users\\devuser\\pgsql\\bin\\dropdb.exe', ['-U', 'bfa', '-h', 'localhost', 'bfa']);
        await new Promise(r => drop.on('close', r));
        
        const create = spawn('C:\\Users\\devuser\\pgsql\\bin\\createdb.exe', ['-U', 'bfa', '-h', 'localhost', 'bfa']);
        await new Promise(r => create.on('close', r));
        console.log("   -> Base de datos limpia y lista.");
    } catch (e) {
        console.error("Error al recrear base de datos:", e);
    }
    
    console.log("1. Iniciando servidor Spring Boot...");
    const server = spawn('cmd.exe', ['/c', '.\\mvnw.cmd', 'spring-boot:run', '-Dspring-boot.run.profiles=dev'], {
        cwd: __dirname
    });
    
    await new Promise((resolve) => {
        let resolved = false;
        server.stdout.on('data', (data) => {
            const text = data.toString();
            process.stdout.write(text);
            if (text.includes("Tomcat started on port") || text.includes("Started BfaEspacialApplication")) {
                if (!resolved) {
                    resolved = true;
                    console.log("\n>>> ¡Servidor Tomcat iniciado!");
                    resolve();
                }
            }
        });
        server.stderr.on('data', (data) => {
            process.stderr.write(data.toString());
        });
        setTimeout(() => {
            if (!resolved) {
                resolved = true;
                console.log("\n>>> Timeout alcanzado. Continuando...");
                resolve();
            }
        }, 65000);
    });
    
    console.log("2. Iniciando Puppeteer...");
    const browser = await browserLaunch();
    
    const page = await browser.newPage();
    await page.setViewport({ width: 1024, height: 768 });
    
    // Enable browser log capturing
    page.on('console', msg => console.log('[BROWSER CONSOLE]:', msg.text()));
    page.on('pageerror', err => console.error('[BROWSER EXCEPTION]:', err.message));
    
    try {
        console.log("\n==================================================");
        console.log("        VERIFICACIÓN DE SEGURIDAD Y ACCESOS       ");
        console.log("==================================================");

        // Step 1: Login Page
        console.log("3. Navegando a la página de Login...");
        await page.goto('http://localhost:8080/login', { waitUntil: 'networkidle2' });
        await page.screenshot({ path: path.join(artDir, '1_login_screen.png') });
        console.log("   -> Captura 1 guardada (Login personalizado).");

        // Step 2: Login as Admin
        console.log("4. Iniciando sesión como Administrador...");
        await page.type('input[name="username"]', 'admin');
        await page.type('input[name="password"]', 'admin123');
        await Promise.all([
            page.click('button[type="submit"]'),
            page.waitForNavigation({ waitUntil: 'networkidle2' })
        ]);
        await page.screenshot({ path: path.join(artDir, '2_admin_reactivos.png') });
        console.log("   -> Captura 2 guardada (Sesión Administrador activa).");

        // Step 2.1: Check CSRF Cookie
        console.log("4.1. Verificando cookie CSRF (CookieCsrfTokenRepository)...");
        const cookiesList = await page.cookies();
        const csrfCookie = cookiesList.find(c => c.name === 'XSRF-TOKEN');
        if (csrfCookie) {
            console.log("   [✓] CHECK PASADO: Cookie 'XSRF-TOKEN' detectada (CSRF habilitado y activo).");
        } else {
            console.warn("   [X] CHECK FALLADO: No se detectó la cookie 'XSRF-TOKEN'.");
        }

        // Step 3: View Baremos
        console.log("5. Navegando a la tabla de Baremos...");
        await page.goto('http://localhost:8080/admin/baremos', { waitUntil: 'networkidle2' });
        await page.screenshot({ path: path.join(artDir, '3_admin_baremos.png') });
        console.log("   -> Captura 3 guardada.");

        // Step 4: View Usuarios
        console.log("6. Navegando a la gestión de usuarios...");
        await page.goto('http://localhost:8080/admin/usuarios', { waitUntil: 'networkidle2' });
        await page.screenshot({ path: path.join(artDir, '4_admin_usuarios.png') });
        console.log("   -> Captura 4 guardada (Gestión de usuarios en base de datos).");

        // Step 5: Clean Session (Logout)
        console.log("7. Cerrando sesión de Administrador (Eliminando cookies)...");
        const activeCookies = await page.cookies();
        for (const cookie of activeCookies) {
            await page.deleteCookie(cookie);
        }
        
        // Navigate back to Login
        await page.goto('http://localhost:8080/login', { waitUntil: 'networkidle2' });
        
        // Step 6: Login as Estudiante (RBAC Checks)
        console.log("8. Iniciando sesión como Estudiante...");
        await page.type('input[name="username"]', 'estudiante');
        await page.type('input[name="password"]', 'estudiante123');
        await Promise.all([
            page.click('button[type="submit"]'),
            page.waitForNavigation({ waitUntil: 'networkidle2' })
        ]);
        await page.screenshot({ path: path.join(artDir, '5_estudiante_dashboard.png') });
        console.log("   -> Captura 5 guardada (Dashboard Estudiante).");

        // Step 6.1: RBAC Security Check
        console.log("8.1. Verificando restricción de roles (Acceso denegado a Estudiante)...");
        const response = await page.goto('http://localhost:8080/admin/usuarios', { waitUntil: 'networkidle2' });
        const status = response.status();
        if (status === 403) {
            console.log("   [✓] CHECK PASADO: Estudiante recibió HTTP 403 Forbidden en zona admin.");
        } else {
            console.error(`   [X] CHECK FALLADO: Estudiante pudo acceder (HTTP ${status}).`);
        }
        await page.screenshot({ path: path.join(artDir, '6_rbac_denied_check.png') });

        // Step 7: Go back to Student Dashboard and click "Continuar al subtest"
        console.log("9. Iniciando la evaluación...");
        await page.goto('http://localhost:8080/evaluacion/inicio', { waitUntil: 'networkidle2' });
        
        // Click the Thymeleaf link to enter React SPA subtest screen
        await Promise.all([
            page.click('a[href="/evaluacion/subtest"]'),
            page.waitForNavigation({ waitUntil: 'networkidle2' })
        ]);
        
        await page.screenshot({ path: path.join(artDir, '7_estudiante_consignas.png') });
        console.log("   -> Captura 7 guardada (Consignas estilo papel).");

        // Step 8: Click "Comenzar"
        console.log("10. Comenzando el Subtest...");
        const startBtn = await page.$('.bfa-consigna button');
        if (startBtn) {
            await startBtn.click();
            
            // Wait explicitly for the reactivo card elements to load
            await page.waitForSelector('.bfa-opciones label input[type=\"radio\"]', { timeout: 12000 });
            
            await page.screenshot({ path: path.join(artDir, '8_subtest_screen.png') });
            console.log("   -> Captura 8 guardada (Preguntas e imágenes).");
            
            // Step 9: Click a radio option to check OMR bubble fill effect
            console.log("11. Marcando una respuesta (Burbuja OMR)...");
            const radio = await page.$('.bfa-opciones label input[type=\"radio\"]');
            if (radio) {
                await radio.click();
                await new Promise(r => setTimeout(r, 800)); // wait for checked animation
                await page.screenshot({ path: path.join(artDir, '9_subtest_marcado.png') });
                console.log("   -> Captura 9 guardada (Burbuja rellenada con lápiz grafito).");
            } else {
                console.error("No se encontró ningún radio button en la página del subtest.");
            }
        } else {
            console.error("No se encontró el botón de comenzar en .bfa-consigna button");
        }
        console.log("==================================================");

    } catch (e) {
        console.error("Error durante la automatización: ", e);
    } finally {
        await browser.close();
        console.log("12. Deteniendo servidor Spring Boot...");
        server.kill();
        
        console.log("13. Deteniendo base de datos PostgreSQL...");
        const stop = spawn('C:\\Users\\devuser\\pgsql\\bin\\pg_ctl.exe', [
            '-D', 'C:\\Users\\devuser\\pgsql\\data',
            'stop'
        ]);
        await new Promise(r => stop.on('close', r));
        
        process.exit(0);
    }
}

async function browserLaunch() {
    const paths = [
        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
        "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
        "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
    ];
    for (const p of paths) {
        if (fs.existsSync(p)) {
            console.log("  Using browser executable:", p);
            return await puppeteer.launch({
                headless: "new",
                executablePath: p,
                defaultViewport: { width: 1024, height: 768 }
            });
        }
    }
    return await puppeteer.launch({
        headless: "new",
        defaultViewport: { width: 1024, height: 768 }
    });
}

main();
