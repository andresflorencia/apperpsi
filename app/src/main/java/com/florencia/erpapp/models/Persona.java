package com.florencia.erpapp.models;


import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class Persona {
    public int IdPersona;
    public String RazonSocial;
    public String Fono;
    public String Direccion;

    private boolean EsNuevo;
    public static SQLiteDatabase sqLiteDatabase;

    public Persona() {
        this.EsNuevo = true;
        this.RazonSocial = "";
        this.IdPersona = 0;
        this.Fono = "";
    }

    public String Codigo() {
        return String.valueOf(this.IdPersona);
    }

    public boolean Guardar() {
        try {
            this.sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            this.sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                    "persona(idpersona, razonsocial, fono) " +
                    "values(?, ?, ?)", new String[]{String.valueOf(this.IdPersona), this.RazonSocial, this.Fono});
            this.sqLiteDatabase.close();
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    static public Persona get(int Codigo) {
        Persona Item = null;
        sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM persona WHERE idpersona = " + Codigo, null);
        if (cursor.moveToFirst()) {
            Item = AsignaDatos(cursor);
        }
        cursor.close();
        sqLiteDatabase.close();
        return Item;
    }

    public static Persona AsignaDatos(Cursor cursor) {
        Persona Item = null;
        Item = new Persona();
        Item.IdPersona = cursor.getInt(0);
        Item.RazonSocial = cursor.getString(1);
        Item.Fono = cursor.getString(2);
        return Item;
    }

    static public List<Persona> getAll() {
        List<Persona> Items = new ArrayList<Persona>();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM persona", null);
        Persona Item;
        if (cursor.moveToFirst()) {
            do {
                Item = AsignaDatos(cursor);
                if (Item != null) Items.add(Item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return Items;
    }
}
