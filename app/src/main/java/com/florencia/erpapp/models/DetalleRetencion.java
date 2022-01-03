package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class DetalleRetencion {
    public Integer retencionid, linea;
    public String codigo, codigoretencion, coddocsustento, numdocsustento, fechaemisiondocsustento, tipo;
    public Double baseimponible, baseimponibleiva, porcentajeretener, porcentajereteneriva,
            valorretenido, valorretenidoiva;

    public static String TAG = "TAGDETALLE_RETENCION";
    public static SQLiteDatabase sqLiteDatabase;

    public DetalleRetencion (){
        this.retencionid = this.linea = 0;
        this.codigo = this.codigoretencion = this.coddocsustento = this.numdocsustento
                = this.fechaemisiondocsustento = this.tipo = "";
        this.baseimponible = this.baseimponibleiva = this.porcentajeretener = this.porcentajereteneriva
                = this.valorretenido = this.valorretenidoiva = 0d;
    }

    public static List<DetalleRetencion> getDetalle(Integer idretencion) {
        List<DetalleRetencion> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM detalleretencion WHERE retencionid = ?", new String[]{idretencion.toString()});
            DetalleRetencion midetalle;
            if (cursor.moveToFirst()) {
                do {
                    midetalle = DetalleRetencion.AsignaDatos(cursor);
                    Items.add(midetalle);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        } catch (SQLiteException e) {
            Log.d(TAG, "getDetalle(): " + e.getMessage());
        }
        return Items;
    }

    public static DetalleRetencion AsignaDatos(Cursor cursor) {
        DetalleRetencion Item;
        try {
            Item = new DetalleRetencion();
            Item.retencionid = cursor.getInt(0);
            Item.linea = cursor.getInt(1);
            Item.codigo = cursor.getString(2);
            Item.codigoretencion = cursor.getString(3);
            Item.coddocsustento = cursor.getString(4);
            Item.numdocsustento = cursor.getString(5);
            Item.fechaemisiondocsustento = cursor.getString(6);
            Item.baseimponible = cursor.getDouble(7);
            Item.baseimponibleiva = cursor.getDouble(8);
            Item.porcentajeretener = cursor.getDouble(9);
            Item.porcentajereteneriva = cursor.getDouble(10);
            Item.valorretenido = cursor.getDouble(11);
            Item.valorretenidoiva = cursor.getDouble(12);
            Item.tipo = cursor.getString(13);
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
            Item = null;
        }
        return Item;
    }
}
