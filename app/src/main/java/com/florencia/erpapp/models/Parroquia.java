package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class Parroquia {
    public Integer idparroquia;
    public String nombreparroquia;
    public Integer cantonid;

    public static SQLiteDatabase sqLiteDatabase;
    public static final String TAG = "TAGPROVINCIA";

    public Parroquia() {
        this.idparroquia = 0;
        this.nombreparroquia = "";
        this.cantonid = 0;
    }

    public static Parroquia get(Integer codigo) {
        Parroquia parroquia = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM parroquia where idparroquia= ?", new String[]{codigo.toString()});
            if (cursor.moveToFirst())
                parroquia = Parroquia.AsignaDatos(cursor);
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ec) {
            Log.d(TAG, "get()" + ec.getMessage());
            ec.printStackTrace();
        }
        return parroquia;
    }

    public static List<Parroquia> getList(Integer idcanton) {
        List<Parroquia> lista = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery(
                    "SELECT DISTINCT * FROM parroquia WHERE cantonid = ? " +
                            "UNION " +
                            "SELECT * FROM parroquia WHERE idparroquia = 0 " +
                            "ORDER BY nombreparroquia", new String[]{idcanton.toString()});
            Parroquia parroquia;
            if (cursor.moveToFirst()) {
                do {
                    parroquia = Parroquia.AsignaDatos(cursor);
                    if (parroquia != null) lista.add(parroquia);
                } while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ec) {
            Log.d(TAG, "getCatalogo" + ec.getMessage());
            ec.printStackTrace();
        }
        return lista;
    }

    public static boolean SaveLista(List<Parroquia> parroquias) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            for (Parroquia item : parroquias) {
                sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                                "parroquia(idparroquia, nombreparroquia, cantonid)" +
                                "values(?, ?, ?)",
                        new String[]{item.idparroquia.toString(), item.nombreparroquia, item.cantonid.toString()});
            }
            sqLiteDatabase.close();
            Log.d(TAG, "Guardó lista parroquias");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "SaveLista(): " + ex.getMessage());
            return false;
        }
    }

    public static Parroquia AsignaDatos(Cursor cursor) {
        Parroquia Item = null;
        try {
            Item = new Parroquia();
            Item.idparroquia = cursor.getInt(0);
            Item.nombreparroquia = cursor.getString(1);
            Item.cantonid = cursor.getInt(2);
        } catch (SQLiteException ec) {
            Log.d(TAG, ec.getMessage());
        }
        return Item;
    }

    @Override
    public String toString() {
        return nombreparroquia;
    }
}

