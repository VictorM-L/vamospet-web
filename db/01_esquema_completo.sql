
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;


CREATE TABLE public.articulo_blog (
    id integer NOT NULL,
    titulo character varying(200) NOT NULL,
    categoria character varying(100),
    contenido text NOT NULL,
    autor character varying(100),
    fecha_publicacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    activo boolean DEFAULT true NOT NULL
);


CREATE SEQUENCE public.articulo_blog_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.articulo_blog_id_seq OWNED BY public.articulo_blog.id;


CREATE TABLE public.categoria_producto (
    id integer NOT NULL,
    nombre character varying(100),
    tipo character varying(30),
    activo boolean DEFAULT true NOT NULL
);


CREATE SEQUENCE public.categoria_producto_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.categoria_producto_id_seq OWNED BY public.categoria_producto.id;


CREATE TABLE public.cita (
    id integer NOT NULL,
    mascota_id integer,
    servicio_id integer,
    especialidad_id integer,
    veterinario_id integer,
    fecha_hora timestamp without time zone NOT NULL,
    estado character varying(50) DEFAULT 'Pendiente'::character varying,
    costo numeric(10,2) NOT NULL
);


CREATE TABLE public.cita_costo_adicional (
    id integer NOT NULL,
    cita_id integer NOT NULL,
    concepto character varying(200) NOT NULL,
    monto numeric(10,2) NOT NULL,
    agregado_por integer,
    fecha timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT cita_costo_adicional_monto_check CHECK ((monto > (0)::numeric))
);


CREATE SEQUENCE public.cita_costo_adicional_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cita_costo_adicional_id_seq OWNED BY public.cita_costo_adicional.id;


CREATE SEQUENCE public.cita_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cita_id_seq OWNED BY public.cita.id;


CREATE TABLE public.codigo_verificacion (
    id integer NOT NULL,
    usuario_id integer NOT NULL,
    codigo_hash character varying(64) NOT NULL,
    expira timestamp without time zone NOT NULL,
    intentos integer DEFAULT 0 NOT NULL,
    usado boolean DEFAULT false NOT NULL,
    creado timestamp without time zone DEFAULT now() NOT NULL
);


CREATE SEQUENCE public.codigo_verificacion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.codigo_verificacion_id_seq OWNED BY public.codigo_verificacion.id;


CREATE TABLE public.compra (
    id integer NOT NULL,
    proveedor_id integer NOT NULL,
    usuario_id integer NOT NULL,
    fecha timestamp without time zone DEFAULT now() NOT NULL,
    total numeric(12,2) DEFAULT 0 NOT NULL,
    observacion character varying(300),
    estado character varying(20) DEFAULT 'Registrada'::character varying NOT NULL
);


CREATE TABLE public.compra_detalle (
    id integer NOT NULL,
    compra_id integer NOT NULL,
    producto_id integer NOT NULL,
    cantidad integer NOT NULL,
    costo_unitario numeric(10,2) NOT NULL,
    CONSTRAINT compra_detalle_cantidad_check CHECK ((cantidad > 0))
);


CREATE SEQUENCE public.compra_detalle_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.compra_detalle_id_seq OWNED BY public.compra_detalle.id;


CREATE SEQUENCE public.compra_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.compra_id_seq OWNED BY public.compra.id;


CREATE TABLE public.comprobante (
    id integer NOT NULL,
    pago_id integer,
    tipo character varying(50) NOT NULL,
    numero character varying(50) NOT NULL,
    archivo character varying(255),
    codigo_verificacion character varying(64)
);


CREATE SEQUENCE public.comprobante_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.comprobante_id_seq OWNED BY public.comprobante.id;


CREATE TABLE public.devolucion (
    id integer NOT NULL,
    pedido_id integer NOT NULL,
    usuario_id integer NOT NULL,
    fecha timestamp without time zone DEFAULT now() NOT NULL,
    motivo character varying(300) NOT NULL,
    total_reembolso numeric(12,2) DEFAULT 0 NOT NULL
);


CREATE TABLE public.devolucion_detalle (
    id integer NOT NULL,
    devolucion_id integer NOT NULL,
    producto_id integer NOT NULL,
    cantidad integer NOT NULL,
    precio_unitario numeric(10,2) NOT NULL,
    CONSTRAINT devolucion_detalle_cantidad_check CHECK ((cantidad > 0))
);


CREATE SEQUENCE public.devolucion_detalle_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.devolucion_detalle_id_seq OWNED BY public.devolucion_detalle.id;


CREATE SEQUENCE public.devolucion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.devolucion_id_seq OWNED BY public.devolucion.id;


CREATE TABLE public.especialidad (
    id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion text,
    activo boolean DEFAULT true NOT NULL
);


CREATE SEQUENCE public.especialidad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.especialidad_id_seq OWNED BY public.especialidad.id;


CREATE TABLE public.historial_evento (
    id integer NOT NULL,
    mascota_id integer,
    tipo_evento character varying(100) NOT NULL,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    descripcion text NOT NULL,
    veterinario_id integer
);


CREATE SEQUENCE public.historial_evento_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.historial_evento_id_seq OWNED BY public.historial_evento.id;


CREATE TABLE public.horario_atencion (
    id integer NOT NULL,
    veterinario_id integer NOT NULL,
    dia_semana integer NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    CONSTRAINT horario_atencion_check CHECK ((hora_fin > hora_inicio)),
    CONSTRAINT horario_atencion_dia_semana_check CHECK (((dia_semana >= 1) AND (dia_semana <= 7)))
);


CREATE SEQUENCE public.horario_atencion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.horario_atencion_id_seq OWNED BY public.horario_atencion.id;


CREATE TABLE public.mascota (
    id integer NOT NULL,
    usuario_id integer,
    nombre character varying(100) NOT NULL,
    especie character varying(50) NOT NULL,
    raza character varying(50),
    sexo character varying(20),
    fecha_nacimiento date,
    foto character varying(255)
);


CREATE SEQUENCE public.mascota_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mascota_id_seq OWNED BY public.mascota.id;


CREATE TABLE public.movimiento_stock (
    id integer NOT NULL,
    producto_id integer,
    tipo character varying(20),
    cantidad integer,
    motivo character varying(255),
    usuario_id integer,
    fecha timestamp without time zone DEFAULT now()
);


CREATE SEQUENCE public.movimiento_stock_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.movimiento_stock_id_seq OWNED BY public.movimiento_stock.id;


CREATE TABLE public.notificacion (
    id integer NOT NULL,
    usuario_id integer,
    titulo character varying(150),
    mensaje text,
    tipo character varying(30),
    leida boolean DEFAULT false,
    fecha timestamp without time zone DEFAULT now(),
    fecha_programada timestamp without time zone,
    enviado boolean DEFAULT true NOT NULL,
    fecha_envio timestamp without time zone,
    referencia character varying(50)
);


CREATE SEQUENCE public.notificacion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notificacion_id_seq OWNED BY public.notificacion.id;


CREATE TABLE public.pago (
    id integer NOT NULL,
    usuario_id integer,
    cita_id integer,
    pedido_id integer,
    monto numeric(10,2) NOT NULL,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    metodo character varying(50) NOT NULL,
    id_registrado_por integer
);


CREATE SEQUENCE public.pago_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pago_id_seq OWNED BY public.pago.id;


CREATE TABLE public.pedido (
    id integer NOT NULL,
    usuario_id integer,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    estado character varying(50) DEFAULT 'Pendiente'::character varying,
    metodo_entrega character varying(100) NOT NULL,
    total numeric(10,2) NOT NULL,
    canal character varying(20) DEFAULT 'Web'::character varying
);


CREATE TABLE public.pedido_detalle (
    id integer NOT NULL,
    pedido_id integer,
    producto_id integer,
    cantidad integer NOT NULL,
    precio_unitario numeric(10,2) NOT NULL
);


CREATE SEQUENCE public.pedido_detalle_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pedido_detalle_id_seq OWNED BY public.pedido_detalle.id;


CREATE SEQUENCE public.pedido_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pedido_id_seq OWNED BY public.pedido.id;


CREATE TABLE public.producto (
    id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    categoria character varying(100),
    precio numeric(10,2) NOT NULL,
    stock integer DEFAULT 0 NOT NULL,
    requiere_receta boolean DEFAULT false,
    tipo character varying(50),
    marca character varying(100),
    imagen character varying(500),
    descripcion text,
    sku character varying(50),
    precio_costo numeric(10,2),
    stock_minimo integer DEFAULT 5,
    unidad_presentacion character varying(80),
    proveedor character varying(150),
    fecha_vencimiento date,
    estado character varying(30) DEFAULT 'Activo'::character varying,
    categoria_id integer,
    activo boolean DEFAULT true NOT NULL
);


CREATE SEQUENCE public.producto_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.producto_id_seq OWNED BY public.producto.id;


CREATE TABLE public.producto_imagen (
    id integer NOT NULL,
    producto_id integer,
    url character varying(500),
    activo boolean DEFAULT true NOT NULL
);


CREATE SEQUENCE public.producto_imagen_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.producto_imagen_id_seq OWNED BY public.producto_imagen.id;


CREATE TABLE public.proveedor (
    id integer NOT NULL,
    razon_social character varying(150) NOT NULL,
    ruc character varying(11) NOT NULL,
    contacto character varying(100),
    telefono character varying(30),
    correo character varying(150),
    direccion character varying(200),
    activo boolean DEFAULT true NOT NULL
);


CREATE SEQUENCE public.proveedor_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.proveedor_id_seq OWNED BY public.proveedor.id;


CREATE TABLE public.receta (
    id integer NOT NULL,
    mascota_id integer,
    veterinario_id integer,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    contenido text NOT NULL,
    archivo_url character varying(500),
    estado character varying(30) DEFAULT 'Pendiente'::character varying
);


CREATE SEQUENCE public.receta_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.receta_id_seq OWNED BY public.receta.id;


CREATE TABLE public.rol (
    id integer NOT NULL,
    nombre character varying(50) NOT NULL
);


CREATE SEQUENCE public.rol_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.rol_id_seq OWNED BY public.rol.id;


CREATE TABLE public.servicio (
    id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion text,
    precio_base numeric(10,2) NOT NULL,
    requiere_pago_previo boolean DEFAULT false,
    activo boolean DEFAULT true NOT NULL
);


CREATE SEQUENCE public.servicio_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.servicio_id_seq OWNED BY public.servicio.id;


CREATE TABLE public.usuario (
    id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    correo character varying(100),
    contrasena character varying(100),
    telefono character varying(20),
    direccion character varying(255),
    fecha_registro timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    rol_id integer,
    dni character varying(8),
    activo boolean DEFAULT true NOT NULL,
    correo_contacto character varying(150)
);


CREATE SEQUENCE public.usuario_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.usuario_id_seq OWNED BY public.usuario.id;


CREATE TABLE public.vacuna (
    id integer NOT NULL,
    mascota_id integer,
    nombre character varying(100) NOT NULL,
    fecha_aplicacion date NOT NULL,
    proxima_dosis date
);


CREATE SEQUENCE public.vacuna_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.vacuna_id_seq OWNED BY public.vacuna.id;


CREATE TABLE public.veterinario_especialidad (
    id integer NOT NULL,
    veterinario_id integer NOT NULL,
    especialidad_id integer NOT NULL
);


CREATE SEQUENCE public.veterinario_especialidad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.veterinario_especialidad_id_seq OWNED BY public.veterinario_especialidad.id;


ALTER TABLE ONLY public.articulo_blog ALTER COLUMN id SET DEFAULT nextval('public.articulo_blog_id_seq'::regclass);


ALTER TABLE ONLY public.categoria_producto ALTER COLUMN id SET DEFAULT nextval('public.categoria_producto_id_seq'::regclass);


ALTER TABLE ONLY public.cita ALTER COLUMN id SET DEFAULT nextval('public.cita_id_seq'::regclass);


ALTER TABLE ONLY public.cita_costo_adicional ALTER COLUMN id SET DEFAULT nextval('public.cita_costo_adicional_id_seq'::regclass);


ALTER TABLE ONLY public.codigo_verificacion ALTER COLUMN id SET DEFAULT nextval('public.codigo_verificacion_id_seq'::regclass);


ALTER TABLE ONLY public.compra ALTER COLUMN id SET DEFAULT nextval('public.compra_id_seq'::regclass);


ALTER TABLE ONLY public.compra_detalle ALTER COLUMN id SET DEFAULT nextval('public.compra_detalle_id_seq'::regclass);


ALTER TABLE ONLY public.comprobante ALTER COLUMN id SET DEFAULT nextval('public.comprobante_id_seq'::regclass);


ALTER TABLE ONLY public.devolucion ALTER COLUMN id SET DEFAULT nextval('public.devolucion_id_seq'::regclass);


ALTER TABLE ONLY public.devolucion_detalle ALTER COLUMN id SET DEFAULT nextval('public.devolucion_detalle_id_seq'::regclass);


ALTER TABLE ONLY public.especialidad ALTER COLUMN id SET DEFAULT nextval('public.especialidad_id_seq'::regclass);


ALTER TABLE ONLY public.historial_evento ALTER COLUMN id SET DEFAULT nextval('public.historial_evento_id_seq'::regclass);


ALTER TABLE ONLY public.horario_atencion ALTER COLUMN id SET DEFAULT nextval('public.horario_atencion_id_seq'::regclass);


ALTER TABLE ONLY public.mascota ALTER COLUMN id SET DEFAULT nextval('public.mascota_id_seq'::regclass);


ALTER TABLE ONLY public.movimiento_stock ALTER COLUMN id SET DEFAULT nextval('public.movimiento_stock_id_seq'::regclass);


ALTER TABLE ONLY public.notificacion ALTER COLUMN id SET DEFAULT nextval('public.notificacion_id_seq'::regclass);


ALTER TABLE ONLY public.pago ALTER COLUMN id SET DEFAULT nextval('public.pago_id_seq'::regclass);


ALTER TABLE ONLY public.pedido ALTER COLUMN id SET DEFAULT nextval('public.pedido_id_seq'::regclass);


ALTER TABLE ONLY public.pedido_detalle ALTER COLUMN id SET DEFAULT nextval('public.pedido_detalle_id_seq'::regclass);


ALTER TABLE ONLY public.producto ALTER COLUMN id SET DEFAULT nextval('public.producto_id_seq'::regclass);


ALTER TABLE ONLY public.producto_imagen ALTER COLUMN id SET DEFAULT nextval('public.producto_imagen_id_seq'::regclass);


ALTER TABLE ONLY public.proveedor ALTER COLUMN id SET DEFAULT nextval('public.proveedor_id_seq'::regclass);


ALTER TABLE ONLY public.receta ALTER COLUMN id SET DEFAULT nextval('public.receta_id_seq'::regclass);


ALTER TABLE ONLY public.rol ALTER COLUMN id SET DEFAULT nextval('public.rol_id_seq'::regclass);


ALTER TABLE ONLY public.servicio ALTER COLUMN id SET DEFAULT nextval('public.servicio_id_seq'::regclass);


ALTER TABLE ONLY public.usuario ALTER COLUMN id SET DEFAULT nextval('public.usuario_id_seq'::regclass);


ALTER TABLE ONLY public.vacuna ALTER COLUMN id SET DEFAULT nextval('public.vacuna_id_seq'::regclass);


ALTER TABLE ONLY public.veterinario_especialidad ALTER COLUMN id SET DEFAULT nextval('public.veterinario_especialidad_id_seq'::regclass);






























ALTER TABLE ONLY public.articulo_blog
    ADD CONSTRAINT articulo_blog_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.categoria_producto
    ADD CONSTRAINT categoria_producto_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.cita_costo_adicional
    ADD CONSTRAINT cita_costo_adicional_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.cita
    ADD CONSTRAINT cita_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.codigo_verificacion
    ADD CONSTRAINT codigo_verificacion_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.compra_detalle
    ADD CONSTRAINT compra_detalle_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.compra
    ADD CONSTRAINT compra_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.comprobante
    ADD CONSTRAINT comprobante_numero_key UNIQUE (numero);


ALTER TABLE ONLY public.comprobante
    ADD CONSTRAINT comprobante_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.devolucion_detalle
    ADD CONSTRAINT devolucion_detalle_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.devolucion
    ADD CONSTRAINT devolucion_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.especialidad
    ADD CONSTRAINT especialidad_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.historial_evento
    ADD CONSTRAINT historial_evento_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.horario_atencion
    ADD CONSTRAINT horario_atencion_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.mascota
    ADD CONSTRAINT mascota_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.movimiento_stock
    ADD CONSTRAINT movimiento_stock_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.notificacion
    ADD CONSTRAINT notificacion_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.pedido_detalle
    ADD CONSTRAINT pedido_detalle_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.pedido
    ADD CONSTRAINT pedido_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.producto_imagen
    ADD CONSTRAINT producto_imagen_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.producto
    ADD CONSTRAINT producto_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.proveedor
    ADD CONSTRAINT proveedor_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.proveedor
    ADD CONSTRAINT proveedor_ruc_key UNIQUE (ruc);


ALTER TABLE ONLY public.receta
    ADD CONSTRAINT receta_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.rol
    ADD CONSTRAINT rol_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.servicio
    ADD CONSTRAINT servicio_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_correo_key UNIQUE (correo);


ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_dni_key UNIQUE (dni);


ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.vacuna
    ADD CONSTRAINT vacuna_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.veterinario_especialidad
    ADD CONSTRAINT veterinario_especialidad_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.veterinario_especialidad
    ADD CONSTRAINT veterinario_especialidad_veterinario_id_especialidad_id_key UNIQUE (veterinario_id, especialidad_id);


CREATE UNIQUE INDEX ux_cita_vet_franja ON public.cita USING btree (veterinario_id, fecha_hora) WHERE ((veterinario_id IS NOT NULL) AND ((estado)::text <> 'Cancelada'::text));


CREATE UNIQUE INDEX ux_comprobante_codigo ON public.comprobante USING btree (codigo_verificacion);


ALTER TABLE ONLY public.cita_costo_adicional
    ADD CONSTRAINT cita_costo_adicional_agregado_por_fkey FOREIGN KEY (agregado_por) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.cita_costo_adicional
    ADD CONSTRAINT cita_costo_adicional_cita_id_fkey FOREIGN KEY (cita_id) REFERENCES public.cita(id);


ALTER TABLE ONLY public.cita
    ADD CONSTRAINT cita_especialidad_id_fkey FOREIGN KEY (especialidad_id) REFERENCES public.especialidad(id);


ALTER TABLE ONLY public.cita
    ADD CONSTRAINT cita_mascota_id_fkey FOREIGN KEY (mascota_id) REFERENCES public.mascota(id);


ALTER TABLE ONLY public.cita
    ADD CONSTRAINT cita_servicio_id_fkey FOREIGN KEY (servicio_id) REFERENCES public.servicio(id);


ALTER TABLE ONLY public.cita
    ADD CONSTRAINT cita_veterinario_id_fkey FOREIGN KEY (veterinario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.codigo_verificacion
    ADD CONSTRAINT codigo_verificacion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.compra_detalle
    ADD CONSTRAINT compra_detalle_compra_id_fkey FOREIGN KEY (compra_id) REFERENCES public.compra(id);


ALTER TABLE ONLY public.compra_detalle
    ADD CONSTRAINT compra_detalle_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES public.producto(id);


ALTER TABLE ONLY public.compra
    ADD CONSTRAINT compra_proveedor_id_fkey FOREIGN KEY (proveedor_id) REFERENCES public.proveedor(id);


ALTER TABLE ONLY public.compra
    ADD CONSTRAINT compra_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.comprobante
    ADD CONSTRAINT comprobante_pago_id_fkey FOREIGN KEY (pago_id) REFERENCES public.pago(id);


ALTER TABLE ONLY public.devolucion_detalle
    ADD CONSTRAINT devolucion_detalle_devolucion_id_fkey FOREIGN KEY (devolucion_id) REFERENCES public.devolucion(id);


ALTER TABLE ONLY public.devolucion_detalle
    ADD CONSTRAINT devolucion_detalle_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES public.producto(id);


ALTER TABLE ONLY public.devolucion
    ADD CONSTRAINT devolucion_pedido_id_fkey FOREIGN KEY (pedido_id) REFERENCES public.pedido(id);


ALTER TABLE ONLY public.devolucion
    ADD CONSTRAINT devolucion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.historial_evento
    ADD CONSTRAINT historial_evento_mascota_id_fkey FOREIGN KEY (mascota_id) REFERENCES public.mascota(id);


ALTER TABLE ONLY public.historial_evento
    ADD CONSTRAINT historial_evento_veterinario_id_fkey FOREIGN KEY (veterinario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.horario_atencion
    ADD CONSTRAINT horario_atencion_veterinario_id_fkey FOREIGN KEY (veterinario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.mascota
    ADD CONSTRAINT mascota_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.movimiento_stock
    ADD CONSTRAINT movimiento_stock_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES public.producto(id) ON DELETE CASCADE;


ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_cita_id_fkey FOREIGN KEY (cita_id) REFERENCES public.cita(id);


ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_id_registrado_por_fkey FOREIGN KEY (id_registrado_por) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_pedido_id_fkey FOREIGN KEY (pedido_id) REFERENCES public.pedido(id);


ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.pedido_detalle
    ADD CONSTRAINT pedido_detalle_pedido_id_fkey FOREIGN KEY (pedido_id) REFERENCES public.pedido(id);


ALTER TABLE ONLY public.pedido_detalle
    ADD CONSTRAINT pedido_detalle_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES public.producto(id);


ALTER TABLE ONLY public.pedido
    ADD CONSTRAINT pedido_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.producto_imagen
    ADD CONSTRAINT producto_imagen_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES public.producto(id) ON DELETE CASCADE;


ALTER TABLE ONLY public.receta
    ADD CONSTRAINT receta_mascota_id_fkey FOREIGN KEY (mascota_id) REFERENCES public.mascota(id);


ALTER TABLE ONLY public.receta
    ADD CONSTRAINT receta_veterinario_id_fkey FOREIGN KEY (veterinario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES public.rol(id);


ALTER TABLE ONLY public.vacuna
    ADD CONSTRAINT vacuna_mascota_id_fkey FOREIGN KEY (mascota_id) REFERENCES public.mascota(id);


ALTER TABLE ONLY public.veterinario_especialidad
    ADD CONSTRAINT veterinario_especialidad_especialidad_id_fkey FOREIGN KEY (especialidad_id) REFERENCES public.especialidad(id);


ALTER TABLE ONLY public.veterinario_especialidad
    ADD CONSTRAINT veterinario_especialidad_veterinario_id_fkey FOREIGN KEY (veterinario_id) REFERENCES public.usuario(id);


ALTER TABLE ONLY public.categoria_producto
    ADD CONSTRAINT uq_categoria_nombre_tipo UNIQUE (nombre, tipo);

INSERT INTO public.rol (id, nombre) VALUES
    (1, 'Admin'), (2, 'Cliente'), (3, 'Veterinario'), (4, 'Auxiliar de Veterinaria');

INSERT INTO public.categoria_producto (nombre, tipo, activo) VALUES
    ('Antipulgas', 'Farmacia', true),
    ('Antibióticos', 'Farmacia', true),
    ('Vitaminas y suplementos', 'Farmacia', true),
    ('Desparasitantes', 'Farmacia', true),
    ('Antiinflamatorios', 'Farmacia', true),
    ('Correas y collares', 'Accesorio', true),
    ('Juguetes', 'Accesorio', true),
    ('Camas y descanso', 'Accesorio', true),
    ('Aseo e higiene', 'Accesorio', true),
    ('Transportadoras', 'Accesorio', true),
    ('Comederos y bebederos', 'Accesorio', true);

INSERT INTO public.usuario (nombre, correo, contrasena, rol_id, activo)
VALUES ('Administrador Vamos Pet', 'admin@vamospet.test', 'Admin123!', 1, true);
