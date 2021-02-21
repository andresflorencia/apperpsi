package com.florencia.erpapp.models;


public class TipoIdentificacion {
    private String codigo;
    private String nombre;

    public TipoIdentificacion(String codigo, String nombre){
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TipoIdentificacion){
            TipoIdentificacion c = (TipoIdentificacion) obj;
            return c.getNombre().equals(nombre) && c.getCodigo().equals(codigo);
        }
        return false;
    }
}
