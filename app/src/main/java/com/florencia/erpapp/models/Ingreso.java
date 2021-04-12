package com.florencia.erpapp.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Ingreso {
    public Integer idingreso, codigosistema, estado, carterafaltante, personaid,
            usuarioid, establecimientoid, secuencial, tipo, formadepagoid;
    public Double totalingreso;
    public String fechadiario, fechacelular, fechadocumento, secuencialdocumento, observacion;
    public Long longdater;
    public List<DetalleIngreso> detalle;
    public List<Foto> fotos;

    public static SQLiteDatabase sqLiteDatabase;
    public static final String TAG = "TAG_INGRESO";

    public Ingreso(){
        idingreso = 0;
        codigosistema = 0;
        estado = 0;
        carterafaltante = 0;
        personaid = 0;
        usuarioid = 0;
        establecimientoid = 0;
        secuencial = 0;
        tipo = 0;
        formadepagoid = 0;
        totalingreso = 0d;
        fechadiario = "";
        fechacelular = "";
        secuencialdocumento = "";
        longdater = 0l;
        observacion = "";
        detalle = new ArrayList<>();
        fotos = new ArrayList<>();
    }

    public static boolean Update(Integer idingreso, ContentValues values) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.update("ingreso",values, "idingreso = ?",new String[]{idingreso.toString()});
            sqLiteDatabase.close();
            Log.d("TAGINGRESO","UPDATE INGRESO OK");
            return true;
        } catch (SQLException ex){
            Log.d("TAGINGRESO", "Update(): " + String.valueOf(ex));
            return false;
        }
    }

    public boolean Save() {
        boolean retorno = false;
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                            "ingreso(idingreso, codigosistema, estado, carterafaltante, personaid," +
                            "usuarioid, establecimientoid, secuencial, tipo,formadepagoid, totalingreso," +
                            "fechadiario, fechacelular, secuencialdocumento, longdater, observacion) " +
                            "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{this.idingreso == 0?null:this.idingreso.toString(), this.codigosistema.toString(), this.estado.toString(),
                            this.carterafaltante.toString(), this.personaid.toString(), this.usuarioid.toString(), this.establecimientoid.toString(),
                            this.secuencial.toString(), this.tipo.toString(), this.formadepagoid.toString(), this.totalingreso.toString(), this.fechadiario,
                            this.fechacelular, this.secuencialdocumento, this.longdater.toString(), this.observacion});
            if (this.idingreso == 0)
                this.idingreso = SQLite.sqlDB.getLastId();
            else
                sqLiteDatabase.execSQL("DELETE FROM detalleingreso WHERE ingresoid = ?", new String[]{this.idingreso.toString()});

            retorno = this.SaveDetalle(this.idingreso);

            if(retorno && this.fotos.size()>0)
                retorno =  retorno && Foto.SaveList(this.personaid, this.idingreso, this.fotos, "I");

            sqLiteDatabase.close();
            Log.d(TAG,"GUARDO ENCABEZADO - ID: " + this.idingreso);
        } catch (SQLException ex) {
            Log.d(TAG, "Save(): " + ex.getMessage());
            return false;
        }
        return  retorno;
    }

    private boolean SaveDetalle(Integer ingresoid) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            for (int i =0; i<this.detalle.size(); i++) {
                sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                                "detalleingreso(ingresoid, linea, monto, numerodocumentoreferencia, fechadocumento," +
                                "entidadfinancieracodigo, tipodecuenta, niptitular, razonsocialtitular, tipodocumento," +
                                "fechadiario, tipo) " +
                                "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{ingresoid.toString(), String.valueOf(i+1), this.detalle.get(i).monto.toString(),
                            this.detalle.get(i).numerodocumentoreferencia,this.detalle.get(i).fechadocumento,
                            this.detalle.get(i).entidadfinanciera.codigocatalogo, this.detalle.get(i).tipodecuenta,
                            this.detalle.get(i).niptitular, this.detalle.get(i).razonsocialtitular, this.detalle.get(i).tipodocumento.toString(),
                                this.detalle.get(i).fechadiario, this.detalle.get(i).tipo.toString()});

                this.detalle.get(i).ingresoid = ingresoid;
            }
            this.actualizasecuencial();
            sqLiteDatabase.close();
            Log.d(TAG,"Guardó detalle ingreso");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "SaveDetalle(): " + ex.getMessage());
            return false;
        }
    }

    public static Ingreso get(Integer idingreso) {
        Ingreso Item = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM ingreso WHERE idingreso = ?",
                    new String[]{idingreso.toString()});
            if (cursor.moveToFirst()) Item = Ingreso.AsignaDatos(cursor);
            sqLiteDatabase.close();
        }catch (Exception e){
            Log.d(TAG, "get(): " + e.getMessage());
        }
        return Item;
    }

    public static ArrayList<Ingreso> getByPersona(Integer idPersona) {
        ArrayList<Ingreso> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery(
                    "SELECT * FROM ingreso WHERE personaid = ? ORDER BY estado, idingreso desc",
                    new String[]{idPersona.toString()});
            Ingreso Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Ingreso.AsignaDatos(cursor);
                    Items.add(Item);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        }catch (Exception e){
            Log.d(TAG, "getByCliente(): " + e.getMessage());
        }
        return Items;
    }

    public static ArrayList<Ingreso> getByUsuario(Integer idUser, Integer establecimientoid, String fechadesde, String fechahasta) {
        ArrayList<Ingreso> Items = new ArrayList<>();
        try {
            List<String> listparams = new ArrayList<>();
            String WHERE = "usuarioid = ? and establecimientoid = ? and estado not in (-1) ";
            listparams.add(idUser.toString());
            listparams.add(establecimientoid.toString());
            if(!fechadesde.equals("")) {
                WHERE += " and longdater >= ?";
                listparams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if(!fechahasta.equals("")) {
                WHERE += " and longdater <= ?";
                listparams.add(String.valueOf(Utils.longDate(fechahasta)));
            }
            String[] itemsArray = new String[listparams.size()];
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM ingreso " +
                    "WHERE " + WHERE +
                    "ORDER BY estado asc, idingreso desc", listparams.toArray(itemsArray));
            Ingreso Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Ingreso.AsignaDatos(cursor);
                    if(Item!=null) Items.add(Item);
                } while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
            Log.d(TAG, "NUMERO INGRESOS: " + Items.size());
        }catch (SQLiteException e){
            Log.d(TAG, "getByUsuario(): " + e.getMessage());
        }
        return Items;
    }

    public static ArrayList<Ingreso> getPorSincronizar(Integer idUser) {
        ArrayList<Ingreso> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM ingreso WHERE usuarioid = ? and estado = 1", new String[]{idUser.toString()});
            Ingreso Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Ingreso.AsignaDatos(cursor);
                    if(Item!=null) Items.add(Item);
                } while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
            Log.d(TAG, "NUMERO COMPROBANTES PS: " + Items.size());
        }catch (SQLiteException e){
            Log.d(TAG, "getPorSincronizar(): " + e.getMessage());
        }
        return Items;
    }

    public static int Delete(Integer id, String fechadesde, String fechahasta, Integer primeros, boolean soloSincronizados){
        int retorno = 0;
        try{
            List<String> listParams = new ArrayList<>();
            String WHERE = "";
            if(id>0) {
                WHERE = "idingreso = ?";
                listParams.add(id.toString());
            }
            if(!fechadesde.trim().equals("")){
                WHERE = (WHERE.trim().equals("")?"":" AND ") + "longdater >= ?";
                listParams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if(!fechahasta.trim().equals("")){
                WHERE = (WHERE.trim().equals("")?"":" AND ") + "longdater <= ?";
                listParams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if(soloSincronizados)
                WHERE += (WHERE.trim().equals("")?"":" AND ") + "codigosistema > 0";
            String[] params = new String[listParams.size()];
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            retorno = sqLiteDatabase.delete("ingreso", WHERE, listParams.toArray(params));
            Log.d(TAG, "Registros eliminados: " + retorno);
        }catch (Exception e){
            Log.d(TAG, "Delete(): " + e.getMessage());
            retorno = 0;
        }
        return retorno;
    }

    public static Ingreso AsignaDatos(Cursor cursor) {
        Ingreso Item;
        try {
            Item = new Ingreso();
            Item.idingreso = cursor.getInt(0);
            Item.codigosistema = cursor.getInt(1);
            Item.estado = cursor.getInt(2);
            Item.carterafaltante = cursor.getInt(3);
            Item.personaid = cursor.getInt(4);
            Item.usuarioid = cursor.getInt(5);
            Item.establecimientoid = cursor.getInt(6);
            Item.secuencial = cursor.getInt(7);
            Item.tipo = cursor.getInt(8);
            Item.formadepagoid = cursor.getInt(9);
            Item.totalingreso = cursor.getDouble(10);
            Item.fechadiario = cursor.getString(11);
            Item.fechacelular = cursor.getString(12);
            Item.secuencialdocumento = cursor.getString(13);
            Item.longdater = cursor.getLong(14);
            Item.observacion = cursor.getString(15);
            Item.detalle = DetalleIngreso.getDetalle(Item.idingreso);
            Item.fotos = Foto.getLista(Item.personaid, Item.idingreso, "I");
            if(Item.fotos == null)
                Item.fotos = new ArrayList<>();
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
            Item = null;
        }
        return Item;
    }

    public String getCodigoTransaccion(){
        String codigo ="";
        try {
            this.secuencial = ultimosecuencial();
            codigo = SQLite.usuario.sucursal.periodo.toString() + SQLite.usuario.sucursal.mesactual +
                    "-OP-" + String.format("%04d", this.secuencial);
            this.secuencialdocumento = codigo;
            Log.d(TAG, codigo);
        }catch (Exception e){
            Log.d(TAG, "getCodigoTransaccion(): " + e.getMessage());
        }
        return codigo;
    }

    public Integer ultimosecuencial() {
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(
                "SELECT secuencial as sec FROM secuencial WHERE sucursalid = ? AND codigoestablecimiento = ? AND puntoemision = ? AND tipocomprobante = ? ",
                new String[]{this.establecimientoid.toString(), "", "", "OP"});
        int n = 0;
        if (cursor.moveToFirst()) {
            n = cursor.getInt(0);
        }
        sqLiteDatabase.close();
        if (n <= 0) n = 1;
        return n;
    }

    public boolean actualizasecuencial() {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO secuencial(secuencial, sucursalid, codigoestablecimiento, puntoemision, tipocomprobante) VALUES(?, ?, ?, ?, ?) ",
                    new String[]{String.valueOf(this.secuencial + 1), this.establecimientoid.toString(), "", "", "OP"});
            //sqLiteDatabase.close();
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "actualizasecuencial(): " + ec.getMessage());
            return false;
        }
        return true;
    }
}
