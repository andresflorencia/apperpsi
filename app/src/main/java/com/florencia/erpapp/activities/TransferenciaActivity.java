package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.DetalleRecepcionAdapter;
import com.florencia.erpapp.interfaces.IComprobante;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.DetalleComprobante;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.models.Sucursal;
import com.florencia.erpapp.services.DeviceList;
import com.florencia.erpapp.services.Printer;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.shasin.notificationbanner.Banner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.florencia.erpapp.services.Printer.btsocket;

public class TransferenciaActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int REQUEST_BUSQUEDA_PRODUCTO = 1;
    private static final int REQUEST_BUSQUEDA_TRANSFERENCIA = 2;
    private static final String TAG = "TAGTRANSFERENCIA_ACT";
    Spinner cbEstablecimientos;
    RecyclerView rvDetalleProducto;
    ProgressDialog pgCargando;
    OkHttpClient okHttpClient;
    List<Sucursal> listEstablecimientos = new ArrayList<>();
    DetalleRecepcionAdapter detalleAdapter;
    List<DetalleComprobante> detalleProductos = new ArrayList<>();
    Comprobante mitransferencia;
    SpinnerAdapter spinnerAdapter;
    Toolbar toolbar;
    ProgressBar pbCargando;
    ImageButton btnRefresh;
    TextView lblObservacion;
    EditText tvObservacion;
    Integer idtransferencia=0;
    List<Comprobante> listTransacciones= new ArrayList<>();
    public static final List<DetalleComprobante> productBusqueda = new ArrayList<>();
    LinearLayout lyCombo, lyDatosInformativos, lyProductos;
    TextView lblTransferencia, lblEnvia, lblRecibe, lblDocTransferencia, lblProducto;
    Sucursal miestablecimiento = new Sucursal();
    Button btnBuscarProducto;

    TextView lblMessage, lblTitle;
    BottomSheetDialog btsDialog;
    Button btnPositive, btnNegative;
    View viewSeparator, rootView;
    String tipoAccion="";
    RadioGroup rgTipoTransaccion;
    RadioButton rbTransferencia, rbDevolucion;
    Retrofit retrofit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transferencia);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        rootView = findViewById(android.R.id.content);
        toolbar.setTitle("Transferencia de Inventario");
        init();

        cbEstablecimientos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    miestablecimiento = listEstablecimientos.get(position);
                }catch (Exception e){
                    Log.d(TAG, "cbEstablecimiento(): " + e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void init(){
        cbEstablecimientos = findViewById(R.id.cbEstablecimiento);
        rvDetalleProducto = findViewById(R.id.rvDetalleProductos);
        pbCargando = findViewById(R.id.pbCargando);
        btnRefresh = findViewById(R.id.btnRefresh);
        lblObservacion = findViewById(R.id.lblObservacion);
        tvObservacion = findViewById(R.id.tvObservacion);
        lyCombo = findViewById(R.id.lyCombo);
        lyDatosInformativos = findViewById(R.id.lyDatosInformativos);
        lblTransferencia = findViewById(R.id.lblTransferencia);
        lblEnvia = findViewById(R.id.lblEnvia);
        lblRecibe = findViewById(R.id.lblRecibe);
        lblDocTransferencia = findViewById(R.id.lblDocTransferencia);
        btnBuscarProducto = findViewById(R.id.btnBuscarProducto);
        rgTipoTransaccion = findViewById(R.id.rgTipoTransaccion);
        rbTransferencia = findViewById(R.id.rbTransferencia);
        rbDevolucion = findViewById(R.id.rbDevolucion);
        lblProducto = findViewById(R.id.lblProducto);
        lyProductos = findViewById(R.id.lyProductos);
        pgCargando = new ProgressDialog(this);

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

        LlenarEstablecimientos(this);

        mitransferencia = new Comprobante();
        detalleAdapter = new DetalleRecepcionAdapter(this,detalleProductos,"",false,"4,20");
        rvDetalleProducto.setAdapter(detalleAdapter);

        btnRefresh.setOnClickListener(this::onClick);
        btnBuscarProducto.setOnClickListener(this::onClick);
        lblObservacion.setOnClickListener(this::onClick);
        lblProducto.setOnClickListener(this::onClick);
        lblTransferencia.setOnClickListener(this::onClick);

        if(getIntent().getExtras()!=null){
            idtransferencia = getIntent().getExtras().getInt("idcomprobante",0);
            if(idtransferencia>0)
                BuscaTransferencia(idtransferencia);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnRefresh:
                LlenarEstablecimientos(v.getContext());
                break;
            case R.id.lblObservacion:
                Utils.EfectoLayout(tvObservacion,lblObservacion);
                break;
            case R.id.btnBuscarProducto:
                Intent i = new Intent(v.getContext(),ProductoBusquedaActivity.class);
                i.putExtra("tipobusqueda", "4,20");
                startActivityForResult(i, REQUEST_BUSQUEDA_PRODUCTO);
                break;
            case R.id.btnPositive:
                if(tipoAccion.equals("MESSAGE")) {
                    btnNegative.setVisibility(View.VISIBLE);
                    viewSeparator.setVisibility(View.VISIBLE);
                    lblTitle.setVisibility(View.VISIBLE);
                    btsDialog.dismiss();
                }
                break;
            case R.id.btnNegative:
                btsDialog.dismiss();
                break;
            case R.id.lblTransferencia:
                if(idtransferencia>0)
                    Utils.EfectoLayout(lyDatosInformativos, lblTransferencia);
                else
                    Utils.EfectoLayout(lyCombo, lblTransferencia);
                break;
            case R.id.lblProducto:
                Utils.EfectoLayout(lyProductos, lblProducto);
                break;
        }
    }

    private void LlenarEstablecimientos(Context context) {
        try{
            /*if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM,3000).show();
                return;
            }*/

            pbCargando.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Call<JsonObject> call=null;
            call=miInterface.getEstablecimientos(SQLite.usuario.Usuario,SQLite.usuario.Clave,SQLite.usuario.sucursal.IdEstablecimiento);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        showError("Error código: " + response.code());
                        pbCargando.setVisibility(View.GONE);
                        btnRefresh.setVisibility(View.VISIBLE);
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            cbEstablecimientos.setAdapter(null);
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonEstablecimientos = obj.getAsJsonArray("sucursales");
                                if(jsonEstablecimientos!=null){
                                    listEstablecimientos.clear();
                                    Sucursal miestablecimiento = new Sucursal();
                                    miestablecimiento.NombreSucursal="Escoja el destino";
                                    listEstablecimientos.add(miestablecimiento);
                                    for (JsonElement ele : jsonEstablecimientos) {
                                        JsonObject trans = ele.getAsJsonObject();
                                        miestablecimiento = new Sucursal();
                                        miestablecimiento.IdEstablecimiento = trans.get("idestablecimiento").getAsInt();
                                        miestablecimiento.CodigoEstablecimiento = trans.get("codigoestablecimiento").getAsString();
                                        miestablecimiento.NombreSucursal = trans.get("nombreestablecimiento").getAsString();
                                        miestablecimiento.Direcion = trans.get("direccion").getAsString();
                                        miestablecimiento.NombreComercial = trans.get("nombrecomercial").getAsString();
                                        listEstablecimientos.add(miestablecimiento);
                                    }
                                    spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, listEstablecimientos);
                                    cbEstablecimientos.setAdapter(spinnerAdapter);
                                    cbEstablecimientos.setSelection(0,true);
                                }
                            }else
                                Utils.showErrorDialog(TransferenciaActivity.this,"Error",obj.get("message").getAsString());
                        }else
                            Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"Ocurrió un error al cargar los datos.",Banner.BOTTOM,3000).show();

                    }catch (Exception e){
                        Log.d(TAG,"onResponse(): " + e.getMessage());
                    }
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(TransferenciaActivity.this,"Error",t.getMessage());
                    Log.d(TAG, "onFailure(): " + t.getMessage());
                    call.cancel();
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }
            });
        }catch (Exception e){
            Log.d(TAG, "LlenarEstablecimientos(): " + e.getMessage());
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
        }
    }

    private boolean ValidarDatos(){
        try{
            if(!rbTransferencia.isChecked() && !rbDevolucion.isChecked()){
                Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"Debe especificar el tipo de transacción a realizar.", Banner.BOTTOM,3000).show();
                return false;
            }
            if(cbEstablecimientos.getAdapter()==null){
                Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"No hay establecimientos o bódegas disponibles para la transferencia.", Banner.BOTTOM,3000).show();
                return false;
            }
            if(cbEstablecimientos.getSelectedItemPosition()==0){
                Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"Escoja el establecimiento/bodega de destino.", Banner.BOTTOM,3000).show();
                return false;
            }
            if(detalleAdapter.detalleComprobante.size()==0){
                Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"No ha especificado los productos para la transferencia.",Banner.BOTTOM,3000).show();
                return false;
            }else{
                for (DetalleComprobante midetalle:detalleAdapter.detalleComprobante){
                    if(midetalle.cantidad<=0){
                        Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"No ha especificado la cantidad a tranferir para «"+midetalle.producto.nombreproducto+"»",Banner.BOTTOM,3000).show();
                        return false;
                    }
                }
            }
        }catch (Exception e){
            Log.d(TAG, "ValidarDatos(): " + e.getMessage());
            return false;
        }
        return true;
    }

    private void GuardarDatos() {
        try{
            if(!ValidarDatos()) return;

            listTransacciones.clear();
            Sucursal destino = new Sucursal();
            destino = (Sucursal) cbEstablecimientos.getSelectedItem();

            mitransferencia.tipotransaccion = rbTransferencia.isChecked()?"4":"20";
            mitransferencia.codigotransaccion = (rbTransferencia.isChecked()?"TR-":"DEVINV-") + SQLite.usuario.sucursal.IdSucursal + "-" + Utils.getDateFormat("yyMMdd-HHmmss");
            mitransferencia.total = 0d;
            mitransferencia.codigoestablecimiento = SQLite.usuario.sucursal.CodigoEstablecimiento;
            mitransferencia.observacion = tvObservacion.getText().toString().trim();
            mitransferencia.usuarioid = SQLite.usuario.IdUsuario;
            mitransferencia.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            mitransferencia.fechacelular = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            mitransferencia.fechadocumento = Utils.getDateFormat("yyyy-MM-dd");

            SQLite.gpsTracker.getLastKnownLocation();
            mitransferencia.lat = SQLite.gpsTracker.getLatitude();
            mitransferencia.lon = SQLite.gpsTracker.getLongitude();
            if(mitransferencia.lat == null)
                mitransferencia.lat = 0d;
            if(mitransferencia.lon == null)
                mitransferencia.lon = 0d;

            for(DetalleComprobante midetalle:mitransferencia.detalle)
                mitransferencia.total += midetalle.Subtotalcosto();
            mitransferencia.subtotal = mitransferencia.total;

            listTransacciones.add(mitransferencia);

            Comprobante mitransferencia2= new Comprobante();
            mitransferencia2.tipotransaccion = rbTransferencia.isChecked()?"5":"21";
            mitransferencia2.codigotransaccion = mitransferencia.codigotransaccion;
            mitransferencia2.total = 0d;
            mitransferencia2.codigoestablecimiento = destino.CodigoEstablecimiento;
            mitransferencia2.observacion = tvObservacion.getText().toString().trim();
            mitransferencia2.usuarioid = SQLite.usuario.IdUsuario;
            mitransferencia2.total = mitransferencia.total;
            mitransferencia2.subtotal = mitransferencia.subtotal;
            mitransferencia2.establecimientoid = destino.IdEstablecimiento;
            mitransferencia2.fechacelular = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            mitransferencia2.fechadocumento = Utils.getDateFormat("yyyy-MM-dd");
            mitransferencia2.detalle.addAll(mitransferencia.detalle);

            mitransferencia2.lat = mitransferencia.lat;
            mitransferencia2.lon = mitransferencia.lon;

            listTransacciones.add(mitransferencia2);

            EnviarDatos(this);
        }catch (Exception e){
            Log.d(TAG, "GuardarDatos(): " + e.getMessage());
        }
    }

    private void EnviarDatos(Context context) {
        try{
            /*if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)){
                Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM,3000).show();
                return;
            }*/
            if(listTransacciones.size()>0){
                pgCargando.setTitle("Guardando transacción");
                pgCargando.setMessage("Espere un momento...");
                pgCargando.setCancelable(false);
                pgCargando.show();

                IComprobante miInterface = retrofit.create(IComprobante.class);

                Map<String,Object> post = new HashMap<>();
                post.put("usuario",SQLite.usuario.Usuario);
                post.put("clave",SQLite.usuario.Clave);
                post.put("establecimientoid",SQLite.usuario.sucursal.IdEstablecimiento);
                post.put("puntoemisionid", SQLite.usuario.sucursal.IdPuntoEmision);
                post.put("transacciones", listTransacciones);
                String json = post.toString();
                Call<JsonObject> call=miInterface.saveTransferencia(post);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if(!response.isSuccessful()){
                            Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"Código: " + response.code() + " - " + response.message(), Banner.BOTTOM,3000).show();
                            pgCargando.dismiss();
                            return;
                        }
                        try {

                            if (response.body() != null) {
                                JsonObject obj = response.body();
                                if (!obj.get("haserror").getAsBoolean()) {
                                    JsonArray jsonTransacciones = obj.getAsJsonArray("transferencia");
                                    if(jsonTransacciones!=null){
                                        for(JsonElement ele:jsonTransacciones){
                                            JsonObject trans = ele.getAsJsonObject();
                                            //INSERTAR TRANSFERENCIA EN LA BD LOCAL
                                            Sucursal destino = (Sucursal) cbEstablecimientos.getSelectedItem();
                                            mitransferencia.codigosistema = trans.get("codigosistema").getAsInt();
                                            mitransferencia.sucursalenvia = SQLite.usuario.sucursal.IdEstablecimiento + "-" + SQLite.usuario.sucursal.NombreSucursal + "-" + SQLite.usuario.Usuario;
                                            mitransferencia.sucursalrecibe = destino.IdEstablecimiento + "-" + destino.NombreSucursal;
                                            mitransferencia.claveacceso = mitransferencia.codigotransaccion;
                                            mitransferencia.longdate = Utils.longDate(mitransferencia.fechadocumento);

                                            if(mitransferencia.Save(false)){
                                                JsonArray jsonProductos = obj.getAsJsonArray("productos");
                                                if(jsonProductos!=null) {
                                                    int numProd = 0;
                                                    Producto.Delete(SQLite.usuario.sucursal.IdEstablecimiento);
                                                    for (JsonElement pro : jsonProductos) {
                                                        JsonObject prod = pro.getAsJsonObject();
                                                        Producto miProducto = new Gson().fromJson(prod, Producto.class);
                                                        if (miProducto != null) {
                                                            if (miProducto.Save())
                                                                numProd++;
                                                            Log.d(TAG, prod.get("nombreproducto").getAsString());
                                                        }
                                                    }
                                                    if (numProd == jsonProductos.size())
                                                        Banner.make(rootView,TransferenciaActivity.this,Banner.SUCCESS,Constants.MSG_PROCESO_COMPLETADO,Banner.BOTTOM,3000).show();
                                                    else
                                                        Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,Constants.MSG_PROCESO_NO_COMPLETADO,Banner.BOTTOM,3000).show();
                                                }
                                                toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                                                ConsultaImpresion();
                                                //LimpiarDatos();
                                            }
                                            break;
                                        }
                                    }else
                                        Banner.make(rootView,TransferenciaActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS,Banner.BOTTOM,3000).show();
                                } else
                                    Utils.showErrorDialog(TransferenciaActivity.this,"Error",obj.get("message").getAsString());
                            } else
                                Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,Constants.MSG_USUARIO_CLAVE_INCORRECTO,Banner.BOTTOM,3000).show();
                        }catch (JsonParseException ex){
                            Log.d(TAG, ex.getMessage());
                        }
                        pgCargando.dismiss();
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Utils.showErrorDialog(TransferenciaActivity.this,"Error",t.getMessage());
                        Log.d(TAG, t.getMessage());
                        call.cancel();
                        pgCargando.dismiss();
                    }
                });
            }
        }catch (Exception e){
            Log.d(TAG, "EnviarDatos(): " + e.getMessage());
            pgCargando.dismiss();
        }
    }

    private void ConsultaImpresion() {
        try{
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            builder.setCancelable(false);
            ((TextView)view.findViewById(R.id.lblTitle)).setText("Imprimir");
            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Desea imprimir este documento?");
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_printer_white);
            ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
            ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                    if (Printer.btsocket == null) {
                        Utils.showMessage(getApplicationContext(), "Emparejar la impresora...");
                        Intent BTIntent = new Intent(getApplicationContext(), DeviceList.class);
                        startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
                        return;
                    }else {
                        imprimirFactura(idtransferencia==0?"* ORIGINAL *":"* REIMPRESIÓN DE DOCUMENTO *", idtransferencia>0);
                    }
                    alertDialog.dismiss();
                });

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> {
                    if(idtransferencia==0)
                        LimpiarDatos();
                    alertDialog.dismiss();
                });

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
        }catch (Exception e){
            Log.d(TAG, "ConsultaImpresion(): "+ e.getMessage());
        }
    }

    private void LimpiarDatos() {
        try{
            mitransferencia = new Comprobante();
            listTransacciones.clear();
            detalleProductos.clear();
            detalleAdapter.visualizacion = false;
            detalleAdapter.detalleComprobante.clear();
            detalleAdapter.notifyDataSetChanged();
            tvObservacion.setText("");
            cbEstablecimientos.setEnabled(true);
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
            idtransferencia = 0;
            lyCombo.setVisibility(View.VISIBLE);
            lyDatosInformativos.setVisibility(View.GONE);
            lblTransferencia.setText("DESTINO");
            tvObservacion.setEnabled(true);
            btnBuscarProducto.setVisibility(View.VISIBLE);
            LlenarEstablecimientos(this);
            rgTipoTransaccion.setEnabled(true);
            rbTransferencia.setEnabled(true);
            rbDevolucion.setEnabled(true);
            rbDevolucion.setChecked(false);
            rbTransferencia.setChecked(false);
            toolbar.setSubtitle("");
        }catch (Exception e){
            Log.d(TAG, "LimpiarDatos(): "+ e.getMessage());
        }
    }

    private void BuscaTransferencia(Integer idtransferencia) {
        pbCargando.setVisibility(View.VISIBLE);
        btnRefresh.setVisibility(View.GONE);

        try {

            Thread th = new Thread() {
                @Override
                public void run() {
                    mitransferencia = new Comprobante();
                    mitransferencia = Comprobante.get(idtransferencia);
                    runOnUiThread(() -> {
                            if (mitransferencia != null) {
                                rbTransferencia.setChecked(mitransferencia.tipotransaccion.equals("4"));
                                rbDevolucion.setChecked(mitransferencia.tipotransaccion.equals("20"));
                                toolbar.setSubtitle("Fecha: " + Utils.fechaMes(mitransferencia.fechadocumento));
                                rgTipoTransaccion.setEnabled(false);
                                rbTransferencia.setEnabled(false);
                                rbDevolucion.setEnabled(false);
                                cbEstablecimientos.setEnabled(false);
                                detalleAdapter.visualizacion = true;
                                detalleProductos.clear();
                                detalleProductos.addAll(mitransferencia.detalle);
                                detalleAdapter.detalleComprobante.clear();
                                detalleAdapter.detalleComprobante.addAll(mitransferencia.detalle);
                                detalleAdapter.notifyDataSetChanged();
                                tvObservacion.setText(mitransferencia.observacion);
                                tvObservacion.setEnabled(false);
                                lyCombo.setVisibility(View.GONE);
                                lyDatosInformativos.setVisibility(View.VISIBLE);
                                lblTransferencia.setText("DATOS INFORMATIVOS");
                                lblEnvia.setText(mitransferencia.sucursalenvia.split("-")[1]);
                                lblDocTransferencia.setText(mitransferencia.claveacceso);
                                lblRecibe.setText(mitransferencia.sucursalrecibe.split("-")[1]);
                                btnBuscarProducto.setVisibility(View.GONE);
                            } else {
                                Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"Ocurrió un error al obtener los datos para este documento.", Banner.BOTTOM,3000).show();
                            }
                            pbCargando.setVisibility(View.GONE);
                        });
                }
            };
            th.start();
        }catch (Exception e){
            Log.d(TAG, "BuscaRecepcion(): " + e.getMessage());
        }
    }

    private boolean imprimirFactura(String strTipo, boolean reimpresion){
        Printer printer = new Printer(this);
        boolean fImp = true;
        try {
            fImp = printer.btsocket.isConnected();
            //if(!reimpresion) fImp = comprobante.Save();
            if (Printer.btsocket != null) {

                if (fImp) {
                    try {
                        int copias = reimpresion?1:2;
                        for(int i=0;i<copias; i++) {
                            printer.printUnicode();
                            printer.printCustom(SQLite.usuario.sucursal.NombreComercial, 0, 1);
                            printer.printCustom(SQLite.usuario.sucursal.RazonSocial, 0, 1);
                            printer.printCustom("RUC: ".concat(SQLite.usuario.sucursal.RUC), 0, 1);
                            printer.printCustom(SQLite.usuario.sucursal.Direcion, 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("**"+ (mitransferencia.tipotransaccion.equals("4")?"TRANSFERENCIA":"DEVOLUCION") +" DE INVENTARIO**", 0, 1);
                            printer.printCustom("Origen  : ".concat(mitransferencia.sucursalenvia.split("-")[1]), 0, 0);
                            printer.printCustom("N. Doc.: ".concat(mitransferencia.codigotransaccion), 0, 0);
                            printer.printCustom("Destino : ".concat(mitransferencia.sucursalrecibe.split("-")[1]), 0, 0);
                            printer.printCustom("Fecha  : ".concat(mitransferencia.fechacelular), 0, 0);
                            printer.printCustom("------------------------------------------", 0, 1);
                            printer.printCustom(" Num. |  Código  |     Nombre     | Cant", 0, 0);
                            printer.printCustom("------------------------------------------", 0, 1);
                            for (DetalleComprobante midetalle : mitransferencia.detalle) {
                                Printer.Data[] Datos = new Printer.Data[]{
                                        new Printer.Data(6, midetalle.linea.toString(), 0),
                                        new Printer.Data(12, midetalle.producto.codigoproducto, 0),
                                        new Printer.Data(17, midetalle.producto.nombreproducto, 0),
                                        new Printer.Data(5, Utils.RoundDecimal(midetalle.cantidad, 2).toString(), 1),
                                };
                                printer.printArray(Datos, 0, 0);
                            }
                            if (!mitransferencia.observacion.trim().equals("")) {
                                printer.printCustom("", 0, 1);
                                printer.printCustom("Observacion:", 0, 0);
                                printer.printCustom(mitransferencia.observacion, 0, 0);
                            }
                            printer.printCustom("", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("RECEPCION BODEGA/ALMACEN", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom(Constants.LINEAS_FIRMA, 0, 1);
                            printer.printCustom("ENTREGA  " + Constants.FORMATO_FECHA_IMPRESION, 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom(Constants.LINEAS_FIRMA, 0, 1);
                            printer.printCustom("RECIBE  " + Constants.FORMATO_FECHA_IMPRESION, 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("SALIDA BODEGA/ALMACEN", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom(Constants.LINEAS_FIRMA, 0, 1);
                            printer.printCustom("ENTREGA  " + Constants.FORMATO_FECHA_IMPRESION, 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom(Constants.LINEAS_FIRMA, 0, 1);
                            printer.printCustom("RECIBE  " + Constants.FORMATO_FECHA_IMPRESION, 0, 1);

                            //printer.printText(printer.leftRightAlign("Fecha: " + dateTime[0], dateTime[1]));
                            printer.printCustom("Usuario: " + SQLite.usuario.Usuario, 0, 0);
                            printer.printCustom("Fecha impresión: " + Utils.getDateFormat("yyyy-MM-dd HH:mm:ss"), 0, 0);
                            //tarea.Fecha = dateTime[0] + " " + dateTime[1];
                            printer.printNewLine();
                            printer.printCustom(strTipo, 0, 1);
                            printer.printUnicode();
                            printer.printNewLine();
                            if(i==0 && copias==2)
                                printer.printCustom("8<----------------------------------------", 0, 1);
                            printer.printNewLine();
                            printer.printNewLine();
                            printer.flush();
                        }
                        if(!reimpresion)
                            this.LimpiarDatos();
                    } catch (IOException e) {
                        fImp = false;
                        e.printStackTrace();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

            } else fImp = false;
        }catch (Exception e){
            Log.d(TAG, "imprimirFactura(): " + e.getMessage());
            fImp = false;
        }
        return fImp;
    }

    private void crearBottonSheet() {
        if(btsDialog==null){
            View view = LayoutInflater.from(this).inflate(R.layout.bottonsheet_message,null);
            btnPositive = view.findViewById(R.id.btnPositive);
            btnNegative = view.findViewById(R.id.btnNegative);
            lblMessage = view.findViewById(R.id.lblMessage);
            lblTitle = view.findViewById(R.id.lblTitle);
            viewSeparator = view.findViewById(R.id.vSeparator);
            btnPositive.setOnClickListener(this::onClick);
            btnNegative.setOnClickListener(this::onClick);

            btsDialog = new BottomSheetDialog(this, R.style.AlertDialogTheme);
            btsDialog.setContentView(view);
        }
    }

    public void showError(String message){
        crearBottonSheet();
        lblMessage.setText(message);
        btnNegative.setVisibility(View.GONE);
        viewSeparator.setVisibility(View.GONE);
        lblTitle.setVisibility(View.GONE);
        tipoAccion = "MESSAGE";
        if(btsDialog.getWindow()!=null)
            btsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        btsDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_BUSQUEDA_PRODUCTO:
                    detalleAdapter.visualizacion = false;
                    String message = "";
                    for (DetalleComprobante miP : productBusqueda) {
                        boolean band = true;
                        for(DetalleComprobante miP2: detalleAdapter.detalleComprobante){
                            if(miP.producto.idproducto.equals(miP2.producto.idproducto)
                                && miP.numerolote.equals(miP2.numerolote)){
                                band = false;
                                message+= "«"+miP.producto.nombreproducto+"» con lote «"+miP.numerolote+"» ya se encuentra agregado.\n";
                                break;
                            }
                        }
                        if(band)
                            detalleAdapter.detalleComprobante.add(miP);
                    }
                    mitransferencia.detalle.clear();
                    mitransferencia.detalle.addAll(detalleAdapter.detalleComprobante);
                    detalleAdapter.notifyDataSetChanged();
                    if(!message.equals(""))
                        Utils.showErrorDialog(TransferenciaActivity.this,"Error", message);
                    Log.d(TAG, String.valueOf(detalleAdapter.detalleComprobante.size()));
                    break;
                case DeviceList.REQUEST_CONNECT_BT:
                    try{
                        btsocket = DeviceList.getSocket();
                        if(btsocket!=null) {
                            Utils.showMessageShort(this,"Imprimiendo comprobante");
                            imprimirFactura(idtransferencia==0?"* ORIGINAL *": "* REIMPRESIÓN DE DOCUMENTO *",
                                    idtransferencia>0);
                            Log.d(TAG, "IMPRESORA SELECCIONADA");
                        }
                    }catch (Exception e){
                        Log.d(TAG,e.getMessage());
                    }
                    break;
                case REQUEST_BUSQUEDA_TRANSFERENCIA:
                    idtransferencia = data.getExtras().getInt("idcomprobante",0);
                    if(idtransferencia>0) {
                        BuscaTransferencia(idtransferencia);
                        toolbar.getMenu().findItem(R.id.option_reimprimir).setVisible(true);
                        toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        if(idtransferencia==0) {
            menu.findItem(R.id.option_reimprimir).setVisible(false);
        }else{
            menu.findItem(R.id.option_save).setVisible(false);
            menu.findItem(R.id.option_newdocument).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_save:
                if(!SQLite.usuario.VerificaPermiso(this,Constants.TRANSFERENCIA_INVENTARIO, "escritura")){
                    Banner.make(rootView,TransferenciaActivity.this,Banner.ERROR,"No tiene permisos para registrar transferencias de inventario.", Banner.BOTTOM,3000).show();
                    break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                        (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView)view.findViewById(R.id.lblTitle)).setText("Guardar transferencia");
                ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea guardar esta transferencia?");
                ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
                ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
                ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
                final AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                        GuardarDatos();
                        alertDialog.dismiss();
                    });

                view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

                if(alertDialog.getWindow()!=null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
                break;
            case R.id.option_reimprimir:
                ConsultaImpresion();
                break;
            case R.id.option_newdocument:
                LimpiarDatos();
                toolbar.getMenu().findItem(R.id.option_reimprimir).setVisible(false);
                toolbar.getMenu().findItem(R.id.option_save).setVisible(true);
                break;
            case R.id.option_listdocument:
                Intent i = new Intent(this, ListaComprobantesActivity.class);
                i.putExtra("tipobusqueda","4,20"); //TRANSFERENCIA - DEVOLUCIONES
                startActivityForResult(i, REQUEST_BUSQUEDA_TRANSFERENCIA);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        toolbar.setTitle("Transferencia de Inventario");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            ((TextView)view.findViewById(R.id.lblTitle)).setText("Cerrar");
            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Desea salir de la ventana de transferencias?");
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_check_white);
            ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
            ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(v ->finish());

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
