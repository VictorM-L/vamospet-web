SELECT 'producto.categoria_id' AS columna, p.id AS fila, p.categoria_id AS valor_huerfano, p.nombre AS detalle
FROM producto p
WHERE p.categoria_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM categoria_producto c WHERE c.id = p.categoria_id)
UNION ALL
SELECT 'movimiento_stock.usuario_id', m.id, m.usuario_id, m.motivo
FROM movimiento_stock m
WHERE m.usuario_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = m.usuario_id)
UNION ALL
SELECT 'notificacion.usuario_id', n.id, n.usuario_id, n.titulo
FROM notificacion n
WHERE n.usuario_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = n.usuario_id)
ORDER BY 1, 2;
