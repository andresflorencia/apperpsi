package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class DetallePedidoInv {
    public Integer pedidoid, orden, usuarioid;
    public Double cantidadpedida, stockactual, cantidadautorizada;
    public String codigoproducto, nombreproducto;
    public Producto producto;

    public static String TAG = "TAGDETALLE_PEDIDOINV";
    public static SQLiteDatabase sqLiteDatabase;

    public DetallePedidoInv() {
        this.pedidoid = 0;
        this.orden = 0;
        this.usuarioid = 0;
        this.cantidadpedida = 0d;
        this.cantidadautorizada = 0d;
        this.stockactual = 0d;
        this.codigoproducto = "";
        this.nombreproducto = "";
        this.producto = new Producto();
    }

    public static List<DetallePedidoInv> getDetalle(Integer idPedido) {
        List<DetallePedidoInv> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM detallepedidoinv WHERE pedidoid = ?", new String[]{idPedido.toString()});
            DetallePedidoInv midetalle;
            if (cursor.moveToFirst()) {
                do {
                    midetalle = DetallePedidoInv.AsignaDatos(cursor);
                    Items.add(midetalle);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        } catch (SQLiteException e) {
            Log.d(TAG, "getDetalle(): " + e.getMessage());
        }
        return Items;
    }

    public static DetallePedidoInv AsignaDatos(Cursor cursor) {
        DetallePedidoInv Item;
        try {
            Item = new DetallePedidoInv();
            Item.pedidoid = cursor.getInt(0);
            Item.orden = cursor.getInt(1);
            Item.producto = Producto.get(cursor.getInt(2), SQLite.usuario.sucursal.IdEstablecimiento);
            Item.cantidadpedida = cursor.getDouble(3);
            Item.cantidadautorizada = cursor.getDouble(4);
            Item.stockactual = cursor.getDouble(5);
            Item.usuarioid = cursor.getInt(6);
            Item.codigoproducto = cursor.getString(7);
            Item.nombreproducto = cursor.getString(8);
            if (Item.producto == null) {
                Item.producto = new Producto();
                Item.producto.idproducto = cursor.getInt(2);
                Item.producto.codigoproducto = Item.codigoproducto;
                Item.producto.nombreproducto = Item.nombreproducto;
            }
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
            Item = null;
        }
        return Item;
    }
}
