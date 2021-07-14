package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Foto {
    public Integer idfoto, personaid, documentoid;
    public String name, path, image_base, tipo;
    public Uri uriFoto;
    public Bitmap bitmap;
    public File file;

    public static SQLiteDatabase sqLiteDatabase;
    public static final String TAG = "TAG_FOTO";

    public Foto() {
        this.idfoto = 0;
        this.personaid = 0;
        this.documentoid = 0;
        this.name = "";
        this.path = "";
        this.tipo = "";
        this.image_base = "";
    }

    public static boolean removeFotos(Integer idpersona) {
        try {
            String[] params = new String[]{idpersona.toString()};
            String where = "WHERE personaid = ?";
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("DELETE FROM foto " + where, params);
            sqLiteDatabase.close();
            Log.d(TAG, "fotos ELIMINADAS personaid: " + idpersona);
            return true;
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "removeFotos(): " + ec.getMessage());
        }
        return false;
    }

    public static boolean removeFotos(Integer idpersona, Integer iddocumento, String tipo) {
        try {
            String[] params = new String[]{idpersona.toString(), iddocumento.toString(), tipo};
            String where = "WHERE personaid = ? and documentoid = ? and tipo = ?";
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("DELETE FROM foto " + where, params);
            sqLiteDatabase.close();
            Log.d(TAG, "fotos ELIMINADAS personaid: " + idpersona + " - documentoid: " + iddocumento + " - tipo: " + tipo);
            return true;
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "removeFotos(): " + ec.getMessage());
        }
        return false;
    }

    public static boolean SaveList(Integer idpersona, Integer iddocumento, List<Foto> fotos, String tipo) {
        try {
            removeFotos(idpersona, iddocumento, tipo);
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            for (Foto foto : fotos) {
                foto.personaid = idpersona;
                foto.documentoid = iddocumento;
                sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                                "foto(idfoto, personaid, documentoid, name, path, tipo) " +
                                "values(?, ?, ?, ?, ?, ?)",
                        new String[]{foto.idfoto == 0 ? null : foto.idfoto.toString(), foto.personaid.toString(), foto.documentoid.toString(),
                                foto.name, foto.path, foto.tipo});
                if (foto.idfoto == 0) foto.idfoto = SQLite.sqlDB.getLastId();
            }
            sqLiteDatabase.close();
            Log.d(TAG, "SAVE LISTA DE FOTOS OK");
            return true;
        } catch (SQLException ex) {
            Log.d(TAG, "SaveList(): " + ex.getMessage());
            return false;
        }
    }

    public static List<Foto> getLista(Integer idpersona, Integer iddocumento, String tipo) {
        List<Foto> retorno = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM foto WHERE personaid = ? AND documentoid = ? and tipo = ?",
                    new String[]{idpersona.toString(), iddocumento.toString(), tipo});
            Foto foto;
            if (cursor.moveToFirst()) {
                do {
                    foto = Foto.AsignaDatos(cursor);
                    if (foto != null) retorno.add(foto);
                } while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (SQLiteException e) {
            Log.d(TAG, "getLista(): " + e.getMessage());
        }
        return retorno;
    }

    public static Foto AsignaDatos(Cursor cursor) {
        Foto Item = null;
        try {
            Item = new Foto();
            Item.idfoto = cursor.getInt(0);
            Item.personaid = cursor.getInt(1);
            Item.documentoid = cursor.getInt(2);
            Item.name = cursor.getString(3);
            Item.path = cursor.getString(4);
            Item.tipo = cursor.getString(5);
        } catch (SQLiteException ec) {
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
        }
        return Item;
    }
}
