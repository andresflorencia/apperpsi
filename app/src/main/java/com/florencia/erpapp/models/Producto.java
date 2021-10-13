package com.florencia.erpapp.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.util.Log;

import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Producto {
    public Integer idproducto;
    public String codigoproducto, nombreproducto, detalleproducto, tipo, nombreclasificacion;
    public Integer unidadid, unidadesporcaja, iva, ice, establecimientoid, clasificacionid;
    public Double factorconversion, pvp, pvp1, pvp2, pvp3, pvp4, pvp5, stock, porcentajeiva, preciocosto, descuento;
    public String numerolote, fechavencimiento;
    public List<Lote> lotes;
    public List<Regla> reglas;
    public List<PrecioCategoria> precioscategoria;

    public static SQLiteDatabase sqLiteDatabase;
    private static String TAG = "TAGPRODUCTO";

    public Producto() {
        this.idproducto = 0;
        this.codigoproducto = "";
        this.nombreproducto = "";
        this.detalleproducto = "";
        this.pvp = 0d;
        this.unidadid = 0;
        this.unidadesporcaja = 0;
        this.iva = 0;
        this.ice = 0;
        this.factorconversion = 0d;
        this.pvp1 = 0d;
        this.pvp2 = 0d;
        this.pvp3 = 0d;
        this.pvp4 = 0d;
        this.pvp5 = 0d;
        this.stock = 0d;
        this.porcentajeiva = 0d;
        this.numerolote = "";
        this.preciocosto = 0d;
        this.fechavencimiento = "";
        this.establecimientoid = 0;
        this.tipo = "";
        this.lotes = new ArrayList<>();
        this.reglas = new ArrayList<>();
        this.precioscategoria = new ArrayList<>();
        this.clasificacionid = 0;
        this.nombreclasificacion = "";
        this.descuento = 0d;
    }

    public Double getPrecioSugerido(boolean aplicadesc) {
        Double pvpR = this.getPrecio("R");
        if (this.precioscategoria.size() > 0) {
            for (PrecioCategoria pc : this.precioscategoria) {
                if (pc.categoriaid.equals(0)) {
                    pvpR = pc.valor;
                    break;
                }
            }
        }
        if(aplicadesc)
            pvpR -= Utils.RoundDecimal(pvpR * this.descuento/100, 2);
        return pvpR + (pvpR * this.porcentajeiva / 100);
    }

    public boolean Save() {
        try {
            this.sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            this.sqLiteDatabase.execSQL("INSERT OR REPLACE INTO " +
                            "producto(idproducto, codigoproducto, nombreproducto, pvp, unidadid, unidadesporcaja, iva," +
                            "ice, factorconversion, pvp1, pvp2, pvp3, pvp4, pvp5, stock, porcentajeiva, establecimientoid, " +
                            "tipo, clasificacionid, nombreclasificacion, descuento) " +
                            "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{this.idproducto.toString(), this.codigoproducto, this.nombreproducto, this.pvp.toString(),
                            this.unidadid.toString(), this.unidadesporcaja.toString(), this.iva.toString(),
                            this.ice.toString(), this.factorconversion.toString(), this.pvp1.toString(), this.pvp2.toString(),
                            this.pvp3.toString(), this.pvp4.toString(), this.pvp5.toString(), this.stock.toString(),
                            this.porcentajeiva.toString(), this.establecimientoid.toString(), this.tipo, this.clasificacionid.toString(),
                            this.nombreclasificacion, this.descuento.toString()});
            this.sqLiteDatabase.close();
            Log.d("TAGPRODUCTO", "SAVE PRODUCTO OK");
            Lote.InsertMultiple(this.idproducto, this.establecimientoid, this.lotes);
            Regla.InsertMultiple(this.idproducto, this.establecimientoid, this.reglas);
            PrecioCategoria.InsertMultiple(this.idproducto, this.establecimientoid, this.precioscategoria);
            return true;
        } catch (SQLException ex) {
            Log.d(TAG, ex.getMessage());
            return false;
        }
    }

    public static Producto AsignaDatos(Cursor cursor) {
        Producto Item = null;
        try {
            Item = new Producto();
            Item.idproducto = cursor.getInt(0);
            Item.codigoproducto = cursor.getString(1);
            Item.nombreproducto = cursor.getString(2);
            Item.pvp = cursor.getDouble(3);
            Item.unidadid = cursor.getInt(4);
            Item.unidadesporcaja = cursor.getInt(5);
            Item.iva = cursor.getInt(6);
            Item.ice = cursor.getInt(7);
            Item.factorconversion = cursor.getDouble(8);
            Item.pvp1 = cursor.getDouble(9);
            Item.pvp2 = cursor.getDouble(10);
            Item.pvp3 = cursor.getDouble(11);
            Item.pvp4 = cursor.getDouble(12);
            Item.pvp5 = cursor.getDouble(13);
            Item.stock = cursor.getDouble(14);
            Item.porcentajeiva = cursor.getDouble(15);
            Item.establecimientoid = cursor.getInt(16);
            Item.tipo = cursor.getString(17);
            Item.clasificacionid = cursor.getInt(18);
            Item.nombreclasificacion = cursor.getString(19);
            Item.descuento = cursor.getDouble(20);
            Item.lotes = Lote.getAll(Item.idproducto, Item.establecimientoid);
            Item.reglas = Regla.getAll(Item.idproducto, SQLite.usuario.establecimiento_fact);
            Item.precioscategoria = PrecioCategoria.getAll(Item.idproducto, SQLite.usuario.establecimiento_fact);
        } catch (Exception e) {
            Log.d(TAG, "Asigna(): " + e.getMessage());
        } finally {
        }
        return Item;
    }

    public static Producto AsignaDatosLote(Cursor cursor) throws Exception {
        Producto Item = null;
        try {
            Item = new Producto();
            Item.idproducto = cursor.getInt(0);
            Item.codigoproducto = cursor.getString(1);
            Item.nombreproducto = cursor.getString(2);
            Item.pvp = cursor.getDouble(3);
            Item.unidadid = cursor.getInt(4);
            Item.unidadesporcaja = cursor.getInt(5);
            Item.iva = cursor.getInt(6);
            Item.ice = cursor.getInt(7);
            Item.factorconversion = cursor.getDouble(8);
            Item.pvp1 = cursor.getDouble(9);
            Item.pvp2 = cursor.getDouble(10);
            Item.pvp3 = cursor.getDouble(11);
            Item.pvp4 = cursor.getDouble(12);
            Item.pvp5 = cursor.getDouble(13);
            Item.stock = cursor.getDouble(14);
            Item.porcentajeiva = cursor.getDouble(15);
            Item.establecimientoid = cursor.getInt(16);
            Item.tipo = cursor.getString(17);
            Item.clasificacionid = cursor.getInt(18);
            Item.nombreclasificacion = cursor.getString(19);
            Lote milote = new Lote();
            milote.productoid = cursor.getInt(21);
            milote.numerolote = cursor.getString(22);
            milote.stock = cursor.getDouble(23);
            milote.preciocosto = cursor.getDouble(24);
            milote.fechavencimiento = cursor.getString(25);
            milote.longdate = cursor.getLong(26);
            milote.establecimientoid = cursor.getInt(27);
            Item.lotes.add(milote);
            //Item.lotes = Lote.getAll(Item.idproducto, Item.establecimientoid);
        } catch (Exception e) {
            Log.d(TAG, "AsignaLote(): " + e.getMessage());
        } finally {

        }
        return Item;
    }

    public static Producto get(Integer id, Integer establecimientoid) {
        Producto Item = null;
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM producto WHERE idproducto  = ? and establecimientoid = ?", new String[]{id.toString(), establecimientoid.toString()});
            if (cursor.moveToFirst()) Item = Producto.AsignaDatos(cursor);
            sqLiteDatabase.close();
        } catch (Exception e) {
            Log.d(TAG, "get():" + e.getMessage());
        }
        return Item;
    }

    public static boolean Update(Integer idproducto, Integer establecimientoid, ContentValues data) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.update("producto", data, "idproducto = ? and establecimientoid = ?", new String[]{idproducto.toString(), establecimientoid.toString()});
            //sqLiteDatabase.close();
            Log.d(TAG, "UPDATE 1 LOTE OK");
            return true;
        } catch (SQLException ex) {
            Log.d(TAG, String.valueOf(ex));
            return false;
        }
    }

    public static boolean Delete(Integer idestablecimiento) {
        try {
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            sqLiteDatabase.delete("producto", "establecimientoid = ?", new String[]{idestablecimiento.toString()});
            //sqLiteDatabase.close();
            Log.d(TAG, "DELETE PRODUCTOS OK");
            return true;
        } catch (SQLException ex) {
            Log.d(TAG, "Delete(): " + ex.getMessage());
            return false;
        }
    }

    public static ArrayList<Producto> getAll(Integer idestablecimiento, boolean isVenta) {
        ArrayList<Producto> Items = new ArrayList<Producto>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            String query = "SELECT * FROM producto WHERE establecimientoid = ? ";
            if (isVenta)
                query += " AND (tipo = 'S' OR stock > 0) ";
            Cursor cursor = sqLiteDatabase.rawQuery(
                    query + "ORDER BY stock DESC, nombreproducto",
                    new String[]{idestablecimiento.toString()});
            Producto Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Producto.AsignaDatos(cursor);
                    Items.add(Item);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        } catch (Exception e) {
            Log.d(TAG, "getAll(): " + e.getMessage());
        }
        return Items;
    }

    public static ArrayList<Producto> getForTransferencia(Integer idestablecimiento) {
        ArrayList<Producto> Items = new ArrayList<Producto>();
        try {
            sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM producto pro " +
                    "JOIN lote lo ON lo.productoid = pro.idproducto AND pro.establecimientoid = lo.establecimientoid AND lo.stock > 0 " +
                    "WHERE pro.establecimientoid = ? AND pro.tipo = ? ORDER BY nombreproducto, longdate, numerolote", new String[]{idestablecimiento.toString(), "P"});
            Producto Item;
            if (cursor.moveToFirst()) {
                do {
                    Item = Producto.AsignaDatosLote(cursor);
                    Items.add(Item);
                } while (cursor.moveToNext());
            }
            sqLiteDatabase.close();
        } catch (Exception e) {
            Log.d(TAG, "getForTransferencia(): " + e.getMessage());
        }
        return Items;
    }

    public static List<Categoria> getCategorias(Integer establecimientoid, String tipobusqueda) {
        List<Categoria> categorias = new ArrayList<>();
        String WHERE = "";
        try {
            if (tipobusqueda.equals("01")) //FACTURACION
                WHERE = " AND (tipo = 'S' OR stock > 0) ";
            if (tipobusqueda.equals("4,20") | tipobusqueda.equals("20,4")) //TRANSFERENCIAS
                WHERE = " AND tipo = 'P' AND stock > 0 ";
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT DISTINCT clasificacionid, nombreclasificacion, count(clasificacionid) AS cantidad FROM producto " +
                            "WHERE establecimientoid = ? " + WHERE + " GROUP BY clasificacionid, nombreclasificacion ORDER BY nombreclasificacion",
                    new String[]{establecimientoid.toString()});
            Integer total = 0;
            Categoria micategoria = new Categoria();
            micategoria.categoriaid = -1;
            micategoria.nombrecategoria = "Todos";
            micategoria.seleccionado = true;
            categorias.add(micategoria);
            if (cursor.moveToFirst()) {
                do {
                    micategoria = new Categoria();
                    micategoria.categoriaid = cursor.getInt(0);
                    micategoria.nombrecategoria = cursor.getString(1);
                    micategoria.cantidad = cursor.getInt(2);
                    total += micategoria.cantidad;
                    categorias.add(micategoria);
                } while (cursor.moveToNext());
            }
            categorias.get(0).cantidad = total;
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception e) {
            Log.d(TAG, "getCategorias(): ".concat(e.getMessage()));
        }
        return categorias;
    }

    public static double getStock(Integer productoid, Integer establecimientoid){
        double retorno = 0d;
        try{
            sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery(
                    "SELECT stock FROM producto WHERE idproducto = ? and establecimientoid = ?",
                    new String[]{productoid.toString(), establecimientoid.toString()});
            if (cursor.moveToFirst()) {
                retorno = cursor.getDouble(0);
                cursor.close();
                sqLiteDatabase.close();
            }
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
        return retorno;

    }

    public double getPrecio(int pos) {
        double[] precios = {
                this.pvp1,
                this.pvp2,
                this.pvp3,
                this.pvp4,
                this.pvp5
        };
        //Arrays.sort(precios);
        return precios[pos];
    }

    public double getPrecio(String categoria) {
        HashMap<String, Double> precios = new HashMap<>();
        precios.put("A", this.pvp1);
        precios.put("B", this.pvp2);
        precios.put("C", this.pvp3);
        precios.put("D", this.pvp4);
        precios.put("E", this.pvp5);
        precios.put("R", this.pvp); //Precio referencia
        //Arrays.sort(precios);
        return precios.get(categoria);
    }

    public String Codigo() {
        return String.valueOf(this.idproducto);
    }

    @Override
    public String toString() {
        return this.nombreproducto;
    }
}