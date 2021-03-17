package com.florencia.erpapp.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Regla {
    public Integer idproductoregla, productoid, establecimientoid;
    public Double cantidad, precio;
    public String numerolote, fechamaxima;
    public Long longdate;

    public static SQLiteDatabase sqLiteDatabase;

    public Regla(){
        this.idproductoregla = 0;
        this.productoid = 0;
        this.establecimientoid = 0;
        this.cantidad = 0d;
        this.precio = 0d;
        this.numerolote = "";
        this.fechamaxima = "";
        this.longdate = 0l;
    }

    public boolean Save() {
        try {
            this.sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            this.sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                    "reglaprecio(idproductoregla, productoid, establecimientoid, cantidad, numerolote, fechamaxima, precio, longdate) " +
                    "values(?, ?, ?, ?, ?, ?, ?, ?)", new String[]{this.idproductoregla.toString(), this.productoid.toString(), this.establecimientoid.toString(),
                    this.cantidad.toString(),this.numerolote, this.fechamaxima, this.precio.toString(), String.valueOf(Utils.longDate(this.fechamaxima))});
            this.sqLiteDatabase.close();
            Log.d("TAGREGLA","SAVE REGLA OK");
            return true;
        } catch (SQLException ex){
            Log.d("TAGREGLA", ex.getMessage());
            return false;
        }
    }

    public static boolean InsertMultiple(Integer idproducto, Integer establecimientoid, List<Regla> reglas){
        try {
            ContentValues values;
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.delete("reglaprecio","productoid = ? and establecimientoid in (?, ?)", new String[]{idproducto.toString(), establecimientoid.toString(), "0"});
            for(Regla regla: reglas) {
                regla.longdate = Utils.longDate(regla.fechamaxima);
                values= new ContentValues();
                values.put("idproductoregla", regla.idproductoregla);
                values.put("productoid", regla.productoid);
                values.put("establecimientoid", regla.establecimientoid);
                values.put("cantidad", regla.cantidad);
                values.put("numerolote", regla.numerolote);
                values.put("fechamaxima", regla.fechamaxima);
                values.put("precio", regla.precio);
                values.put("longdate", regla.longdate);
                sqLiteDatabase.insert("reglaprecio", "", values);
            }
            sqLiteDatabase.close();
            Log.d("TAGREGLA", "INSERT REGLAS OK");
            return true;
        }catch (SQLiteException e){
            Log.d("TAGREGLA", e.getMessage());
            return false;
        }
    }

    public static Regla AsignaDatos(Cursor cursor) {
        Regla Item = null;
        try {
            Item = new Regla();
            Item.idproductoregla = cursor.getInt(0);
            Item.productoid = cursor.getInt(1);
            Item.establecimientoid = cursor.getInt(2);
            Item.cantidad = cursor.getDouble(3);
            Item.numerolote = cursor.getString(4);
            Item.fechamaxima = cursor.getString(5);
            Item.precio = cursor.getDouble(6);
            Item.longdate = cursor.getLong(7);
        }catch(Exception e){
            Log.d("TAGREGLA", "AsignaDatos(): " + e.getMessage());
        } finally { }
        return Item;
    }

    public static ArrayList<Regla> getAll(Integer idproducto, Integer establecimientoid){
        Long lToday = Utils.longDate(Utils.getDateFormat("yyyy-MM-dd"));
        ArrayList<Regla> Items = new ArrayList<>();
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(
                "SELECT * FROM reglaprecio WHERE productoid = ? AND establecimientoid IN (?,?) AND longdate >= ? ORDER BY cantidad DESC, establecimientoid DESC",
                new String[]{idproducto.toString(), establecimientoid.toString(), "0", lToday.toString()});
        Regla Item;
        if (cursor.moveToFirst()) {
            do {
                Item = AsignaDatos(cursor);
                if (Item!=null) Items.add(Item);
            } while(cursor.moveToNext());
        }
        sqLiteDatabase.close();
        return Items;
    }

    public static Regla get(Integer id){
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM reglaprecio WHERE idproductoregla = ?",
                new String[]{id.toString()});
        Regla Item = null;
        if (cursor.moveToFirst())
            Item = AsignaDatos(cursor);
        sqLiteDatabase.close();
        return Item;
    }

    public static boolean Delete(String [] where) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.delete("reglaprecio", "productoid = ? and establecimientoid = ?",where);
            sqLiteDatabase.close();
            Log.d("TAGREGLA","DELETE REGLAS OK");
            return true;
        } catch (SQLException ex){
            Log.d("TAGREGLA", ex.getMessage());
            return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return  " Pro: "+ this.productoid +
                " - E: " + this.establecimientoid +
                " - C: " + this.cantidad +
                " - P: " + this.precio +
                " - F: " + this.fechamaxima;
    }
}
