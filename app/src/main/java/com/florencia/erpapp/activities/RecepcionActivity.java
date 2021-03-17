package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.DetallePedidoAdapter;
import com.florencia.erpapp.adapters.DetalleRecepcionAdapter;
import com.florencia.erpapp.interfaces.ComprobanteInterface;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.DetalleComprobante;
import com.florencia.erpapp.models.DetallePedido;
import com.florencia.erpapp.models.Producto;
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
public class RecepcionActivity extends AppCompatActivity {
    private static final int REQUEST_BUSQUEDA_RECEPCION = 1;
    Spinner cbTransferencias;
    RecyclerView rvDetalleProducto;
    ProgressDialog pgCargando;
    OkHttpClient okHttpClient;
    List<Comprobante> listTransferencias = new ArrayList<>();
    DetalleRecepcionAdapter detalleAdapter;
    List<DetalleComprobante> detalleProductos = new ArrayList<>();
    Comprobante mitransferencia, mirecepcion;
    SpinnerAdapter spinnerAdapter;
    Toolbar toolbar;
    ProgressBar pbCargando;
    ImageButton btnRefresh;
    TextView lblObservacion;
    EditText tvObservacion;
    Integer idrecepcion=0;
    List<Comprobante> listTransacciones= new ArrayList<>();
    LinearLayout lyCombo, lyDatosInformativos, lyProductos;
    TextView lblTransferencia, lblEnvia, lblRecibe, lblDocTransferencia, lblDocRecepcion, lblProducto;

    TextView lblMessage, lblTitle;
    BottomSheetDialog btsDialog;
    Button btnPositive, btnNegative;
    View viewSeparator, rootView;
    String tipoAccion="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recepcion);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        rootView = findViewById(android.R.id.content);
        toolbar.setTitle("Recepción de Inventario");
        init();

        cbTransferencias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    mitransferencia = listTransferencias.get(position);
                    if(mitransferencia.codigosistema>0) {
                        BuscarDetalleTransferencia(mitransferencia.codigosistema, view.getContext());
                        Log.d("TAG",mitransferencia.codigotransaccion);
                    }else if(idrecepcion.equals(0)){
                        detalleAdapter.detalleComprobante.clear();
                        detalleAdapter.notifyDataSetChanged();
                    }
                }catch (Exception e){
                    Log.d("TAGRECEPCION_ACT", "cbTransferencia(): " + e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void init(){
        cbTransferencias = findViewById(R.id.cbTransferencias);
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
        lblDocRecepcion = findViewById(R.id.lblDocRecepcion);
        lblProducto = findViewById(R.id.lblProducto);
        lyProductos = findViewById(R.id.lyProductos);
        pgCargando = new ProgressDialog(this);

        okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        detalleAdapter = new DetalleRecepcionAdapter(this,detalleProductos,"",false, "8,23");
        rvDetalleProducto.setAdapter(detalleAdapter);

        btnRefresh.setOnClickListener(onClick);
        lblObservacion.setOnClickListener(onClick);
        lblTransferencia.setOnClickListener(onClick);
        lblProducto.setOnClickListener(onClick);

        if(getIntent().getExtras()!=null)
            idrecepcion = getIntent().getExtras().getInt("idcomprobante",0);

        LlenarTransferencias(this);

        if(idrecepcion>0)
            BuscaRecepcion(idrecepcion);
    }

    private View.OnClickListener onClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnRefresh:
                    LlenarTransferencias(v.getContext());
                    break;
                case R.id.lblObservacion:
                    Utils.EfectoLayout(tvObservacion,lblObservacion);
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
                    if(idrecepcion>0)
                        Utils.EfectoLayout(lyDatosInformativos, lblTransferencia);
                    else
                        Utils.EfectoLayout(lyCombo, lblTransferencia);
                    break;
                case R.id.lblProducto:
                    Utils.EfectoLayout(lyProductos, lblProducto);
                    break;
            }
        }
    };

    private void LlenarTransferencias(Context context) {
        try{
            if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                Banner.make(rootView,this,Banner.ERROR,Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM, 3000).show();
                return;
            }

            pgCargando.setTitle("Iniciando sesión");
            pgCargando.setMessage("Espere un momento...");
            pgCargando.setCancelable(false);
            //pgCargando.show();
            pbCargando.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(SQLite.configuracion.url_ws)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();
            ComprobanteInterface miInterface = retrofit.create(ComprobanteInterface.class);
            Call<JsonObject> call = miInterface.getTransferencias(SQLite.usuario.Usuario,SQLite.usuario.Clave,SQLite.usuario.sucursal.IdEstablecimiento);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        showError("Código error: " + response.code());
                        pbCargando.setVisibility(View.GONE);
                        btnRefresh.setVisibility(View.VISIBLE);
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            cbTransferencias.setAdapter(null);
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonTransferencias = obj.getAsJsonArray("transferencias");
                                if(jsonTransferencias!=null){
                                    listTransferencias.clear();
                                    Comprobante mitransfer = new Comprobante();
                                    mitransfer.codigotransaccion="Escoja una transferencia";
                                    listTransferencias.add(mitransfer);
                                    for (JsonElement ele : jsonTransferencias) {
                                        JsonObject trans = ele.getAsJsonObject();
                                        mitransfer = new Comprobante();
                                        mitransfer.codigosistema = trans.get("idtransaccioninventario").getAsInt();
                                        mitransfer.codigotransaccion = trans.get("codigotransaccion").getAsString();
                                        mitransfer.tipotransaccion = trans.get("tipotransaccion").getAsString();
                                        listTransferencias.add(mitransfer);
                                    }
                                    spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, listTransferencias);
                                    cbTransferencias.setAdapter(spinnerAdapter);
                                    cbTransferencias.setSelection(0,true);
                                }
                            }else
                                Banner.make(rootView,RecepcionActivity.this,Banner.ERROR, obj.get("message").getAsString(),Banner.BOTTOM,2000).show();
                        }else
                            Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,"Error al cargar los datos.",Banner.BOTTOM,2500).show();

                    }catch (Exception e){
                        Log.d("TAGRECEPCION_ACT","onResponse(): " + e.getMessage());
                    }
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(RecepcionActivity.this,"Error",t.getMessage());
                    Log.d("TAGRECEPCION_ACT", "onFailure(): " + t.getMessage());
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }
            });
        }catch (Exception e){
            Log.d("TAGRECEPCION_ACT", "LlenarTransferencias(): " + e.getMessage());
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void BuscarDetalleTransferencia(Integer idtransaccion, Context context) {
        try{
            if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM,3000).show();
                return;
            }

            pgCargando.setTitle("Buscando detalle");
            pgCargando.setMessage("Espere un momento...");
            pgCargando.setCancelable(false);
            //pgCargando.show();
            pbCargando.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);
            detalleProductos.clear();
            detalleAdapter.detalleComprobante.clear();
            detalleAdapter.notifyDataSetChanged();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(SQLite.configuracion.url_ws)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();
            ComprobanteInterface miInterface = retrofit.create(ComprobanteInterface.class);
            Call<JsonObject> call=null;
            call=miInterface.getDetalleTransferencia(idtransaccion);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        showError("Error Código: " + response.code());
                        pbCargando.setVisibility(View.GONE);
                        btnRefresh.setVisibility(View.VISIBLE);
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonTransferencias = obj.getAsJsonArray("detalle");
                                if(jsonTransferencias!=null){
                                    detalleProductos.clear();
                                    detalleAdapter.detalleComprobante.clear();
                                    DetalleComprobante midetalle;
                                    for (JsonElement ele : jsonTransferencias) {
                                        JsonObject trans = ele.getAsJsonObject();
                                        midetalle = new DetalleComprobante();
                                        midetalle.producto.idproducto = trans.get("productoid").getAsInt();
                                        midetalle.producto.nombreproducto = trans.has("nombreproducto")? trans.get("nombreproducto").getAsString():"";
                                        midetalle.producto.codigoproducto = trans.has("codigoproducto")?trans.get("codigoproducto").getAsString():"";
                                        midetalle.numerolote = trans.has("numerolote")? trans.get("numerolote").getAsString():"";
                                        midetalle.fechavencimiento = trans.has("fechavencimiento")? trans.get("fechavencimiento").getAsString():"1900-01-01";
                                        midetalle.preciocosto = trans.has("preciocosto")? trans.get("preciocosto").getAsDouble():0;
                                        midetalle.cantidad = trans.has("cantidad")?trans.get("cantidad").getAsDouble():0;
                                        midetalle.marquetas = trans.has("marquetas")?trans.get("marquetas").getAsDouble():0;
                                        detalleProductos.add(midetalle);
                                        Log.d("TAG",midetalle.producto.nombreproducto);
                                    }
                                    //detalleAdapter.detalleComprobante.addAll(detalleProductos);
                                    mitransferencia.detalle.addAll(detalleProductos);
                                    //mirecepcion.detalle.addAll(detalleProductos);
                                    detalleAdapter.notifyDataSetChanged();
                                }
                            }else
                                Utils.showErrorDialog(RecepcionActivity.this,"Error", obj.get("message").getAsString());
                        }else
                            Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,"Error al cargar los datos.", Banner.BOTTOM,3000).show();

                    }catch (Exception e){
                        Log.d("TAGRECEPCION_ACT","onResponse(): " + e.getMessage());
                    }
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(RecepcionActivity.this,"Error",t.getMessage());
                    Log.d("TAGRECEPCION_ACT", "onFailure(): " + t.getMessage());
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }
            });
        }catch (Exception e){
            Log.d("TAGRECEPCION_ACT", "BuscarDetalleTransferencia(): " + e.getMessage());
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        if(idrecepcion==0) {
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
                if(!SQLite.usuario.VerificaPermiso(this,Constants.RECEPCION_INVENTARIO, "escritura")){
                    Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,"No tiene permisos para registrar recepciones de inventario.", Banner.BOTTOM,3000).show();
                    break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                        (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView)view.findViewById(R.id.lblTitle)).setText("Guardar recepción");
                ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea recibir esta transferencia?");
                ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
                ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
                ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
                final AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnConfirm).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GuardarDatos();
                        alertDialog.dismiss();
                    }
                });

                view.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { alertDialog.dismiss();}
                });

                if(alertDialog.getWindow()!=null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
                break;
            case R.id.option_reimprimir:
                /*try {
                    if (Printer.btsocket == null) {
                        Utils.showMessage(this, "Emparejar la impresora...");
                        Intent BTIntent = new Intent(this.getApplicationContext(), DeviceList.class);
                        startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
                        return true;
                    }
                }catch (Exception e){
                    Log.d("TAGIMPRIMIR3", e.getMessage());
                }
                imprimirFactura("* REIMPRESIÓN DE RECEPCIÓN *",true);*/
                ConsultaImpresion();
                break;
            case R.id.option_newdocument:
                LimpiarDatos();
                toolbar.getMenu().findItem(R.id.option_reimprimir).setVisible(false);
                toolbar.getMenu().findItem(R.id.option_save).setVisible(true);
                break;
            case R.id.option_listdocument:
                Intent i = new Intent(this, ListaComprobantesActivity.class);
                i.putExtra("tipobusqueda","8,23"); //RECEPCION - RECEPDEVOLUCION
                startActivityForResult(i, REQUEST_BUSQUEDA_RECEPCION);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void GuardarDatos() {
        try{
            if(!ValidarDatos()) return;

            listTransacciones.clear();
            String tiptransaccion = listTransferencias.get(cbTransferencias.getSelectedItemPosition()).tipotransaccion;
            mitransferencia.tipotransaccion = tiptransaccion.equals("21")?"22":"7";
            mitransferencia.total = 0d;
            mitransferencia.codigoestablecimiento = SQLite.usuario.sucursal.CodigoEstablecimiento;
            mitransferencia.observacion = tvObservacion.getText().toString().trim();
            mitransferencia.usuarioid = SQLite.usuario.IdUsuario;
            mitransferencia.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            mitransferencia.fechacelular = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            mitransferencia.fechadocumento = Utils.getDateFormat("yyyy-MM-dd");
            for(DetalleComprobante midetalle:mitransferencia.detalle)
                mitransferencia.total += midetalle.Subtotalcosto();
            mitransferencia.subtotal = mitransferencia.total;

            listTransacciones.add(mitransferencia);

            mirecepcion = new Comprobante();
            mirecepcion.tipotransaccion = tiptransaccion.equals("21")?"23":"8";
            mirecepcion.codigosistema = mitransferencia.codigosistema;
            mirecepcion.total = 0d;
            mirecepcion.codigoestablecimiento = SQLite.usuario.sucursal.CodigoEstablecimiento;
            mirecepcion.observacion = tvObservacion.getText().toString().trim();
            mirecepcion.usuarioid = SQLite.usuario.IdUsuario;
            mirecepcion.total = mitransferencia.total;
            mirecepcion.subtotal = mitransferencia.subtotal;
            mirecepcion.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            mirecepcion.fechacelular = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            mirecepcion.fechadocumento = Utils.getDateFormat("yyyy-MM-dd");
            mirecepcion.detalle.addAll(mitransferencia.detalle);

            listTransacciones.add(mirecepcion);

            EnviarDatos(this, tiptransaccion);
        }catch (Exception e){
            Log.d("TAGRECEPCION_ACT", "GuardarDatos(): " + e.getMessage());
        }
    }

    private void EnviarDatos(Context context, String tipotrans) {
        try{
            if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)){
                showError(Constants.MSG_COMPROBAR_CONEXION_INTERNET);
                return;
            }
            if(listTransacciones.size()>0){
                pgCargando.setTitle("Guardando recepción");
                pgCargando.setMessage("Espere un momento...");
                pgCargando.setCancelable(false);
                pgCargando.show();

                Gson gson = new GsonBuilder()
                        .setLenient()
                        .create();

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(SQLite.configuracion.url_ws)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .client(okHttpClient)
                        .build();
                ComprobanteInterface miInterface = retrofit.create(ComprobanteInterface.class);

                Map<String,Object> post = new HashMap<>();
                post.put("usuario",SQLite.usuario.Usuario);
                post.put("clave",SQLite.usuario.Clave);
                post.put("codigotransferencia", mitransferencia.codigotransaccion);
                post.put("puntoemisionid", SQLite.usuario.sucursal.IdPuntoEmision);
                post.put("transacciones", listTransacciones);
                post.put("tipotransferencia", tipotrans);
                String json = post.toString();
                Log.d("TAGJSON", json);
                Call<JsonObject> call=null;
                call=miInterface.saveRecepcion(post);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if(!response.isSuccessful()){
                            showError("Error código:" + response.code());
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
                                            //INSERTAR RECEPCION EN LA BD LOCAL
                                            mirecepcion.codigosistema = trans.get("codigosistema").getAsInt();
                                            mirecepcion.codigotransaccion = trans.get("codigotransaccion").getAsString();
                                            mirecepcion.sucursalenvia = trans.get("sucursalenvia").getAsString();
                                            mirecepcion.sucursalrecibe = SQLite.usuario.sucursal.IdEstablecimiento + "-" + SQLite.usuario.sucursal.NombreSucursal;
                                            mirecepcion.claveacceso = mitransferencia.codigotransaccion;
                                            mirecepcion.longdate = Utils.longDate(mirecepcion.fechadocumento);

                                            if(mirecepcion.Save(false)){
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
                                                            Log.d("TAG", prod.get("nombreproducto").getAsString());
                                                        }
                                                    }
                                                    if (numProd == jsonProductos.size())
                                                        Banner.make(rootView,RecepcionActivity.this,Banner.SUCCESS,Constants.MSG_PROCESO_COMPLETADO, Banner.BOTTOM,3000).show();
                                                    else
                                                        Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,Constants.MSG_PROCESO_NO_COMPLETADO, Banner.BOTTOM,3500).show();
                                                }
                                                ConsultaImpresion();
                                                //LimpiarDatos();
                                            }
                                            break;
                                        }
                                    }

                                } else
                                    Utils.showErrorDialog(RecepcionActivity.this,"Error",obj.get("message").getAsString());
                            } else {
                                Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM,3000).show();
                            }
                        }catch (JsonParseException ex){
                            Log.d("TAG", ex.getMessage());
                        }
                        pgCargando.dismiss();
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Utils.showErrorDialog(RecepcionActivity.this,"Error",t.getMessage());
                        Log.d("TAG", t.getMessage());
                        pgCargando.dismiss();
                    }
                });
            }
        }catch (Exception e){
            Log.d("TAGRECEPCION_ACT", "EnviarDatos(): " + e.getMessage());
            pgCargando.dismiss();
        }
    }

    private void ConsultaImpresion() {
        try{
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            ((TextView)view.findViewById(R.id.lblTitle)).setText("Imprimir");
            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Desea imprimir este documento?");
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
            ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
            ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Printer.btsocket == null) {
                        Utils.showMessage(getApplicationContext(), "Emparejar la impresora...");
                        Intent BTIntent = new Intent(getApplicationContext(), DeviceList.class);
                        startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
                        return;
                    }else {
                        imprimirFactura(idrecepcion==0?"* ORIGINAL *":"* REIMPRESIÓN DE RECEPCIÓN *", idrecepcion>0);
                    }
                    alertDialog.dismiss();
                }
            });

            view.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(idrecepcion==0)
                        LimpiarDatos();
                    alertDialog.dismiss();
                }
            });

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
        }catch (Exception e){
            Log.d("TAGRECEPCION_ACT", "ConsultaImpresion(): "+ e.getMessage());
        }
    }

    private void LimpiarDatos() {
        try{
            mitransferencia = new Comprobante();
            mirecepcion = new Comprobante();
            listTransferencias.clear();
            listTransferencias.clear();
            detalleProductos.clear();
            detalleAdapter.detalleComprobante.clear();
            detalleAdapter.notifyDataSetChanged();
            tvObservacion.setText("");
            cbTransferencias.setEnabled(true);
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
            idrecepcion = 0;
            lyCombo.setVisibility(View.VISIBLE);
            lyDatosInformativos.setVisibility(View.GONE);
            lblTransferencia.setText("TRANSFERENCIA A RECIBIR");
            tvObservacion.setEnabled(true);
            toolbar.setSubtitle("");
            LlenarTransferencias(this);
        }catch (Exception e){
            Log.d("TAGRECEPCION_ACT", "LimpiarDatos(): "+ e.getMessage());
        }
    }

    private boolean ValidarDatos(){
        try{
            if(cbTransferencias.getAdapter()==null){
                Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,"No hay datos para guardar.", Banner.BOTTOM,3000).show();
                return false;
            }
            if(cbTransferencias.getSelectedItemPosition()==0){
                Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,"Escoja la tranferencia/devolución a recibir.", Banner.BOTTOM,3000).show();
                return false;
            }
            if(detalleAdapter.detalleComprobante.size()==0){
                Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,"No hay productos por recibir en la transferencia seleccionada.",Banner.BOTTOM,3000).show();
                return false;
            }
        }catch (Exception e){
            Log.d("TAGRECEPCION_ACT", "ValidarDatos(): " + e.getMessage());
            return false;
        }
        return true;
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
                            printer.printCustom("**RECEPCION"+ (mirecepcion.tipotransaccion.equals("22")?" POR DEVOLUCION":"") +" DE INVENTARIO**", 0, 1);
                            printer.printCustom("Envia  : ".concat(mirecepcion.sucursalenvia.split("-")[1]), 0, 0);
                            printer.printCustom("N. Doc.: ".concat(mirecepcion.claveacceso), 0, 0);
                            printer.printCustom("Recibe : ".concat(mirecepcion.sucursalrecibe.split("-")[1]), 0, 0);
                            printer.printCustom("N. Doc.: ".concat(mirecepcion.codigotransaccion), 0, 0);
                            printer.printCustom("Fecha  : ".concat(mirecepcion.fechacelular), 0, 0);
                            printer.printCustom("------------------------------------------", 0, 1);
                            printer.printCustom(" Num. |  Código  |     Nombre     | Cant", 0, 0);
                            printer.printCustom("------------------------------------------", 0, 1);
                            for (DetalleComprobante midetalle : mirecepcion.detalle) {
                                Printer.Data[] Datos = new Printer.Data[]{
                                        new Printer.Data(6, midetalle.linea.toString(), 0),
                                        new Printer.Data(12, midetalle.producto.codigoproducto, 0),
                                        new Printer.Data(17, midetalle.producto.nombreproducto, 0),
                                        new Printer.Data(5, Utils.RoundDecimal(midetalle.cantidad, 2).toString(), 1),
                                };
                                printer.printArray(Datos, 0, 0);
                            }
                            if (!mirecepcion.observacion.trim().equals("")) {
                                printer.printCustom("", 0, 1);
                                printer.printCustom("Observacion:", 0, 0);
                                printer.printCustom(mirecepcion.observacion, 0, 0);
                            }
                            printer.printCustom("", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("________________", 0, 1);
                            printer.printCustom("Entrega", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("", 0, 1);
                            printer.printCustom("________________", 0, 1);
                            printer.printCustom("Recibe", 0, 1);

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
            Log.d("TAGIMPRIMIR1", e.getMessage());
            fImp = false;
        }
        return fImp;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_BUSQUEDA_RECEPCION:
                    idrecepcion = data.getExtras().getInt("idcomprobante",0);
                    if(idrecepcion>0) {
                        BuscaRecepcion(idrecepcion);
                        toolbar.getMenu().findItem(R.id.option_reimprimir).setVisible(true);
                        toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                    }
                    break;
                case DeviceList.REQUEST_CONNECT_BT:
                    try{
                        btsocket = DeviceList.getSocket();
                        if(btsocket!=null) {
                            Utils.showMessageShort(this,"Imprimiendo comprobante");
                            imprimirFactura(idrecepcion==0?"* ORIGINAL *": "* REIMPRESIÓN DE DOCUMENTO *",
                                    idrecepcion>0);
                            Log.d("TAGIMPRIMIR2", "IMPRESORA SELECCIONADA");
                        }
                    }catch (Exception e){
                        Log.d("TAGIMPRIMIR2",e.getMessage());
                    }
                    break;
            }
        }
    }

    private void BuscaRecepcion(Integer idrecepcion) {
        pbCargando.setVisibility(View.VISIBLE);
        btnRefresh.setVisibility(View.GONE);

        try {

            Thread th = new Thread() {
                @Override
                public void run() {
                    mirecepcion = new Comprobante();
                    mirecepcion = Comprobante.get(idrecepcion);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mirecepcion != null) {
                                toolbar.setSubtitle("Fecha: " + Utils.fechaMes(mirecepcion.fechadocumento));
                                cbTransferencias.setEnabled(false);
                                detalleAdapter.visualizacion = true;
                                detalleProductos.clear();
                                detalleProductos.addAll(mirecepcion.detalle);
                                detalleAdapter.detalleComprobante.clear();
                                detalleAdapter.detalleComprobante.addAll(mirecepcion.detalle);
                                detalleAdapter.notifyDataSetChanged();
                                tvObservacion.setText(mirecepcion.observacion);
                                tvObservacion.setEnabled(false);
                                lyCombo.setVisibility(View.GONE);
                                lyDatosInformativos.setVisibility(View.VISIBLE);
                                lblTransferencia.setText("DATOS INFORMATIVOS");
                                lblEnvia.setText(mirecepcion.sucursalenvia.split("-")[1]);
                                lblDocTransferencia.setText(mirecepcion.claveacceso);
                                lblRecibe.setText(mirecepcion.sucursalrecibe.split("-")[1]);
                                lblDocRecepcion.setText(mirecepcion.codigotransaccion);
                            } else {
                                Banner.make(rootView,RecepcionActivity.this,Banner.ERROR,"Ocurrió un error al obtener los datos para este documento.",Banner.BOTTOM,3000).show();
                            }
                            pbCargando.setVisibility(View.GONE);
                        }
                    });
                }
            };
            th.start();
        }catch (Exception e){
            Log.d("TAGRECEPCION_ACT", "BuscaRecepcion(): " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        toolbar.setTitle("Recepción de Inventario");
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
            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Desea salir de la ventana de recepción?");
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_check_white);
            ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
            ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            view.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { alertDialog.dismiss();}
            });

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void crearBottonSheet() {
        if(btsDialog==null){
            View view = LayoutInflater.from(this).inflate(R.layout.bottonsheet_message,null);
            btnPositive = view.findViewById(R.id.btnPositive);
            btnNegative = view.findViewById(R.id.btnNegative);
            lblMessage = view.findViewById(R.id.lblMessage);
            lblTitle = view.findViewById(R.id.lblTitle);
            viewSeparator = view.findViewById(R.id.vSeparator);
            btnPositive.setOnClickListener(onClick);
            btnNegative.setOnClickListener(onClick);

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
}
