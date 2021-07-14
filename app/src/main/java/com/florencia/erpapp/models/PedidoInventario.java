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

public class PedidoInventario {
    public Integer idpedido, codigosistema, establecimientoid, usuarioid, diasabastecimiento, estadomovil, secuencial;
    public String codigopedido, fecharegistro, fechahora, estado, observacion, tipotransaccion;
    public Long longdate;
    public List<DetallePedidoInv> detalle;

    public static String TAG = "TAGPEDIDOINV";
    public static SQLiteDatabase sqLiteDatabase;

    public PedidoInventario() {
        this.idpedido = 0;
        this.codigosistema = 0;
        this.establecimientoid = 0;
        this.usuarioid = 0;
        this.diasabastecimiento = 7;
        this.estadomovil = 0;
        this.secuencial = 0;
        this.codigopedido = "";
        this.fecharegistro = "";
        this.fechahora = "";
        this.estado = "P";
        this.observacion = "";
        this.longdate = 0l;
        this.tipotransaccion = "";
        this.detalle = new ArrayList<>();
    }

    public static boolean Update(Integer idpedido, ContentValues values) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.update("pedidoinv", values, "idpedido = ?", new String[]{idpedido.toString()});
            sqLiteDatabase.close();
            Log.d(TAG, "UPDATE PEDIDOINV OK");
            return true;
        } catch (SQLException ex) {
            Log.d(TAG, "Update(): " + String.valueOf(ex));
            return false;
        }
    }

    public boolean Save() {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                            "pedidoinv(idpedido, codigosistema, establecimientoid, usuarioid, diasabastecimiento, estadomovil, " +
                            "secuencial, codigopedido, fecharegistro, fechahora, estado, observacion, longdate, tipotransaccion) " +
                            "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{this.idpedido == 0 ? null : this.idpedido.toString(), this.codigosistema.toString(), this.establecimientoid.toString(), this.usuarioid.toString(),
                            this.diasabastecimiento.toString(), this.estadomovil.toString(), this.secuencial.toString(), this.codigopedido,
                            this.fecharegistro, this.fechahora, this.estado, this.observacion, this.longdate.toString(), this.tipotransaccion});
            if (this.idpedido == 0)
                this.idpedido = SQLite.sqlDB.getLastId();
            else
                sqLiteDatabase.execSQL("DELETE FROM detallepedidoinv WHERE pedidoid = ?", new String[]{String.valueOf(this.idpedido)});
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
                                "detallepedidoinv(pedidoid, orden, productoid, cantidadpedida, cantidadautorizada, " +
                                "stockactual, usuarioid, codigoproducto, nombreproducto) " +
                                "values(?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{idpedido.toString(), String.valueOf(i + 1), detalle.get(i).producto.idproducto.toString(), detalle.get(i).cantidadpedida.toString(),
                                detalle.get(i).cantidadautorizada.toString(), detalle.get(i).stockactual.toString(), String.valueOf(SQLite.usuario.IdUsuario),
                                detalle.get(i).producto.codigoproducto, detalle.get(i).producto.nombreproducto});

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

    public static PedidoInventario get(Integer idpedido) {
        PedidoInventario Item = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM pedidoinv WHERE idpedido = ?", new String[]{idpedido.toString()});
            if (cursor.moveToFirst()) Item = PedidoInventario.AsignaDatos(cursor);
            sqLiteDatabase.close();
        } catch (Exception e) {
            Log.d(TAG, "get(): " + e.getMessage());
        }
        return Item;
    }

    public static ArrayList<PedidoInventario> getByUsuario(Integer idUser, Integer establecimientoid, String fechadesde, String fechahasta) {
        ArrayList<PedidoInventario> Items = new ArrayList<>();
        try {
            List<String> listparams = new ArrayList<>();
            String WHERE = "usuarioid = ? and establecimientoid = ? and estadomovil not in (-1) ";
            listparams.add(idUser.toString());
            listparams.add(establecimientoid.toString());
            if (!fechadesde.equals("")) {
                WHERE += " and longdate >= ?";
                listparams.add(String.valueOf(Utils.longDate(fechadesde)));
            }
            if (!fechahasta.equals("")) {
                WHERE += " and longdate <= ?";
                listparams.add(String.valueOf(Utils.longDate(fechahasta)));
            }
            String[] itemsArray = new String[listparams.size()];
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM pedidoinv " +
                    "WHERE " + WHERE +
                    "ORDER BY estadomovil asc, idpedido desc", listparams.toArray(itemsArray));
            PedidoInventario Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = PedidoInventario.AsignaDatos(cursor);
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

    public static ArrayList<PedidoInventario> getPorSincronizar(Integer idUser) {
        ArrayList<PedidoInventario> Items = new ArrayList<>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM pedidoinv WHERE usuarioid = ? and estadomovil = 1", new String[]{idUser.toString()});
            PedidoInventario Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = PedidoInventario.AsignaDatos(cursor);
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
            retorno = sqLiteDatabase.delete("pedidoinv", WHERE, listParams.toArray(params));
            Log.d(TAG, "Registros eliminados: " + retorno);
        } catch (Exception e) {
            Log.d(TAG, "Delete(): " + e.getMessage());
            retorno = 0;
        }
        return retorno;
    }

    public static PedidoInventario AsignaDatos(Cursor cursor) {
        PedidoInventario Item;
        try {
            Item = new PedidoInventario();
            Item.idpedido = cursor.getInt(0);
            Item.codigosistema = cursor.getInt(1);
            Item.establecimientoid = cursor.getInt(2);
            Item.usuarioid = cursor.getInt(3);
            Item.diasabastecimiento = cursor.getInt(4);
            Item.estadomovil = cursor.getInt(5);
            Item.secuencial = cursor.getInt(6);
            Item.codigopedido = cursor.getString(7);
            Item.fecharegistro = cursor.getString(8);
            Item.fechahora = cursor.getString(9);
            Item.estado = cursor.getString(10);
            Item.observacion = cursor.getString(11);
            Item.longdate = cursor.getLong(12);
            Item.tipotransaccion = cursor.getString(13);
            Item.detalle = DetallePedidoInv.getDetalle(Item.idpedido);
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
            codigo = SQLite.usuario.sucursal.periodo.toString() + SQLite.usuario.sucursal.mesactual +
                    "-PED-" + SQLite.usuario.sucursal.IdSucursal + "-" + String.format("%04d", this.secuencial);
            this.codigopedido = codigo;
            Log.d(TAG, codigo);
        } catch (Exception e) {
            Log.d(TAG, "getCodigoTransaccion(): " + e.getMessage());
        }
        return codigo;
    }

    public Integer ultimosecuencial() {
        sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(
                "SELECT secuencial as sec FROM secuencial WHERE sucursalid = ? AND codigoestablecimiento = ? AND puntoemision = ? AND tipocomprobante = ? ",
                new String[]{this.establecimientoid.toString(), "", "", this.tipotransaccion});
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
                    new String[]{String.valueOf(this.secuencial + 1), this.establecimientoid.toString(), "", "", this.tipotransaccion});
            //sqLiteDatabase.close();
        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "actualizasecuencial(): " + ec.getMessage());
            return false;
        }
        return true;
    }
}
