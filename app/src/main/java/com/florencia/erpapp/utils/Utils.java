package com.florencia.erpapp.utils;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.florencia.erpapp.R;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.DataFormatException;

public class Utils {
    // UNICODE 0x23 = #
    public static final byte[] UNICODE_TEXT = new byte[] {0x23, 0x23, 0x23,
            0x23, 0x23, 0x23,0x23, 0x23, 0x23,0x23, 0x23, 0x23,0x23, 0x23, 0x23,
            0x23, 0x23, 0x23,0x23, 0x23, 0x23,0x23, 0x23, 0x23,0x23, 0x23, 0x23,
            0x23, 0x23, 0x23};

    private static String hexStr = "0123456789ABCDEF";
    private static String[] binaryArray = { "0000", "0001", "0010", "0011",
            "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011",
            "1100", "1101", "1110", "1111" };

    public static byte[] decodeBitmap(Bitmap bmp){
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();

        List<String> list = new ArrayList<String>(); //binaryString list
        StringBuffer sb;


        int bitLen = bmpWidth / 8;
        int zeroCount = bmpWidth % 8;

        String zeroStr = "";
        if (zeroCount > 0) {
            bitLen = bmpWidth / 8 + 1;
            for (int i = 0; i < (8 - zeroCount); i++) {
                zeroStr = zeroStr + "0";
            }
        }

        for (int i = 0; i < bmpHeight; i++) {
            sb = new StringBuffer();
            for (int j = 0; j < bmpWidth; j++) {
                int color = bmp.getPixel(j, i);

                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;

                // if color close to white，bit='0', else bit='1'
                if (r > 160 && g > 160 && b > 160)
                    sb.append("0");
                else
                    sb.append("1");
            }
            if (zeroCount > 0) {
                sb.append(zeroStr);
            }
            list.add(sb.toString());
        }

        List<String> bmpHexList = binaryListToHexStringList(list);
        String commandHexString = "1D763000";
        String widthHexString = Integer
                .toHexString(bmpWidth % 8 == 0 ? bmpWidth / 8
                        : (bmpWidth / 8 + 1));
        if (widthHexString.length() > 2) {
            Log.e("decodeBitmap error", " width is too large");
            return null;
        } else if (widthHexString.length() == 1) {
            widthHexString = "0" + widthHexString;
        }
        widthHexString = widthHexString + "00";

        String heightHexString = Integer.toHexString(bmpHeight);
        if (heightHexString.length() > 2) {
            Log.e("decodeBitmap error", " height is too large");
            return null;
        } else if (heightHexString.length() == 1) {
            heightHexString = "0" + heightHexString;
        }
        heightHexString = heightHexString + "00";

        List<String> commandList = new ArrayList<String>();
        commandList.add(commandHexString+widthHexString+heightHexString);
        commandList.addAll(bmpHexList);

        return hexList2Byte(commandList);
    }

    public static List<String> binaryListToHexStringList(List<String> list) {
        List<String> hexList = new ArrayList<String>();
        for (String binaryStr : list) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < binaryStr.length(); i += 8) {
                String str = binaryStr.substring(i, i + 8);

                String hexString = myBinaryStrToHexString(str);
                sb.append(hexString);
            }
            hexList.add(sb.toString());
        }
        return hexList;

    }

    public static String myBinaryStrToHexString(String binaryStr) {
        String hex = "";
        String f4 = binaryStr.substring(0, 4);
        String b4 = binaryStr.substring(4, 8);
        for (int i = 0; i < binaryArray.length; i++) {
            if (f4.equals(binaryArray[i]))
                hex += hexStr.substring(i, i + 1);
        }
        for (int i = 0; i < binaryArray.length; i++) {
            if (b4.equals(binaryArray[i]))
                hex += hexStr.substring(i, i + 1);
        }

        return hex;
    }

    public static byte[] hexList2Byte(List<String> list) {
        List<byte[]> commandList = new ArrayList<byte[]>();

        for (String hexStr : list) {
            commandList.add(hexStringToBytes(hexStr));
        }
        byte[] bytes = sysCopy(commandList);
        return bytes;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    public static byte[] sysCopy(List<byte[]> srcArrays) {
        int len = 0;
        for (byte[] srcArray : srcArrays) {
            len += srcArray.length;
        }
        byte[] destArray = new byte[len];
        int destLen = 0;
        for (byte[] srcArray : srcArrays) {
            System.arraycopy(srcArray, 0, destArray, destLen, srcArray.length);
            destLen += srcArray.length;
        }
        return destArray;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String getDateNow(){
        String fecha="";
        try {
            Locale idioma = new Locale("es", "ES");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", idioma);
            Date date = new Date();
            fecha = dateFormat.format(date);
        }catch (Exception e){
            e.printStackTrace();
        }
        return fecha;
    }


    public static String getDateFormat(String formato){
        String fecha = "";
        try {
            Locale idioma = new Locale("es", "ES");
            SimpleDateFormat sdf = new SimpleDateFormat(formato,idioma);
            fecha = sdf.format(new Date());
        }catch (Exception e){
            fecha="";
        }
        return fecha;
    }

    public static void showMessage(Context context, String Message){
        try{
            Toast.makeText(context, Message, Toast.LENGTH_LONG).show();
        }catch (Exception e){
            Toast.makeText(context, "MostrarError(): "+ e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    public static void showMessageShort(Context context, String Message){
        try{
            Toast.makeText(context, Message, Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Toast.makeText(context, "MostrarError(): "+ e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static Double RoundDecimal(Double numero, Integer numeroDecimales) {
        return Math.round(numero * Math.pow(10, numeroDecimales)) / Math.pow(10, numeroDecimales);
    }

    public static void verificarPermisos(Activity context){
        ArrayList<String> pe = new ArrayList<>();
        try{
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                pe.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                pe.add(Manifest.permission.ACCESS_FINE_LOCATION);
                pe.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            //if(ActivityCompat.checkSelfPermission(context,Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
              //  pe.add(Manifest.permission.READ_CONTACTS);
            if(ActivityCompat.checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pe.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                pe.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            if(ActivityCompat.checkSelfPermission(context,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                pe.add(Manifest.permission.CAMERA);

            if(ActivityCompat.checkSelfPermission(context,Manifest.permission.INSTALL_PACKAGES) != PackageManager.PERMISSION_GRANTED)
                pe.add(Manifest.permission.INSTALL_PACKAGES);

            if(pe.size()>0) {
                String[] permisos = new String[pe.size()];
                for (int i=0; i<pe.size();i++)
                    permisos[i]=pe.get(i);
                ActivityCompat.requestPermissions(context, permisos, 1000);
            }

        }catch (Exception e){
            Log.d("TAGP",e.getMessage());
        }
    }


    //metodo verficar conexion a internet
    public static boolean verificaConexion(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null; //connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }
    //fin de "metodo verficar conexion a internet"
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
    public static boolean verificaConexion1(Context ctx) {
        boolean bConectado = false;
        ConnectivityManager connec = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        // No sólo wifi, también GPRS
        NetworkInfo[] redes = null; //connec.getAllNetworkInfo();
        // este bucle debería no ser tan ñapa
        for (int i = 0; i < 2; i++) {
            // ¿Tenemos conexión? ponemos a true
            if ((redes[i].getState() == NetworkInfo.State.CONNECTED)) {
                bConectado = true;
            }
        }
        return bConectado;
    }
    public static boolean isOnlineNet( String IPServer) {

        try {
            Process p = Runtime.getRuntime().getRuntime().exec(Constants.PING_Server + IPServer);
            int val           = p.waitFor();
            boolean reachable = (val == 0);
            return reachable;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    ///// Animacion para los CardView a medida que se cargan o eliminan de la Vista /////
    public static void animateCircularReveal(View view) {
        int centerX = 0;
        int centerY = 0;
        int startRadius = 0;
        int endRadius = Math.max(view.getWidth(), view.getHeight());
        Animator animation = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius);
        view.setVisibility(View.VISIBLE);
        animation.start();
    }
    /////// Fin Animacion ///////////////


    public static String FormatoMoneda(Double valor, int numeroDecimales){
        String retorno = "$0,00";
        try {
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "EC"));
            valor = RoundDecimal(valor, numeroDecimales);
            retorno = format.format(valor);
        }catch (Exception e){
            Log.d("TAGERROR","FormatoMoneda(): " + e.getMessage());
        }
        return retorno;
    }

    public static long longDate(String fecha){
        long lon =0;
        try {
            Locale idioma = new Locale("es", "ES");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", idioma);
            Date date =dateFormat.parse(fecha);
            lon = date.getTime();
        }catch (ParseException e){
            lon = 0;
            Log.d("TAGFECHA",e.getMessage());
        }
        return lon;

    }

    public static void MostrarTeclado(Context context, EditText et){
        et.requestFocus();//Asegurar que editText tiene focus
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(et, InputMethodManager.SHOW_FORCED);
    }
    public static void EfectoLayout(LinearLayout lyEfecto){
        if (lyEfecto.getVisibility() == View.GONE){
            lyEfecto.setVisibility(View.VISIBLE);
        }else if (lyEfecto.getVisibility() == View.VISIBLE){
            lyEfecto.setVisibility(View.GONE);
        }
    }

    public static void EfectoLayout(View lyEfecto, TextView lblEfecto){
        if (lyEfecto.getVisibility() == View.GONE){
            lyEfecto.setVisibility(View.VISIBLE);
            lblEfecto.setCompoundDrawablesWithIntrinsicBounds( 0, 0, R.drawable.ic_row_up,0);
        }else if (lyEfecto.getVisibility() == View.VISIBLE){
            lyEfecto.setVisibility(View.GONE);
            lblEfecto.setCompoundDrawablesWithIntrinsicBounds( 0, 0, R.drawable.ic_row_down,0);
        }
    }

    public static void showErrorDialog(Activity context, String title, String message){
        try{
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(context).inflate(R.layout.layout_error_dialog,
                    (ConstraintLayout) context.findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_info);
            ((TextView) view.findViewById(R.id.lblTitle)).setText(title);
            ((TextView) view.findViewById(R.id.lblMessage)).setText(message);
            ((Button)view.findViewById(R.id.btnAction)).setText("Ok");
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnAction).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {alertDialog.dismiss();}
            });
            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
        }catch (Exception e){
            Log.d("TAGUTILS","showErrorDialog(): " + e.getMessage());
        }
    }

    public static void showSuccessDialog(Activity context, String title, String message, boolean finishActivity, boolean returnOK){
        try{
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(context).inflate(R.layout.layout_success_dialog,
                    (ConstraintLayout) context.findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_info);
            ((TextView) view.findViewById(R.id.lblTitle)).setText(title);
            ((TextView) view.findViewById(R.id.lblMessage)).setText(message);
            ((Button)view.findViewById(R.id.btnAction)).setText("Ok");
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnAction).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(returnOK){
                        context.setResult(Activity.RESULT_OK);
                        context.finish();
                    }else if(finishActivity)
                        context.finish();
                    else
                        alertDialog.dismiss();
                }
            });
            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
        }catch (Exception e){
            Log.d("TAGUTILS","showSuccessDialog(): " + e.getMessage());
        }
    }

    public static String CambiarFecha(String fecha, int tipo, int cantidad) {
        String retorno = "";
        try {
            Locale idioma = new Locale("es", "ES");
            long fechaActual = Utils.longDate(fecha);
            Calendar c = Calendar.getInstance(idioma);
            c.setTimeInMillis(fechaActual);
            c.add(tipo, cantidad);
            Date d = c.getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", idioma);
            retorno = dateFormat.format(d);
        }catch (Exception e) {
            Log.d("TAGUTILS", "SumarFecha(): " + e.getMessage());
        }
        return retorno;
    }

    public static boolean CopyToClipboard(Context c, String text){
        boolean retorno = false;
        try{
            ClipboardManager clipboard = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text",  text);
            clipboard.setPrimaryClip(clip);
            showMessage(c, "Enlace de descarga copiado al portapapeles");
            retorno = true;
        }catch (Exception e){
            retorno = false;
        }
        return retorno;
    }

    public static String getMes(int mes, boolean abreviatura){
        String[] meses = new String[]{"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        String[] meses_ab = new String[]{"Ene", "Feb", "Mar", "Abr", "May", "Jun",
                "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        return abreviatura ? meses_ab[mes] : meses[mes];
    }

    public static String fechaMes(String fecha) {
        if(fecha.equals(""))
            fecha = Utils.getDateFormat("yyyy-MM-dd");
        String[] newF = fecha.split("-");
        fecha = newF[2] + "-" + Utils.getMes(Integer.valueOf( newF[1])-1,true) + "-" + newF[0];
        return fecha;
    }
}
