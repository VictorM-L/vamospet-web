
const API_URL = 'http://localhost:8080/api/usuarios';
const API_BASE = 'http://localhost:8080/api';

function usuarioActualId() {
    try { return (JSON.parse(localStorage.getItem('usuarioLogueado')) || {}).id; } catch (e) { return null; }
}
function fetchUsuarios() {
    return fetch(`${API_BASE}/usuarios?usuarioId=${encodeURIComponent(usuarioActualId())}`);
}

function esc(valor) {
    return String(valor == null ? '' : valor).replace(/[&<>"']/g, c => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

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

function desplazarCarrusel(idTrack, direccion) {
    const track = document.getElementById(idTrack);
    if (!track) return;
    const primeraTarjeta = track.querySelector('.shop-card');
    const ancho = primeraTarjeta ? primeraTarjeta.offsetWidth + 22 : 240;
    track.scrollBy({ left: ancho * 2 * direccion, behavior: 'smooth' });
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
        return `http://localhost:8080${data.url}`;
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
    const usuarioJSON = localStorage.getItem('usuarioLogueado');
    if (usuarioJSON) {
        cargarVista('VistaCliente/agendar-cita');
    } else {
        cargarVista('login');
    }
}

function verProductoDetalle(id) {
    localStorage.setItem('productoSeleccionadoId', id);
    cargarVista('producto-detalle');
}

function verHistorialMascota(id) {
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
    1: ['dashboard-admin', 'citas', 'examenes', 'emergencias', 'clientes', 'servicios', 'inventario', 'proveedores', 'compras', 'devoluciones', 'horarios', 'reportes', 'comprobantes', 'finanzas', 'personal', 'configuracion'],
    3: ['dashboard-veterinario', 'citas', 'examenes', 'emergencias', 'clientes'],
    4: ['dashboard-auxiliar', 'venta-mostrador', 'clientes', 'citas', 'examenes', 'emergencias', 'comprobantes']
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
        'Cancelada': 'status-cancelada'
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
                        <p style="font-size:12px; color: var(--text-muted); margin: 2px 0;">${esc(n.mensaje)}</p>
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
            { pagina: 'citas', etiqueta: 'Ver Citas', icono: 'fa-calendar-check' },
            { pagina: 'emergencias', etiqueta: 'Ingreso por Emergencia', icono: 'fa-truck-medical' },
            { pagina: 'examenes', etiqueta: 'Exámenes (subir resultados)', icono: 'fa-flask-vial' },
            { pagina: 'comprobantes', etiqueta: 'Comprobantes', icono: 'fa-receipt' }
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
        }
        
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

window.reenviarCodigoRegistro = async function () {
    const g = id => document.getElementById(id).value;
    const res = await fetch(`${API_URL}/registro`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nombre: g('registro-nombre'), dni: g('registro-dni'), correo: g('registro-email'), contrasena: g('registro-password') })
    });
    const data = await res.json().catch(() => ({}));
    alert(data.requiereCodigo ? 'Te enviamos un código nuevo.' : (data.error || 'No se pudo reenviar.'));
};

document.addEventListener('submit', async (e) => {
    e.preventDefault(); 

    if (e.target.id === 'form-registro') {
        const nombre = document.getElementById('registro-nombre').value;
        const dni = document.getElementById('registro-dni').value;
        const email = document.getElementById('registro-email').value;
        const password = document.getElementById('registro-password').value;
        const confirm = document.getElementById('registro-confirm').value;

        if (password !== confirm) {
            alert('¡Las contraseñas no coinciden, mi kin!');
            return;
        }
        if (password.length < 8) {
            alert('La contraseña debe tener al menos 8 caracteres.');
            return;
        }
        if (!/^\d{8}$/.test(dni.trim())) {
            alert('El DNI debe tener exactamente 8 dígitos.');
            return;
        }

        const codigoInput = document.getElementById('registro-codigo');
        const enPasoCodigo = codigoInput && document.getElementById('registro-paso-codigo').style.display !== 'none';

        try {
            const url = enPasoCodigo ? `${API_URL}/registro/confirmar` : `${API_URL}/registro`;
            const cuerpo = { nombre, dni, correo: email, contrasena: password };
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
                document.getElementById('registro-boton').innerText = 'Verificar y crear cuenta';
                return;
            }

            if (response.ok) {
                alert('¡Cuenta creada con éxito! Ahora inicia sesión.');
                cargarVista('login');
            } else {
                alert(data.error || 'Error al registrar. Intenta con otro correo.');
            }
        } catch (error) {
            console.error('Error de conexión:', error);
            alert('No pudimos conectar con el servidor (Verifica que Spring Boot esté corriendo).');
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
                
                localStorage.setItem('usuarioLogueado', JSON.stringify(usuario));
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
        const nombre = document.getElementById('contacto-nombre').value;
        const mensaje = document.getElementById('contacto-mensaje').value;

        alert(`¡Gracias ${nombre}! Recibimos tu mensaje y te responderemos a la brevedad.`);
        console.log('Mensaje de contacto (pendiente de endpoint backend):', {
            nombre,
            email: document.getElementById('contacto-email').value,
            telefono: document.getElementById('contacto-telefono').value,
            motivo: document.getElementById('contacto-motivo').value,
            mensaje
        });
        e.target.reset();
    }
});

function actualizarMenuUsuario() {
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
                    <span style="font-size: 11px; color: var(--text-muted); display: block;">Emergencias 24/7</span>
                    <strong style="color: var(--dark-text); font-size: 14px;"><i class="fa-solid fa-phone" style="color: var(--primary-green);"></i> +51 912 345 678</strong>
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
                <span style="font-size: 11px; color: var(--text-muted); display: block;">Emergencias 24/7</span>
                <strong style="color: var(--dark-text); font-size: 14px;"><i class="fa-solid fa-phone" style="color: var(--primary-green);"></i> +51 912 345 678</strong>
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
}

function cerrarSesion() {
    localStorage.removeItem('usuarioLogueado');
    reiniciarChatAsistente();
    actualizarMenuUsuario();
    cargarVista('home');
}

document.addEventListener('DOMContentLoaded', () => {
    actualizarMenuUsuario();
    actualizarBadgeCarrito();
    if (new URLSearchParams(window.location.search).get('verificar')) cargarVista('verificar-comprobante');
    else cargarVista('home');
});