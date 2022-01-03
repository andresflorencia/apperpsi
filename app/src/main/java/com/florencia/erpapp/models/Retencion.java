package com.florencia.erpapp.models;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class Retencion {
    public String numeroautorizacion, numerodocumento, periodofiscal, puntoemision, establecimiento, estadosri, fechahora;
    public Integer idretencion, comprobanteid, personaid, ambiente, tipoemision, origenretencion, usuarioid;
    public List<DetalleRetencion> detalle;

    public static SQLiteDatabase sqLiteDatabase;
    public static final String TAG = "TAGRETENCION";

    public Retencion() {
        this.numeroautorizacion = this.numerodocumento = this.periodofiscal
                = this.puntoemision = this.establecimiento = this.estadosri = this.fechahora = "";
        this.idretencion = this.comprobanteid = this.personaid = this.ambiente
                = this.tipoemision = this.origenretencion = this.usuarioid = 0;
        this.detalle = new ArrayList<>();
    }

    public boolean Save() {
        boolean retorno = false;
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                            "retencion(idretencion, comprobanteid, personaid, numeroautorizacion, numerodocumento, " +
                            "periodofiscal, puntoemision, establecimiento, fechahora, usuarioid) " +
                            "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{this.idretencion == 0 ? null : this.idretencion.toString(), this.comprobanteid.toString(), this.personaid.toString(),
                            this.numeroautorizacion, this.numerodocumento, this.periodofiscal, this.puntoemision, this.establecimiento, this.fechahora, this.usuarioid.toString()});
            if (this.idretencion == 0)
                this.idretencion = SQLite.sqlDB.getLastId();
            else
                sqLiteDatabase.execSQL("DELETE FROM detalleretencion WHERE retencionid = ?", new String[]{this.idretencion.toString()});

            retorno = this.SaveDetalle(this.idretencion);

            sqLiteDatabase.close();
            Log.d(TAG, "GUARDO ENCABEZADO - ID: " + this.idretencion);
        } catch (SQLException ex) {
            Log.d(TAG, "Save(): " + ex.getMessage());
            return false;
        }
        return retorno;
    }

    private boolean SaveDetalle(Integer retencionid) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            for (int i = 0; i < this.detalle.size(); i++) {
                sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                                "detalleretencion(retencionid, linea, codigo, codigoretencion, coddocsustento," +
                                "numdocsustento, fechaemisiondocumento, baseimponible, baseimponibleiva, porcentajeretener, " +
                                "porcentajereteneriva, valorretenido, valorretenidoiva, tipo) " +
                                "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{retencionid.toString(), String.valueOf(i+1), detalle.get(i).codigo, detalle.get(i).codigoretencion, detalle.get(i).coddocsustento,
                                detalle.get(i).numdocsustento, detalle.get(i).fechaemisiondocsustento, detalle.get(i).baseimponible.toString(), detalle.get(i).baseimponibleiva.toString(),
                                detalle.get(i).porcentajeretener.toString(), detalle.get(i).porcentajereteneriva.toString(), detalle.get(i).valorretenido.toString(),
                                detalle.get(i).valorretenidoiva.toString(), detalle.get(i).tipo});

                this.detalle.get(i).retencionid = retencionid;
            }
            sqLiteDatabase.close();
            Log.d(TAG, "Guardó detalle retencion");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "SaveDetalle(): " + ex.getMessage());
            return false;
        }
    }

    public static Retencion get(Integer id, boolean byComprobante) {
        Retencion Item = null;
        try {
            String query = "SELECT * FROM retencion WHERE idretencion = ?";
            if(byComprobante)
                query = "SELECT * FROM retencion WHERE comprobanteid = ?";
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery(query,
                    new String[]{id.toString()});
            if (cursor.moveToFirst()) Item = Retencion.AsignaDatos(cursor);
            sqLiteDatabase.close();
        } catch (Exception e) {
            Log.d(TAG, "get(): " + e.getMessage());
        }
        return Item;
    }

    public static Retencion AsignaDatos(Cursor cursor) {
        Retencion Item;
        try {
            Item = new Retencion();
            Item.idretencion = cursor.getInt(0);
            Item.comprobanteid = cursor.getInt(1);
            Item.personaid = cursor.getInt(2);
            Item.ambiente = cursor.getInt(3);
            Item.tipoemision = cursor.getInt(4);
            Item.origenretencion = cursor.getInt(5);
            Item.numeroautorizacion = cursor.getString(6);
            Item.numerodocumento = cursor.getString(7);
            Item.periodofiscal = cursor.getString(8);
            Item.puntoemision = cursor.getString(9);
            Item.establecimiento = cursor.getString(10);
            Item.estadosri = cursor.getString(11);
            Item.fechahora = cursor.getString(12);
            Item.usuarioid = cursor.getInt(13);
            Item.detalle = DetalleRetencion.getDetalle(Item.idretencion);

        } catch (Exception ec) {
            ec.printStackTrace();
            Log.d(TAG, "AsignaDatos(): " + ec.getMessage());
            Item = null;
        }
        return Item;
    }
}
