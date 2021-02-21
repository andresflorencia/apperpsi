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

public class Lote {
    public Integer productoid, establecimientoid;
    public Double stock, preciocosto;
    public String numerolote, fechavencimiento;
    public Long longdate;

    public static SQLiteDatabase sqLiteDatabase;

    public Lote(){
        this.productoid = 0;
        this.numerolote = "";
        this.stock = 0d;
        this.preciocosto = 0d;
        this.fechavencimiento = "1900-01-01";
        this.longdate = 0l;
        this.establecimientoid = 0;
    }

    public boolean Save() {
        try {
            this.sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            this.sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                    "lote(productoid, numerolote, stock, preciocosto, fechavencimiento, longdate) " +
                    "values(?, ?, ?, ?, ?)", new String[]{this.productoid.toString(), this.numerolote, this.stock.toString(),
                    this.preciocosto.toString(), this.fechavencimiento, this.longdate.toString(), this.establecimientoid.toString()});
            this.sqLiteDatabase.close();
            Log.d("TAGLOTE","SAVE LOTE OK");
            return true;
        } catch (SQLException ex){
            Log.d("TAGLOTE",String.valueOf(ex));
            return false;
        }
    }

    public static boolean InsertMultiple(Integer idproducto, Integer establecimientoid, List<Lote> lotes){
        try {
            ContentValues values;
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.delete("lote","productoid = ? and establecimientoid = ?", new String[]{idproducto.toString(), establecimientoid.toString()});
            for(Lote lote: lotes) {
                lote.longdate = Utils.longDate(lote.fechavencimiento);
                values= new ContentValues();
                values.put("productoid",lote.productoid);
                values.put("numerolote",lote.numerolote);
                values.put("stock",lote.stock);
                values.put("preciocosto",lote.preciocosto);
                values.put("fechavencimiento",lote.fechavencimiento);
                values.put("longdate", lote.longdate);
                values.put("establecimientoid", lote.establecimientoid);
                sqLiteDatabase.insert("lote", "", values);
            }
            sqLiteDatabase.close();
            Log.d("TAGLOTE", "INSERT LOTE OK");
            return true;
        }catch (SQLiteException e){
            Log.d("TAGLOTE", e.getMessage());
            return false;
        }
    }

    public static Lote AsignaDatos(Cursor cursor) {
        Lote Item = null;
        try {
            Item = new Lote();
            Item.productoid = cursor.getInt(0);
            Item.numerolote = cursor.getString(1);
            Item.stock = cursor.getDouble(2);
            Item.preciocosto = cursor.getDouble(3);
            Item.fechavencimiento = cursor.getString(4);
            Item.longdate = cursor.getLong(5);
            Item.establecimientoid = cursor.getInt(6);
        }catch(Exception e){
            Log.d("TAGLOTE", "AsignaDatos(): " + e.getMessage());
        } finally { }
        return Item;
    }

    public static ArrayList<Lote> getAll(Integer idproducto, Integer establecimientoid){
        ArrayList<Lote> Items = new ArrayList<>();
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM lote WHERE productoid = ? and establecimientoid = ? and stock > 0", new String[]{idproducto.toString(), establecimientoid.toString()});
        Lote Item;
        if (cursor.moveToFirst()) {
            do {
                Item = AsignaDatos(cursor);
                if (Item!=null) Items.add(Item);
            } while(cursor.moveToNext());
        }
        sqLiteDatabase.close();
        return Items;
    }

    public static Lote get(Integer idproducto, Integer establecimientoid, String numLote){
        ArrayList<Lote> Items = new ArrayList<>();
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM lote WHERE productoid = ? and establecimientoid = ? and numerolote = ?", new String[]{idproducto.toString(), establecimientoid.toString(), numLote});
        Lote Item = null;
        if (cursor.moveToFirst())
            Item = AsignaDatos(cursor);
        sqLiteDatabase.close();
        return Item;
    }

    public static boolean Update(String [] where, ContentValues data) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.update("lote",data, "productoid = ? and numerolote = ? and establecimientoid = ?",where);
            //sqLiteDatabase.close();
            Log.d("TAGLOTE","UPDATE 2 LOTE OK");
            return true;
        } catch (SQLException ex){
            Log.d("TAGLOTE",String.valueOf(ex));
            return false;
        }
    }

    public static boolean Delete(String [] where) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.delete("lote", "productoid = ? and numerolote = ? and establecimientoid = ?",where);
            sqLiteDatabase.close();
            Log.d("TAGLOTE","UPDATE 2 LOTE OK");
            return true;
        } catch (SQLException ex){
            Log.d("TAGLOTE",String.valueOf(ex));
            return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return  " FV: "+ this.fechavencimiento +
                " - S: " + this.stock +
                " - L: " + this.numerolote;
    }
}
