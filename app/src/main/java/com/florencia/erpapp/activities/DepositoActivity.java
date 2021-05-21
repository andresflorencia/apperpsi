package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.florencia.erpapp.BuildConfig;
import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.FotoAdapter;
import com.florencia.erpapp.interfaces.IComprobante;
import com.florencia.erpapp.interfaces.IUsuario;
import com.florencia.erpapp.models.Catalogo;
import com.florencia.erpapp.models.DetalleIngreso;
import com.florencia.erpapp.models.Foto;
import com.florencia.erpapp.models.Ingreso;
import com.florencia.erpapp.models.Usuario;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.shasin.notificationbanner.Banner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Url;

public class DepositoActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TAGDEPOSITO_ACT";
    private static final int REQUEST_NEW_FOTO = 20;
    private static final int REQUEST_SELECCIONA_FOTO = 30;

    Button btnFechaVenta, btnFechaDocumento;
    ImageButton btnRefresh;
    public Button btnCargaDocumento;
    EditText txtMonto, txtNumDocumento, txtConcepto;
    TextView lblTotalVentas, lblFaltante, lblDepositado;
    RadioButton rbDeposito, rbTransferencia, rbAhorro, rbCorriente;
    Spinner spEntidad;
    Toolbar toolbar;
    View rootView;
    Calendar calendar;
    RadioGroup rgTipoTransaccion, rgTipoCuenta;
    DatePickerDialog dtpDialog;

    RecyclerView rvFotos;
    FotoAdapter fotoAdapter;
    String path, nameFoto;
    File fileImage;
    Bitmap bitmap;
    String ExternalDirectory = "";
    Integer idingreso = 0;
    Ingreso miIngreso;
    OkHttpClient okHttpClient;
    Retrofit retrofit;
    ProgressDialog pbProgreso;
    ProgressBar pgCargando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposito);

        init();
    }

    private void init() {
        try{
            toolbar = findViewById(R.id.appbar);
            setSupportActionBar(toolbar);
            rootView = findViewById(android.R.id.content);

            btnFechaVenta = findViewById(R.id.btnFechaVenta);
            btnFechaDocumento = findViewById(R.id.btnFechaDocumento);
            btnCargaDocumento = findViewById(R.id.btnCargaDocumento);
            txtMonto = findViewById(R.id.txtMontoIngreso);
            txtNumDocumento = findViewById(R.id.txtNumDocumento);
            txtConcepto = findViewById(R.id.txtConcepto);
            lblTotalVentas = findViewById(R.id.lblTotalVentas);
            lblFaltante = findViewById(R.id.lblFaltante);
            rbDeposito = findViewById(R.id.rbDeposito);
            rbTransferencia = findViewById(R.id.rbTransferencia);
            rbAhorro = findViewById(R.id.rbAhorro);
            rbCorriente = findViewById(R.id.rbCorriente);
            spEntidad = findViewById(R.id.spEntidad);
            rvFotos = findViewById(R.id.rvFotos);
            lblDepositado = findViewById(R.id.lblDepositado);
            rgTipoTransaccion = findViewById(R.id.rgTipoTransaccion);
            rgTipoCuenta = findViewById(R.id.rgTipoCuenta);
            btnRefresh = findViewById(R.id.btnRefresh);
            pgCargando = findViewById(R.id.pbCargando);

            okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(SQLite.configuracion.url_ws)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();

            btnFechaVenta.setOnClickListener(this::onClick);
            btnFechaDocumento.setOnClickListener(this::onClick);
            btnCargaDocumento.setOnClickListener(this::onClick);
            btnRefresh.setOnClickListener(this::onClick);

            btnFechaVenta.setText(Utils.getDateFormat("yyyy-MM-dd"));
            btnFechaDocumento.setText(Utils.getDateFormat("yyyy-MM-dd"));
            lblTotalVentas.setTag(0);
            lblDepositado.setTag(0);

            fotoAdapter = new FotoAdapter(this, new ArrayList<>());
            rvFotos.setAdapter(fotoAdapter);
            ExternalDirectory = getExternalMediaDirs()[0] + File.separator + Constants.FOLDER_FILES;

            LlenarComboEntidades();

            txtMonto.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }
                @Override
                public void afterTextChanged(Editable s) {
                    Double c = 0d;
                    try{
                        c = Double.parseDouble(s.toString().trim().length() == 0?"0.0":s.toString().trim());
                    }catch (Exception e){
                        Banner.make(rootView, DepositoActivity.this, Banner.ERROR, "Ingrese un valor válido", Banner.TOP, 2500).show();
                        c = 0d;
                    }
                    c = Utils.RoundDecimal(((Double)lblTotalVentas.getTag()) - ((Double)lblDepositado.getTag()) - c,2);
                    lblFaltante.setText("Faltante: ".concat(Utils.FormatoMoneda(c,2)));
                    lblFaltante.setTag(c);
                    Log.d(TAG, "Ventas: " + lblTotalVentas.getTag().toString()
                            + " - Depositado: " + lblDepositado.getTag().toString()
                            + " - Monto: " + c);
                }
            });
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }

    private void LimpiarDatos(){
        try{
            toolbar.setTitle("Comprobante de Depósito");
            idingreso = 0;
            miIngreso = new Ingreso();
            btnFechaVenta.setText(Utils.getDateFormat("yyyy-MM-dd"));
            btnFechaDocumento.setText(Utils.getDateFormat("yyyy-MM-dd"));
            lblTotalVentas.setTag(0d); lblTotalVentas.setText("Total: $0.00");
            lblDepositado.setTag(0d);  lblDepositado.setText("Depositado: $0.00");
            lblFaltante.setTag(0d);    lblFaltante.setText("Faltante: $0.00");
            txtMonto.setText("");
            BuscarTotalVenta(Utils.getDateFormat("yyyy-MM-dd"));
            spEntidad.setSelection(0,true);
            txtConcepto.setText("");
            txtNumDocumento.setText("");
            btnCargaDocumento.setVisibility(View.VISIBLE);
            rgTipoTransaccion.clearCheck();
            rgTipoCuenta.clearCheck();
            fotoAdapter.listFoto.clear();
            fotoAdapter.notifyDataSetChanged();
            /*if(toolbar.getMenu()!=null){
                toolbar.getMenu().findItem(R.id.option_save).setVisible(true);
            }*/
        }catch (Exception e){
            Log.d(TAG, "LimpiarDatos(): " + e.getMessage());
        }
    }

    private boolean ValidarDatos(){
        try{
            if(btnFechaVenta.getText().toString().trim().equals("")){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Debe especificar la fecha de las ventas", Banner.BOTTOM, 2500).show();
                return false;
            }
            if(Double.valueOf(lblTotalVentas.getTag().toString()) <= 0){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "No puede registrar depósitos porque no hay ventas para el día " + btnFechaVenta.getText(), Banner.BOTTOM, 3000).show();
                return false;
            }
            if(btnFechaDocumento.getText().toString().trim().equals("")){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Debe especificar la fecha del documento", Banner.BOTTOM, 2500).show();
                return false;
            }
            if(txtMonto.getText().toString().trim().equals("") || Double.valueOf(txtMonto.getText().toString().trim()) <= 0){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Debe especificar un monto válido del comprobante", Banner.BOTTOM, 2500).show();
                return false;
            }
            if(Double.parseDouble(lblFaltante.getTag().toString())<0){
                Log.d(TAG, "Faltante: " + lblFaltante.getTag().toString());
                Double valPermitido = Double.valueOf(lblTotalVentas.getTag().toString()) - Double.valueOf(lblDepositado.getTag().toString());
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Al parecer hay un excedente "
                                + Utils.FormatoMoneda(Math.abs(Double.parseDouble(lblFaltante.getTag().toString())),2)
                                + " en el monto del depósito. Máximo permitido ".concat(Utils.FormatoMoneda(valPermitido,2)), Banner.BOTTOM, 3000).show();
                return false;
            }
            if(txtNumDocumento.getText().toString().trim().equals("")){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Debe especificar el número del comprobante", Banner.BOTTOM, 2500).show();
                return  false;
            }
            if(!rbDeposito.isChecked() && !rbTransferencia.isChecked()){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Debe especificar el tipo de comprobante", Banner.BOTTOM, 2500).show();
                return  false;
            }
            if(spEntidad.getAdapter() == null || spEntidad.getSelectedItemPosition()<=0){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Debe especificar la entidad bancaria", Banner.BOTTOM, 2500).show();
                return  false;
            }
            if(!rbAhorro.isChecked() && !rbCorriente.isChecked()){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Debe especificar el tipo de cuenta", Banner.BOTTOM, 2500).show();
                return  false;
            }
            if(fotoAdapter.listFoto == null || fotoAdapter.listFoto.size() == 0){
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                        "Debe cargar una foto del comprobante", Banner.BOTTOM, 2500).show();
                return  false;
            }
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
            return false;
        }
        return true;
    }

    private void GuardarDatos(){
        try {
            if(!ValidarDatos())
                return;
            miIngreso = new Ingreso();
            miIngreso.estado = 1;
            miIngreso.carterafaltante = 0;
            miIngreso.personaid = SQLite.usuario.IdUsuario;
            miIngreso.usuarioid = SQLite.usuario.IdUsuario;
            miIngreso.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            miIngreso.getCodigoTransaccion();
            miIngreso.tipo = 0;
            miIngreso.formadepagoid = 2;
            miIngreso.totalingreso = Double.valueOf(txtMonto.getText().toString().trim());
            miIngreso.fechadiario = btnFechaVenta.getText().toString();
            miIngreso.fechacelular = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            //miIngreso.fechadocumento = btnFechaDocumento.getText().toString();
            miIngreso.longdater = Utils.longDate(miIngreso.fechacelular);
            miIngreso.observacion = txtConcepto.getText().toString().trim();
            DetalleIngreso miDetalle = new DetalleIngreso();
            miDetalle.tipo = 0;
            miDetalle.fechadiario = miIngreso.fechadiario;
            miDetalle.tipodocumento = rbDeposito.isChecked()?4:3;
            miDetalle.razonsocialtitular = SQLite.usuario.RazonSocial;
            miDetalle.niptitular = SQLite.usuario.nip;
            miDetalle.tipodecuenta = rbAhorro.isChecked()?"A":"C";
            miDetalle.entidadfinanciera = ((Catalogo)spEntidad.getSelectedItem());
            miDetalle.fechadocumento = btnFechaDocumento.getText().toString();
            miDetalle.numerodocumentoreferencia = txtNumDocumento.getText().toString();
            miDetalle.monto = miIngreso.totalingreso;
            miIngreso.detalle.clear();
            miIngreso.detalle.add(miDetalle);
            miIngreso.fotos.clear();
            miIngreso.fotos.addAll(fotoAdapter.listFoto);

            SubirDeposito(DepositoActivity.this);
            /*if(miIngreso.Save()){
                Banner.make(rootView, DepositoActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS, Banner.BOTTOM, 3000).show();
                LimpiarDatos();
            }else{
                Banner.make(rootView, DepositoActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
            }*/
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
            Banner.make(rootView, DepositoActivity.this, Banner.ERROR, "Excepcion: " + Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
        }
    }

    private void SubirDeposito(final Context context){
        try{
            List<Ingreso> listIngresos = new ArrayList<>();
            for (int i = 0; i < miIngreso.fotos.size(); i++) {
                try {
                    miIngreso.fotos.get(i).image_base = Utils.convertImageToString(miIngreso.fotos.get(i).bitmap);
                } catch (Exception e) {
                    Log.d(TAG, "NotFound(): " + e.getMessage());
                }
            }
            listIngresos.add(miIngreso);

            pbProgreso = new ProgressDialog(DepositoActivity.this);
            pbProgreso.setTitle("Sincronizando depósitos");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Map<String,Object> post = new HashMap<>();
            post.put("usuario",SQLite.usuario.Usuario);
            post.put("clave",SQLite.usuario.Clave);
            post.put("depositos", listIngresos);
            post.put("periodo", SQLite.usuario.sucursal.periodo.toString() + SQLite.usuario.sucursal.mesactual);
            post.put("periodoactual", SQLite.usuario.sucursal.periodo);
            post.put("mesactual", SQLite.usuario.sucursal.mesactual);
            post.put("establecimientoid", SQLite.usuario.sucursal.IdEstablecimiento);
            post.put("version", BuildConfig.VERSION_NAME);
            Call<JsonObject> call = miInterface.LoadDepositos(post);

            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        //Banner.make(rootView,DepositoActivity.this,Banner.ERROR,"Código: " + response.code() + " - " + response.message(), Banner.BOTTOM,3000).show();
                        if(miIngreso.Save()){
                            Banner.make(rootView, DepositoActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS + " Este comprobante aún no se ha sincronizado.", Banner.BOTTOM, 3500).show();
                            LimpiarDatos();
                        }else{
                            Banner.make(rootView, DepositoActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
                        }
                        pbProgreso.dismiss();
                        return;
                    }

                    try {

                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonDepositosUpdate = obj.getAsJsonArray("depositosupdate");
                                if(jsonDepositosUpdate!=null && jsonDepositosUpdate.size()>0){
                                    JsonObject upd =  jsonDepositosUpdate.get(0).getAsJsonObject();
                                    miIngreso.codigosistema = upd.get("codigosistema_deposito").getAsInt();
                                    miIngreso.estado = upd.get("codigosistema_deposito").getAsInt();

                                    if(miIngreso.Save()) {
                                        if (obj.has("secuencial_dep")) {
                                            Integer secuencial_pe = obj.get("secuencial_dep").getAsInt();
                                            Ingreso comprobante = new Ingreso();
                                            comprobante.secuencial = secuencial_pe;
                                            comprobante.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
                                            comprobante.actualizasecuencial();
                                        }
                                        Banner.make(rootView, DepositoActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS, Banner.BOTTOM, 3000).show();
                                        LimpiarDatos();
                                    }else{
                                        Banner.make(rootView, DepositoActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
                                    }
                                }else{
                                    if(miIngreso.Save()){
                                        Banner.make(rootView, DepositoActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS + " Este comprobante aún no se ha sincronizado.", Banner.BOTTOM, 3500).show();
                                        LimpiarDatos();
                                    }else{
                                        Banner.make(rootView, DepositoActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
                                    }
                                }

                            } else
                                Utils.showErrorDialog(DepositoActivity.this,"Error", obj.get("message").getAsString());
                        } else {
                            Banner.make(rootView,DepositoActivity.this,Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM,3000).show();
                        }
                    }catch (JsonParseException ex){
                        Log.d(TAG, ex.getMessage());
                        if(miIngreso.Save()){
                            Banner.make(rootView, DepositoActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS + " Este comprobante aún no se ha sincronizado.", Banner.BOTTOM, 3500).show();
                            LimpiarDatos();
                        }else{
                            Banner.make(rootView, DepositoActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
                        }
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.d(TAG, t.getMessage());
                    call.cancel();
                    if(miIngreso.Save()){
                        Banner.make(rootView, DepositoActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS + " Este comprobante aún no se ha sincronizado.", Banner.BOTTOM, 3500).show();
                        LimpiarDatos();
                    }else{
                        Banner.make(rootView, DepositoActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
                    }
                    pbProgreso.dismiss();
                }
            });

        }catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
            Utils.showErrorDialog(this, "Error",e.getMessage());
            pbProgreso.dismiss();
        }
    }

    public void showDatePickerDialog(View v) {
        Locale l = new Locale("ES-es");
        calendar = Calendar.getInstance(l);
        int day=calendar.get(Calendar.DAY_OF_MONTH);
        int month=calendar.get(Calendar.MONTH);
        int year=calendar.get(Calendar.YEAR);
        String[] fecha= ((Button)v).getText().toString().split("-");
        day = Integer.valueOf(fecha[2]);
        month = Integer.valueOf(fecha[1])-1;
        year = Integer.valueOf(fecha[0]);
        dtpDialog = new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                String dia = (day>=0 && day<10?"0"+(day):String.valueOf(day));
                String mes = (month>=0 && month<9?"0"+(month+1):String.valueOf(month+1));

                String miFecha = year + "-" + mes + "-" + dia;
                ((Button)v).setText(miFecha);
                if(v.getId() == R.id.btnFechaVenta) {
                    if(Utils.longDate(miFecha)>Utils.longDate(Utils.getDateFormat("yyyy-MM-dd"))){
                        Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                                "Debe elegir una fecha menor o igual a la fecha actual",
                                Banner.BOTTOM, 3000).show();
                        return;
                    }
                    BuscarTotalVenta(miFecha);
                }
            }
        },year,month,day);
        dtpDialog.show();
    }

    private void BuscarTotalVenta2(String fecha){
        try{
            if(toolbar.getMenu()!=null){
                toolbar.getMenu().findItem(R.id.option_save).setVisible(true);
            }
            Double[] totales = Usuario.getTotalVentas(fecha);
            lblTotalVentas.setText("Total: ".concat(Utils.FormatoMoneda(totales[0],2)));
            lblTotalVentas.setTag(totales[0]);
            lblDepositado.setText("Depositado: ".concat(Utils.FormatoMoneda(totales[1],2)));
            lblDepositado.setTag(totales[1]);

            Double m = txtMonto.getText().toString().trim().equals("")?0d:Double.parseDouble(txtMonto.getText().toString().trim());
            lblFaltante.setText("Faltante: ".concat(Utils.FormatoMoneda(totales[0]-totales[1]-m,2)));
            lblFaltante.setTag(totales[0]-totales[1]-m);
            Log.d(TAG, "Ventas: " + totales[0] + " - Depositado: " + totales[1] + " - Monto: " + m);

            Integer docNS = Usuario.numDocNoSincronizados(SQLite.usuario.IdUsuario, "01", fecha, SQLite.usuario.sucursal.IdEstablecimiento);
            if(docNS > 0){
                Banner.make(rootView, DepositoActivity.this, Banner.INFO,
                        "Tiene facturas pendientes por sincronizar, y no podrá registrar comprobante de depósito", Banner.BOTTOM, 3000).show();
                if(toolbar.getMenu()!=null){
                    toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                }
                return;
            }

            if(docNS == 0 && totales[0] > 0 && totales[0].equals(totales[1])){
                Banner.make(rootView, DepositoActivity.this, Banner.INFO,
                        "Las ventas del día " + fecha + " ya han sido depositadas en su totalidad.", Banner.BOTTOM, 3000).show();
                if(toolbar.getMenu()!=null){
                    toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                }
            }
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }

    private void BuscarTotalVenta(String fecha){
        try{
            pgCargando.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);
            if(toolbar.getMenu()!=null){
                toolbar.getMenu().findItem(R.id.option_save).setVisible(true);
            }

            IUsuario miInterface = retrofit.create(IUsuario.class);

            Call<JsonObject> call = miInterface.getTotalVentas(
                                            SQLite.usuario.Usuario, SQLite.usuario.Clave,
                                            fecha, SQLite.usuario.sucursal.IdEstablecimiento);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                        Banner.make(rootView, DepositoActivity.this, Banner.SUCCESS,
                        "No se pudo obtener los datos de las ventas. Verifique su conexión a internet.", Banner.BOTTOM, 3000).show();
                        pgCargando.setVisibility(View.GONE);
                        btnRefresh.setVisibility(View.VISIBLE);
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonObject jVentas = obj.getAsJsonObject("totales");
                                if(jVentas!=null){
                                    Double[] totales = Usuario.getTotalVentas(fecha);
                                    lblTotalVentas.setText("Total: ".concat(Utils.FormatoMoneda(jVentas.get("ventas").getAsDouble(),2)));
                                    lblTotalVentas.setTag(jVentas.get("ventas").getAsDouble());
                                    lblDepositado.setText("Depositado: ".concat(Utils.FormatoMoneda(jVentas.get("depositado").getAsDouble(),2)));
                                    lblDepositado.setTag(jVentas.get("depositado").getAsDouble());

                                    Double m = txtMonto.getText().toString().trim().equals("")?0d:Double.parseDouble(txtMonto.getText().toString().trim());
                                    lblFaltante.setText("Faltante: ".concat(Utils.FormatoMoneda(jVentas.get("ventas").getAsDouble()-jVentas.get("depositado").getAsDouble()-m,2)));
                                    lblFaltante.setTag(jVentas.get("ventas").getAsDouble()-jVentas.get("depositado").getAsDouble()-m);
                                    Log.d(TAG, "Ventas: " + jVentas.get("ventas").getAsDouble() + " - Depositado: " + jVentas.get("depositado").getAsDouble() + " - Monto: " + m);

                                    Integer docNS = Usuario.numDocNoSincronizados(SQLite.usuario.IdUsuario, "01", fecha, SQLite.usuario.sucursal.IdEstablecimiento);
                                    if(docNS > 0){
                                        Banner.make(rootView, DepositoActivity.this, Banner.INFO,
                                                "Tiene facturas pendientes por sincronizar, y no podrá registrar comprobante de depósito", Banner.BOTTOM, 3000).show();
                                        if(toolbar.getMenu()!=null){
                                            toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                                        }
                                        return;
                                    }

                                    if(docNS == 0 && jVentas.get("ventas").getAsDouble() > 0 && jVentas.get("ventas").getAsDouble() == jVentas.get("depositado").getAsDouble()){
                                        Banner.make(rootView, DepositoActivity.this, Banner.INFO,
                                                "Las ventas del día " + fecha + " ya han sido depositadas en su totalidad.", Banner.BOTTOM, 3000).show();
                                        if(toolbar.getMenu()!=null){
                                            toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                                        }
                                    }
                                }
                            }
                        }
                    }catch (Exception e){
                        Log.d(TAG,"onResponse(): " + e.getMessage());
                        toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                        Banner.make(rootView, DepositoActivity.this, Banner.SUCCESS,
                                "No se pudo obtener los datos de las ventas. Verifique su conexión a internet.", Banner.BOTTOM, 3000).show();
                    }
                    pgCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.d(TAG, "onFailure(): " + t.getMessage());
                    call.cancel();
                    toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                    Banner.make(rootView, DepositoActivity.this, Banner.ERROR,
                            "No se pudo obtener los datos de las ventas. Verifique su conexión a internet.", Banner.BOTTOM, 3000).show();
                    pgCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }
            });
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
            pgCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void LlenarComboEntidades(){
        Thread th = new Thread(){
            @Override
            public void run(){
                List<Catalogo> entidades = new ArrayList<>();
                entidades.add(new Catalogo(-1, "","NONE","-Seleccione-",0));
                entidades.addAll(Catalogo.getCatalogo("ENTIDADFINANCIE"));
                ArrayAdapter<Catalogo> adapter = new ArrayAdapter<>(DepositoActivity.this, android.R.layout.simple_spinner_dropdown_item, entidades);
                runOnUiThread(() -> spEntidad.setAdapter(adapter));
            }
        };
        th.start();
    }

    private void ElegirOpcionFoto() {
        final CharSequence[] opciones = {"Desde cámara", "Desde galería"};
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Elija una opción");
            builder.setItems(opciones,
                (dialog, which) -> {
                    if (which == 0) { //DESDE LA CAMARA
                        openCamera();
                    } else if (which == 1) { //DESDE LA GALERIA
                        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        i.setType("image/*");
                        startActivityForResult(i.createChooser(i, "Seleccione"), REQUEST_SELECCIONA_FOTO);
                    } else {
                        dialog.dismiss();
                    }
                }
            );
            builder.show();
        } catch (Exception e) {
            Log.d(TAG, "elegirOpcionFoto(): " + e.getMessage());
        }
    }

    private void openCamera() {
        try {
            boolean exists = false;
            File miFile = new File(getExternalMediaDirs()[0], Constants.FOLDER_FILES);
            exists = miFile.exists();
            if (!exists) {
                exists = miFile.mkdirs();
                Log.d(TAG, "NO EXISTE LA CARPETA: " + String.valueOf(exists) + " - " + miFile.canRead());
                //exists = true;
            }
            if (exists) {
                Long consecutivo = System.currentTimeMillis() / 1000;
                //nameFoto = consecutivo.toString() + ".jpg";
                nameFoto = SQLite.usuario.nip + "_ingre_" + Utils.getDateFormat("yyyyMMddHHmmss")+".jpg";
                path = getExternalMediaDirs()[0] + File.separator + Constants.FOLDER_FILES
                        + File.separator + nameFoto;
                fileImage = new File(path);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(this,
                                BuildConfig.APPLICATION_ID + ".services.GenericFileProvider",
                                fileImage));
                startActivityForResult(i, REQUEST_NEW_FOTO);
            }
        } catch (Exception e) {
            Log.d(TAG, "openCamera(): " + e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnFechaVenta:
            case R.id.btnFechaDocumento:
                showDatePickerDialog(v);
                break;
            case R.id.btnCargaDocumento:
                File miFile = new File(getExternalMediaDirs()[0], Constants.FOLDER_FILES);
                if (!miFile.exists())
                    miFile.mkdirs();
                ElegirOpcionFoto();
                break;
            case R.id.btnRefresh:
                BuscarTotalVenta(btnFechaVenta.getText().toString().trim());
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        menu.findItem(R.id.option_listdocument).setVisible(false);
        menu.findItem(R.id.option_reimprimir).setVisible(false);

        BuscarTotalVenta(Utils.getDateFormat("yyyy-MM-dd"));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_save:
                if(!SQLite.usuario.VerificaPermiso(this,Constants.PUNTO_VENTA, "escritura")){
                    Banner.make(rootView,DepositoActivity.this,Banner.ERROR,"No tiene permisos para registrar depósitos.", Banner.BOTTOM, 3000).show();
                    break;
                }
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                        (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView)view.findViewById(R.id.lblTitle)).setText("Guardar comprobante");
                ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea guardar este comprobante de depósito?");
                ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
                ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
                ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
                final android.app.AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnConfirm).setOnClickListener(v ->{
                        GuardarDatos();
                        alertDialog.dismiss();
                    });

                view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

                if(alertDialog.getWindow()!=null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
                break;
            case R.id.option_newdocument:
                LimpiarDatos();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            Foto mifoto;
            switch (requestCode){
                case REQUEST_SELECCIONA_FOTO:

                    mifoto = new Foto();
                    Uri miPath = data.getData();
                    mifoto.uriFoto = miPath;
                    String[] projection = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE };
                    Cursor cursor = managedQuery(miPath, projection, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    int column_index2 = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                    cursor.moveToFirst();
                    String path1= cursor.getString(column_index);
                    Log.d(TAG, "Index: " + column_index2 + " -> Tamaño: " + Utils.RoundDecimal((cursor.getDouble(column_index2)/1024)/1024,2));

                    try{
                        mifoto.bitmap = MediaStore.Images.Media.getBitmap(DepositoActivity.this.getContentResolver(),miPath);
                        String nombre = SQLite.usuario.nip + "_ingre_" + Utils.getDateFormat("yyyyMMddHHmmss")+".jpg";
                        path = getExternalMediaDirs()[0]+File.separator+Constants.FOLDER_FILES
                                +File.separator+nombre;

                        Utils.insert_image(mifoto.bitmap, nombre, ExternalDirectory, getContentResolver());
                        mifoto.path = path;
                        mifoto.name = nombre;
                        mifoto.tipo = "I";
                    } catch (IOException e) {
                        Log.d(TAG, e.getMessage());
                    }
                    fotoAdapter.listFoto.add(mifoto);
                    fotoAdapter.notifyDataSetChanged();
                    btnCargaDocumento.setVisibility(View.INVISIBLE);
                    break;
                case REQUEST_NEW_FOTO:
                    MediaScannerConnection.scanFile(DepositoActivity.this, new String[]{path},
                            null, (path, uri) -> Log.d(TAG, path));
                    bitmap = BitmapFactory.decodeFile(path);
                    Utils.insert_image(bitmap, nameFoto, ExternalDirectory, getContentResolver());
                    mifoto = new Foto();
                    mifoto.bitmap = bitmap;
                    mifoto.path = path;
                    mifoto.name = nameFoto;
                    mifoto.tipo = "I";
                    fotoAdapter.listFoto.add(mifoto);
                    fotoAdapter.notifyDataSetChanged();
                    btnCargaDocumento.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        toolbar.setTitle("Comprobante de Depósito");
        toolbar.setTitleTextColor(Color.WHITE);
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            ((TextView)view.findViewById(R.id.lblTitle)).setText("Cerrar");
            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Desea salir de la ventana de comprobante de depósito?");
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_check_white);
            ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
            ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
            final android.app.AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                onBackPressed();
                overridePendingTransition(R.anim.zoom_back_in, R.anim.zoom_back_out);
            });

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
