package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class Permiso
{
    public String nombreopcion;
    public Integer perfilid;
    public Integer opcionid;
    public String permisoescritura;
    public String permisoimpresion;
    public String permisomodificacion;
    public String permisoborrar;
    public String permisosubirarchivo;
    public String rutaopcion;

    public static SQLiteDatabase sqLiteDatabase;
    public static final String TAG = "TAGPERMISO";
    
    public Permiso(){
        this.nombreopcion = "";
        this.perfilid = 0;
        this.opcionid = 0;
        this.permisoescritura = "f";
        this.permisoimpresion = "f";
        this.permisomodificacion = "f";
        this.permisoborrar = "f";
        this.permisosubirarchivo = "f";
        this.rutaopcion = "";
    }

    public static List<Permiso> getPermisos(Integer idPerfil) {
        List<Permiso> lista = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM permiso where perfilid = ? ORDER BY nombreopcion", new String[]{idPerfil.toString()});
            Permiso permiso = new Permiso();
            if (cursor.moveToFirst()) {
                do {
                    permiso = Permiso.AsignaDatos(cursor);
                    if (permiso != null) lista.add(permiso);
                }while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ec) {
            Log.d(TAG,ec.getMessage());
            ec.printStackTrace();
        }
        return lista;
    }

    public static boolean SaveLista(List<Permiso> permisos) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            for (Permiso item:permisos) {
                sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                                "permiso(nombreopcion, perfilid, opcionid, permisoescritura, permisoimpresion, permisomodificacion, " +
                                "permisoborrar, permisosubirarchivo, rutaopcion)" +
                                "values(?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{item.nombreopcion, item.perfilid.toString(), item.opcionid.toString(),
                                item.permisoescritura,item.permisoimpresion, item.permisomodificacion, item.permisoborrar, item.permisosubirarchivo, item.rutaopcion});
            }
            sqLiteDatabase.close();
            Log.d(TAG,"Guardó lista permisos");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "SaveLista(): " + ex.getMessage());
            return false;
        }
    }

    public static Permiso AsignaDatos(Cursor cursor) {
        Permiso Item = null;
        try {
            Item = new Permiso();
            Item.nombreopcion = cursor.getString(0);
            Item.perfilid = cursor.getInt(1);
            Item.opcionid = cursor.getInt(2);
            Item.permisoescritura = cursor.getString(3);
            Item.permisoimpresion = cursor.getString(4);
            Item.permisomodificacion = cursor.getString(5);
            Item.permisoborrar = cursor.getString(6);
            Item.permisosubirarchivo = cursor.getString(7);
            Item.rutaopcion = cursor.getString(8);
        } catch (SQLiteException ec) {
            Log.d(TAG, ec.getMessage());
        }
        return Item;
    }
}
