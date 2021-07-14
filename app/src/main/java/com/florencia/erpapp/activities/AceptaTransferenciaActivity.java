package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
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
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.DetalleRecepcionAdapter;
import com.florencia.erpapp.interfaces.IComprobante;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.DetalleComprobante;
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

public class AceptaTransferenciaActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_BUSQUEDA_RECEPCION = 1;
    private static String TAG = "TAGACEPTATRANSF_ACT";
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
    Integer idrecepcion = 0;
    List<Comprobante> listTransacciones = new ArrayList<>();
    LinearLayout lyCombo, lyDatosInformativos, lyProductos;
    TextView lblTransferencia, lblEnvia, lblRecibe, lblDocTransferencia, lblDocRecepcion, lblProducto;

    Button btnAceptar, btnRechazar;
    CardView cvBotones;
    View rootView;
    Retrofit retrofit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acepta_transferencia);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        rootView = findViewById(android.R.id.content);
        toolbar.setTitle("Entrega de Transferencia");
        init();

        cbTransferencias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    mitransferencia = listTransferencias.get(position);
                    if (mitransferencia.codigosistema > 0) {
                        BuscarDetalleTransferencia(mitransferencia.codigosistema);
                        Log.d(TAG, mitransferencia.codigotransaccion);
                    } else if (idrecepcion.equals(0)) {
                        detalleAdapter.detalleComprobante.clear();
                        detalleAdapter.notifyDataSetChanged();
                        cvBotones.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "cbTransferencia(): " + e.getMessage());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void init(){
        cbTransferencias = findViewById(R.id.cbTransferencias);
        rvDetalleProducto = findViewById(R.id.rvDetalleProductos);
        pbCargando = findViewById(R.id.pbCargando);
        btnRefresh = findViewById(R.id.btnRefresh);
        lyCombo = findViewById(R.id.lyCombo);
        lyDatosInformativos = findViewById(R.id.lyDatosInformativos);
        lblTransferencia = findViewById(R.id.lblTransferencia);
        lblEnvia = findViewById(R.id.lblEnvia);
        lblRecibe = findViewById(R.id.lblRecibe);
        lblDocTransferencia = findViewById(R.id.lblDocTransferencia);
        lblDocRecepcion = findViewById(R.id.lblDocRecepcion);
        lblProducto = findViewById(R.id.lblProducto);
        lyProductos = findViewById(R.id.lyProductos);
        cvBotones = findViewById(R.id.cvBotones);
        btnAceptar = findViewById(R.id.btnAceptar);
        btnRechazar = findViewById(R.id.btnRechazar);
        cvBotones.setVisibility(View.GONE);
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

        detalleAdapter = new DetalleRecepcionAdapter(this, detalleProductos, "", false, "8,23");
        rvDetalleProducto.setAdapter(detalleAdapter);

        btnRefresh.setOnClickListener(this::onClick);
        lblTransferencia.setOnClickListener(this::onClick);
        lblProducto.setOnClickListener(this::onClick);
        btnAceptar.setOnClickListener(this::onClick);
        btnRechazar.setOnClickListener(this::onClick);

        if (getIntent().getExtras() != null)
            idrecepcion = getIntent().getExtras().getInt("idcomprobante", 0);

        LlenarTransferencias(this);

        //if (idrecepcion > 0)
          //  BuscaTransferencia(idrecepcion);
    }

    private void LlenarTransferencias(Context context) {
        try {
            pgCargando.setTitle("Consultando transferencias");
            pgCargando.setMessage("Espere un momento...");
            pgCargando.setCancelable(false);
            //pgCargando.show();
            pbCargando.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Call<JsonObject> call = miInterface.getTransferencias(SQLite.usuario.Usuario, SQLite.usuario.Clave, SQLite.usuario.sucursal.IdEstablecimiento, 1);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful()) {
                        pbCargando.setVisibility(View.GONE);
                        btnRefresh.setVisibility(View.VISIBLE);
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            cbTransferencias.setAdapter(null);
                            listTransferencias.clear();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonTransferencias = obj.getAsJsonArray("transferencias");
                                if (jsonTransferencias != null) {
                                    Comprobante mitransfer = new Comprobante();
                                    for (JsonElement ele : jsonTransferencias) {
                                        JsonObject trans = ele.getAsJsonObject();
                                        mitransfer = new Comprobante();
                                        mitransfer.codigosistema = trans.get("idtransaccioninventario").getAsInt();
                                        mitransfer.codigotransaccion = trans.get("codigotransaccion").getAsString();
                                        mitransfer.tipotransaccion = trans.get("tipotransaccion").getAsString();
                                        mitransfer.fechadocumento = trans.get("fechadocumento").getAsString();
                                        listTransferencias.add(mitransfer);
                                    }
                                }
                            } //else
                                //Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, obj.get("message").getAsString(), Banner.BOTTOM, 2000).show();

                            Comprobante mitransfer = new Comprobante();
                            mitransfer.codigotransaccion = "Escoja una transferencia";
                            listTransferencias.add(0,mitransfer);
                            spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, listTransferencias);
                            cbTransferencias.setAdapter(spinnerAdapter);
                            cbTransferencias.setSelection(0, true);
                        } else
                            Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, "Error al cargar los datos.", Banner.BOTTOM, 2500).show();

                    } catch (Exception e) {
                        Log.d(TAG, "onResponse(): " + e.getMessage());
                    }
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(AceptaTransferenciaActivity.this, "Error", t.getMessage());
                    Log.d(TAG, "onFailure(): " + t.getMessage());
                    call.cancel();
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "LlenarTransferencias(): " + e.getMessage());
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void BuscarDetalleTransferencia(Integer idtransaccion) {
        try {
            pgCargando.setTitle("Buscando detalle");
            pgCargando.setMessage("Espere un momento...");
            pgCargando.setCancelable(false);
            //pgCargando.show();
            pbCargando.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);
            detalleProductos.clear();
            detalleAdapter.detalleComprobante.clear();
            detalleAdapter.notifyDataSetChanged();

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Call<JsonObject> call = miInterface.getDetalleTransferencia(idtransaccion);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful()) {
                        pbCargando.setVisibility(View.GONE);
                        btnRefresh.setVisibility(View.VISIBLE);
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonTransferencias = obj.getAsJsonArray("detalle");
                                if (jsonTransferencias != null) {
                                    detalleProductos.clear();
                                    detalleAdapter.detalleComprobante.clear();
                                    DetalleComprobante midetalle;
                                    for (JsonElement ele : jsonTransferencias) {
                                        JsonObject trans = ele.getAsJsonObject();
                                        midetalle = new DetalleComprobante();
                                        midetalle.producto.idproducto = trans.get("productoid").getAsInt();
                                        midetalle.producto.nombreproducto = trans.has("nombreproducto") ? trans.get("nombreproducto").getAsString() : "";
                                        midetalle.producto.codigoproducto = trans.has("codigoproducto") ? trans.get("codigoproducto").getAsString() : "";
                                        midetalle.numerolote = trans.has("numerolote") ? trans.get("numerolote").getAsString() : "";
                                        midetalle.fechavencimiento = trans.has("fechavencimiento") ? trans.get("fechavencimiento").getAsString() : "1900-01-01";
                                        midetalle.preciocosto = trans.has("preciocosto") ? trans.get("preciocosto").getAsDouble() : 0;
                                        midetalle.cantidad = trans.has("cantidad") ? trans.get("cantidad").getAsDouble() : 0;
                                        midetalle.marquetas = trans.has("marquetas") ? trans.get("marquetas").getAsDouble() : 0;
                                        detalleProductos.add(midetalle);
                                        Log.d(TAG, midetalle.producto.nombreproducto);
                                    }
                                    //detalleAdapter.detalleComprobante.addAll(detalleProductos);
                                    mitransferencia.detalle.addAll(detalleProductos);
                                    //mirecepcion.detalle.addAll(detalleProductos);
                                    detalleAdapter.notifyDataSetChanged();
                                    cvBotones.setVisibility(View.VISIBLE);
                                }
                            } else
                                Utils.showErrorDialog(AceptaTransferenciaActivity.this, "Error", obj.get("message").getAsString());
                        } else
                            Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, "Error al cargar los datos.", Banner.BOTTOM, 3000).show();

                    } catch (Exception e) {
                        Log.d(TAG, "onResponse(): " + e.getMessage());
                    }
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(AceptaTransferenciaActivity.this, "Error", t.getMessage());
                    Log.d(TAG, "onFailure(): " + t.getMessage());
                    call.cancel();
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "BuscarDetalleTransferencia(): " + e.getMessage());
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void DialogoGuardar(boolean aceptar){

        if (!SQLite.usuario.VerificaPermiso(this, Constants.RECEPCION_INVENTARIO, "escritura")) {
            Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, "No tiene permisos para registrar recepciones de inventario.", Banner.BOTTOM, 3000).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                (ConstraintLayout) findViewById(R.id.lyDialogContainer));
        builder.setView(view);
        ((TextView) view.findViewById(R.id.lblTitle)).setText("Confirmación");
        ((TextView) view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea "+(aceptar?"aceptar":"rechazar")+" el traslado y entrega de esta transferencia?");
        ((ImageView) view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
        ((Button) view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
        ((Button) view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            GeneraDatosGuardar(aceptar);
            alertDialog.dismiss();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

        if (alertDialog.getWindow() != null)
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        alertDialog.show();
    }

    private boolean ValidarDatos() {
        try {
            if (cbTransferencias.getAdapter() == null) {
                Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, "No hay datos para guardar.", Banner.BOTTOM, 3000).show();
                return false;
            }
            if (cbTransferencias.getSelectedItemPosition() == 0) {
                Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, "Escoja la tranferencia.", Banner.BOTTOM, 3000).show();
                return false;
            }
            if (detalleAdapter.detalleComprobante.size() == 0) {
                Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, "No hay productos por en la transferencia seleccionada. Consulte a soporte.", Banner.BOTTOM, 3000).show();
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "ValidarDatos(): " + e.getMessage());
            return false;
        }
        return true;
    }

    private void GeneraDatosGuardar(boolean aceptar){
        try {
            if(!ValidarDatos())
                return;
            mitransferencia.estadoresponsable = aceptar?"AC":"RE";
            mitransferencia.usuarioid = SQLite.usuario.IdUsuario;
            mitransferencia.fechacelular = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            mitransferencia.longdate = Utils.longDate(mitransferencia.fechacelular);
            mitransferencia.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;

            SQLite.gpsTracker.getLastKnownLocation();
            mitransferencia.lat = SQLite.gpsTracker.getLatitude();
            mitransferencia.lon = SQLite.gpsTracker.getLongitude();
            if (mitransferencia.lat == null)
                mitransferencia.lat = 0d;
            if (mitransferencia.lon == null)
                mitransferencia.lon = 0d;

            pgCargando.setTitle("Guardando datos");
            pgCargando.setMessage("Espere un momento...");
            pgCargando.setCancelable(false);
            pgCargando.show();

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Map<String, Object> post = new HashMap<>();
            post.put("usuario", SQLite.usuario.Usuario);
            post.put("clave", SQLite.usuario.Clave);
            post.put("transferencia", mitransferencia);
            Call<JsonObject> call = miInterface.estadoResponsableTransf(post);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful()) {
                        Utils.showErrorDialog(AceptaTransferenciaActivity.this,"Error",
                                "Código: " + response.code() + "\n- " + response.message());
                        pgCargando.dismiss();
                        return;
                    }
                    try {

                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonObject data = obj.getAsJsonObject("data");
                                if(data != null){
                                    mitransferencia.sucursalenvia = data.get("sucenvia").getAsString();
                                    mitransferencia.sucursalrecibe = data.get("sucrecibe").getAsString();
                                }
                                if (mitransferencia.Save(false)) {
                                    Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.SUCCESS, Constants.MSG_PROCESO_COMPLETADO, Banner.BOTTOM, 3000).show();
                                    //toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                                    LimpiarDatos();
                                    ConsultaImpresion();
                                }else
                                    Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, Constants.MSG_PROCESO_NO_COMPLETADO, Banner.BOTTOM, 3500).show();
                            }else
                                Utils.showErrorDialog(AceptaTransferenciaActivity.this, "Error", obj.get("message").getAsString());
                        }else
                            Banner.make(rootView, AceptaTransferenciaActivity.this, Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM, 3000).show();
                    } catch (JsonParseException ex) {
                        Log.d(TAG, ex.getMessage());
                    }
                    pgCargando.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(AceptaTransferenciaActivity.this, "Error", t.getMessage());
                    Log.d(TAG, t.getMessage());
                    pgCargando.dismiss();
                }
            });
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }
    private void LimpiarDatos() {
        try {
            mitransferencia = new Comprobante();
            mirecepcion = new Comprobante();
            listTransferencias.clear();
            listTransferencias.clear();
            detalleProductos.clear();
            detalleAdapter.detalleComprobante.clear();
            detalleAdapter.notifyDataSetChanged();
            cbTransferencias.setEnabled(true);
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
            idrecepcion = 0;
            lyCombo.setVisibility(View.VISIBLE);
            lyDatosInformativos.setVisibility(View.GONE);
            toolbar.setSubtitle("");
            LlenarTransferencias(this);
        } catch (Exception e) {
            Log.d(TAG, "LimpiarDatos(): " + e.getMessage());
        }
    }
    private void ConsultaImpresion(){}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRefresh:
                LlenarTransferencias(v.getContext());
                break;
            case R.id.lblTransferencia:
                if (idrecepcion > 0)
                    Utils.EfectoLayout(lyDatosInformativos, lblTransferencia);
                else
                    Utils.EfectoLayout(lyCombo, lblTransferencia);
                break;
            case R.id.lblProducto:
                Utils.EfectoLayout(lyProductos, lblProducto);
                break;
            case R.id.btnAceptar:
            case R.id.btnRechazar:
                DialogoGuardar(v.getId()==R.id.btnAceptar);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        menu.findItem(R.id.option_save).setVisible(false);
        if (idrecepcion == 0) {
            menu.findItem(R.id.option_reimprimir).setVisible(false);
        } else {
            menu.findItem(R.id.option_newdocument).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_reimprimir:
                ConsultaImpresion();
                break;
            case R.id.option_newdocument:
                LimpiarDatos();
                toolbar.getMenu().findItem(R.id.option_reimprimir).setVisible(false);
                break;
            case R.id.option_listdocument:
                Intent i = new Intent(this, ListaComprobantesActivity.class);
                i.putExtra("tipobusqueda", "5"); //RECEPCION - RECEPDEVOLUCION
                startActivityForResult(i, REQUEST_BUSQUEDA_RECEPCION);
                overridePendingTransition(R.anim.left_in, R.anim.left_out);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        toolbar.setTitle("Entrega de Transferencia");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            ((TextView) view.findViewById(R.id.lblTitle)).setText("Cerrar");
            ((TextView) view.findViewById(R.id.lblMessage)).setText("¿Desea salir de la ventana de recepción?");
            ((ImageView) view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_check_white);
            ((Button) view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
            ((Button) view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                onBackPressed();
                overridePendingTransition(R.anim.zoom_back_in, R.anim.zoom_back_out);
            });

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

            if (alertDialog.getWindow() != null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
