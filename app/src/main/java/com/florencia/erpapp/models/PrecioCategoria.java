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

public class PrecioCategoria {
    public Integer idproductocategoria, productoid, establecimientoid, categoriaid;
    public Double valor;
    public String nombrecategoria, prioridad, aplicacredito;

    public static SQLiteDatabase sqLiteDatabase;
    public static final String TAG = "TAGCATEGORIA";
    public PrecioCategoria(){
        this.idproductocategoria = 0;
        this.productoid = 0;
        this.establecimientoid = 0;
        this.categoriaid = 0;
        this.prioridad = "";
        this.aplicacredito = "";
        this.valor = 0d;
        this.nombrecategoria = "";
    }

    public boolean Save() {
        try {
            this.sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            this.sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                    "preciocategoria(idproductocategoria, productoid, establecimientoid, categoriaid, valor, nombrecategoria, prioridad, aplicacredito) " +
                    "values(?, ?, ?, ?, ?, ?, ?, ?)", new String[]{this.idproductocategoria.toString(), this.productoid.toString(), this.establecimientoid.toString(),
                    this.categoriaid.toString(),this.valor.toString(), this.nombrecategoria, this.prioridad, this.aplicacredito});
            this.sqLiteDatabase.close();
            Log.d(TAG,"SAVE PrecioCategoria OK");
            return true;
        } catch (SQLException ex){
            Log.d(TAG, ex.getMessage());
            return false;
        }
    }

    public static boolean InsertMultiple(Integer idproducto, Integer establecimientoid, List<PrecioCategoria> categorias){
        try {
            ContentValues values;
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.delete("preciocategoria","productoid = ?", new String[]{idproducto.toString()});
            for(PrecioCategoria categoria: categorias) {
                sqLiteDatabase.delete("preciocategoria","idproductocategoria = ?", new String[]{categoria.idproductocategoria.toString()});
                values= new ContentValues();
                values.put("idproductocategoria", categoria.idproductocategoria);
                values.put("productoid", categoria.productoid);
                values.put("establecimientoid", categoria.establecimientoid);
                values.put("categoriaid", categoria.categoriaid);
                values.put("valor", categoria.valor);
                values.put("nombrecategoria", categoria.nombrecategoria);
                values.put("prioridad", categoria.prioridad);
                values.put("aplicacredito", categoria.aplicacredito);
                sqLiteDatabase.insert("preciocategoria", "", values);
            }
            sqLiteDatabase.close();
            Log.d(TAG, "INSERT PRECIOS CATEGORIAS OK");
            return true;
        }catch (SQLiteException e){
            Log.d(TAG, e.getMessage());
            return false;
        }
    }

    public static PrecioCategoria AsignaDatos(Cursor cursor) {
        PrecioCategoria Item = null;
        try {
            Item = new PrecioCategoria();
            Item.idproductocategoria = cursor.getInt(0);
            Item.productoid = cursor.getInt(1);
            Item.establecimientoid = cursor.getInt(2);
            Item.categoriaid = cursor.getInt(3);
            Item.valor = cursor.getDouble(4);
            Item.nombrecategoria = cursor.getString(5);
            Item.prioridad = cursor.getString(6);
            Item.aplicacredito = cursor.getString(7);
        }catch(Exception e){
            Log.d(TAG, "AsignaDatos(): " + e.getMessage());
        } finally { }
        return Item;
    }

    public static ArrayList<PrecioCategoria> getAll(Integer idproducto, Integer establecimientoid){
        ArrayList<PrecioCategoria> Items = new ArrayList<>();
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(
                "SELECT * FROM preciocategoria WHERE productoid = ? AND establecimientoid IN (?)",
                new String[]{idproducto.toString(), establecimientoid.toString()});
        PrecioCategoria Item;
        if (cursor.moveToFirst()) {
            do {
                Item = AsignaDatos(cursor);
                if (Item!=null) Items.add(Item);
            } while(cursor.moveToNext());
        }
        sqLiteDatabase.close();
        return Items;
    }

    public static PrecioCategoria get(Integer id){
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM preciocategoria WHERE idproductocategoria = ?",
                new String[]{id.toString()});
        PrecioCategoria Item = null;
        if (cursor.moveToFirst())
            Item = AsignaDatos(cursor);
        sqLiteDatabase.close();
        return Item;
    }

    public static boolean Delete(String [] where) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.delete("preciocategoria", "productoid = ? and establecimientoid = ?",where);
            sqLiteDatabase.close();
            Log.d(TAG,"DELETE CATEGORIAS PRECIO OK");
            return true;
        } catch (SQLException ex){
            Log.d(TAG, ex.getMessage());
            return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return  this.nombrecategoria +
                " - Pro: "+ this.productoid +
                " - Est: " + this.establecimientoid +
                " - Val: " + this.valor +
                " - Pri: " + this.prioridad +
                " - ACr: " + this.aplicacredito;
    }
}
