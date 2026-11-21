BEGIN;

DO $$
DECLARE
    huerfanos INTEGER;
BEGIN
    SELECT (SELECT count(*) FROM producto p WHERE p.categoria_id IS NOT NULL
                AND NOT EXISTS (SELECT 1 FROM categoria_producto c WHERE c.id = p.categoria_id))
         + (SELECT count(*) FROM movimiento_stock m WHERE m.usuario_id IS NOT NULL
                AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = m.usuario_id))
         + (SELECT count(*) FROM notificacion n WHERE n.usuario_id IS NOT NULL
                AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = n.usuario_id))
      INTO huerfanos;
    IF huerfanos > 0 THEN
        RAISE EXCEPTION 'Hay % fila(s) huérfanas. Ejecuta db/14_verificar_huerfanos.sql, corrígelas y vuelve a ejecutar este script.', huerfanos;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'producto_categoria_id_fkey') THEN
        ALTER TABLE producto ADD CONSTRAINT producto_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES categoria_producto(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'movimiento_stock_usuario_id_fkey') THEN
        ALTER TABLE movimiento_stock ADD CONSTRAINT movimiento_stock_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuario(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'notificacion_usuario_id_fkey') THEN
        ALTER TABLE notificacion ADD CONSTRAINT notificacion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuario(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_producto_categoria ON producto (categoria_id);
CREATE INDEX IF NOT EXISTS ix_movimiento_stock_usuario ON movimiento_stock (usuario_id);
CREATE INDEX IF NOT EXISTS ix_notificacion_usuario ON notificacion (usuario_id);

ALTER TABLE receta ADD COLUMN IF NOT EXISTS aprobada_en TIMESTAMP;
ALTER TABLE receta ADD COLUMN IF NOT EXISTS usada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE receta ADD COLUMN IF NOT EXISTS usada_en TIMESTAMP;
ALTER TABLE receta ADD COLUMN IF NOT EXISTS pedido_id INTEGER REFERENCES pedido(id);
UPDATE receta SET aprobada_en = fecha WHERE estado = 'Aprobada' AND aprobada_en IS NULL;
ALTER TABLE receta DROP CONSTRAINT IF EXISTS receta_uso_check;
ALTER TABLE receta ADD CONSTRAINT receta_uso_check CHECK ((usada AND usada_en IS NOT NULL) OR (NOT usada AND usada_en IS NULL));
CREATE INDEX IF NOT EXISTS ix_receta_mascota_vigente ON receta (mascota_id, estado, usada);

ALTER TABLE pedido_detalle ADD COLUMN IF NOT EXISTS receta_id INTEGER REFERENCES receta(id);
CREATE INDEX IF NOT EXISTS ix_pedido_detalle_receta ON pedido_detalle (receta_id);

COMMIT;
