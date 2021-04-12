package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class DetalleComprobante {
    public Integer comprobanteid, linea;
    public Producto producto;
    public Double cantidad, precio, total;
    public String numerolote, fechavencimiento, codigoproducto, nombreproducto;
    public Double stock, preciocosto, precioreferencia, valoriva, valorice, descuento, marquetas;

    public static SQLiteDatabase sqLiteDatabase;

    public DetalleComprobante(){
        this.comprobanteid =0;
        this.linea =0;
        this.producto = new Producto();
        this.cantidad = 0d;
        this.total = 0d;
        this.precio = 0d;
        this.numerolote = "";
        this.fechavencimiento = "";
        this.stock = 0d;
        this.preciocosto = 0d;
        this.precioreferencia = 0d;
        this.valoriva = 0d;
        this.valorice = 0d;
        this.descuento = 0d;
        this.codigoproducto = "";
        this.nombreproducto = "";
        this.marquetas = 0d;
    }

    public Double Subtotal(){
        Double retorno= 0d;
        try{
            retorno =this.cantidad * this.precio;
        }catch (Exception e){
            Log.d("TAGPRODUCTO", e.getMessage());
        }
        return retorno;
    }

    public Double Subtotalcosto(){
        Double retorno= 0d;
        try{
            retorno =this.cantidad * this.preciocosto;
        }catch (Exception e){
            Log.d("TAGPRODUCTO", e.getMessage());
        }
        return retorno;
    }

    public Double Subtotaliva(){
        Double retorno= 0d;
        try{
            retorno =this.cantidad * (this.precio +(this.precio * this.producto.porcentajeiva/100));
        }catch (Exception e){
            Log.d("TAGPRODUCTO", e.getMessage());
        }
        return retorno;
    }

    public static List<DetalleComprobante> getDetalle(Integer idComprobante) {
        List<DetalleComprobante> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM detallecomprobante WHERE comprobanteid = ?", new String[]{idComprobante.toString()});
            DetalleComprobante midetalle;
            if (cursor.moveToFirst()) {
                do {
                    midetalle = DetalleComprobante.AsignaDatos(cursor);
                    Items.add(midetalle);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        }catch (SQLiteException e){
            Log.d("TAGCOMPROBANTE", "getDetalle(): " + e.getMessage());
        }
        return Items;
    }

    public static DetalleComprobante AsignaDatos(Cursor cursor) {
        DetalleComprobante Item;
        try {
            Item = new DetalleComprobante();
            Item.comprobanteid = cursor.getInt(0);
            Item.linea = cursor.getInt(1);
            Item.producto = Producto.get(cursor.getInt(2), SQLite.usuario.sucursal.IdEstablecimiento);
            Item.cantidad = cursor.getDouble(3);
            Item.total = cursor.getDouble(4);
            Item.precio = cursor.getDouble(5);
            Item.numerolote = cursor.getString(6);
            Item.fechavencimiento = cursor.getString(7);
            Item.stock = cursor.getDouble(8);
            Item.preciocosto = cursor.getDouble(9);
            Item.precioreferencia = cursor.getDouble(10);
            Item.valoriva = cursor.getDouble(11);
            Item.valorice = cursor.getDouble(12);
            Item.descuento = cursor.getDouble(13);
            Item.codigoproducto = cursor.getString(14);
            Item.nombreproducto = cursor.getString(15);
            Item.marquetas = cursor.getDouble(16);
            if(Item.producto == null){
                Item.producto = new Producto();
                Item.producto.codigoproducto = Item.codigoproducto;
                Item.producto.nombreproducto = Item.nombreproducto;
                Item.producto.iva = Item.valoriva>0?1:0;
            }
        } catch (Exception ec) {
            Log.d("TAGCOMPROBANTE", "AsignaDatos(): " + ec.getMessage());
            ec.printStackTrace();
            Item = null;
        }
        return Item;
    }

    public Double getPrecio(String categoria){
        try {
            this.precio = this.producto.getPrecio(categoria);
            Double ptemp = this.precio;//GUARDAMOS EL PRECIO DE LA CATEGORIA
            if (this.producto.reglas.size() > 0) {
                for (Regla r : this.producto.reglas) {
                    if (this.cantidad >= r.cantidad) {
                        this.precio = r.precio;
                        break;
                    }
                }
            }
            //SI PRECIO DE CATEGORIA ES MENOR AL PRECIO DE ALGUNA REGLAPRECIO, CONSERVAMOS EL DE LA CATEGORIA
            if (ptemp < this.precio)
                this.precio = ptemp;
        }catch (Exception e){
            Log.d("TAGDETALLECOMPROBANTE", "getPrecio(): " + e.getMessage());
        }
        return this.precio;
    }

    public Double getPrecio(List<Regla> reglas, List<PrecioCategoria> categorias,
                            String categcliente, Double cantidad, Double precio_act, boolean credito){
        try {
            this.precio = precio_act;
            Double precioregla = 0d;
            Double ptemp = this.precio;//GUARDAMOS EL PRECIO DE LA CATEGORIA
            if (reglas.size() > 0) {
                for (Regla r : reglas) {
                    if (cantidad >= r.cantidad) {
                        this.precio = r.precio;
                        precioregla = r.precio;
                        break;
                    }
                }
            }
            PrecioCategoria categ_temp = null;
            Double pvpNormal = 0d;
            for (PrecioCategoria cat: categorias) {
                if(cat.categoriaid == 0)
                    pvpNormal = cat.valor;
                if(cat.categoriaid == Integer.valueOf(categcliente)){
                    precio_act = cat.valor;
                    categ_temp = cat;
                }
            }

            //SI PRECIO DE CATEGORIA ES MENOR AL PRECIO DE ALGUNA REGLAPRECIO, CONSERVAMOS EL DE LA CATEGORIA
            if (this.precio > precio_act)
                this.precio = precio_act;

            if(categ_temp != null){
                if(precioregla == 0 || categ_temp.prioridad.equalsIgnoreCase("t")
                        || (categ_temp.aplicacredito.equalsIgnoreCase("t") && credito))
                    this.precio = categ_temp.valor;
                if(categ_temp.aplicacredito.equalsIgnoreCase("f") && credito)
                    this.precio = pvpNormal>precioregla && precioregla>0 ? precioregla : pvpNormal;
            }
        }catch (Exception e){
            Log.d("TAGDETALLECOMPROBANTE", "getPrecio2(): " + e.getMessage());
        }
        return this.precio;
    }
}
