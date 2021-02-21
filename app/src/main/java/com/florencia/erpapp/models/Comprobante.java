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

public class Comprobante {
    public Integer idcomprobante, establecimientoid, usuarioid, codigosistema;
    public String codigotransaccion, tipotransaccion, fechacelular, fechadocumento, observacion,
                    codigoestablecimiento, puntoemision, claveacceso, nip, sucursalenvia, sucursalrecibe;
    public Double subtotal, subtotaliva, descuento, porcentajeiva, total, lat, lon;
    public List<DetalleComprobante> detalle;
    public Integer estado, secuencial;
    public Long longdate;
    public Cliente cliente;

    public static SQLiteDatabase sqLiteDatabase;

    public Comprobante(){
        this.idcomprobante = 0;
        this.establecimientoid = 0;
        this.cliente = new Cliente();
        this.usuarioid = 0;
        this.codigosistema = 0;
        this.codigotransaccion = "";
        this.tipotransaccion = "";
        this.fechacelular = "";
        this.fechadocumento = "";
        this.observacion = "";
        this.subtotal = 0d;
        this.subtotaliva = 0d;
        this.descuento = 0d;
        this.porcentajeiva = 0d;
        this.total = 0d;
        this.estado = 0;
        this.lat = 0d;
        this.lon = 0d;
        this.claveacceso ="";
        this.secuencial = 0;
        this.nip = "";
        this.sucursalenvia = "";
        this.sucursalrecibe ="";
        this.detalle = new ArrayList<>();
        this.longdate = 0l;
    }

    public static boolean Update(Integer idcomprobante, ContentValues values) {
        boolean retorno = true;
        try {
            //EN CASO DE ANULARSE UNA FACTURA ACTUALIZA EL STOCK DE LOS PRODUCTOS Y DE LOS LOTES
            if(values.getAsInteger("estado").equals(-1)){
                Comprobante micomprobante = Comprobante.get(idcomprobante);
                if(micomprobante!=null){
                    for (DetalleComprobante midetalle:micomprobante.detalle) {
                        ContentValues valPro = new ContentValues();
                        valPro.put("stock", (midetalle.producto.stock + midetalle.cantidad));
                        retorno = retorno && Producto.Update(midetalle.producto.idproducto,micomprobante.establecimientoid,valPro);

                        if(retorno) {
                            Lote milote = Lote.get(midetalle.producto.idproducto, micomprobante.establecimientoid, midetalle.numerolote);
                            if (milote != null) {
                                valPro = new ContentValues();
                                valPro.put("stock", (milote.stock + midetalle.cantidad));
                                retorno = retorno && Lote.Update(new String[]{}, valPro);
                            }else {
                                Log.d("TAGCOMPROBANTE", "Lote null: P: " + midetalle.producto.idproducto +" - E: "+ micomprobante.establecimientoid +" - L: "+ midetalle.numerolote);
                                retorno = false;
                            }
                        }
                    }
                }
            }

            if(retorno) {
                sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
                sqLiteDatabase.update("comprobante", values, "idcomprobante = ?", new String[]{idcomprobante.toString()});
                sqLiteDatabase.close();
                Log.d("TAGCOMPROBANTE", "UPDATE COMPROBANTE OK");
            }
        } catch (SQLException ex){
            Log.d("TAGCOMPROBANTE", "Update(): " + String.valueOf(ex));
            return false;
        }
        return retorno;
    }

    public boolean Save(boolean updateStock) {
        //this.Fecha = Printer.getFecha();
        try {
            if(updateStock)
                this.getTotal();
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                    "comprobante(idcomprobante, establecimientoid, clienteid, usuarioid, codigosistema, codigotransaccion, tipotransaccion, " +
                    "fechacelular, fechadocumento, observacion, subtotal, subtotaliva, descuento, porcentajeiva, total, estado, lat, lon, claveacceso, secuencial, nip, sucursalenvia, sucursalrecibe, longdate) " +
                    "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{this.idcomprobante == 0?null:this.idcomprobante.toString(), this.establecimientoid.toString(), this.cliente.idcliente.toString(), this.usuarioid.toString(), this.codigosistema.toString(),
                        this.codigotransaccion, this.tipotransaccion, this.fechacelular, this.fechadocumento, this.observacion, this.subtotal.toString(),
                        this.subtotaliva.toString(), this.descuento.toString(), this.porcentajeiva.toString(), this.total.toString(), this.estado.toString(),
                        this.lat.toString(), this.lon.toString(), this.claveacceso, this.secuencial.toString(), this.nip, this.sucursalenvia, this.sucursalrecibe, this.longdate.toString()});
            if (this.idcomprobante == 0)
                this.idcomprobante = SQLite.sqlDB.getLastId();
            else
                sqLiteDatabase.execSQL("DELETE FROM detallecomprobante WHERE comprobanteid = ?", new String[]{String.valueOf(this.idcomprobante)});
            sqLiteDatabase.close();
            Log.d("TAGCOMPROBANTE","GUARDO ENCABEZADO - ID: " + this.idcomprobante);
            return this.SaveDetalle(this.idcomprobante, updateStock);
        } catch (SQLException ex) {
            //Toast.makeText(context,ex.getMessage(),Toast.LENGTH_LONG).show();
            Log.d("TAGCOMPROBANTE", "Save(): " + ex.getMessage());
            return false;
        }
    }

    boolean SaveDetalle(Integer idComprobante, boolean updateStock) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            for (int i =0; i<this.detalle.size(); i++) {
                this.detalle.get(i).linea = i+1;
                sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                        "detallecomprobante(comprobanteid, linea, productoid, cantidad, precio, numerolote, " +
                        "fechavencimiento, stock, preciocosto, precioreferencia, valoriva, valorice, descuento, codigoproducto, nombreproducto) " +
                        "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{idComprobante.toString(), this.detalle.get(i).linea.toString(), this.detalle.get(i).producto.idproducto.toString(),
                                this.detalle.get(i).cantidad.toString(), this.detalle.get(i).precio.toString(), this.detalle.get(i).numerolote,
                                this.detalle.get(i).fechavencimiento, this.detalle.get(i).stock.toString(), this.detalle.get(i).preciocosto.toString(),
                                this.detalle.get(i).precioreferencia.toString(), this.detalle.get(i).valoriva.toString(), this.detalle.get(i).valorice.toString(),
                                this.detalle.get(i).descuento.toString(), this.detalle.get(i).producto.codigoproducto, this.detalle.get(i).producto.nombreproducto});

                this.detalle.get(i).comprobanteid = idComprobante;

                if(updateStock) {
                    //UPDATE STOCK PRODUCTO
                    ContentValues values = new ContentValues();
                    values.put("stock", this.detalle.get(i).producto.stock); //this.detalle.get(i).stock);
                    Producto.Update(this.detalle.get(i).producto.idproducto, SQLite.usuario.sucursal.IdEstablecimiento, values);

                    //UPDATE STOCK LOTE
                    if (this.detalle.get(i).producto.lotes.size() > 0) {
                        values = new ContentValues();
                        values.put("stock", this.detalle.get(i).producto.lotes.get(0).stock);
                        Lote.Update(new String[]{this.detalle.get(i).producto.idproducto.toString(),
                                this.detalle.get(i).producto.lotes.get(0).numerolote,
                                SQLite.usuario.sucursal.IdEstablecimiento.toString()}, values);
                    }
                }
            }
            if(updateStock)
                this.actualizasecuencial();
            sqLiteDatabase.close();
            Log.d("TAGDETALLECOMPROBANTE","Guardó detalle comprobante");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d("TAGDETALLECOMPROBANTE", "SaveDetalle(): " + ex.getMessage());
            return false;
        }
    }

    public static Comprobante get(Integer idComprobante) {
        Comprobante Item = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM comprobante WHERE idcomprobante = ?", new String[]{idComprobante.toString()});
            if (cursor.moveToFirst()) Item = Comprobante.AsignaDatos(cursor);
            sqLiteDatabase.close();
        }catch (Exception e){
            Log.d("TAGCOMPROBANTE", "get(): " + e.getMessage());
        }
        return Item;
    }

    public static ArrayList<Comprobante> getByClient(Integer idCliente) {
        ArrayList<Comprobante> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM comprobante WHERE clienteid = ? and tipotransaccion = ?", new String[]{idCliente.toString(), "01"});
            Comprobante Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Comprobante.AsignaDatos(cursor);
                    Items.add(Item);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        }catch (Exception e){
            Log.d("TAGCOMPROBANTE", "getByCliente(): " + e.getMessage());
        }
        return Items;
    }

    public static ArrayList<Comprobante> getByUsuario(Integer idUser, Integer establecimientoid, String tipotransaccion, String fechadesde, String fechahasta) {
        ArrayList<Comprobante> Items = new ArrayList<>();
        try {
            List<String> listparams = new ArrayList<>();
            listparams.add(idUser.toString());
            listparams.add(establecimientoid.toString());
            String[]tiptrans = tipotransaccion.split(",");
            String in = "";
            for (int i=0; i<tiptrans.length;i++) {
                in += i==tiptrans.length-1?"?":"?,";
                listparams.add(tiptrans[i]);
            }
            String WHERE = "usuarioid = ? and establecimientoid = ? and estado not in (-1) and tipotransaccion in ("+in+") ";

            if(!fechadesde.equals("")) {
                WHERE += "and longdate >= ? ";
                listparams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if(!fechahasta.equals("")) {
                WHERE += "and longdate <= ? ";
                listparams.add(String.valueOf(Utils.longDate(fechahasta)));
            }
            String[] itemsArray = new String[listparams.size()];
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM comprobante " +
                    "WHERE " + WHERE +
                    "ORDER BY estado, idcomprobante desc", listparams.toArray(itemsArray));

            Comprobante Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Comprobante.AsignaDatos(cursor);
                    if(Item!=null) Items.add(Item);
                } while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
            Log.d("TAGCOMPROBANTE", "NUMERO COMPROBANTES: " + Items.size());
        }catch (SQLiteException e){
            Log.d("TAGCOMPROBANTE", "getByUsuario(): " + e.getMessage());
        }
        return Items;
    }

    public static ArrayList<Comprobante> getPorSincronizar(Integer idUser) {
        ArrayList<Comprobante> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM comprobante WHERE usuarioid = ? and estado = 0 and tipotransaccion = ?", new String[]{idUser.toString(), "01"});
            Comprobante Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Comprobante.AsignaDatos(cursor);
                    if(Item!=null) Items.add(Item);
                } while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
            Log.d("TAGCOMPROBANTE", "NUMERO COMPROBANTES PS: " + Items.size());
        }catch (SQLiteException e){
            Log.d("TAGCOMPROBANTE", "getPorSincronizar(): " + e.getMessage());
        }
        return Items;
    }

    public static Comprobante AsignaDatos(Cursor cursor) {
        Comprobante Item;
        try {
            Item = new Comprobante();
            Item.idcomprobante = cursor.getInt(0);
            Item.establecimientoid = cursor.getInt(1);
            Item.cliente = Cliente.get(cursor.getInt(2));
            if(Item.cliente == null)
                Item.cliente = Cliente.get(cursor.getString(20));
            Item.usuarioid = cursor.getInt(3);
            Item.codigosistema = cursor.getInt(4);
            Item.codigotransaccion = cursor.getString(5);
            Item.tipotransaccion = cursor.getString(6);
            Item.fechacelular = cursor.getString(7);
            Item.fechadocumento = cursor.getString(8);
            Item.observacion = cursor.getString(9);
            Item.subtotal = cursor.getDouble(10);
            Item.subtotaliva = cursor.getDouble(11);
            Item.descuento = cursor.getDouble(12);
            Item.porcentajeiva = cursor.getDouble(13);
            Item.total = cursor.getDouble(14);
            Item.estado = cursor.getInt(15);
            Item.lat = cursor.getDouble(16);
            Item.lon = cursor.getDouble(17);
            Item.claveacceso= cursor.getString(18);
            Item.secuencial = cursor.getInt(19);
            Item.nip = cursor.getString(20);
            Item.sucursalenvia = cursor.getString(21);
            Item.sucursalrecibe = cursor.getString(22);
            Item.longdate = cursor.getLong(23);
            Item.detalle = DetalleComprobante.getDetalle(Item.idcomprobante);
            Log.d("TAG", Item.observacion);
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d("TAGCOMRPROBANTE", "AsignaDatos(): " + ec.getMessage());
            Item = null;
        }
        return Item;
    }

    public static int Delete(Integer id, String fechadesde, String fechahasta, Integer primeros, boolean soloSincronizados){
        int retorno = 0;
        try{
            List<String> listParams = new ArrayList<>();
            String WHERE = "";
            if(id>0) {
                WHERE = "idcomprobante = ?";
                listParams.add(id.toString());
            }
            if(!fechadesde.trim().equals("")){
                WHERE = (WHERE.trim().equals("")?"":" AND ") + "longdate >= ?";
                listParams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if(!fechahasta.trim().equals("")){
                WHERE = (WHERE.trim().equals("")?"":" AND ") + "longdate <= ?";
                listParams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if(soloSincronizados)
                WHERE += (WHERE.trim().equals("")?"":" AND ") + "codigosistema > 0";
            String[] params = new String[listParams.size()];
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            retorno = sqLiteDatabase.delete("comprobante", WHERE, listParams.toArray(params));
            Log.d("TAGCOMPROBANTE", "Registros eliminados: " + retorno);
        }catch (Exception e){
            Log.d("TAGCOMPROBANTE", "Delete(): " + e.getMessage());
            retorno = 0;
        }
        return retorno;
    }

    public String getCodigoTransaccion(){
        String codigo ="";
        try {
            codigo = this.codigoestablecimiento + "-" + this.puntoemision + "-" + String.format("%09d", this.secuencial);
            this.codigotransaccion = codigo;
        }catch (Exception e){
            Log.d("TAGCOMPROBANTE", "getCodigoTransaccion(): " + e.getMessage());
        }
        return codigo;
    }

    private int ultimosecuencial() {
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(
                "SELECT secuencial as sec FROM secuencial WHERE sucursalid = ? AND codigoestablecimiento = ? AND puntoemision = ? AND tipocomprobante = ? ",
                new String[]{this.establecimientoid.toString(), this.codigoestablecimiento, this.puntoemision, this.tipotransaccion});
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
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO secuencial(secuencial, sucursalid, codigoestablecimiento, puntoemision, tipocomprobante) VALUES(?, ?, ?, ?, ?) ", new String[]{String.valueOf(this.secuencial + 1), this.establecimientoid.toString(), this.codigoestablecimiento, this.puntoemision, this.tipotransaccion});
            //sqLiteDatabase.close();
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d("TAGCOMPROBANTE", "actualizasecuencial(): " + ec.getMessage());
            return false;
        }
        return true;
    }

    public String GenerarClaveAcceso() {
        String cod;
        this.secuencial = this.ultimosecuencial();
        cod = Utils.getDateFormat("ddMMyyyy");
        cod = cod.concat("01");
        cod = cod.concat(SQLite.usuario.sucursal.RUC);
        cod = cod.concat(SQLite.usuario.sucursal.Ambiente);
        cod = cod.concat(getCodigoTransaccion().replace("-",""));
        cod = cod.concat("123456781");
        cod = cod.concat(String.valueOf(SQLite.DigitoVerificador(cod)));
        this.claveacceso = cod;
        return cod;
    }

    public double getTotal() {
        this.total = 0d;
        this.subtotaliva = 0d;
        this.subtotal = 0d;
        for (DetalleComprobante item : this.detalle) {
            this.total += item.Subtotaliva();
            if (item.producto.porcentajeiva > 0)
                this.subtotaliva += item.Subtotal();
            else
                this.subtotal += item.Subtotal();
        }
        return this.total;
    }

    @Override
    public String toString() {
        return codigotransaccion;
    }
}
