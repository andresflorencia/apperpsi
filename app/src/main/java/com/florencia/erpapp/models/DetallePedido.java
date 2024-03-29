package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class DetallePedido {
    public Integer pedidoid, orden, usuarioid;
    public Double cantidad, factorconversion, precio, porcentajeiva, descuento, porcentajedesc;
    public String observacion, codigoproducto, nombreproducto;
    public Producto producto;

    public static SQLiteDatabase sqLiteDatabase;
    public static String TAG = "TAGDETALLEPEDIDO";

    public DetallePedido() {
        this.pedidoid = 0;
        this.orden = 0;
        this.usuarioid = 0;
        this.cantidad = 0d;
        this.factorconversion = 0d;
        this.precio = 0d;
        this.observacion = "";
        this.producto = new Producto();
        this.descuento = 0d;
        this.porcentajedesc = 0d;
    }

    public Double Subtotal() {
        Double retorno = 0d;
        try {
            retorno = this.cantidad * this.precio;
        } catch (Exception e) {
            Log.d(TAG, "Subtotal(): " + e.getMessage());
        }
        return retorno;
    }

    public Double Subtotaliva() {
        Double retorno = 0d;
        try {
            //retorno = this.cantidad * (this.precio + (this.precio * this.producto.porcentajeiva / 100));
            retorno = Utils.RoundDecimal (((this.cantidad * this.precio) - this.Descuento(this.producto.descuento)) * (1 + (this.producto.porcentajeiva /100)),2);
        } catch (Exception e) {
            Log.d(TAG, "Subtotaliva(): " + e.getMessage());
        }
        return retorno;
    }

    public Double Descuento(Double percent){
        Double retorno = 0d;
        try{
            retorno = Utils.RoundDecimal(this.cantidad * (this.precio * percent/100), 2);
        }catch (Exception e){
            Log.d(TAG, "Descuento(): " + e.getMessage());
        }
        Log.d(TAG, "Descuento: " + retorno);
        this.descuento = retorno;
        return retorno;
    }

    public static List<DetallePedido> getDetalle(Integer idPedido) {
        List<DetallePedido> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM detallepedido WHERE pedidoid = ?", new String[]{idPedido.toString()});
            DetallePedido midetalle;
            if (cursor.moveToFirst()) {
                do {
                    midetalle = DetallePedido.AsignaDatos(cursor);
                    Items.add(midetalle);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        } catch (SQLiteException e) {
            Log.d(TAG, "getDetalle(): " + e.getMessage());
        }
        return Items;
    }

    public static DetallePedido AsignaDatos(Cursor cursor) {
        DetallePedido Item;
        try {
            Item = new DetallePedido();
            Item.pedidoid = cursor.getInt(0);
            Item.orden = cursor.getInt(1);
            Item.cantidad = cursor.getDouble(2);
            Item.factorconversion = cursor.getDouble(3);
            Item.precio = cursor.getDouble(4);
            Item.producto = Producto.get(cursor.getInt(5), SQLite.usuario.sucursal.IdEstablecimiento);
            Item.observacion = cursor.getString(6);
            Item.usuarioid = cursor.getInt(7);
            Item.porcentajeiva = cursor.getDouble(8);
            Item.codigoproducto = cursor.getString(9);
            Item.nombreproducto = cursor.getString(10);
            Item.descuento = cursor.getDouble(11);
            Item.porcentajedesc = cursor.getDouble(12);
            if (Item.producto == null) {
                Item.producto = new Producto();
                Item.producto.idproducto = cursor.getInt(5);
                Item.producto.codigoproducto = Item.codigoproducto;
                Item.producto.nombreproducto = Item.nombreproducto;
                Item.producto.porcentajeiva = Item.porcentajeiva;
            }
            Item.producto.descuento = Item.porcentajedesc;
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
            Item = null;
        }
        return Item;
    }

    public Double getPrecio(String categoria) {
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
        } catch (Exception e) {
            Log.d(TAG, "getPrecio(): " + e.getMessage());
        }
        return this.precio;
    }

    public Double getPrecio(List<Regla> reglas, List<PrecioCategoria> categorias, String categcliente, Double cantidad, Double precio_act) {
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
            for (PrecioCategoria cat : categorias) {
                if (cat.categoriaid == Integer.valueOf(categcliente)) {
                    precio_act = cat.valor;
                    categ_temp = cat;
                    break;
                }
            }

            //SI PRECIO DE CATEGORIA ES MENOR AL PRECIO DE ALGUNA REGLAPRECIO, CONSERVAMOS EL DE LA CATEGORIA
            if (this.precio > precio_act)
                this.precio = ptemp;

            if (categ_temp != null) {
                if (precioregla == 0 || categ_temp.prioridad.equalsIgnoreCase("t"))
                    this.precio = categ_temp.valor;
            }
        } catch (Exception e) {
            Log.d(TAG, "getPrecio2(): " + e.getMessage());
        }
        return this.precio;
    }
}
