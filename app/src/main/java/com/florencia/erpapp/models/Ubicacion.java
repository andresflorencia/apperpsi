package com.florencia.erpapp.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;

import java.util.ArrayList;

public class Ubicacion {
    public Integer idubicacion, usuarioid, estado;
    public Double lat, lon;
    public String fechaapp, rucempresa;

    public static SQLiteDatabase sqLiteDatabase;
    public static String TAG = "TAGUBICACION";

    public Ubicacion() {
        this.idubicacion = 0;
        this.usuarioid = 0;
        this.estado = 0;
        this.lat = 0d;
        this.lon = 0d;
        this.fechaapp = "";
        this.rucempresa = "";
    }

    public void Save() {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                            "ubicacion(usuarioid, lat, lon, estado, fechaapp, rucempresa) " +
                            "values(?, ?, ?, ?, ?, ?)",
                    new String[]{usuarioid.toString(), lat.toString(), lon.toString(), "0",
                            Utils.getDateFormat("yyyy-MM-dd HH:mm"), rucempresa});
            sqLiteDatabase.close();
            Log.d("TAG", " User: " + usuarioid + ". Se agregó la posicion -> Lat: "
                    + lat + " - Lon: " + lon + " RUC: " + rucempresa);
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, ec.getMessage());
        }
    }

    public static void Save(Integer usuarioid, String rucempresa, Double lat, Double lon) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                            "ubicacion(usuarioid, lat, lon, estado, fechaapp, rucempresa) " +
                            "values(?, ?, ?, ?, ?, ?)",
                    new String[]{usuarioid.toString(), lat.toString(), lon.toString(), "0",
                            Utils.getDateFormat("yyyy-MM-dd HH:mm"), rucempresa});
            sqLiteDatabase.close();
            Log.d("TAG", " User: " + usuarioid + ". Se agregó la posicion -> Lat: "
                    + lat + " - Lon: " + lon + " RUC: " + rucempresa);
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, ec.getMessage());
        }
    }

    public static ArrayList<Ubicacion> getListSC(Integer usuarioid) {
        ArrayList<Ubicacion> items = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM ubicacion WHERE usuarioid = ? AND estado = 0 ORDER BY idubicacion", new String[]{usuarioid.toString()});
            if (cursor.moveToFirst()) {
                items = new ArrayList<>();
                do {
                    Ubicacion item = new Ubicacion();
                    item.idubicacion = cursor.getInt(0);
                    item.usuarioid = cursor.getInt(1);
                    item.lat = cursor.getDouble(2);
                    item.lon = cursor.getDouble(3);
                    item.estado = cursor.getInt(4);
                    item.fechaapp = cursor.getString(5);
                    item.rucempresa = cursor.getString(6);
                    items.add(item);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, ec.getMessage() + " User: " + usuarioid);
        }
        return items;
    }

    public static boolean Update(Integer idubicacion, ContentValues values) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.update("ubicacion", values, "idubicacion = ?", new String[]{idubicacion.toString()});
            sqLiteDatabase.close();
            Log.d(TAG, "UPDATE UBICACION OK");
            return true;
        } catch (SQLException ex) {
            Log.d(TAG, "Update(): " + ex.getMessage());
            return false;
        }
    }
}
