
const SERVIDOR_BASE = window.location.origin;
const API_BASE = `${SERVIDOR_BASE}/api`;
const API_URL = `${API_BASE}/usuarios`;

function usuarioActualId() {
    try { return (JSON.parse(localStorage.getItem('usuarioLogueado')) || {}).id; } catch (e) { return null; }
}
const CLAVE_TOKEN = 'vamospetToken';
const CLAVE_TOKEN_EXPIRA = 'vamospetTokenExpira';
const CLAVE_TOKEN_RENOVADO = 'vamospetTokenRenovado';
const RUTAS_SIN_TOKEN = ['/usuarios/login', '/usuarios/registro', '/usuarios/recuperar/'];
const RENOVAR_CADA_MS = 2 * 60 * 1000;
let __sesionVencidaEnCurso = false;

function tokenActual() {
    return localStorage.getItem(CLAVE_TOKEN);
}

function guardarToken(token, expira) {
    if (!token) return;
    localStorage.setItem(CLAVE_TOKEN, token);
    localStorage.setItem(CLAVE_TOKEN_EXPIRA, String(expira || ''));
    localStorage.setItem(CLAVE_TOKEN_RENOVADO, String(Date.now()));
}

function esUrlApi(url) {
    return url.startsWith(API_BASE) || url.startsWith(API_URL);
}

function rutaSinToken(url) {
    const ruta = url.slice(API_BASE.length);
    return RUTAS_SIN_TOKEN.some(r => ruta.startsWith(r));
}

const __fetchOriginal = window.fetch.bind(window);
window.fetch = async function (recurso, opciones) {
    const url = typeof recurso === 'string' ? recurso : (recurso && recurso.url) || '';
    if (!esUrlApi(url)) return __fetchOriginal(recurso, opciones);
    const token = rutaSinToken(url) ? null : tokenActual();
    const conToken = Object.assign({}, opciones || {});
    const cabeceras = new Headers(conToken.headers || (typeof recurso !== 'string' && recurso.headers) || {});
    if (token) cabeceras.set('Authorization', 'Bearer ' + token);
    conToken.headers = cabeceras;
    const respuesta = await __fetchOriginal(recurso, conToken);
    if (respuesta.status === 401 && token && !url.includes('/sesion/cerrar')) manejarSesionVencida();
    return respuesta;
};

function manejarSesionVencida() {
    if (__sesionVencidaEnCurso || !localStorage.getItem('usuarioLogueado')) return;
    __sesionVencidaEnCurso = true;
    cerrarSesionPorTiempo('Tu sesión expiró o ya no es válida. Vuelve a iniciar sesión.');
    setTimeout(() => { __sesionVencidaEnCurso = false; }, 1000);
}

async function renovarToken(forzar) {
    const usuario = usuarioEnSesion();
    if (!usuario || esPersonalInterno(usuario) || !tokenActual()) return;
    const ultima = Number(localStorage.getItem(CLAVE_TOKEN_RENOVADO)) || 0;
    if (!forzar && Date.now() - ultima < RENOVAR_CADA_MS) return;
    localStorage.setItem(CLAVE_TOKEN_RENOVADO, String(Date.now()));
    try {
        const res = await fetch(`${API_BASE}/sesion/renovar`, { method: 'POST' });
        if (!res.ok) return;
        const data = await res.json();
        guardarToken(data.token, data.token_expira);
    } catch (e) {
        localStorage.setItem(CLAVE_TOKEN_RENOVADO, String(ultima));
    }
}

async function abrirArchivoApi(url, nombreDescarga) {
    const ventana = nombreDescarga ? null : window.open('', '_blank');
    try {
        const res = await fetch(url);
        if (!res.ok) {
            let mensaje = 'No se pudo abrir el archivo.';
            try { mensaje = (await res.json()).error || mensaje; } catch (e) { }
            throw new Error(mensaje);
        }
        const enlace = URL.createObjectURL(await res.blob());
        if (ventana) {
            ventana.location.href = enlace;
        } else {
            const a = document.createElement('a');
            a.href = enlace;
            a.download = nombreDescarga;
            document.body.appendChild(a);
            a.click();
            a.remove();
        }
        setTimeout(() => URL.revokeObjectURL(enlace), 60000);
    } catch (err) {
        if (ventana) ventana.close();
        if (localStorage.getItem('usuarioLogueado')) alert(err.message);
    }
}

function rolActual() {
    try { return (JSON.parse(localStorage.getItem('usuarioLogueado')) || {}).rol_id || null; } catch (e) { return null; }
}

function marcarRolEnPagina() {
    const rol = rolActual();
    if (rol) document.body.dataset.rol = String(rol);
    else delete document.body.dataset.rol;
}

const VISTAS_SUPERVISION_ADMIN = ['mensajes', 'citas', 'examenes', 'emergencias', 'clientes', 'inventario', 'banners', 'proveedores', 'compras', 'devoluciones', 'comprobantes', 'finanzas', 'reservas'];

function mostrarAvisoSupervision(vista) {
    const pagina = (vista || '').split('/')[1];
    if (rolActual() !== 1 || !VISTAS_SUPERVISION_ADMIN.includes(pagina)) return;
    const contenido = document.querySelector('#app-content .admin-content');
    if (!contenido || contenido.querySelector('.aviso-supervision')) return;
    const aviso = document.createElement('div');
    aviso.className = 'aviso-supervision';
    aviso.setAttribute('role', 'note');
    aviso.innerHTML = '<i class="fa-solid fa-eye"></i> Modo supervisión: el Administrador ve esta sección en solo lectura. Las operaciones las realiza el personal a cargo.';
    contenido.prepend(aviso);
}

function bloquearSiEsPersonal(accion) {
    const usuario = usuarioEnSesion();
    if (usuario && [1, 3, 4].includes(usuario.rol_id)) {
        alert(`El personal de la clínica no ${accion} desde la web. ${usuario.rol_id === 4 ? 'Usa Venta en Mostrador o atiende al cliente desde tu panel.' : ''}`.trim());
        return true;
    }
    return false;
}

function fetchUsuarios() {
    return fetch(`${API_BASE}/usuarios?usuarioId=${encodeURIComponent(usuarioActualId())}`);
}

function esc(valor) {
    return String(valor == null ? '' : valor).replace(/[&<>"']/g, c => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const REGLAS_CUENTA = {
    nombre: { rx: /^\p{L}+(?: \p{L}+)*$/u, msg: 'Solo letras y un espacio entre palabras.' },
    dni: { rx: /^\d{8}$/, msg: 'El DNI debe tener exactamente 8 dígitos.' },
    telefono: { rx: /^9\d{8}$/, msg: 'El teléfono debe tener 9 dígitos y empezar con 9.' },
    correo: { rx: /^[^@\s]+@[^@\s]+\.[^@\s]+$/, msg: 'Escribe un correo válido, por ejemplo nombre@correo.com.' },
    codigo: { rx: /^\d{6}$/, msg: 'El código tiene 6 dígitos.' }
};

const REGLAS_SOLO_DIGITOS = ['dni', 'telefono', 'codigo'];

const REQUISITOS_CLAVE = [
    { texto: 'Entre 8 y 64 caracteres', ok: v => v.length >= 8 && v.length <= 64 },
    { texto: 'Al menos una letra', ok: v => /\p{L}/u.test(v) },
    { texto: 'Al menos un número', ok: v => /\d/.test(v) }
];

function claveCumpleRequisitos(valor) {
    return REQUISITOS_CLAVE.every(r => r.ok(valor || ''));
}

function normalizarNombre(valor) {
    return String(valor || '').trim().replace(/\s+/g, ' ');
}

function mensajeCampo(input, texto, tipo) {
    if (!input) return;
    const destino = document.querySelector(`[data-mensaje-para="${input.id}"]`);
    if (destino) {
        destino.textContent = texto || '';
        destino.classList.toggle('error', tipo === 'error');
        destino.classList.toggle('ok', tipo === 'ok');
    }
    input.classList.toggle('campo-invalido', tipo === 'error');
    if (tipo === 'error') input.setAttribute('aria-invalid', 'true'); else input.removeAttribute('aria-invalid');
}

function validarCampoCuenta(input, mostrarVacio) {
    const regla = REGLAS_CUENTA[input.dataset.regla];
    if (!regla) return true;
    const valor = input.dataset.regla === 'nombre' ? normalizarNombre(input.value) : input.value.trim();
    if (!valor) {
        if (input.dataset.opcional !== undefined) { mensajeCampo(input, '', null); return true; }
        if (mostrarVacio) { mensajeCampo(input, 'Este campo es obligatorio.', 'error'); return false; }
        mensajeCampo(input, '', null);
        return false;
    }
    if (!regla.rx.test(valor)) { mensajeCampo(input, regla.msg, 'error'); return false; }
    if (input.dataset.duplicado === '1') { mensajeCampo(input, input.dataset.mensajeDuplicado || 'Ya está registrado.', 'error'); return false; }
    mensajeCampo(input, '', null);
    return true;
}

function validarConfirmacion(input) {
    const original = document.getElementById(input.dataset.confirma);
    if (!original) return true;
    if (!input.value) { mensajeCampo(input, '', null); return false; }
    if (input.value !== original.value) { mensajeCampo(input, 'Las contraseñas no coinciden.', 'error'); return false; }
    mensajeCampo(input, 'Las contraseñas coinciden.', 'ok');
    return true;
}

function pintarRequisitosClave(input) {
    const lista = document.querySelector(`[data-requisitos-para="${input.id}"]`);
    if (!lista) return;
    const valor = input.value || '';
    lista.innerHTML = REQUISITOS_CLAVE.map(r => {
        const cumple = r.ok(valor);
        return `<li class="${cumple ? 'cumple' : ''}"><i class="fa-solid ${cumple ? 'fa-circle-check' : 'fa-circle'}"></i> ${esc(r.texto)}</li>`;
    }).join('');
}

function prepararFormulariosCuenta(raiz) {
    (raiz || document).querySelectorAll('[data-requisitos-para]').forEach(lista => {
        const input = document.getElementById(lista.dataset.requisitosPara);
        if (input) pintarRequisitosClave(input);
    });
}

function campoVisible(input) {
    for (let el = input; el && el !== document.body; el = el.parentElement) {
        if (el.hidden || el.style.display === 'none') return false;
        if (window.getComputedStyle(el).display === 'none') return false;
    }
    return true;
}

function formularioCuentaValido(form) {
    let primero = null;
    form.querySelectorAll('[data-regla]').forEach(input => {
        if (!campoVisible(input)) { mensajeCampo(input, '', null); return; }
        if (!validarCampoCuenta(input, true) && !primero) primero = input;
    });
    form.querySelectorAll('[data-requisitos-para]').forEach(lista => {
        const input = document.getElementById(lista.dataset.requisitosPara);
        if (input && campoVisible(input) && !claveCumpleRequisitos(input.value)) {
            mensajeCampo(input, 'La contraseña aún no cumple todos los requisitos.', 'error');
            if (!primero) primero = input;
        }
    });
    form.querySelectorAll('[data-confirma]').forEach(input => {
        if (!campoVisible(input)) return;
        if (!validarConfirmacion(input)) {
            if (!input.value) mensajeCampo(input, 'Repite la contraseña.', 'error');
            if (!primero) primero = input;
        }
    });
    if (primero) {
        if (typeof primero.scrollIntoView === 'function') primero.scrollIntoView({ block: 'center' });
        primero.focus();
    }
    return !primero;
}

function mostrarErroresServidor(campos, mapa) {
    if (!campos) return false;
    let alguno = false;
    Object.entries(campos).forEach(([campo, texto]) => {
        const input = document.getElementById(mapa[campo]);
        if (input) { mensajeCampo(input, texto, 'error'); alguno = true; }
    });
    return alguno;
}

const __consultasDuplicado = {};

function consultarDuplicado(input) {
    const tipo = input.dataset.verificarDuplicado;
    if (!tipo) return;
    clearTimeout(__consultasDuplicado[input.id]);
    input.dataset.duplicado = '0';
    const valor = input.value.trim();
    const regla = REGLAS_CUENTA[tipo];
    if (!regla || !regla.rx.test(valor)) return;
    __consultasDuplicado[input.id] = setTimeout(async () => {
        try {
            const res = await fetch(`${API_URL}/existe?${tipo}=${encodeURIComponent(valor)}`);
            if (!res.ok || input.value.trim() !== valor) return;
            const data = await res.json();
            input.dataset.duplicado = data.existe ? '1' : '0';
            input.dataset.mensajeDuplicado = tipo === 'dni'
                ? 'Este DNI ya tiene una cuenta. Inicia sesión o recupera tu contraseña.'
                : 'Este correo ya está registrado. Inicia sesión o recupera tu contraseña.';
            if (data.existe) mensajeCampo(input, input.dataset.mensajeDuplicado, 'error');
            else mensajeCampo(input, tipo === 'dni' ? 'DNI disponible.' : 'Correo disponible.', 'ok');
        } catch (e) {
            input.dataset.duplicado = '0';
        }
    }, 400);
}

document.addEventListener('beforeinput', (e) => {
    const input = e.target;
    if (!input.dataset || !REGLAS_SOLO_DIGITOS.includes(input.dataset.regla)) return;
    if (e.data && /\D/.test(e.data)) {
        e.preventDefault();
        mensajeCampo(input, 'Solo se permiten números.', 'error');
    }
});

document.addEventListener('input', (e) => {
    const input = e.target;
    if (!input.dataset) return;
    if (REGLAS_SOLO_DIGITOS.includes(input.dataset.regla)) {
        const limpio = input.value.replace(/\D/g, '');
        const maximo = parseInt(input.getAttribute('maxlength'), 10) || limpio.length;
        if (limpio !== input.value) {
            input.value = limpio.slice(0, maximo);
            mensajeCampo(input, 'Solo se permiten números.', 'error');
            return;
        }
    }
    if (input.dataset.regla) {
        validarCampoCuenta(input, false);
        consultarDuplicado(input);
    }
    if (input.type === 'password' || input.dataset.ojitoActivo === '1') {
        pintarRequisitosClave(input);
        if (input.classList.contains('campo-invalido') && claveCumpleRequisitos(input.value)) mensajeCampo(input, '', null);
        document.querySelectorAll(`[data-confirma="${input.id}"]`).forEach(c => { if (c.value) validarConfirmacion(c); });
    }
    if (input.dataset.confirma) validarConfirmacion(input);
});

document.addEventListener('focusout', (e) => {
    const input = e.target;
    if (input.dataset && input.dataset.regla && input.value) validarCampoCuenta(input, false);
});

document.addEventListener('click', (e) => {
    const boton = e.target.closest('[data-ojito]');
    if (!boton) return;
    const input = document.getElementById(boton.dataset.ojito);
    if (!input) return;
    const mostrar = input.type === 'password';
    input.type = mostrar ? 'text' : 'password';
    input.dataset.ojitoActivo = '1';
    boton.setAttribute('aria-pressed', String(mostrar));
    boton.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
    boton.innerHTML = `<i class="fa-regular ${mostrar ? 'fa-eye-slash' : 'fa-eye'}"></i>`;
    input.focus();
});

const SESION_CLIENTE_INACTIVIDAD_MS = 20 * 60 * 1000;
const SESION_CLIENTE_AVISO_MS = 19 * 60 * 1000;
const SESION_PERSONAL_TOTAL_MS = 8 * 60 * 60 * 1000;
const CLAVE_ULTIMA_ACTIVIDAD = 'vamospetUltimaActividad';
const CLAVE_INICIO_SESION = 'vamospetInicioSesion';
let __ultimaEscrituraActividad = 0;

function usuarioEnSesion() {
    try { return JSON.parse(localStorage.getItem('usuarioLogueado')); } catch (e) { return null; }
}

function esPersonalInterno(usuario) {
    return !!usuario && [1, 3, 4].includes(usuario.rol_id);
}

function iniciarSesionLocal(datos) {
    const ahora = String(Date.now());
    const usuario = Object.assign({}, datos);
    const token = usuario.token;
    const expira = usuario.token_expira;
    delete usuario.token;
    delete usuario.token_expira;
    guardarToken(token, expira);
    localStorage.setItem('usuarioLogueado', JSON.stringify(usuario));
    localStorage.setItem(CLAVE_INICIO_SESION, ahora);
    localStorage.setItem(CLAVE_ULTIMA_ACTIVIDAD, ahora);
    __ultimaEscrituraActividad = Date.now();
}

function actualizarUsuarioEnSesion(datos) {
    const actual = usuarioEnSesion();
    if (!actual) return;
    localStorage.setItem('usuarioLogueado', JSON.stringify(Object.assign({}, actual, datos)));
}

function limpiarSesionLocal() {
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_TOKEN_EXPIRA);
    localStorage.removeItem(CLAVE_TOKEN_RENOVADO);
    localStorage.removeItem('usuarioLogueado');
    localStorage.removeItem(CLAVE_INICIO_SESION);
    localStorage.removeItem(CLAVE_ULTIMA_ACTIVIDAD);
}

function avisoSesionVisible() {
    const aviso = document.getElementById('aviso-sesion');
    return !!aviso && aviso.style.display !== 'none';
}

function modalPagoAbierto() {
    const modal = document.getElementById('modal-pago-global');
    return !!modal && modal.style.display !== 'none';
}

function registrarActividad(opciones) {
    const usuario = usuarioEnSesion();
    if (!usuario || esPersonalInterno(usuario)) return;
    const forzar = opciones && opciones.forzar;
    if (avisoSesionVisible() && !forzar && !(opciones && opciones.ignorarAviso)) return;
    const ahora = Date.now();
    if (!forzar && ahora - __ultimaEscrituraActividad < 5000) return;
    __ultimaEscrituraActividad = ahora;
    localStorage.setItem(CLAVE_ULTIMA_ACTIVIDAD, String(ahora));
    renovarToken(forzar);
}

function mostrarAvisoSesion(segundos) {
    let aviso = document.getElementById('aviso-sesion');
    if (!aviso) {
        aviso = document.createElement('div');
        aviso.id = 'aviso-sesion';
        aviso.className = 'modal-overlay aviso-sesion';
        aviso.setAttribute('role', 'alertdialog');
        aviso.setAttribute('aria-modal', 'true');
        aviso.setAttribute('aria-labelledby', 'aviso-sesion-titulo');
        aviso.innerHTML = `
            <div class="modal-box">
                <h3 id="aviso-sesion-titulo"><i class="fa-regular fa-clock" style="color: var(--primary-green);"></i> ¿Sigues ahí?</h3>
                <p style="font-size: 14px; color: var(--text-muted); line-height: 1.6; margin: 10px 0 20px;">Llevas un rato sin actividad. Por seguridad cerraremos tu sesión en <strong id="aviso-sesion-cuenta">60</strong> segundos. Tu carrito se mantiene guardado.</p>
                <div style="display: flex; gap: 10px;">
                    <button type="button" class="btn-outline" style="flex: 1; padding: 12px;" onclick="cerrarSesion()">Cerrar sesión</button>
                    <button type="button" class="btn-primary" style="flex: 1; padding: 12px; justify-content: center;" id="aviso-sesion-seguir" onclick="seguirConectado()">Seguir conectado</button>
                </div>
            </div>`;
        document.body.appendChild(aviso);
    }
    document.getElementById('aviso-sesion-cuenta').textContent = Math.max(0, segundos);
    if (aviso.style.display !== 'flex') {
        aviso.style.display = 'flex';
        document.getElementById('aviso-sesion-seguir').focus();
    }
}

function ocultarAvisoSesion() {
    const aviso = document.getElementById('aviso-sesion');
    if (aviso) aviso.style.display = 'none';
}

function seguirConectado() {
    registrarActividad({ forzar: true });
    ocultarAvisoSesion();
}

function cerrarSesionPorTiempo(mensaje) {
    ocultarAvisoSesion();
    const modalPago = document.getElementById('modal-pago-global');
    if (modalPago) modalPago.remove();
    limpiarSesionLocal();
    reiniciarChatAsistente();
    actualizarMenuUsuario();
    cargarVista('login');
    setTimeout(() => alert(mensaje), 50);
}

function revisarSesion() {
    const usuario = usuarioEnSesion();
    if (!usuario) { ocultarAvisoSesion(); return false; }
    const ahora = Date.now();
    if (!tokenActual()) {
        cerrarSesionPorTiempo('Por seguridad, vuelve a iniciar sesión.');
        return true;
    }
    const tokenExpira = Number(localStorage.getItem(CLAVE_TOKEN_EXPIRA)) || 0;
    if (esPersonalInterno(usuario) && tokenExpira && ahora >= tokenExpira) {
        cerrarSesionPorTiempo('Pasaron 8 horas desde que iniciaste sesión. Vuelve a ingresar para continuar.');
        return true;
    }
    if (!localStorage.getItem(CLAVE_INICIO_SESION)) localStorage.setItem(CLAVE_INICIO_SESION, String(ahora));
    if (!localStorage.getItem(CLAVE_ULTIMA_ACTIVIDAD)) localStorage.setItem(CLAVE_ULTIMA_ACTIVIDAD, String(ahora));
    if (esPersonalInterno(usuario)) {
        const inicio = Number(localStorage.getItem(CLAVE_INICIO_SESION)) || ahora;
        if (ahora - inicio >= SESION_PERSONAL_TOTAL_MS) {
            cerrarSesionPorTiempo('Pasaron 8 horas desde que iniciaste sesión. Vuelve a ingresar para continuar.');
            return true;
        }
        return false;
    }
    if (modalPagoAbierto()) {
        registrarActividad({ ignorarAviso: true });
        ocultarAvisoSesion();
        return false;
    }
    const ultima = Number(localStorage.getItem(CLAVE_ULTIMA_ACTIVIDAD)) || ahora;
    const inactivo = ahora - ultima;
    if (inactivo >= SESION_CLIENTE_INACTIVIDAD_MS) {
        cerrarSesionPorTiempo('Cerramos tu sesión porque pasaron 20 minutos sin actividad. Tu carrito sigue guardado.');
        return true;
    }
    if (inactivo >= SESION_CLIENTE_AVISO_MS) mostrarAvisoSesion(Math.ceil((SESION_CLIENTE_INACTIVIDAD_MS - inactivo) / 1000));
    else ocultarAvisoSesion();
    return false;
}

['click', 'keydown', 'scroll', 'wheel', 'touchstart', 'mousemove'].forEach(evento => {
    document.addEventListener(evento, () => registrarActividad(), { passive: true, capture: true });
});

window.addEventListener('storage', (e) => {
    if (e.key === 'usuarioLogueado') {
        actualizarMenuUsuario();
        if (!e.newValue) {
            ocultarAvisoSesion();
            cargarVista('home');
        }
    }
    if (e.key === CLAVE_ULTIMA_ACTIVIDAD) revisarSesion();
});

setInterval(revisarSesion, 1000);

function obtenerCarrito() {
    try {
        return JSON.parse(localStorage.getItem('carritoVamosPet')) || [];
    } catch (e) {
        return [];
    }
}

function guardarCarrito(carrito) {
    localStorage.setItem('carritoVamosPet', JSON.stringify(carrito));
    actualizarBadgeCarrito();
}

function agregarAlCarrito(producto) {
    if (bloquearSiEsPersonal('compra')) return;
    const carrito = obtenerCarrito();
    const existente = carrito.find(p => p.producto_id === producto.producto_id);
    if (existente) {
        existente.cantidad += 1;
    } else {
        carrito.push({ ...producto, cantidad: 1 });
    }
    guardarCarrito(carrito);
}

function actualizarBadgeCarrito() {
    const total = obtenerCarrito().reduce((sum, p) => sum + p.cantidad, 0);
    document.querySelectorAll('.carrito-badge').forEach(el => el.innerText = total);
}

function toggleMenuMovil() {
    const nav = document.getElementById('nav-links');
    if (nav) nav.classList.toggle('menu-movil-abierto');
}

function cerrarMenuMovil() {
    const nav = document.getElementById('nav-links');
    if (nav) nav.classList.remove('menu-movil-abierto');
}

const IMAGEN_SIN_FOTO = 'img/producto-sin-foto.svg';
const ATRIBUTO_SIN_FOTO = `onerror="this.onerror=null;this.src='${IMAGEN_SIN_FOTO}'"`;
const IMAGEN_NO_DISPONIBLE = 'img/imagen-no-disponible.svg';

function reemplazarImagenCaida(evento) {
    const img = evento.target;
    if (!img || img.tagName !== 'IMG' || img.hasAttribute('onerror') || img.dataset.reemplazada) return;
    img.dataset.reemplazada = '1';
    img.src = IMAGEN_NO_DISPONIBLE;
}

document.addEventListener('error', reemplazarImagenCaida, true);

function urlImagen(url) {
    if (!url) return IMAGEN_SIN_FOTO;
    return url.startsWith('/uploads/') ? SERVIDOR_BASE + url : url;
}

const __carruselesBanner = {};

async function montarBanners(idContenedor, seccion) {
    const cont = document.getElementById(idContenedor);
    if (!cont) return;
    if (__carruselesBanner[idContenedor]) clearInterval(__carruselesBanner[idContenedor]);
    cont.hidden = true;
    let lista = [];
    try {
        const res = await fetch(`${API_BASE}/banners?seccion=${encodeURIComponent(seccion)}`);
        lista = res.ok ? await res.json() : [];
    } catch (e) { lista = []; }
    if (!lista.length) { cont.innerHTML = ''; return; }
    const varios = lista.length > 1;
    cont.innerHTML = `
        <div class="banner-pista">${lista.map((b, i) => `
            <picture class="banner-slide${i === 0 ? ' activo' : ''}" aria-hidden="${i === 0 ? 'false' : 'true'}">
                <source media="(max-width: 700px)" srcset="${esc(urlImagen(b.imagen_movil || b.imagen_pc))}">
                <img src="${esc(urlImagen(b.imagen_pc))}" alt="Promoción de ${esc(seccion)} ${i + 1}" ${i === 0 ? '' : 'loading="lazy"'} onerror="this.closest('.banner-slide').classList.add('rota')">
            </picture>`).join('')}
        </div>
        ${varios ? `<button type="button" class="banner-flecha anterior" aria-label="Banner anterior"><i class="fa-solid fa-chevron-left"></i></button>
        <button type="button" class="banner-flecha siguiente" aria-label="Banner siguiente"><i class="fa-solid fa-chevron-right"></i></button>
        <div class="banner-puntos">${lista.map((b, i) => `<button type="button" class="banner-punto${i === 0 ? ' activo' : ''}" aria-label="Ir al banner ${i + 1}" data-i="${i}"></button>`).join('')}</div>` : ''}`;
    cont.hidden = false;
    if (!varios) return;
    const slides = [...cont.querySelectorAll('.banner-slide')];
    const puntos = [...cont.querySelectorAll('.banner-punto')];
    let actual = 0, pausado = false, inicioX = null;
    const ir = (n) => {
        actual = (n + slides.length) % slides.length;
        slides.forEach((s, i) => { s.classList.toggle('activo', i === actual); s.setAttribute('aria-hidden', i === actual ? 'false' : 'true'); });
        puntos.forEach((p, i) => p.classList.toggle('activo', i === actual));
    };
    cont.querySelector('.banner-flecha.anterior').addEventListener('click', () => ir(actual - 1));
    cont.querySelector('.banner-flecha.siguiente').addEventListener('click', () => ir(actual + 1));
    puntos.forEach(p => p.addEventListener('click', () => ir(Number(p.dataset.i))));
    cont.addEventListener('mouseenter', () => { pausado = true; });
    cont.addEventListener('mouseleave', () => { pausado = false; });
    cont.addEventListener('touchstart', (e) => { inicioX = e.touches[0].clientX; pausado = true; }, { passive: true });
    cont.addEventListener('touchend', (e) => {
        if (inicioX !== null) {
            const dx = e.changedTouches[0].clientX - inicioX;
            if (Math.abs(dx) > 40) ir(actual + (dx < 0 ? 1 : -1));
        }
        inicioX = null;
        pausado = false;
    });
    cont.__irBanner = ir;
    __carruselesBanner[idContenedor] = setInterval(() => {
        if (!document.body.contains(cont)) { clearInterval(__carruselesBanner[idContenedor]); return; }
        if (!pausado && !document.hidden) ir(actual + 1);
    }, 5000);
}

async function subirImagen(inputFileElement) {
    if (!inputFileElement || !inputFileElement.files || inputFileElement.files.length === 0) {
        return null;
    }
    const formData = new FormData();
    formData.append('archivo', inputFileElement.files[0]);

    try {
        const res = await fetch(`${API_BASE}/uploads`, { method: 'POST', body: formData });
        const data = await res.json();
        if (!res.ok) {
            alert(data.error || 'No se pudo subir la imagen.');
            return null;
        }
        return data.url;
    } catch (error) {
        console.error('Error subiendo imagen:', error);
        alert('No se pudo conectar con el servidor para subir la imagen.');
        return null;
    }
}

function previsualizarImagen(inputFileElement, imgElementId) {
    const archivo = inputFileElement.files[0];
    if (!archivo) return;
    const lector = new FileReader();
    lector.onload = (e) => {
        const img = document.getElementById(imgElementId);
        if (img) img.src = e.target.result;
    };
    lector.readAsDataURL(archivo);
}

function irAAgendar() {
    if (bloquearSiEsPersonal('agenda citas')) return;
    const usuarioJSON = localStorage.getItem('usuarioLogueado');
    if (usuarioJSON) {
        cargarVista('VistaCliente/agendar-cita');
    } else {
        cargarVista('login');
    }
}

function irAAgendarGrooming() {
    if (bloquearSiEsPersonal('agenda citas')) return;
    if (localStorage.getItem('usuarioLogueado')) cargarVista('VistaCliente/agendar-grooming');
    else cargarVista('login');
}

const CLAVE_RECETA_CARRITO = 'vamospetRecetaCarrito';

async function estadoRecetaVenta(items, clienteId) {
    const estado = { conReceta: [], recetas: [], error: null };
    if (!items || !items.length) return estado;
    try {
        const res = await fetch(`${API_BASE}/productos`);
        const productos = res.ok ? await res.json() : [];
        const ids = new Set(items.map(i => Number(i.producto_id)));
        estado.conReceta = productos.filter(p => ids.has(p.id) && p.requiere_receta).map(p => p.nombre);
        if (estado.conReceta.length && clienteId) {
            const r = await fetch(`${API_BASE}/recetas/vigentes/cliente/${clienteId}`);
            estado.recetas = r.ok ? await r.json() : [];
        }
    } catch (e) {
        estado.error = 'No pudimos revisar las recetas. Intenta de nuevo.';
    }
    return estado;
}

function bloqueRecetaHtml(estado, idSelect, elegida, textoSinRecetas) {
    if (!estado.conReceta.length) return '';
    const lista = esc(estado.conReceta.join(', '));
    if (!estado.recetas.length) {
        return `<div class="aviso-receta bloqueado" role="alert"><i class="fa-solid fa-file-prescription"></i>
            <div><strong>Necesitas una receta aprobada</strong><p>${lista} se vende${estado.conReceta.length > 1 ? 'n' : ''} solo con receta veterinaria aprobada y vigente (30 días desde su aprobación). ${textoSinRecetas}</p></div></div>`;
    }
    return `<div class="aviso-receta"><i class="fa-solid fa-file-prescription"></i>
        <div><label for="${idSelect}"><strong>Receta para:</strong> ${lista}</label>
        <select id="${idSelect}">
            <option value="">Elige una receta vigente</option>
            ${estado.recetas.map(r => `<option value="${r.id}" ${String(r.id) === String(elegida) ? 'selected' : ''}>${esc(r.mascota)} · vence ${esc(r.vence)} · ${esc(String(r.contenido || '').slice(0, 40))}</option>`).join('')}
        </select>
        <p>Una receta sirve para una sola compra.</p></div></div>`;
}

function irAVistaCliente(vista) {
    if (localStorage.getItem('usuarioLogueado')) cargarVista(vista);
    else cargarVista('login');
}

function verProductoDetalle(id) {
    localStorage.setItem('productoSeleccionadoId', id);
    cargarVista('producto-detalle');
}

function verHistorialMascota(id) {
    if (rolActual() === 4) {
        alert('El historial clínico lo revisan el veterinario y el dueño de la mascota.');
        return;
    }
    localStorage.setItem('mascotaSeleccionadaId', id);
    cargarVista('VistaCliente/mascota-detalle');
}

function mostrarInfoTest(titulo, descripcion) {
    const existente = document.getElementById('modal-info-test');
    if (existente) existente.remove();

    const overlay = document.createElement('div');
    overlay.id = 'modal-info-test';
    overlay.className = 'modal-overlay';
    overlay.onclick = (e) => { if (e.target === overlay) overlay.remove(); };
    overlay.innerHTML = `
        <div class="modal-box">
            <h3><i class="fa-solid fa-circle-info" style="color: var(--primary-green);"></i> ${titulo}</h3>
            <p style="color: var(--text-muted); font-size: 14px; line-height: 1.6; margin-bottom: 20px;">${descripcion}</p>
            <button class="btn-primary" style="width: 100%; padding: 12px;" onclick="document.getElementById('modal-info-test').remove()">Entendido</button>
        </div>
    `;
    document.body.appendChild(overlay);
}

function enmascararDni(dni) {
    if (!dni) return '—';
    const d = String(dni).trim();
    if (d.length <= 2) return d;
    return d.slice(0, 2) + '•'.repeat(d.length - 2);
}

const PERMISOS_ADMIN = {
    1: ['dashboard-admin', 'citas', 'examenes', 'emergencias', 'clientes', 'servicios', 'inventario', 'banners', 'proveedores', 'compras', 'devoluciones', 'horarios', 'reportes', 'comprobantes', 'finanzas', 'reservas', 'personal', 'configuracion', 'mensajes'],
    3: ['dashboard-veterinario', 'citas', 'examenes', 'reservas', 'emergencias', 'clientes'],
    4: ['dashboard-auxiliar', 'venta-mostrador', 'clientes', 'citas', 'examenes', 'reservas', 'emergencias', 'comprobantes', 'finanzas', 'inventario', 'banners', 'proveedores', 'compras', 'devoluciones', 'mensajes']
};

function dashboardHomeParaRol(rolId) {
    const mapa = { 1: 'VistaAdmin/dashboard-admin', 3: 'VistaAdmin/dashboard-veterinario', 4: 'VistaAdmin/dashboard-auxiliar' };
    return mapa[rolId] || 'VistaAdmin/dashboard-admin';
}

function irAlPanelSegunRol() {
    const usuarioJSON = localStorage.getItem('usuarioLogueado');
    if (!usuarioJSON) { cargarVista('login'); return; }
    const usuario = JSON.parse(usuarioJSON);
    cargarVista(dashboardHomeParaRol(usuario.rol_id));
}

function paginasPermitidasParaRol(rolId) {
    return PERMISOS_ADMIN[rolId] || null;
}

function claseEstadoCita(estado) {
    const mapa = {
        'Pendiente': 'status-pendiente',
        'Confirmada': 'status-confirmada',
        'Atendida': 'status-completado',
        'No asistió': 'status-cancelada',
        'Cancelada': 'status-cancelada',
        'Rechazada': 'status-rechazada',
        'Por reasignar': 'status-por-reasignar',
        'En curso': 'status-en-curso'
    };
    return mapa[estado] || 'status-pendiente';
}

let __ultimasNotificaciones = [];

async function actualizarBadgeNotificaciones() {
    const usuarioJSON = localStorage.getItem('usuarioLogueado');
    const badge = document.querySelector('.notif-badge');
    if (!usuarioJSON || !badge) return;

    try {
        const usuario = JSON.parse(usuarioJSON);
        const res = await fetch(`${API_BASE}/notificaciones/usuario/${usuario.id}`);
        if (!res.ok) return;
        __ultimasNotificaciones = await res.json();

        const noLeidas = __ultimasNotificaciones.filter(n => !n.leida).length;
        if (noLeidas > 0) {
            badge.style.display = 'flex';
            badge.innerText = noLeidas > 9 ? '9+' : noLeidas;
        } else {
            badge.style.display = 'none';
        }
    } catch (error) {
        console.warn('No se pudieron cargar las notificaciones.');
    }
}

function tiempoRelativo(fechaISO) {
    if (!fechaISO) return '';
    const diffMs = Date.now() - new Date(fechaISO).getTime();
    const minutos = Math.floor(diffMs / 60000);
    if (minutos < 1) return 'Ahora mismo';
    if (minutos < 60) return `Hace ${minutos} min`;
    const horas = Math.floor(minutos / 60);
    if (horas < 24) return `Hace ${horas} h`;
    return `Hace ${Math.floor(horas / 24)} d`;
}

function iconoPorTipoNotif(tipo) {
    const mapa = { 'Cita': 'fa-calendar-check', 'Receta': 'fa-file-prescription', 'Pedido': 'fa-bag-shopping' };
    return mapa[tipo] || 'fa-bell';
}

async function toggleNotificaciones() {
    const existente = document.getElementById('panel-notificaciones');
    if (existente) {
        existente.remove();
        return;
    }

    await actualizarBadgeNotificaciones();

    const campana = document.getElementById('campana-notificaciones');
    const rect = campana ? campana.getBoundingClientRect() : { bottom: 70, right: 200 };

    const panel = document.createElement('div');
    panel.id = 'panel-notificaciones';
    panel.style.cssText = `position: fixed; top: ${rect.bottom + 10}px; right: ${Math.max(10, window.innerWidth - rect.right - 20)}px; width: 320px; max-height: 420px; overflow-y: auto; background: white; border-radius: 16px; box-shadow: var(--shadow-hover-card); border: 1px solid #E4E7EC; z-index: 500; padding: 10px;`;

    if (__ultimasNotificaciones.length === 0) {
        panel.innerHTML = `<p style="text-align:center; color: var(--text-muted); font-size: 13px; padding: 30px 10px;">No tienes notificaciones todavía.</p>`;
    } else {
        panel.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; padding: 6px 8px 12px 8px; border-bottom: 1px solid #F3F4F6;">
                <strong style="font-size: 14px;">Notificaciones</strong>
                <button style="background:none; border:none; color: var(--primary-green); font-size: 12px; font-weight:600; cursor:pointer;" onclick="marcarTodasNotificacionesLeidas()">Marcar todo leído</button>
            </div>
            ${__ultimasNotificaciones.map(n => `
                <div onclick="marcarNotificacionLeida(${n.id})" style="display:flex; gap:10px; padding: 12px 8px; border-bottom: 1px solid #F9FAFB; cursor:pointer; ${n.leida ? '' : 'background:#F0FDF4;'}">
                    <div style="width:34px; height:34px; min-width:34px; border-radius:10px; background: var(--light-green); color: var(--primary-green); display:flex; align-items:center; justify-content:center;">
                        <i class="fa-solid ${iconoPorTipoNotif(n.tipo)}"></i>
                    </div>
                    <div>
                        <p style="font-size:13px; font-weight:700; color: var(--dark-text);">${esc(n.titulo)}</p>
                        <p style="font-size:12px; color: var(--text-muted); margin: 2px 0;">${enlazarTexto(n.mensaje)}</p>
                        <span style="font-size:11px; color:#9CA3AF;">${tiempoRelativo(n.fecha)}</span>
                    </div>
                </div>
            `).join('')}
        `;
    }

    document.body.appendChild(panel);

    setTimeout(() => {
        document.addEventListener('click', function cerrarSiFuera(e) {
            const panelActual = document.getElementById('panel-notificaciones');
            if (panelActual && !panelActual.contains(e.target) && e.target.id !== 'campana-notificaciones' && !e.target.closest('#campana-notificaciones')) {
                panelActual.remove();
                document.removeEventListener('click', cerrarSiFuera);
            }
        });
    }, 0);
}

async function marcarNotificacionLeida(id) {
    try {
        await fetch(`${API_BASE}/notificaciones/${id}/leer`, { method: 'PATCH' });
        const n = __ultimasNotificaciones.find(x => x.id === id);
        if (n) n.leida = true;
        await actualizarBadgeNotificaciones();
        document.getElementById('panel-notificaciones')?.remove();
        toggleNotificaciones();
    } catch (error) {
        console.warn('No se pudo marcar como leída.');
    }
}

async function marcarTodasNotificacionesLeidas() {
    const usuarioJSON = localStorage.getItem('usuarioLogueado');
    if (!usuarioJSON) return;
    const usuario = JSON.parse(usuarioJSON);
    try {
        await fetch(`${API_BASE}/notificaciones/usuario/${usuario.id}/leer-todas`, { method: 'PATCH' });
        document.getElementById('panel-notificaciones')?.remove();
        await actualizarBadgeNotificaciones();
    } catch (error) {
        console.warn('No se pudo marcar todo como leído.');
    }
}

function actualizarVisibilidadAsistenteIA() {
    const boton = document.getElementById('btn-asistente-ia');
    if (!boton) return;
    const haySesion = !!localStorage.getItem('usuarioLogueado');
    boton.style.display = haySesion ? 'flex' : 'none';
    if (!haySesion) {
        const panel = document.getElementById('panel-asistente-ia');
        if (panel) panel.style.display = 'none';
    }
}

function reiniciarChatAsistente() {
    const mensajes = document.getElementById('asistente-ia-mensajes');
    if (mensajes) mensajes.innerHTML = '';
    const panel = document.getElementById('panel-asistente-ia');
    if (panel) panel.style.display = 'none';
}

function agregarMensajeAsistente(texto, esUsuario) {
    const contenedor = document.getElementById('asistente-ia-mensajes');
    const burbuja = document.createElement('div');
    burbuja.style.cssText = esUsuario
        ? 'align-self: flex-end; background: var(--green-gradient); color: white; padding: 10px 14px; border-radius: 16px 16px 4px 16px; max-width: 80%; font-size: 13px; line-height: 1.4;'
        : 'align-self: flex-start; background: #F3F4F6; color: var(--dark-text); padding: 10px 14px; border-radius: 16px 16px 16px 4px; max-width: 80%; font-size: 13px; line-height: 1.4;';
    burbuja.innerText = texto;
    contenedor.appendChild(burbuja);
    contenedor.scrollTop = contenedor.scrollHeight;
}

function toggleAsistenteIA() {
    const panel = document.getElementById('panel-asistente-ia');
    if (!panel) return;
    const abrir = panel.style.display === 'none' || panel.style.display === '';
    panel.style.display = abrir ? 'flex' : 'none';

    if (abrir && document.getElementById('asistente-ia-mensajes').children.length === 0) {
        agregarMensajeAsistente('¡Hola! 🐾 Soy el asistente de Vamos Pet. Puedo ayudarte con dudas sobre servicios, agendar citas, farmacia o cuidado de tu mascota. ¿En qué te ayudo?', false);
    }
}

document.addEventListener('submit', async (e) => {
    if (e.target.id !== 'form-asistente-ia') return;
    e.preventDefault();

    const input = document.getElementById('asistente-ia-input');
    const mensaje = input.value.trim();
    if (!mensaje) return;

    agregarMensajeAsistente(mensaje, true);
    input.value = '';

    const contenedor = document.getElementById('asistente-ia-mensajes');
    const indicador = document.createElement('div');
    indicador.id = 'asistente-ia-cargando';
    indicador.style.cssText = 'align-self: flex-start; color: var(--text-muted); font-size: 12px;';
    indicador.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Escribiendo...';
    contenedor.appendChild(indicador);
    contenedor.scrollTop = contenedor.scrollHeight;

    try {
        const res = await fetch(`${API_BASE}/asistente-ia`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mensaje })
        });
        const data = await res.json();
        document.getElementById('asistente-ia-cargando')?.remove();

        if (res.ok) {
            agregarMensajeAsistente(data.respuesta, false);
        } else {
            agregarMensajeAsistente(data.error || 'No pude responder en este momento.', false);
        }
    } catch (error) {
        document.getElementById('asistente-ia-cargando')?.remove();
        agregarMensajeAsistente('No pude conectar con el servidor. Verifica que el backend esté corriendo.', false);
    }
});

const SIDEBAR_POR_ROL = {
    3: {
        titulo: 'Panel Veterinario', icono: 'fa-stethoscope',
        items: [
            { pagina: 'dashboard-veterinario', etiqueta: 'Dashboard', icono: 'fa-chart-pie' },
            { pagina: 'citas', etiqueta: 'Gestión de Citas', icono: 'fa-calendar-check' },
            { pagina: 'examenes', etiqueta: 'Exámenes y Cirugías', icono: 'fa-flask-vial' },
            { pagina: 'reservas', etiqueta: 'Reservas y viajes', icono: 'fa-hotel' },
            { pagina: 'emergencias', etiqueta: 'Emergencias', icono: 'fa-truck-medical' },
            { pagina: 'clientes', etiqueta: 'Clientes y Mascotas', icono: 'fa-users' }
        ]
    },
    4: {
        titulo: 'Panel Auxiliar', icono: 'fa-user-nurse',
        items: [
            { pagina: 'dashboard-auxiliar', etiqueta: 'Dashboard', icono: 'fa-chart-pie' },
            { pagina: 'venta-mostrador', etiqueta: 'Venta en Mostrador', icono: 'fa-cash-register' },
            { pagina: 'clientes', etiqueta: 'Clientes y Mascotas', icono: 'fa-users' },
            { pagina: 'citas', etiqueta: 'Citas y reasignaciones', icono: 'fa-calendar-check' },
            { pagina: 'emergencias', etiqueta: 'Ingreso por Emergencia', icono: 'fa-truck-medical' },
            { pagina: 'examenes', etiqueta: 'Exámenes (subir resultados)', icono: 'fa-flask-vial' },
            { pagina: 'reservas', etiqueta: 'Reservas y viajes', icono: 'fa-hotel' },
            { pagina: 'finanzas', etiqueta: 'Por cobrar y Yape', icono: 'fa-file-invoice-dollar' },
            { pagina: 'comprobantes', etiqueta: 'Comprobantes', icono: 'fa-receipt' },
            { pagina: 'inventario', etiqueta: 'Inventario Tienda', icono: 'fa-box-open' },
            { pagina: 'banners', etiqueta: 'Banners de la tienda', icono: 'fa-images' },
            { pagina: 'mensajes', etiqueta: 'Mensajes de contacto', icono: 'fa-envelope-open-text' },
            { pagina: 'proveedores', etiqueta: 'Proveedores', icono: 'fa-truck-field' },
            { pagina: 'compras', etiqueta: 'Compras', icono: 'fa-cart-flatbed' },
            { pagina: 'devoluciones', etiqueta: 'Devoluciones', icono: 'fa-rotate-left' }
        ]
    }
};

const PANEL_POR_ROL = {
    1: { etiqueta: 'Panel Admin', icono: 'fa-gauge-high' },
    3: { etiqueta: 'Panel Veterinario', icono: 'fa-stethoscope' },
    4: { etiqueta: 'Panel Auxiliar', icono: 'fa-user-nurse' }
};

function aplicarSidebarPorRol(vista) {
    const usuarioJSON = localStorage.getItem('usuarioLogueado');
    if (!usuarioJSON) return;
    const usuario = JSON.parse(usuarioJSON);
    const config = SIDEBAR_POR_ROL[usuario.rol_id];
    if (!config) return;

    const sidebar = document.querySelector('.admin-sidebar');
    if (!sidebar) return;

    const paginaActual = (vista || '').split('/')[1];
    sidebar.innerHTML = `
        <div style="color: white; font-size: 18px; font-weight: 800; padding: 10px 18px 20px 18px; border-bottom: 1px solid #374151; margin-bottom: 10px;">
            <i class="fa-solid ${config.icono}" style="color: var(--primary-green);"></i> ${esc(config.titulo)}
        </div>
        ${config.items.map(item => `
            <button class="sidebar-item ${item.pagina === paginaActual ? 'active' : ''}" onclick="cargarVista('VistaAdmin/${item.pagina}')"><i class="fa-solid ${item.icono}"></i> ${item.etiqueta}</button>
        `).join('')}
    `;
}

async function cargarVista(vista) {
    const appContent = document.getElementById('app-content');
    cerrarMenuMovil();

    if (vista.startsWith('VistaAdmin/')) {
        const usuarioJSON = localStorage.getItem('usuarioLogueado');
        if (!usuarioJSON) {
            alert('Debes iniciar sesión para acceder al panel.');
            vista = 'login';
        } else {
            const usuario = JSON.parse(usuarioJSON);
            const permitidas = paginasPermitidasParaRol(usuario.rol_id);
            const paginaSolicitada = vista.split('/')[1];
            if (!permitidas) {
                alert('Tu cuenta no tiene acceso al panel interno.');
                vista = 'VistaCliente/perfil-cliente';
            } else if (!permitidas.includes(paginaSolicitada)) {
                alert('No tienes permiso para ver esa sección.');
                vista = dashboardHomeParaRol(usuario.rol_id);
            }
        }
    }
    
    try {
        const response = await fetch(`vistas/${vista}.html`);
        if (!response.ok) throw new Error('Vista no encontrada');
        
        const html = await response.text();
        appContent.innerHTML = html;

        appContent.querySelectorAll('script').forEach(scriptViejo => {
            const scriptNuevo = document.createElement('script');
            if (scriptViejo.src) {
                scriptNuevo.src = scriptViejo.src;
            } else {
                scriptNuevo.textContent = scriptViejo.textContent;
            }
            scriptViejo.replaceWith(scriptNuevo);
        });

        if (vista.startsWith('VistaAdmin/')) {
            aplicarSidebarPorRol(vista);
            mostrarAvisoSupervision(vista);
        }
        prepararFormulariosCuenta(appContent);
        pintarClinica(appContent);

        window.scrollTo({ top: 0, behavior: 'smooth' });

    } catch (error) {
        appContent.innerHTML = `
            <div style="text-align:center; padding: 100px 20px;">
                <h2 style="color: var(--primary-green); font-size: 40px;"><i class="fa-solid fa-triangle-exclamation"></i></h2>
                <h3 style="margin: 20px 0; color: var(--dark-text);">Página en construcción</h3>
                <p style="color: var(--text-muted); margin-bottom: 30px;">Estamos trabajando en la vista: <b>${vista}</b></p>
                <button class="btn-primary" onclick="cargarVista('home')">Volver al Inicio</button>
            </div>
        `;
    }
}

function datosRegistro() {
    return {
        nombre: normalizarNombre(document.getElementById('registro-nombre').value),
        dni: document.getElementById('registro-dni').value.trim(),
        correo: document.getElementById('registro-email').value.trim(),
        telefono: document.getElementById('registro-telefono').value.trim(),
        contrasena: document.getElementById('registro-password').value
    };
}

const CAMPOS_REGISTRO = { nombre: 'registro-nombre', dni: 'registro-dni', correo: 'registro-email', telefono: 'registro-telefono', contrasena: 'registro-password', codigo: 'registro-codigo' };

window.reenviarCodigoRegistro = async function () {
    const res = await fetch(`${API_URL}/registro`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(datosRegistro())
    });
    const data = await res.json().catch(() => ({}));
    alert(data.requiereCodigo ? 'Te enviamos un código nuevo.' : (data.error || 'No se pudo reenviar.'));
};

document.addEventListener('submit', async (e) => {
    e.preventDefault(); 

    if (e.target.id === 'form-registro') {
        const form = e.target;
        if (!formularioCuentaValido(form)) return;

        const codigoInput = document.getElementById('registro-codigo');
        const enPasoCodigo = codigoInput && document.getElementById('registro-paso-codigo').style.display !== 'none';
        if (enPasoCodigo && !REGLAS_CUENTA.codigo.rx.test(codigoInput.value.trim())) {
            mensajeCampo(codigoInput, REGLAS_CUENTA.codigo.msg, 'error');
            codigoInput.focus();
            return;
        }

        const boton = document.getElementById('registro-boton');
        boton.disabled = true;
        try {
            const url = enPasoCodigo ? `${API_URL}/registro/confirmar` : `${API_URL}/registro`;
            const cuerpo = datosRegistro();
            if (enPasoCodigo) cuerpo.codigo = codigoInput.value.trim();
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(cuerpo)
            });
            const data = await response.json().catch(() => ({}));

            if (data.requiereCodigo) {
                document.getElementById('registro-paso-codigo').style.display = 'block';
                document.getElementById('registro-codigo-msg').innerText = data.mensaje || 'Enviamos un código a tu correo.';
                boton.innerText = 'Verificar y crear cuenta';
                codigoInput.focus();
                return;
            }

            if (response.ok) {
                alert('¡Cuenta creada con éxito! Ahora inicia sesión.');
                cargarVista('login');
            } else if (!mostrarErroresServidor(data.campos, CAMPOS_REGISTRO)) {
                alert(data.error || 'No se pudo crear la cuenta. Revisa los datos.');
            }
        } catch (error) {
            console.error('Error de conexión:', error);
            alert('No pudimos conectar con el servidor (Verifica que Spring Boot esté corriendo).');
        } finally {
            boton.disabled = false;
        }
    }

    if (e.target.id === 'form-login') {
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;

        try {
            const response = await fetch(`${API_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ correo: email, contrasena: password })
            });

            if (response.ok) {
                const usuario = await response.json();
                
                iniciarSesionLocal(usuario);
                actualizarMenuUsuario();
                reiniciarChatAsistente();
                
                if (usuario.rol_id === 1 || usuario.rol_id === 3 || usuario.rol_id === 4) {
                    alert(`¡Acceso concedido, ${usuario.nombre}!`);
                    cargarVista(dashboardHomeParaRol(usuario.rol_id));
                } else {
                    alert(`¡Bienvenido de vuelta, ${usuario.nombre}!`);
                    cargarVista('VistaCliente/perfil-cliente');
                }
            } else {
                const err = await response.json().catch(() => ({}));
                alert(err.error || 'Correo o contraseña incorrectos.');
            }
        } catch (error) {
            console.error('Error de conexión:', error);
            alert('No pudimos conectar con el servidor (Verifica que Spring Boot esté corriendo).');
        }
    }

    if (e.target.id === 'form-contacto') {
        const form = e.target;
        const boton = form.querySelector('button[type="submit"]');
        const valor = (id) => ((document.getElementById(id) || {}).value || '').trim();
        const campos = { nombre: 'contacto-nombre', correo: 'contacto-email', telefono: 'contacto-telefono', asunto: 'contacto-motivo', mensaje: 'contacto-mensaje' };
        Object.values(campos).forEach(id => mensajeCampo(document.getElementById(id), '', ''));
        const confirmacion = document.getElementById('contacto-confirmacion');
        if (confirmacion) confirmacion.hidden = true;
        const errores = {};
        if (!REGLAS_CUENTA.nombre.rx.test(normalizarNombre(valor('contacto-nombre')))) errores.nombre = REGLAS_CUENTA.nombre.msg;
        if (!REGLAS_CUENTA.correo.rx.test(valor('contacto-email'))) errores.correo = REGLAS_CUENTA.correo.msg;
        if (valor('contacto-telefono') && !REGLAS_CUENTA.telefono.rx.test(valor('contacto-telefono'))) errores.telefono = REGLAS_CUENTA.telefono.msg;
        if (!valor('contacto-motivo')) errores.asunto = 'Elige el motivo.';
        const largo = valor('contacto-mensaje').length;
        if (largo < 10 || largo > 2000) errores.mensaje = 'El mensaje debe tener entre 10 y 2000 caracteres.';
        if (Object.keys(errores).length) { mostrarErroresServidor(errores, campos); return; }
        boton.disabled = true;
        try {
            const res = await fetch(`${API_BASE}/contacto`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    nombre: normalizarNombre(valor('contacto-nombre')),
                    correo: valor('contacto-email'),
                    telefono: valor('contacto-telefono'),
                    asunto: valor('contacto-motivo'),
                    mensaje: valor('contacto-mensaje'),
                    sitio_web: valor('contacto-sitio')
                })
            });
            const data = await res.json().catch(() => ({}));
            if (res.ok) {
                form.reset();
                if (confirmacion) { confirmacion.textContent = '¡Gracias! ' + (data.mensaje || 'Recibimos tu mensaje.'); confirmacion.hidden = false; }
            } else if (!mostrarErroresServidor(data.campos, campos)) {
                alert(data.error || 'No se pudo enviar tu mensaje. Intenta de nuevo.');
            }
        } catch (error) {
            alert('No pudimos conectar con el servidor. Intenta de nuevo en unos minutos.');
        } finally {
            boton.disabled = false;
        }
    }
});

let __configClinica = null;
let __promesaConfigClinica = null;

function cargarConfiguracionClinica(forzar) {
    if (forzar || !__promesaConfigClinica) {
        __promesaConfigClinica = fetch(`${API_BASE}/configuracion`)
            .then(r => r.ok ? r.json() : null)
            .catch(() => null)
            .then(c => { if (c) __configClinica = c; pintarClinica(document); return __configClinica; });
    }
    return __promesaConfigClinica;
}

function horasCancelacionClinica() {
    return __configClinica && __configClinica.horas_cancelacion ? __configClinica.horas_cancelacion : 24;
}

function pintarClinica(raiz) {
    const c = __configClinica;
    if (!c || !raiz) return;
    raiz.querySelectorAll('[data-clinica]').forEach(el => {
        const v = c[el.dataset.clinica];
        el.textContent = v == null ? '' : String(v);
        el.hidden = v == null || String(v).trim() === '';
    });
    raiz.querySelectorAll('[data-clinica-href]').forEach(el => {
        const tipo = el.dataset.clinicaHref;
        if (tipo === 'whatsapp') el.href = c.whatsapp ? `https://wa.me/${encodeURIComponent(c.whatsapp)}` : '#';
        else if (tipo === 'tel') el.href = c.telefono ? `tel:${c.telefono.replace(/\s/g, '')}` : '#';
        else if (tipo === 'correo') el.href = c.correo ? `mailto:${c.correo}` : '#';
    });
    raiz.querySelectorAll('[data-clinica-si]').forEach(el => { el.hidden = !c[el.dataset.clinicaSi]; });
    raiz.querySelectorAll('[data-clinica-mapa]').forEach(el => {
        if (c.direccion) el.src = `https://www.google.com/maps?q=${encodeURIComponent(c.direccion)}&output=embed`;
    });
}

function actualizarMenuUsuario() {
    marcarRolEnPagina();
    const usuarioJSON = localStorage.getItem('usuarioLogueado');
    const headerActions = document.querySelector('header > div:last-child');
    
    if (usuarioJSON) {
        const usuario = JSON.parse(usuarioJSON);
        
        if (usuario.rol_id === 1 || usuario.rol_id === 3 || usuario.rol_id === 4) {
            headerActions.innerHTML = `
                <div style="display: flex; align-items: center; gap: 15px;">
                    <div class="hover-card" style="position: relative; cursor: pointer; font-size: 20px; color: var(--dark-text);" onclick="toggleNotificaciones()" id="campana-notificaciones">
                        <i class="fa-regular fa-bell"></i>
                        <span class="notif-badge" style="display:none; position: absolute; top: -6px; right: -8px; background: #EF4444; color: white; font-size: 10px; font-weight: bold; padding: 2px 5px; border-radius: 50%;">0</span>
                    </div>
                    <button class="btn-primary" onclick="irAlPanelSegunRol()" style="background: #1B2939;">
                        <i class="fa-solid ${(PANEL_POR_ROL[usuario.rol_id] || PANEL_POR_ROL[1]).icono}"></i> ${(PANEL_POR_ROL[usuario.rol_id] || PANEL_POR_ROL[1]).etiqueta}
                    </button>
                    <button class="btn-outline hover-card" onclick="cerrarSesion()" style="padding: 8px 15px; font-size: 13px; border-radius: 8px;">
                        <i class="fa-solid fa-right-from-bracket"></i> Salir
                    </button>
                </div>
                <button class="mobile-menu-toggle" onclick="toggleMenuMovil()" aria-label="Abrir menú"><i class="fa-solid fa-bars"></i></button>
            `;
        } 
        else {
            headerActions.innerHTML = `
                <div style="text-align: right; line-height: 1.2;">
                    <span data-clinica-si="emergencias_24h" style="font-size: 11px; color: var(--text-muted); display: block;">Emergencias 24/7</span>
                    <strong style="color: var(--dark-text); font-size: 14px;"><i class="fa-solid fa-phone" style="color: var(--primary-green);"></i> <a data-clinica="telefono" data-clinica-href="tel" style="color: inherit; text-decoration: none;"></a></strong>
                </div>
                
                <div class="hover-card" style="position: relative; cursor: pointer; font-size: 22px; color: var(--dark-text);" onclick="cargarVista('carrito')">
                    <i class="fa-solid fa-cart-shopping"></i>
                    <span class="carrito-badge" style="position: absolute; top: -8px; right: -10px; background: var(--primary-green); color: white; font-size: 11px; font-weight: bold; padding: 2px 6px; border-radius: 50%;">0</span>
                </div>

                <div class="hover-card" style="position: relative; cursor: pointer; font-size: 20px; color: var(--dark-text);" onclick="toggleNotificaciones()" id="campana-notificaciones">
                    <i class="fa-regular fa-bell"></i>
                    <span class="notif-badge" style="display:none; position: absolute; top: -6px; right: -8px; background: #EF4444; color: white; font-size: 10px; font-weight: bold; padding: 2px 5px; border-radius: 50%;">0</span>
                </div>

                <div style="display: flex; align-items: center; gap: 15px; margin-left: 15px; border-left: 1px solid #eee; padding-left: 15px;">
                    <div style="text-align: right; cursor: pointer;" onclick="cargarVista('VistaCliente/perfil-cliente')">
                        <span style="display: block; font-size: 12px; color: var(--text-muted);">Hola,</span>
                        <strong style="color: var(--primary-green); font-size: 14px;">${usuario.nombre.split(' ')[0]}</strong>
                    </div>
                    <button class="btn-outline hover-card" onclick="cerrarSesion()" style="padding: 8px 15px; font-size: 13px; border-radius: 8px;">
                        <i class="fa-solid fa-right-from-bracket"></i> Salir
                    </button>
                </div>
                <button class="mobile-menu-toggle" onclick="toggleMenuMovil()" aria-label="Abrir menú"><i class="fa-solid fa-bars"></i></button>
            `;
        }
    } else {
        headerActions.innerHTML = `
            <div style="text-align: right; line-height: 1.2;">
                <span data-clinica-si="emergencias_24h" style="font-size: 11px; color: var(--text-muted); display: block;">Emergencias 24/7</span>
                <strong style="color: var(--dark-text); font-size: 14px;"><i class="fa-solid fa-phone" style="color: var(--primary-green);"></i> <a data-clinica="telefono" data-clinica-href="tel" style="color: inherit; text-decoration: none;"></a></strong>
            </div>
            
            <div class="hover-card" style="position: relative; cursor: pointer; font-size: 22px; color: var(--dark-text);" onclick="cargarVista('carrito')">
                <i class="fa-solid fa-cart-shopping"></i>
                <span class="carrito-badge" style="position: absolute; top: -8px; right: -10px; background: var(--primary-green); color: white; font-size: 11px; font-weight: bold; padding: 2px 6px; border-radius: 50%;">0</span>
            </div>

            <button class="btn-primary" onclick="cargarVista('login')">
                <i class="fa-regular fa-user"></i> Mi cuenta
            </button>
            <button class="mobile-menu-toggle" onclick="toggleMenuMovil()" aria-label="Abrir menú"><i class="fa-solid fa-bars"></i></button>
        `;
    }
    actualizarBadgeCarrito();
    actualizarBadgeNotificaciones();
    actualizarVisibilidadAsistenteIA();
    pintarClinica(headerActions);
}

function cerrarSesion() {
    ocultarAvisoSesion();
    if (tokenActual()) fetch(`${API_BASE}/sesion/cerrar`, { method: 'POST' }).catch(() => {});
    limpiarSesionLocal();
    reiniciarChatAsistente();
    actualizarMenuUsuario();
    cargarVista('home');
}

document.addEventListener('DOMContentLoaded', () => {
    if (revisarSesion()) return;
    actualizarMenuUsuario();
    actualizarBadgeCarrito();
    cargarConfiguracionClinica();
    if (new URLSearchParams(window.location.search).get('verificar')) cargarVista('verificar-comprobante');
    else cargarVista('home');
});

function luhnValido(numero) {
    const d = String(numero).replace(/\D/g, '');
    if (d.length < 13 || d.length > 19) return false;
    let suma = 0, doble = false;
    for (let i = d.length - 1; i >= 0; i--) {
        let n = parseInt(d[i], 10);
        if (doble) { n *= 2; if (n > 9) n -= 9; }
        suma += n;
        doble = !doble;
    }
    return suma % 10 === 0;
}

function vencimientoValido(mes, anio) {
    const m = parseInt(mes, 10);
    let a = parseInt(anio, 10);
    if (isNaN(m) || isNaN(a) || m < 1 || m > 12) return false;
    if (a < 100) a += 2000;
    const fin = new Date(a, m, 0, 23, 59, 59);
    return fin >= new Date();
}

function formatearNumeroTarjeta(input) {
    const d = input.value.replace(/\D/g, '').slice(0, 19);
    input.value = d.replace(/(.{4})/g, '$1 ').trim();
}

let __alCancelarPago = null;

function cerrarModalPago(pagado) {
    const m = document.getElementById('modal-pago-global');
    if (m) m.remove();
    const alCancelar = __alCancelarPago;
    __alCancelarPago = null;
    if (m && !pagado && alCancelar) alCancelar();
}

function abrirModalPago(opciones) {
    const usuarioJSON = localStorage.getItem('usuarioLogueado');
    if (!usuarioJSON) { cargarVista('login'); return; }
    const usuario = JSON.parse(usuarioJSON);
    const personal = usuario.rol_id !== 2;
    cerrarModalPago();
    __alCancelarPago = opciones.alCancelar || null;
    const metodos = personal ? ['Efectivo', 'Tarjeta', 'Yape'] : ['Tarjeta', 'Yape'];
    const html = `
    <div class="modal-overlay" id="modal-pago-global" style="display:flex;">
        <div class="modal-box" style="max-height:92vh; overflow-y:auto;">
            <h3><i class="fa-solid fa-credit-card" style="color:var(--primary-green);"></i> ${esc(opciones.titulo || 'Pagar')}</h3>
            <p style="font-size:22px; font-weight:800; margin-bottom:12px;">S/ ${Number(opciones.monto).toFixed(2)}</p>
            ${opciones.detalle ? `<p style="font-size:13px; color:var(--text-muted); margin-bottom:12px;">${esc(opciones.detalle)}</p>` : ''}
            <div class="input-group">
                <label>Método de pago</label>
                <select id="pg-metodo">${metodos.map(m => `<option value="${m}">${m === 'Yape' ? 'Yape (QR)' : m}</option>`).join('')}</select>
            </div>
            <div id="pg-tarjeta">
                <div class="input-group"><label>Nombre en la tarjeta</label><input type="text" id="pg-nombre" maxlength="60" autocomplete="off"></div>
                <div class="input-group"><label>Número de tarjeta</label><input type="text" id="pg-numero" inputmode="numeric" maxlength="23" placeholder="0000 0000 0000 0000" autocomplete="off" oninput="formatearNumeroTarjeta(this)"></div>
                <div style="display:flex; gap:10px;">
                    <div class="input-group" style="flex:1;"><label>Mes</label><input type="text" id="pg-mes" inputmode="numeric" maxlength="2" placeholder="MM"></div>
                    <div class="input-group" style="flex:1;"><label>Año</label><input type="text" id="pg-anio" inputmode="numeric" maxlength="4" placeholder="AA"></div>
                    <div class="input-group" style="flex:1;"><label>CVV</label><input type="password" id="pg-cvv" inputmode="numeric" maxlength="4" autocomplete="off"></div>
                </div>
                <div class="input-group"><label>Correo asociado a la tarjeta *</label><input type="email" id="pg-correo" maxlength="120" placeholder="correo@ejemplo.com" value="${esc(usuario.email || usuario.correo || '')}"></div>
                <p style="font-size:11px; color:var(--text-muted);">Solo guardamos los últimos 4 dígitos. Te enviaremos el comprobante a este correo.</p>
            </div>
            <div id="pg-yape" style="display:none; text-align:center;">
                <img src="img/yape-qr.png" alt="QR de Yape" style="width:200px; height:200px; object-fit:contain; border:1px solid #E5E7EB; border-radius:12px; margin-bottom:10px;">
                <p style="font-size:13px; color:var(--text-muted); margin-bottom:10px;">Escanea el QR, paga S/ ${Number(opciones.monto).toFixed(2)} y escribe el número de operación.</p>
                <div class="input-group" style="text-align:left;"><label>Número de operación *</label><input type="text" id="pg-operacion" maxlength="20" placeholder="Ej. 12345678"></div>
                <p style="font-size:11px; color:var(--text-muted);">${personal ? 'El pago se registra de inmediato.' : 'Nuestro equipo confirmará tu pago y recibirás el comprobante.'}</p>
            </div>
            <div id="pg-efectivo" style="display:none;">
                <p style="font-size:13px; color:var(--text-muted);">Registra el cobro en efectivo recibido en caja.</p>
            </div>
            <p id="pg-error" style="color:#DC2626; font-size:13px; margin:10px 0; display:none;"></p>
            <button class="btn-primary" id="pg-confirmar" style="width:100%; padding:12px;">Confirmar pago</button>
            <button class="btn-outline" style="width:100%; padding:12px; margin-top:10px;" onclick="cerrarModalPago()">Cancelar</button>
        </div>
    </div>`;
    document.body.insertAdjacentHTML('beforeend', html);
    const sel = document.getElementById('pg-metodo');
    const alternar = () => {
        document.getElementById('pg-tarjeta').style.display = sel.value === 'Tarjeta' ? 'block' : 'none';
        document.getElementById('pg-yape').style.display = sel.value === 'Yape' ? 'block' : 'none';
        document.getElementById('pg-efectivo').style.display = sel.value === 'Efectivo' ? 'block' : 'none';
    };
    sel.addEventListener('change', alternar);
    alternar();
    document.getElementById('pg-confirmar').addEventListener('click', async () => {
        const err = document.getElementById('pg-error');
        const boton = document.getElementById('pg-confirmar');
        err.style.display = 'none';
        const mostrar = (t) => { err.textContent = t; err.style.display = 'block'; };
        const datos = { metodo: sel.value };
        if (sel.value === 'Tarjeta') {
            const numero = document.getElementById('pg-numero').value.replace(/\D/g, '');
            const correo = document.getElementById('pg-correo').value.trim();
            if (document.getElementById('pg-nombre').value.trim().length < 3) return mostrar('Escribe el nombre que aparece en la tarjeta.');
            if (!luhnValido(numero)) return mostrar('El número de tarjeta no es válido.');
            if (!vencimientoValido(document.getElementById('pg-mes').value, document.getElementById('pg-anio').value)) return mostrar('La tarjeta está vencida o la fecha no es válida.');
            if (!/^\d{3,4}$/.test(document.getElementById('pg-cvv').value)) return mostrar('El CVV debe tener 3 o 4 dígitos.');
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(correo)) return mostrar('Ingresa el correo asociado a la tarjeta.');
            datos.correo_tarjeta = correo;
            datos.tarjeta_ultimos4 = numero.slice(-4);
        } else if (sel.value === 'Yape') {
            const op = document.getElementById('pg-operacion').value.trim();
            if (!/^[A-Za-z0-9]{6,20}$/.test(op)) return mostrar('El número de operación debe tener entre 6 y 20 caracteres.');
            datos.operacion_yape = op;
        }
        boton.disabled = true;
        try {
            const resultado = await opciones.enviar(datos);
            cerrarModalPago(true);
            if (opciones.alExito) opciones.alExito(resultado);
        } catch (e) {
            boton.disabled = false;
            mostrar(e.message || 'No se pudo procesar el pago.');
        }
    });
}

async function enviarJson(url, metodo, cuerpo) {
    const res = await fetch(url, { method: metodo, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(cuerpo) });
    let data = null;
    try { data = await res.json(); } catch (e) { data = null; }
    if (!res.ok) throw new Error((data && data.error) || 'No se pudo completar la operación.');
    return data;
}

function pagarReferencia(referencia, referenciaId, monto, titulo, detalle, alExito) {
    const usuario = JSON.parse(localStorage.getItem('usuarioLogueado') || 'null');
    if (!usuario) { cargarVista('login'); return; }
    abrirModalPago({
        titulo, monto, detalle,
        enviar: (datos) => enviarJson(`${API_BASE}/pagos/registrar`, 'POST', { usuario_id: usuario.id, referencia, referencia_id: referenciaId, ...datos }),
        alExito: (r) => {
            const pago = r.pago || {};
            const comp = r.comprobante;
            alert(pago.estado === 'Por_confirmar'
                ? 'Recibimos tu número de operación. Confirmaremos tu pago y te avisaremos.'
                : 'Pago registrado correctamente.' + (comp ? ' Tu comprobante ya está disponible.' : ''));
            if (comp) abrirArchivoApi(`${API_BASE}/comprobantes/${comp.id}/pdf?usuarioId=${usuario.id}`);
            if (alExito) alExito(r);
        }
    });
}

function enlazarTexto(texto) {
    return esc(texto).replace(/(https:\/\/[^\s<>"']+)/g, '<a href="$1" target="_blank" rel="noopener noreferrer" style="color:var(--primary-green); font-weight:700;">$1</a>');
}
