package com.florencia.erpapp.models;

public class Categoria {
    public Integer categoriaid;
    public String nombrecategoria;
    public Boolean seleccionado;
    public Integer cantidad;

    public Categoria() {
        this.categoriaid = 0;
        this.nombrecategoria = "";
        this.seleccionado = false;
        this.cantidad = 0;
    }
}
