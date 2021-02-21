package com.florencia.erpapp.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Cliente {
    public Integer idcliente;
    public String tiponip;
    public String nip;
    public String razonsocial;
    public String nombrecomercial;
    public String direccion;
    public Double lat, lon;
    public String categoria;
    public Integer usuarioid;
    public String fono1, fono2, email, observacion, ruc;
    public Integer codigosistema, actualizado, establecimientoid;

    public static SQLiteDatabase sqLiteDatabase;

    public Cliente(){
        this.idcliente= 0;
        this.tiponip= "";
        this.nip= "";
        this.razonsocial= "";
        this.nombrecomercial= "";
        this.direccion= "";
        this.lat = 0d;
        this.lon = 0d;
        this.categoria= "";
        this.usuarioid= 0;
        this.fono1 = "";
        this.fono2 ="";
        this.email="";
        this.observacion="";
        this.ruc= "";
        this.codigosistema = 0;
        this.actualizado =0;
        this.establecimientoid = 0;
    }

    public static boolean removeClientes(Integer idUsuario) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("DELETE FROM cliente WHERE usuarioid = ? AND codigosistema <> 0", new String[] {idUsuario.toString()});
            sqLiteDatabase.close();
            Log.d("TAGCLIENTE", "CLIENTES ELIMINADOS");
            return true;
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d("TAGCLIENTE", ec.getMessage());
        }
        return false;
    }

    public boolean Save() {
        try {
            this.sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            if (this.idcliente == 0)
                this.sqLiteDatabase.execSQL("INSERT INTO " +
                        "cliente(tiponip, nip, razonsocial, nombrecomercial, direccion, lat, lon, categoria, " +
                                "usuarioid, fono1, fono2, email, observacion, ruc, codigosistema, actualizado, establecimientoid) " +
                        "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{ this.tiponip, this.nip, this.razonsocial, this.nombrecomercial, this.direccion,
                                this.lat.toString(), this.lon.toString(), this.categoria.equals("")?"A":this.categoria, this.usuarioid.toString(), this.fono1,
                                this.fono2, this.email, this.observacion, this.ruc, this.codigosistema.toString(), this.actualizado.toString(), this.establecimientoid.toString()});
            else
                this.sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                                "cliente(idcliente, tiponip, nip, razonsocial, nombrecomercial, direccion, lat, lon, categoria, " +
                                "usuarioid, fono1, fono2, email, observacion, ruc, codigosistema, actualizado, establecimientoid) " +
                                "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{ this.idcliente.toString(), this.tiponip, this.nip, this.razonsocial, this.nombrecomercial, this.direccion,
                                this.lat.toString(), this.lon.toString(), this.categoria.equals("")?"A":this.categoria, this.usuarioid.toString(), this.fono1,
                                this.fono2, this.email, this.observacion, this.ruc, this.codigosistema.toString(), this.actualizado.toString(), this.establecimientoid.toString()});
            if (this.idcliente == 0) this.idcliente = SQLite.sqlDB.getLastId();

            this.sqLiteDatabase.close();
            Log.d("TAGCLIENTE","SAVE CLIENTE OK");
            return true;
        } catch (SQLException ex){
            Log.d("TAGCLIENTE",String.valueOf(ex));
            return false;
        }
    }

    public static Cliente get(Integer Id) {
        Cliente Item = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM cliente WHERE idcliente  = ?", new String[]{Id.toString()});
            if (cursor.moveToFirst()) Item = Cliente.AsignaDatos(cursor);
            sqLiteDatabase.close();
        }catch (Exception e){
            Log.d("TAGCLIENTE",e.getMessage());
        }
        return Item;
    }

    public static Cliente get(String nip) {
        Cliente Item = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM cliente WHERE nip  = ?", new String[]{nip});
            if (cursor.moveToFirst()) Item = Cliente.AsignaDatos(cursor);
            sqLiteDatabase.close();
        }catch (SQLiteException e){
            Log.d("TAGCLIENTE", e.getMessage());
        }
        return Item;
    }

    public static Cliente AsignaDatos(Cursor cursor) {
        Cliente Item = null;
        try {
            Item = new Cliente();
            Item.idcliente = cursor.getInt(0);
            Item.tiponip = cursor.getString(1);
            Item.nip = cursor.getString(2);
            Item.razonsocial = cursor.getString(3);
            Item.nombrecomercial = cursor.getString(4);
            Item.direccion = cursor.getString(5);
            Item.lat = cursor.getDouble(6);
            Item.lon = cursor.getDouble(7);
            Item.categoria = cursor.getString(8);
            Item.usuarioid = cursor.getInt(9);
            Item.fono1 = cursor.getString(10);
            Item.fono2 = cursor.getString(11);
            Item.email = cursor.getString(12);
            Item.observacion = cursor.getString(13);
            Item.ruc = cursor.getString(14);
            Item.codigosistema = cursor.getInt(15);
            Item.actualizado = cursor.getInt(16);
            Item.establecimientoid= cursor.getInt(17);
        } catch (SQLiteException ec) {
            Log.d("TAGCLIENTE", ec.getMessage());
        }
        return Item;
    }

    public String Validate() {
        String str = "";
        if (this.nip.equals("")) str += "   -   NIP" + '\n';
        if (this.razonsocial.equals("")) str += "  -  Razón Social" + '\n';
        if (this.nombrecomercial.equals("")) str += "   -   Nombre de la tienda o comercio" + '\n';
        if (this.fono1.equals("") && this.fono2.equals("")) str += "   -   Celular o convencional" + '\n';
        if (this.direccion.equals("")) str += "   -   Dirección" + '\n';
        //if (this.lat == 0 || this.lon == 0) str += "   -   Coordenadas. Por favor verificar que el GPS esté activo";
        return str;
    }

    public static List<Cliente> getClientes(Integer idUser) {
        List<Cliente> lista = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM cliente where usuarioid = ? ORDER BY razonsocial", new String[]{idUser.toString()});
            Cliente cliente = new Cliente();
            if (cursor.moveToFirst()) {
                do {
                    cliente = Cliente.AsignaDatos(cursor);
                    if (cliente != null) lista.add(cliente);
                }while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ec) {
            Log.d("TAGCLIENTE",ec.getMessage());
            ec.printStackTrace();
        }
        return lista;
    }

    public static ArrayList<Cliente> getClientesSC(Integer idUser) {
        ArrayList<Cliente> items = new ArrayList<Cliente>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM cliente where usuarioid = ? and nip <> ? and (codigosistema = 0 or actualizado = 1) ORDER BY razonsocial", new String[]{idUser.toString(), "9999999999999"});
            Cliente cliente;
            if (cursor.moveToFirst()) {
                do {
                    cliente = Cliente.AsignaDatos(cursor);
                    if (cliente != null) items.add(cliente);
                }while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception ec) {
            ec.printStackTrace();
        }
        return items;
    }

    public static boolean Update(Integer id, ContentValues data) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.update("cliente",data, "idcliente = ?",new String[]{id.toString()});
            sqLiteDatabase.close();
            Log.d("TAGCLIENTE","UPDATE CLIENTE OK");
            return true;
        } catch (SQLException ex){
            Log.d("TAGCLIENTE", "Update(): " + String.valueOf(ex));
            return false;
        }
    }
}
