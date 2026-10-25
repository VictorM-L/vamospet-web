package com.vamospet.backend.controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        MovimientoStockController.class, PedidoDetalleController.class,
        DevolucionController.class, CompraController.class })
public class ManejadorErroresStock {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresStock.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> stockInsuficiente(IllegalStateException e) {
        log.warn("Movimiento de stock rechazado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
