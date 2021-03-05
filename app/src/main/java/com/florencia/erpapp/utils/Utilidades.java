package com.florencia.erpapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.florencia.erpapp.R;
import com.florencia.erpapp.services.SQLite;

import java.util.Calendar;

public class Utilidades {
    static public void ActionBarFormat(ActionBar actionBar) {
        try {
            if (actionBar == null) return;
            actionBar.setIcon(R.mipmap.ic_launcher);
            actionBar.setDisplayShowHomeEnabled(true);
        } finally { }
    }

    static public void ActionBarFormat(ActionBar actionBar, String SubTitle) {
        ActionBarFormat(actionBar);
        try
        {
            actionBar.setSubtitle(SubTitle);
        }finally {

        }
    }
    static public void createdb(Context context){
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbUsuario));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbSucursal));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbSecuencial));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbProducto));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbCliente));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbLote));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbComprobante));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbDetalleComprobante));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbPedido));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbDetallePedido));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbPermiso));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbConfig));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbReglaPrecio));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbProvincia));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbCanton));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbParroquia));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbPedidoInv));
        SQLite.sqlDB.SQLEstructura(context.getString(R.string.dbDetallePedidoInv));
    }

    public static String[] getDateTime() {
        final Calendar c = Calendar.getInstance();
        String dateTime [] = new String[2];
        dateTime[0] = c.get(Calendar.DAY_OF_MONTH) +"/"+ c.get(Calendar.MONTH) +"/"+ c.get(Calendar.YEAR);
        dateTime[1] = c.get(Calendar.HOUR_OF_DAY) +":"+ c.get(Calendar.MINUTE);
        return dateTime;
    }

    public static void checkPermiso(Activity thisActivity, @NonNull String permission, int r) {
        if (ContextCompat.checkSelfPermission(thisActivity, permission)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    permission)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(thisActivity,
                        new String[]{permission},
                        r);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }

    }
}
