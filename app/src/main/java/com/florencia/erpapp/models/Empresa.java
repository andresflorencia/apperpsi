package com.florencia.erpapp.models;

public class Empresa {
    public String ruc, razonsocial, hostname, alias;

    public Empresa(){
        this.ruc = "";
        this.razonsocial = "";
        this.hostname = "";
        this.alias = "";
    }

    @Override
    public String toString(){
        return alias;
    }
}