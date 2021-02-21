package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

public class Configuracion {
    public Integer idconfiguracion;
    public String urlbase, url_ws;
    public boolean hasSSL;

    public static SQLiteDatabase sqLiteDatabase;
    public static final String TAG = "TAGCONFIGURACION";

    public Configuracion(){
        this.idconfiguracion = 0;
        this.urlbase = "";
        this.url_ws = "";
        this.hasSSL = false;
    }

    public boolean Save() {
        try {
            this.sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                            "configuracion(idconfiguracion, urlbase, ssl) "+
                            "values(?, ?, ?)",
                    new String[]{this.idconfiguracion==0?null: this.idconfiguracion.toString(),
                            this.urlbase, this.hasSSL?"1":"0"});
            if (this.idconfiguracion== 0)this.idconfiguracion= SQLite.sqlDB.getLastId();
            sqLiteDatabase.close();
            Log.d(TAG,"SAVE CONFIGURACION OK");
            return true;
        } catch (SQLException ex){
            Log.d(TAG,"Save(): " +ex.getMessage());
            return false;
        }
    }

    public static Configuracion GetLast(){
        Configuracion retorno = new Configuracion();
        try{
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM configuracion ORDER BY idconfiguracion DESC LIMIT 1", null);
            if (cursor.moveToFirst()) {
                retorno = AsignaDatos(cursor);
            }
            cursor.close();
            sqLiteDatabase.close();
        }catch (SQLiteException e){
            Log.d(TAG, "GetLast(): " + e.getMessage());
        }
        return retorno;
    }

    public static Configuracion AsignaDatos(Cursor cursor) {
        Configuracion Item = null;
        try {
            Item = new Configuracion();
            Item.idconfiguracion = cursor.getInt(0);
            Item.urlbase = cursor.getString(1);
            Item.hasSSL = (cursor.getInt(2)==1);
        } catch (SQLiteException ec) {
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
        }
        return Item;
    }
}
