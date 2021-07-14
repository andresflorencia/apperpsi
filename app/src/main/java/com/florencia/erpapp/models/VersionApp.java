package com.florencia.erpapp.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.BuildConfig;
import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class VersionApp {
    public Integer idversion;
    public String version, newversion, link, requerida, instalada;

    public static SQLiteDatabase sqLiteDatabase;
    public static final String TAG = "TAGVERSION";

    public VersionApp(){
        this.idversion = 0;
        this.version = BuildConfig.VERSION_NAME;
        this.newversion = "";
        this.link = "";
        this.requerida = "f";
        this.instalada = "f";
    }

    public static VersionApp getLast() {
        VersionApp version = null;
        try {

            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM version order by idversion desc limit 1", null);
            if (cursor.moveToFirst())
                version = VersionApp.AsignaDatos(cursor);
            cursor.close();
            sqLiteDatabase.close();

        } catch (Exception ec) {
            Log.d(TAG, "getLast()" + ec.getMessage());
            ec.printStackTrace();
        }

        if(version == null){
            version = new VersionApp();
            version.version = BuildConfig.VERSION_NAME;
            version.newversion = BuildConfig.VERSION_NAME;
            version.requerida = "f";
            version.instalada = "t";
        }
        return version;
    }

    public static boolean Save(VersionApp version ) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                                "version(idversion, version, link, requerida, instalada)" +
                                "values(?, ?, ?, ?, ?)",
                        new String[]{version.idversion.toString(), version.newversion, version.link, version.requerida, version.instalada});

            sqLiteDatabase.close();
            Log.d(TAG, "Guardó version");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "SaveLista(): " + ex.getMessage());
            return false;
        }
    }

    public static VersionApp AsignaDatos(Cursor cursor) {
        VersionApp Item = null;
        try {
            Item = new VersionApp();
            Item.idversion = cursor.getInt(0);
            Item.version = BuildConfig.VERSION_NAME;
            Item.newversion = cursor.getString(1);
            Item.link = cursor.getString(2);
            Item.requerida = cursor.getString(3);
            Item.instalada = cursor.getString(4);
            if(Item.version.equals(Item.newversion))
                Item.instalada = "f";
        } catch (SQLiteException ec) {
            Log.d(TAG, ec.getMessage());
        }
        return Item;
    }

    public static boolean Remove(Integer idversion, String version) {
        try {
            List<String> params = new ArrayList<>();
            String WHERE = "WHERE ";
            if(idversion != 0) {
                WHERE += "idversion = ?";
                params.add(idversion.toString());
            }else if(!version.equals("")){
                WHERE += "version = ?";
                params.add(version);
            }
            String[] arrayP = new String[params.size()];
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("DELETE FROM version " + WHERE, params.toArray(arrayP));
            sqLiteDatabase.close();
            return true;
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "removeFotos(): " + ec.getMessage());
        }
        return false;
    }

    public static boolean Update(String version, ContentValues values) {
        try{
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.update("version", values, "version = ?", new String[]{version});
            sqLiteDatabase.close();
            Log.d(TAG, "UPDATE VERSION "+ version +" OK");
            return true;
        }catch (Exception e){}
        return false;
    }
}
