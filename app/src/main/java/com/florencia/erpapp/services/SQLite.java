package com.florencia.erpapp.services;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.florencia.erpapp.models.Configuracion;
import com.florencia.erpapp.models.Usuario;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class SQLite extends SQLiteOpenHelper {
    public static String MensajeError = "Si desea continuar, los siguientes campos deben ser revisados:" + "\n";
    private static String DB_NAME = "sanisidro.db";
    public static SQLite sqlDB;
    public static Usuario usuario;
    public static Configuracion configuracion;
    public static GPSTracker gpsTracker;
    public static String newversion = "";
    public static String linkdescarga = "";


    public SQLite(Context context){
        super(context, DB_NAME, null, 2);
        SQLite.usuario = null;
    }

    public SQLite(Context context, boolean servicio){
        super(context, DB_NAME, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Se ejecuta la sentencia SQL de creación de la tabla
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {
        //NOTA: Por simplicidad del ejemplo aquí utilizamos directamente la opción de
        //      eliminar la tabla anterior y crearla de nuevo vacía con el nuevo formato.
        //      Sin embargo lo normal será que haya que migrar datos de la tabla antigua
        //      a la nueva, por lo que este método debería ser más elaborado.

        //Se elimina la versión anterior de la tabla
    }

    public int getLastId()
    {
        int Id = 0;
        try {
            SQLiteDatabase sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT last_insert_rowid()", null);
            if (cursor.moveToFirst()) Id = cursor.getInt(0);
            cursor.close();
        }catch (Exception ec){
            ec.printStackTrace();
        }
        return Id;
    }

    public static int getLastId(String tblSource, String strColumn,String strWhere){
        int Id = 1;
        try {
            SQLiteDatabase sqLiteDatabase = SQLite.sqlDB.getReadableDatabase();
            Cursor cursor;
            cursor = sqLiteDatabase.rawQuery("SELECT MAX(" + strColumn + ") + 1 AS Codigo FROM " + tblSource + (!strWhere.equals("") ? " WHERE " + strWhere: ""), null);
            if (cursor.moveToFirst()) Id = cursor.getInt(0);
            cursor.close();
            if (Id == 0) Id = 1;
        }catch (Exception ec) {
            ec.printStackTrace();
        }
        return Id;
    }


    private static char[] hextable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String md5(String s)
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes(), 0, s.length());
            byte[] bytes = digest.digest();
            String hash = "";
            for (byte aByte : bytes) {
                int di = (aByte + 256) & 0xFF;
                hash = hash + hextable[(di >> 4) & 0xF] + hextable[di & 0xF];
            }

            return hash;
        }
        catch (NoSuchAlgorithmException ignored)
        {
        }

        return "";
    }

    public static double DoubleVal(String s){
        double nD = 0;
        try {
            nD = Double.valueOf(s);
        } catch (Exception ec){ ec.printStackTrace();}
        return nD;
    }

    public static int IntVal(String s){
        int nD = 0;
        try {
            nD = Integer.valueOf(s);
        } catch (Exception ec){ ec.printStackTrace();}
        return nD;
    }

    public void
    SQLEstructura(String S){
        SQLiteDatabase sqLiteDatabase;
        sqLiteDatabase = SQLite.sqlDB.getWritableDatabase();
        for (String str: S.split(";")) {
            try {
                sqLiteDatabase.execSQL(str);
            }catch (Exception ec) {
                ec.printStackTrace();
            }
        }
        sqLiteDatabase.close();
    }

    public static void setTextView(View v, int ResourceID, String Value){
        try {
            TextView txtOrigen = v.findViewById(ResourceID);
            txtOrigen.setText(Value);
        }
        catch (Exception ec){
            ec.printStackTrace();
        }
    }

    public static void setTextView(Activity v, String Value, int ResourceID){
        try {
            TextView txtOrigen = (TextView) v.findViewById(ResourceID);
            txtOrigen.setText(Value);
        }
        catch (Exception ec){
            ec.printStackTrace();
        }
    }


    public static void setText(View v, int ResourceID, String Value){
        try {
            EditText txtOrigen = (EditText) v.findViewById(ResourceID);
            txtOrigen.setText(Value);
        }
        catch (Exception ec){
            ec.printStackTrace();
        }
    }

    public static void setText(Activity v, int ResourceID, String Value){
        try {
            EditText txtOrigen = v.findViewById(ResourceID);
            txtOrigen.setText(Value);
        }
        catch (Exception ec){
            ec.printStackTrace();
        }
    }

    public static String getText(View v, int ResourceID)
    {
        try {
            EditText txtOrigen = v.findViewById(ResourceID);
            return txtOrigen.getText().toString();
        } catch (Exception ec)
        {
            ec.printStackTrace();
            return "";
        }
    }

    public static String getText(Activity act, int ResourceID)
    {
        try {
            EditText txtOrigen = act.findViewById(ResourceID);
            return txtOrigen.getText().toString();
        } catch (Exception ec)
        {
            ec.printStackTrace();
            return "";
        }
    }

    public static String getSpinner(View v, int ResourceID){
        try {
            Spinner txtOrigen = v.findViewById(ResourceID);
            return txtOrigen.getSelectedItem().toString();
        }
        catch (Exception ec){ return ""; }
    }

    public static String getSpinner(Activity v, int ResourceID){
        try {
            Spinner txtOrigen = v.findViewById(ResourceID);
            Object obj = txtOrigen.getSelectedItem();
            return DataSpinner(txtOrigen);
        }
        catch (Exception ec){
            return "";
        }
    }

    private static String DataSpinner(Spinner spinner){
        Object obj = spinner.getSelectedItem();
        return  obj.toString();
    }

    public static String getSpinner(Spinner spinner){
        try {
            return spinner.getSelectedItem().toString();
        }
        catch (Exception ec){ return ""; }
    }

    public static void setSpinner(Spinner spinner, String Value, int ArrayResource){
        try {
            if (Value.trim().equals("")) return;
            String[] arrayAdapter = spinner.getRootView().getResources().getStringArray(ArrayResource);
            int i = 0, c = 0;
            for (String item: arrayAdapter) {
                if (item.equals(Value)) { i = c; break; }
                c++;
            }
            spinner.setSelection(i);
        }
        catch (Exception ec){
            ec.printStackTrace();
        }
    }

    public static void setSpinner(View v, int ResourceID, String Value, int ArrayResource){
        try {
            if (Value.trim().equals("")) return;
            Spinner txtOrigen = v.findViewById(ResourceID);
            String[] arrayAdapter = v.getResources().getStringArray(ArrayResource);
            int i = 0, c = 0;
            for (String item: arrayAdapter) {
                if (item.equals(Value)) { i = c; break; }
                c++;
            }
            txtOrigen.setSelection(i);
        }
        catch (Exception ec){
            ec.printStackTrace();
        }
    }

    public static void setSpinner(View v, int ResourceID, Object value){
        try {
            if (value == null) return;
            Spinner txtOrigen = (Spinner) v.findViewById(ResourceID);
            for (int i =0; i< txtOrigen.getAdapter().getCount(); i++)
                if (value.equals(txtOrigen.getAdapter().getItem(i))) txtOrigen.setSelection(i);
        }
        catch (Exception ec){
            ec.printStackTrace();
        }
    }

    private static void disableitems(ViewGroup vg){
        for (int i = 0; i < vg.getChildCount(); i++){
            View child = vg.getChildAt(i);
            if (child instanceof ViewGroup){
                disableitems((ViewGroup)child);
            } else {
                child.setEnabled(false);
            }
        }
    }

    public static int DigitoVerificador(String str) {
        int sum = 0;
        int mul = 2;
        for(int i = str.length(); i > 0; i--) {
            sum += SQLite.IntVal(str.substring(i-1, i)) * mul;
            mul += 1;
            if (mul > 7) mul = 2;
        }
        int mod = sum % 11;
        if (mod <= 1) return mod; else return 11 - mod;
    }

    public ArrayList llenar_lv(String usuario){
        ArrayList<String> lista = new ArrayList<>();
        SQLiteDatabase database = this.getWritableDatabase();
        String q = "SELECT * FROM comprobante WHERE clienteid =?";
//        new String[]{this.Codigo()}
        Cursor registros = database.rawQuery(q,new String[]{usuario});
        if (registros.moveToFirst()){
            do{
                lista.add(registros.getString(0));

            }while (registros.moveToNext());
        }

        return lista;
    }
}

