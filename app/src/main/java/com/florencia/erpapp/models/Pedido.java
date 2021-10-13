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

public class Pedido {
    public Integer idpedido, codigosistema, estado, usuarioid, parroquiaid, establecimientoid, secuencial;
    public String fechapedido, fechacelular, observacion, categoria, secuencialpedido, nip, codigoestablecimiento,
            puntoemision, tipotransaccion, secuencialsistema;
    public Double total, subtotal, subtotaliva, porcentajeiva, descuento, lat, lon;
    public List<DetallePedido> detalle;
    public Cliente cliente;
    public Long longdate;

    public static SQLiteDatabase sqLiteDatabase;
    public static String TAG = "TAGPEDIDO";

    public Pedido() {
        this.idpedido = 0;
        this.codigosistema = 0;
        this.estado = 0;
        this.usuarioid = 0;
        this.parroquiaid = 0;
        this.establecimientoid = 0;
        this.secuencial = 0;
        this.fechapedido = "";
        this.fechacelular = "";
        this.observacion = "";
        this.categoria = "";
        this.secuencialpedido = "";
        this.nip = "";
        this.detalle = new ArrayList<>();
        this.cliente = new Cliente();
        this.codigoestablecimiento = "";
        this.puntoemision = "";
        this.tipotransaccion = "";
        this.total = 0d;
        this.subtotal = 0d;
        this.subtotaliva = 0d;
        this.porcentajeiva = 0d;
        this.descuento = 0d;
        this.lat = 0d;
        this.lon = 0d;
        this.longdate = 0l;
        this.secuencialsistema = "";
    }

    public static boolean Update(Integer idcomprobante, ContentValues values) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.update("pedido", values, "idpedido = ?", new String[]{idcomprobante.toString()});
            sqLiteDatabase.close();
            Log.d(TAG, "UPDATE PEDIDO OK");
            return true;
        } catch (SQLException ex) {
            Log.d(TAG, "Update(): " + String.valueOf(ex));
            return false;
        }
    }

    public boolean Save() {
        try {
            this.getTotal();
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                            "pedido(idpedido, codigosistema, clienteid, estado, usuarioid, parroquiaid, establecimientoid, secuencial, " +
                            "fechapedido, fechacelular, observacion, categoria, secuencialpedido, nip, " +
                            "total, subtotal, subtotaliva, porcentajeiva, descuento, lat, lon, codigoestablecimiento, puntoemision, " +
                            "tipotransaccion, longdate, secuencialsistema) " +
                            "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{this.idpedido == 0 ? null : this.idpedido.toString(), this.codigosistema.toString(), this.cliente.idcliente.toString(), this.estado.toString(),
                            this.usuarioid.toString(), this.parroquiaid.toString(), this.establecimientoid.toString(), this.secuencial.toString(),
                            this.fechapedido, this.fechacelular, this.observacion, this.categoria, this.secuencialpedido, this.nip, this.total.toString(),
                            this.subtotal.toString(), this.subtotaliva.toString(), this.porcentajeiva.toString(), this.descuento.toString(), this.lat.toString(), this.lon.toString(),
                            this.codigoestablecimiento, this.puntoemision, this.tipotransaccion, this.longdate.toString(), this.secuencialsistema});
            if (this.idpedido == 0)
                this.idpedido = SQLite.sqlDB.getLastId();
            else
                sqLiteDatabase.execSQL("DELETE FROM detallepedido WHERE pedidoid = ?", new String[]{String.valueOf(this.idpedido)});
            sqLiteDatabase.close();
            Log.d(TAG, "GUARDO ENCABEZADO - ID: " + this.idpedido);
            return this.SaveDetalle(this.idpedido);
        } catch (SQLException ex) {
            Log.d(TAG, "Save(): " + ex.getMessage());
            return false;
        }
    }

    private boolean SaveDetalle(Integer idpedido) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            for (int i = 0; i < this.detalle.size(); i++) {
                sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                                "detallepedido(pedidoid, orden, cantidad, factorconversion, precio, idproducto, observacion, usuarioid, " +
                                "porcentajeiva, codigoproducto, nombreproducto, descuento, porcentajedesc) " +
                                "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{idpedido.toString(), String.valueOf(i + 1), detalle.get(i).cantidad.toString(), detalle.get(i).factorconversion.toString(),
                                detalle.get(i).precio.toString(), detalle.get(i).producto.idproducto.toString(), detalle.get(i).observacion,
                                String.valueOf(SQLite.usuario.IdUsuario), detalle.get(i).producto.porcentajeiva.toString(), detalle.get(i).producto.codigoproducto,
                                detalle.get(i).producto.nombreproducto, detalle.get(i).descuento.toString(), detalle.get(i).producto.descuento.toString()});

                this.detalle.get(i).pedidoid = idpedido;
            }
            this.actualizasecuencial();
            sqLiteDatabase.close();
            Log.d(TAG, "Guardó detalle comprobante");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "SaveDetalle(): " + ex.getMessage());
            return false;
        }
    }

    public static Pedido get(Integer idpedido) {
        Pedido Item = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM pedido WHERE idpedido = ?", new String[]{idpedido.toString()});
            if (cursor.moveToFirst()) Item = Pedido.AsignaDatos(cursor);
            sqLiteDatabase.close();
        } catch (Exception e) {
            Log.d(TAG, "get(): " + e.getMessage());
        }
        return Item;
    }

    public static ArrayList<Pedido> getByClient(Integer idCliente) {
        ArrayList<Pedido> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM pedido WHERE clienteid = ? ORDER BY estado, idpedido desc", new String[]{idCliente.toString()});
            Pedido Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Pedido.AsignaDatos(cursor);
                    Items.add(Item);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        } catch (Exception e) {
            Log.d(TAG, "getByCliente(): " + e.getMessage());
        }
        return Items;
    }

    public static ArrayList<Pedido> getByUsuario(Integer idUser, Integer establecimientoid, String fechadesde, String fechahasta) {
        ArrayList<Pedido> Items = new ArrayList<>();
        try {
            List<String> listparams = new ArrayList<>();
            String WHERE = "usuarioid = ? and establecimientoid = ? and estado not in (-1) ";
            listparams.add(idUser.toString());
            listparams.add(establecimientoid.toString());
            if (!fechadesde.equals("")) {
                //WHERE += "and (fechapedido = ? or fechacelular like '"+fecha+"%')";
                WHERE += " and longdate >= ?";
                listparams.add(String.valueOf(Utils.longDate(fechadesde)));
                //params =new String[]{ idUser.toString(), establecimientoid.toString(), fecha};
            }
            if (!fechahasta.equals("")) {
                WHERE += " and longdate <= ?";
                listparams.add(String.valueOf(Utils.longDate(fechahasta)));
            }
            String[] itemsArray = new String[listparams.size()];
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM pedido " +
                    "WHERE " + WHERE +
                    "ORDER BY estado asc, idpedido desc", listparams.toArray(itemsArray));
            Pedido Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Pedido.AsignaDatos(cursor);
                    if (Item != null) Items.add(Item);
                } while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
            Log.d(TAG, "NUMERO COMPROBANTES: " + Items.size());
        } catch (SQLiteException e) {
            Log.d(TAG, "getByUsuario(): " + e.getMessage());
        }
        return Items;
    }

    public static ArrayList<Pedido> getPorSincronizar(Integer idUser) {
        ArrayList<Pedido> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM pedido WHERE usuarioid = ? and estado = 1", new String[]{idUser.toString()});
            Pedido Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Pedido.AsignaDatos(cursor);
                    if (Item != null) Items.add(Item);
                } while (cursor.moveToNext());
            }
            cursor.close();
            sqLiteDatabase.close();
            Log.d(TAG, "NUMERO COMPROBANTES PS: " + Items.size());
        } catch (SQLiteException e) {
            Log.d(TAG, "getPorSincronizar(): " + e.getMessage());
        }
        return Items;
    }

    public static int Delete(Integer id, String fechadesde, String fechahasta, Integer primeros, boolean soloSincronizados) {
        int retorno = 0;
        try {
            List<String> listParams = new ArrayList<>();
            String WHERE = "";
            if (id > 0) {
                WHERE = "idpedido = ?";
                listParams.add(id.toString());
            }
            if (!fechadesde.trim().equals("")) {
                WHERE = (WHERE.trim().equals("") ? "" : " AND ") + "longdate >= ?";
                listParams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if (!fechahasta.trim().equals("")) {
                WHERE = (WHERE.trim().equals("") ? "" : " AND ") + "longdate <= ?";
                listParams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if (soloSincronizados)
                WHERE += (WHERE.trim().equals("") ? "" : " AND ") + "codigosistema > 0";
            String[] params = new String[listParams.size()];
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            retorno = sqLiteDatabase.delete("pedido", WHERE, listParams.toArray(params));
            Log.d(TAG, "Registros eliminados: " + retorno);
        } catch (Exception e) {
            Log.d(TAG, "Delete(): " + e.getMessage());
            retorno = 0;
        }
        return retorno;
    }

    public static Pedido AsignaDatos(Cursor cursor) {
        Pedido Item;
        try {
            Item = new Pedido();
            Item.idpedido = cursor.getInt(0);
            Item.codigosistema = cursor.getInt(1);
            Item.cliente = Cliente.get(cursor.getInt(2));
            if (Item.cliente == null)
                Item.cliente = Cliente.get(cursor.getString(13));
            Item.estado = cursor.getInt(3);
            Item.usuarioid = cursor.getInt(4);
            Item.parroquiaid = cursor.getInt(5);
            Item.establecimientoid = cursor.getInt(6);
            Item.secuencial = cursor.getInt(7);
            Item.fechapedido = cursor.getString(8);
            Item.fechacelular = cursor.getString(9);
            Item.observacion = cursor.getString(10);
            Item.categoria = cursor.getString(11);
            Item.secuencialpedido = cursor.getString(12);
            Item.nip = cursor.getString(13);
            Item.total = cursor.getDouble(14);
            Item.subtotal = cursor.getDouble(15);
            Item.subtotaliva = cursor.getDouble(16);
            Item.porcentajeiva = cursor.getDouble(17);
            Item.descuento = cursor.getDouble(18);
            Item.lat = cursor.getDouble(19);
            Item.lon = cursor.getDouble(20);
            Item.codigoestablecimiento = cursor.getString(21);
            Item.puntoemision = cursor.getString(22);
            Item.tipotransaccion = cursor.getString(23);
            Item.longdate = cursor.getLong(24);
            Item.secuencialsistema = cursor.getString(25);
            Item.detalle = DetallePedido.getDetalle(Item.idpedido);
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
            Item = null;
        }
        return Item;
    }

    public String getCodigoTransaccion() {
        String codigo = "";
        try {
            this.secuencial = ultimosecuencial();
            //codigo = "PC-" + this.codigoestablecimiento + "-" + this.puntoemision + "-" + String.format("%09d", this.secuencial);
            codigo = "PC-" + String.format("%03d", this.establecimientoid) + "-" + String.format("%09d", this.secuencial);
            this.secuencialpedido = codigo;
        } catch (Exception e) {
            Log.d(TAG, "getCodigoTransaccion(): " + e.getMessage());
        }
        return codigo;
    }

    public Integer ultimosecuencial() {
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
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO secuencial(secuencial, sucursalid, codigoestablecimiento, puntoemision, tipocomprobante) VALUES(?, ?, ?, ?, ?) ",
                    new String[]{String.valueOf(this.secuencial + 1), this.establecimientoid.toString(), this.codigoestablecimiento, this.puntoemision, this.tipotransaccion});
            //sqLiteDatabase.close();
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "actualizasecuencial(): " + ec.getMessage());
            return false;
        }
        return true;
    }

    public double getTotal() {
        this.total = 0d;
        this.subtotaliva = 0d;
        this.subtotal = 0d;
        this.descuento = 0d;
        for (DetallePedido item : this.detalle) {
            this.total += item.Subtotaliva();
            if (item.producto.porcentajeiva > 0)
                this.subtotaliva += item.Subtotal();
            else
                this.subtotal += item.Subtotal();
            this.descuento += item.descuento;
        }
        return this.total;
    }
}
