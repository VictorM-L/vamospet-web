package com.vamospet.backend.permisos;

public enum Rol {
    ADMIN(1, "Administrador"),
    CLIENTE(2, "Cliente"),
    VETERINARIO(3, "Veterinario"),
    AUXILIAR(4, "Auxiliar de Veterinaria");

    private final int id;
    private final String nombre;

    Rol(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }

    public static Rol deId(Integer id) {
        if (id == null) return null;
        for (Rol r : values()) {
            if (r.id == id) return r;
        }
        return null;
    }
}
