package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class DetalleIngreso {
    public Integer ingresoid, linea, tipodocumento, tipo;
    public Double monto;
    public String numerodocumentoreferencia, fechadocumento, tipodecuenta,
    niptitular, razonsocialtitular, fechadiario;
    public Catalogo entidadfinanciera;

    public static String TAG = "TAGDETALLE_INGRESO";
    public static SQLiteDatabase sqLiteDatabase;

    public DetalleIngreso(){
        ingresoid = 0;
        linea = 0;
        tipo = 0;
        monto = 0d;
        numerodocumentoreferencia = "";
        fechadocumento = "";
        entidadfinanciera = new Catalogo();
        tipodecuenta = "";
        niptitular = "";
        razonsocialtitular = "";
        fechadiario = "";
        tipodocumento = 0;
    }

    public static List<DetalleIngreso> getDetalle(Integer idingreso) {
        List<DetalleIngreso> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM detalleingreso WHERE ingresoid = ?", new String[]{idingreso.toString()});
            DetalleIngreso midetalle;
            if (cursor.moveToFirst()) {
                do {
                    midetalle = DetalleIngreso.AsignaDatos(cursor);
                    Items.add(midetalle);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        }catch (SQLiteException e){
            Log.d(TAG, "getDetalle(): " + e.getMessage());
        }
        return Items;
    }

    public static DetalleIngreso AsignaDatos(Cursor cursor) {
        DetalleIngreso Item;
        try {
            Item = new DetalleIngreso();
            Item.ingresoid = cursor.getInt(0);
            Item.linea = cursor.getInt(1);
            Item.monto = cursor.getDouble(2);
            Item.numerodocumentoreferencia = cursor.getString(3);
            Item.fechadocumento = cursor.getString(4);
            Item.entidadfinanciera = Catalogo.getByPadre(cursor.getString(5), "ENTIDADFINANCIE");
            Item.tipodecuenta = cursor.getString(6);
            Item.niptitular = cursor.getString(7);
            Item.razonsocialtitular = cursor.getString(8);
            Item.tipodocumento = cursor.getInt(9);
            Item.fechadiario = cursor.getString(10);
            Item.tipo = cursor.getInt(11);
            if(Item.entidadfinanciera == null)
                Item.entidadfinanciera = new Catalogo();
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
            Item = null;
        }
        return Item;
    }
}
