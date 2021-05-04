package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.DetalleComprobanteAdapter;
import com.florencia.erpapp.fragments.InfoDialogFragment;
import com.florencia.erpapp.interfaces.ICliente;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.DetalleComprobante;
import com.florencia.erpapp.models.Lote;
import com.florencia.erpapp.models.Sucursal;
import com.florencia.erpapp.services.DeviceList;
import com.florencia.erpapp.services.Printer;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.shasin.notificationbanner.Banner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.florencia.erpapp.services.Printer.btsocket;
public class ComprobanteActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int REQUEST_BUSQUEDA = 1;
    public static final int REQUEST_CLIENTE = 2;
    public static final int REQUEST_BUSQUEDA_COMPROBANTE = 3;
    public static String TAG = "TAGCOMPROBANTE_ACT";
    Button btnBuscarProducto;
    EditText txtCliente;
    RecyclerView rvDetalle;
    Cliente cliente = new Cliente();
    Comprobante comprobante = new Comprobante();
    DetalleComprobanteAdapter detalleAdapter;
    List<DetalleComprobante> detalleProductos = new ArrayList<>();
    public static final List<DetalleComprobante> productBusqueda = new ArrayList<>();
    public TextView lblTotal, lblSubtotales;
    public LinearLayout lySubtotales;
    Integer idcomprobante=0;
    ProgressDialog pgCargando;
    Toolbar toolbar;
    ImageButton btViewSubtotales;

    //CONTROLES DEL DIALOG_BOTTOMSHEET
    TextView lblMessage, lblTitle, lblCliente, lblProducto, lblLeyendaCF, lblEstablecimiento;
    LinearLayout lyCliente, lyProductos, lyBotones, lyFormaPago, lyEstablecimiento;
    BottomSheetDialog btsDialog;
    Button btnPositive, btnNegative;
    public Button btnCambiaEstablecimiento;
    View viewSeparator, rootView;
    RadioButton rbEfectivo, rbCredito;
    String tipoAccion="";
    OkHttpClient okHttpClient;
    Retrofit retrofit;

    Integer posEstablec = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comprobante);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootView = findViewById(android.R.id.content);
        toolbar.setTitle("Nueva factura");
        init();

        txtCliente.setOnKeyListener(
            (v, keyCode, event) -> {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    Intent i = new Intent(v.getContext(), ClienteBusquedaActivity.class);
                    i.putExtra("busqueda", txtCliente.getText().toString().trim());
                    startActivityForResult(i, REQUEST_CLIENTE);
                    return true;
                }
                return false;
            }
        );

        txtCliente.setOnTouchListener(
            (v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= txtCliente.getRight() - txtCliente.getTotalPaddingRight()) {
                        txtCliente.setText("");
                        cliente = new Cliente();
                        cliente = Cliente.get(0);
                        if (cliente == null)
                            cliente = Cliente.get("9999999999999");

                        rbEfectivo.setChecked(true);
                        rbCredito.setEnabled(false);
                        rbCredito.setText("Crédito (Disp:$0.00)");
                        cliente.montocredito = 0d;
                        cliente.deudatotal = 0d;
                        cliente.montodisponible = 0d;

                        detalleAdapter.categoria = "0";
                        detalleAdapter.isCredito = false;
                        detalleAdapter.CambiarPrecio("0", false);
                        detalleAdapter.notifyDataSetChanged();
                        return true;
                    } else if (event.getRawX() <= txtCliente.getTotalPaddingLeft()
                            && cliente.idcliente > 0 && !cliente.nip.contains("999999999")) {
                        MostrarInfoDialog(cliente.idcliente);
                    }
                }
                return false;
            }
        );

        txtCliente.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try{
                    if(s.length()>0 && idcomprobante == 0)
                        txtCliente.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_user,0, R.drawable.ic_close,0);
                    else
                        txtCliente.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_user,0, 0,0);
                }catch (Exception e){
                    Log.d(TAG, e.getMessage());
                }
            }
        });
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

    private void init(){
        pgCargando = new ProgressDialog(this);
        pgCargando.setTitle("Cargando factura");
        pgCargando.setMessage("Espere un momento...");
        pgCargando.setCancelable(false);

        btnBuscarProducto = findViewById(R.id.btnBuscarProducto);
        txtCliente = findViewById(R.id.txtCliente);
        rvDetalle = findViewById(R.id.rvDetalleProductos);
        lblTotal = findViewById(R.id.lblTotal);
        lblSubtotales = findViewById(R.id.tvsubtotales);
        lySubtotales = findViewById(R.id.lySubtotales);
        btViewSubtotales = findViewById(R.id.btViewSubtotales);
        lblCliente = findViewById(R.id.lblCliente);
        lblProducto = findViewById(R.id.lblProducto);
        lyCliente = findViewById(R.id.lyCliente);
        lyProductos = findViewById(R.id.lyProductos);
        lyBotones = findViewById(R.id.lyBotones);
        lblLeyendaCF = findViewById(R.id.lblLeyendaCF);
        lyFormaPago = findViewById(R.id.lyFormaPago);
        rbEfectivo = findViewById(R.id.rbEfectivo);
        rbCredito = findViewById(R.id.rbCredito);
        lblEstablecimiento = findViewById(R.id.lblEstablecimiento);
        btnCambiaEstablecimiento = findViewById(R.id.btnCambiaEstablecimiento);
        lyEstablecimiento = findViewById(R.id.lyEstablecimiento);
        lyEstablecimiento.setVisibility(View.VISIBLE);
        lyFormaPago.setVisibility(View.VISIBLE);
        rbCredito.setEnabled(false);

        lblTotal.setOnClickListener(this::onClick);
        lySubtotales.setOnClickListener(this::onClick);
        btViewSubtotales.setOnClickListener(this::onClick);
        btnBuscarProducto.setOnClickListener(this::onClick);
        lblCliente.setOnClickListener(this::onClick);
        lblProducto.setOnClickListener(this::onClick);
        btnCambiaEstablecimiento.setOnClickListener(this::onClick);

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            lblLeyendaCF.setText(Html.fromHtml(getResources().getString(R.string.leyendaConsumidorFinal), Html.FROM_HTML_MODE_COMPACT));
        else
            lblLeyendaCF.setText(Html.fromHtml(getResources().getString(R.string.leyendaConsumidorFinal)));

        rbEfectivo.setChecked(true);
        rbCredito.setEnabled(false);
        rbCredito.setText("Crédito (Disp:$0.00)");
        cliente.montocredito = 0d;
        cliente.deudatotal = 0d;
        cliente.montodisponible = 0d;

        if(getIntent().getExtras()!=null){
            int idcliente = getIntent().getExtras().getInt("idcliente",0);
            if(idcliente>0) {
                cliente = Cliente.get(idcliente);
                comprobante.cliente = cliente;
                txtCliente.setText(cliente.razonsocial);
                if(cliente.codigosistema>0) {
                    ConsultarDeudaCliente(ComprobanteActivity.this, cliente.codigosistema);
                }
            }
            idcomprobante = getIntent().getExtras().getInt("idcomprobante",0);
        }

        if(cliente.nip.equals("")){
            cliente = Cliente.get(0);
            if(cliente == null)
                cliente = Cliente.get("9999999999999");
        }

        detalleProductos = new ArrayList<>();
        detalleAdapter = new DetalleComprobanteAdapter(this, detalleProductos, cliente.categoria.equals("")?"0":cliente.categoria, idcomprobante>0);
        rvDetalle.setAdapter(detalleAdapter);

        if(idcomprobante>0){
            BuscaComprobante(idcomprobante);
        }

        rbEfectivo.setOnCheckedChangeListener(
            (buttonView, isChecked) -> {
                if(idcomprobante > 0)
                    return;
                if(isChecked){
                    detalleAdapter.isCredito = false;
                    detalleAdapter.CambiarPrecio(cliente.categoria, false);
                    detalleAdapter.CalcularTotal();
                    detalleAdapter.notifyDataSetChanged();
                }
            }
         );

        rbCredito.setOnCheckedChangeListener(
            (v, isChecked) -> {
                if(idcomprobante > 0)
                    return;
                detalleAdapter.isCredito = isChecked;
                detalleAdapter.CambiarPrecio(cliente.categoria, isChecked);
                detalleAdapter.CalcularTotal();
                detalleAdapter.notifyDataSetChanged();
            }
        );

        for(Sucursal e:SQLite.usuario.establecimientos){
            if(e.IdEstablecimiento == SQLite.usuario.establecimiento_fact){
                posEstablec = SQLite.usuario.establecimientos.indexOf(e);
                lblEstablecimiento.setText("PVP >> " + e.NombreSucursal);
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.lblTotal:
            case R.id.lySubtotales:
            case R.id.btViewSubtotales:
                Utils.EfectoLayout(lySubtotales);
                break;
            case R.id.btnBuscarProducto:
                Intent i = new Intent(v.getContext(),ProductoBusquedaActivity.class);
                i.putExtra("tipobusqueda", "01");
                startActivityForResult(i, REQUEST_BUSQUEDA);
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
            case R.id.lblCliente:
                Utils.EfectoLayout(lyCliente, lblCliente);
                break;
            case R.id.lblProducto:
                Utils.EfectoLayout(lyProductos, lblProducto);
                break;
            case R.id.btnCambiaEstablecimiento:
                CambiarEstablecimiento();
                break;
        }
    }

    private void CambiarEstablecimiento(){

        final ArrayAdapter<Sucursal> adapter =
                new ArrayAdapter<Sucursal>(ComprobanteActivity.this,
                        android.R.layout.select_dialog_singlechoice, SQLite.usuario.establecimientos);

        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        //alt_bld.setIcon(R.drawable.icon);
        alt_bld.setTitle("Seleccione el establecimiento");
        alt_bld.setSingleChoiceItems(adapter, posEstablec,
                (dialog,item) ->{
                posEstablec = item;
                lblEstablecimiento.setText("PVP >> " + SQLite.usuario.establecimientos.get(item).NombreSucursal);
                SQLite.usuario.establecimiento_fact = SQLite.usuario.establecimientos.get(item).IdEstablecimiento;
                SQLite.usuario.GuardarSesionLocal(ComprobanteActivity.this);
                Utils.showMessage(ComprobanteActivity.this,
                        "Los productos se facturarán con los precios y reglas de precio de «" + SQLite.usuario.establecimientos.get(item).NombreSucursal + "»");
                dialog.dismiss();
            });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    private void MostrarInfoDialog(Integer idcliente){
        DialogFragment dialogFragment = new InfoDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("idcliente", idcliente);
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(), "dialog");
    }

    private void BuscaComprobante(Integer idcomprobante) {

        txtCliente.setEnabled(false);
        lyBotones.setVisibility(View.GONE);
        pgCargando.show();

        try {

            Thread th = new Thread() {
                @Override
                public void run() {
                    comprobante = new Comprobante();
                    comprobante = Comprobante.get(idcomprobante);
                    runOnUiThread(
                            () -> {
                            if (comprobante != null) {
                                toolbar.setTitle("N°: " + comprobante.codigotransaccion);
                                toolbar.setSubtitle("Fecha: " + Utils.fechaMes(comprobante.fechadocumento));
                                txtCliente.setText(comprobante.cliente.razonsocial);
                                detalleAdapter.visualizacion = true;
                                detalleProductos.clear();
                                detalleProductos.addAll(comprobante.detalle);
                                detalleAdapter.detalleComprobante.clear();
                                detalleAdapter.detalleComprobante.addAll(comprobante.detalle);
                                detalleAdapter.CalcularTotal();
                                comprobante.getTotal();
                                detalleAdapter.notifyDataSetChanged();
                                setSubtotales(comprobante.total, comprobante.subtotal, comprobante.subtotaliva);
                                lblLeyendaCF.setVisibility(View.GONE);
                                rbEfectivo.setChecked(comprobante.formapago==1);
                                rbCredito.setChecked(comprobante.formapago==0);
                                rbCredito.setText("Crédito");
                                rbEfectivo.setEnabled(false);
                                rbCredito.setEnabled(false);
                                lyEstablecimiento.setVisibility(View.GONE);
                            } else {
                                Banner.make(rootView, ComprobanteActivity.this,Banner.ERROR,"Ocurrió un error al obtener los datos para esta factura.", Banner.BOTTOM, 3500).show();
                            }
                            pgCargando.dismiss();
                        }
                    );
                }
            };
            th.start();
        }catch (Exception e){
            pgCargando.dismiss();
            Log.d(TAG, e.getMessage());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        if(idcomprobante==0) {
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
                if(!SQLite.usuario.VerificaPermiso(this,Constants.PUNTO_VENTA, "escritura")){
                    Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"No tiene permisos para registrar facturas.", Banner.BOTTOM, 3000).show();
                    break;
                }
                if(detalleAdapter.detalleComprobante.size()==0) {
                    Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"Agregue productos para la venta...", Banner.BOTTOM,3000).show();
                    break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                        (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView)view.findViewById(R.id.lblTitle)).setText("Guardar factura");
                ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea guardar esta factura?");
                ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
                ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
                ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
                final AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnConfirm).setOnClickListener(
                    v -> {
                        GuardarDatos();
                        alertDialog.dismiss();
                    }
                );

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
                i.putExtra("tipobusqueda","01");
                startActivityForResult(i, REQUEST_BUSQUEDA_COMPROBANTE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void GuardarDatos() {
        try{
            if(!ValidaDatos()) return;

            comprobante.detalle.clear();
            List<DetalleComprobante> newDetalleCom = new ArrayList<>();
            DetalleComprobante newDetalle;
            for(DetalleComprobante miDetalle: detalleAdapter.detalleComprobante){
                if(miDetalle.producto.lotes.size()==1 || !miDetalle.producto.tipo.equals("P")){

                    newDetalle = new DetalleComprobante();
                    Lote miLote = new Lote();
                    miLote.productoid = miDetalle.producto.idproducto;
                    miLote.numerolote = miDetalle.producto.tipo.equals("P")?miDetalle.producto.lotes.get(0).numerolote:"";
                    miLote.stock = miDetalle.producto.tipo.equals("P")?miDetalle.producto.stock - miDetalle.cantidad:0;
                    miLote.fechavencimiento = miDetalle.producto.tipo.equals("P")?miDetalle.producto.lotes.get(0).fechavencimiento:"1900-01-01";
                    miLote.preciocosto = miDetalle.producto.tipo.equals("P")?miDetalle.producto.lotes.get(0).preciocosto: miDetalle.producto.preciocosto;

                    newDetalle.producto.lotes.add(miLote);

                    newDetalle.producto.idproducto = miDetalle.producto.idproducto;
                    newDetalle.cantidad = miDetalle.cantidad;
                    newDetalle.precio = miDetalle.precio;
                    newDetalle.producto.porcentajeiva = miDetalle.producto.porcentajeiva;
                    newDetalle.numerolote = miLote.numerolote;
                    newDetalle.fechavencimiento = miLote.fechavencimiento;
                    newDetalle.stock = miLote.stock;
                    newDetalle.producto.stock = miLote.stock;
                    newDetalle.preciocosto = miLote.preciocosto;
                    newDetalle.precioreferencia = miDetalle.producto.getPrecio("R");
                    newDetalle.valoriva = miDetalle.producto.porcentajeiva > 0?1d:0d;
                    newDetalle.producto.nombreproducto = miDetalle.producto.nombreproducto;
                    newDetalle.producto.codigoproducto = miDetalle.producto.codigoproducto;

                    newDetalleCom.add(newDetalle);
                }else {
                    //ORDENA LA LISTA DE LOTES POR FECHA DE VENCIMIENTO
                    Collections.sort(miDetalle.producto.lotes,
                            (lot1, lot2) -> lot1.longdate.compareTo(lot2.longdate)
                    );

                    Double cantFaltante = miDetalle.cantidad;
                    for(Lote lote:miDetalle.producto.lotes){
                        newDetalle = new DetalleComprobante();
                        newDetalle.producto.idproducto = miDetalle.producto.idproducto;
                        if (cantFaltante >= lote.stock && miDetalle.producto.tipo.equalsIgnoreCase("P")) {
                            Lote miLote = new Lote();
                            miLote.productoid = miDetalle.producto.idproducto;
                            miLote.numerolote = lote.numerolote;
                            miLote.stock = 0d;
                            miLote.fechavencimiento = lote.fechavencimiento;
                            miLote.preciocosto = lote.preciocosto;

                            newDetalle.producto.lotes.add(miLote);
                            miDetalle.producto.stock -= lote.stock;
                            newDetalle.producto.stock = miDetalle.producto.stock;
                            newDetalle.stock = 0d;//miDetalle.producto.stock;
                            newDetalle.precio = miDetalle.precio;
                            newDetalle.producto.porcentajeiva = miDetalle.producto.porcentajeiva;
                            newDetalle.cantidad = lote.stock;
                            newDetalle.fechavencimiento = lote.fechavencimiento;
                            newDetalle.preciocosto = lote.preciocosto;
                            newDetalle.precioreferencia = miDetalle.producto.getPrecio("R");
                            newDetalle.valoriva = miDetalle.producto.porcentajeiva > 0?1d:0d;
                            newDetalle.producto.nombreproducto = miDetalle.producto.nombreproducto;
                            newDetalle.numerolote = lote.numerolote;
                            newDetalle.producto.codigoproducto = miDetalle.producto.codigoproducto;

                            newDetalleCom.add(newDetalle);
                            cantFaltante -= lote.stock;

                            if(cantFaltante<=0)
                                break;
                        }else{
                            Lote miLote = new Lote();
                            miLote.productoid = miDetalle.producto.idproducto;
                            miLote.numerolote = lote.numerolote;
                            miLote.stock = miDetalle.producto.tipo.equalsIgnoreCase("P")? lote.stock - cantFaltante:0;
                            miLote.fechavencimiento = lote.fechavencimiento;
                            miLote.preciocosto = lote.preciocosto;

                            newDetalle.producto.lotes.add(miLote);

                            newDetalle.producto.stock = miDetalle.producto.stock - cantFaltante;
                            newDetalle.stock = miDetalle.producto.tipo.equals("P")? miLote.stock:0;
                            newDetalle.precio = miDetalle.precio;
                            newDetalle.cantidad = cantFaltante;
                            newDetalle.fechavencimiento = lote.fechavencimiento;
                            newDetalle.preciocosto = lote.preciocosto;
                            newDetalle.producto.porcentajeiva = miDetalle.producto.porcentajeiva;
                            newDetalle.precioreferencia = miDetalle.producto.getPrecio("R");
                            newDetalle.valoriva = miDetalle.producto.porcentajeiva > 0?1d:0d;
                            newDetalle.producto.nombreproducto = miDetalle.producto.nombreproducto;
                            newDetalle.numerolote = lote.numerolote;
                            newDetalle.producto.codigoproducto = miDetalle.producto.codigoproducto;
                            newDetalleCom.add(newDetalle);
                            break;
                        }

                    }

                }
            }

            if(newDetalleCom.size()<=0)
                return;
            comprobante.detalle.addAll(newDetalleCom);
            comprobante.cliente = cliente;
            comprobante.tipotransaccion = "01";
            comprobante.getTotal();
            comprobante.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            comprobante.codigoestablecimiento = SQLite.usuario.sucursal.CodigoEstablecimiento;
            comprobante.puntoemision = SQLite.usuario.sucursal.PuntoEmision;
            comprobante.GenerarClaveAcceso();
            comprobante.estado = 0;
            comprobante.nip = cliente.nip;
            comprobante.porcentajeiva = comprobante.subtotaliva > 0?12d:0d;
            comprobante.fechacelular = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            comprobante.fechadocumento = Utils.getDateFormat("yyyy-MM-dd");
            comprobante.usuarioid = SQLite.usuario.IdUsuario;
            comprobante.longdate = Utils.longDate(comprobante.fechadocumento);
            comprobante.formapago = rbCredito.isChecked()?0:1;
            comprobante.establecimientoprecioid = SQLite.usuario.establecimiento_fact;

            SQLite.gpsTracker.getLastKnownLocation();
            comprobante.lat = SQLite.gpsTracker.getLatitude();
            comprobante.lon = SQLite.gpsTracker.getLongitude();
            if(comprobante.lat == null)
                comprobante.lat = 0d;
            if(comprobante.lon == null)
                comprobante.lon = 0d;

            if (comprobante.Save(true)) {
                toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                ConsultaImpresion();
                Banner.make(rootView,ComprobanteActivity.this,Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS, Banner.BOTTOM,3000).show();
            }else
                Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3500).show();
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
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
            view.findViewById(R.id.btnConfirm).setOnClickListener(
                v -> {
                    if (Printer.btsocket == null) {
                        Utils.showMessage(getApplicationContext(), "Emparejar la impresora...");
                        Intent BTIntent = new Intent(getApplicationContext(), DeviceList.class);
                        startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
                        return;
                    }else {
                        imprimirFactura(idcomprobante==0?"* ORIGINAL CLIENTE *":"* REIMPRESIÓN DE FACTURA *", idcomprobante>0);
                    }
                    alertDialog.dismiss();
                }
            );

            view.findViewById(R.id.btnCancel).setOnClickListener(
                v -> {
                    if(idcomprobante==0)
                        LimpiarDatos();
                    alertDialog.dismiss();
                }
            );

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
        }catch (Exception e){
            Log.d(TAG, "ConsultaImpresion(): "+ e.getMessage());
        }
    }

    private void LimpiarDatos() {
        try{
            toolbar.setTitle("Nueva factura");
            toolbar.setSubtitle("");
            comprobante = new Comprobante();
            cliente = new Cliente();
            cliente = Cliente.get(0);
            if(cliente==null)
                cliente = Cliente.get("9999999999999");
            txtCliente.setText("");
            txtCliente.setEnabled(true);

            rbEfectivo.setEnabled(true);
            rbEfectivo.setChecked(true);
            rbCredito.setEnabled(false);
            rbCredito.setText("Crédito (Disp:$0.00)");
            cliente.montocredito = 0d;
            cliente.deudatotal = 0d;
            cliente.montodisponible = 0d;

            detalleAdapter.visualizacion = false;
            detalleAdapter.detalleComprobante.clear();
            detalleProductos.clear();
            productBusqueda.clear();
            detalleAdapter.CalcularTotal();
            detalleAdapter.notifyDataSetChanged();
            lyBotones.setVisibility(View.VISIBLE);
            idcomprobante = 0;
            toolbar.getMenu().findItem(R.id.option_save).setVisible(true);
            lblLeyendaCF.setVisibility(View.VISIBLE);
            lyEstablecimiento.setVisibility(View.VISIBLE);
            btnCambiaEstablecimiento.setVisibility(View.VISIBLE);
        }catch (Exception e){
            Log.d(TAG, "LimpiarDatos(): " + e.getMessage());
        }
    }

    private boolean ValidaDatos() throws Exception{
        comprobante.detalle.clear();
        comprobante.detalle.addAll(detalleAdapter.detalleComprobante);
        comprobante.getTotal();

        if(cliente == null || cliente.nip.equals("")){
            txtCliente.setError("Debe especificar un cliente.");
            Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"Debe especificar un cliente.", Banner.BOTTOM,3000).show();
            return false;
        }
        if( SQLite.usuario.sucursal == null){
            Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"Datos incompletos de la Sucursal para emitir facturas.", Banner.BOTTOM, 3000).show();
            return false;
        }else if(SQLite.usuario.sucursal.CodigoEstablecimiento.equals("") || SQLite.usuario.sucursal.PuntoEmision.equals("")) {
            Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"Datos incompletos de la Sucursal para emitir facturas.", Banner.BOTTOM,3000).show();
            return false;
        }
        if(comprobante.detalle.size()==0){
            Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"Especifique un detalle para la venta", Banner.BOTTOM, 3000).show();
            return false;
        }else if(comprobante.total == 0){
            Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"El total de la factura debe ser mayor que $0.", Banner.BOTTOM, 3000).show();
            return false;
        }else{
            for(DetalleComprobante miDetalle: comprobante.detalle){
                if(miDetalle.cantidad <= 0) {
                    Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"Debe ingresar una cantidad mayor a 0 para el producto " + miDetalle.producto.nombreproducto, Banner.BOTTOM, 3500).show();
                    return false;
                }
                if(miDetalle.cantidad>miDetalle.producto.stock && miDetalle.producto.tipo.equalsIgnoreCase("P")){
                    Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"El producto: " + miDetalle.producto.nombreproducto +
                            " tiene stock insuficiente para la venta.", Banner.BOTTOM, 3500).show();
                    return false;
                }
            }
        }

        if(rbCredito.isChecked() && cliente.montodisponible>0 && comprobante.total>cliente.montodisponible){
            Banner.make(rootView,ComprobanteActivity.this, Banner.ERROR,
                    "El total de la factura es mayor al monto disponible para ventas a crédito." +
                            "\nMonto disponible: " + Utils.FormatoMoneda(cliente.montodisponible,2),
                    Banner.BOTTOM, 3000).show();
            return false;
        }

        if(cliente.nip.contains("99999999") && comprobante.total > 200){
            Banner.make(rootView,ComprobanteActivity.this,Banner.ERROR,"El total de la factura para CONSUMIDOR FINAL no puede ser mayor a $200.", Banner.BOTTOM, 3000).show();
            return false;
        }

        return true;
    }

    private boolean conectarImpresora(){
        try {
            if (Printer.btsocket == null) {
                Utils.showMessage(this,"Emparejar la impresora...");
                Intent BTIntent = new Intent(this, DeviceList.class);
                startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
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
                        String dateTime[] = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss").split(" ");
                        printer.printUnicode();
                        printer.printCustom(SQLite.usuario.sucursal.NombreComercial, 0, 1);
                        printer.printCustom(SQLite.usuario.sucursal.RazonSocial, 0, 1);
                        printer.printCustom("RUC: ".concat(SQLite.usuario.sucursal.RUC), 0, 1);
                        printer.printCustom(SQLite.usuario.sucursal.Direcion, 0, 1);
                        printer.printCustom("", 0, 1);
                        printer.printCustom("Cliente: ".concat(comprobante.cliente.razonsocial), 0, 0);
                        printer.printCustom("CI|RUC: ".concat(comprobante.cliente.nip), 0, 0);
                        printer.printCustom("Fecha: ".concat(comprobante.fechadocumento), 0, 0);
                        printer.printCustom("Factura #: " + comprobante.codigotransaccion, 0, 0);
                        printer.printCustom("Clave Acceso: ".concat(comprobante.claveacceso), 0, 0);
                        printer.printCustom("------------------------------------------", 0, 1);
                        printer.printCustom(" Cant. |     Detalle    | P. Uni | S. Tot", 0, 0);
                        printer.printCustom("------------------------------------------", 0, 1);
                        for (DetalleComprobante midetalle : comprobante.detalle) {
                            Printer.Data[] Datos = new Printer.Data[]{
                                    new Printer.Data(6, midetalle.cantidad.toString(), 0),
                                    new Printer.Data(15, (midetalle.producto.porcentajeiva>0?"** ":"")+ midetalle.producto.nombreproducto, 0),
                                    new Printer.Data(8, Utils.FormatoMoneda(midetalle.precio,2), 1),
                                    new Printer.Data(11, Utils.FormatoMoneda(midetalle.Subtotal(),2), 1),
                            };
                            printer.printArray(Datos, 0, 0);
                        }
                        printer.printCustom("------------------------------------------", 0, 1);
                        printer.printCustom("SUB TOTAL 0%: " + Utils.FormatoMoneda(comprobante.subtotal,2), 0, 2);
                        printer.printCustom("SUB TOTAL 12%: " + Utils.FormatoMoneda(comprobante.subtotaliva,2), 0, 2);
                        printer.printCustom("IVA 12%: " + Utils.FormatoMoneda((comprobante.total -comprobante.subtotal - comprobante.subtotaliva),2), 0, 2);
                        printer.printCustom("TOTAL: " + Utils.FormatoMoneda(comprobante.total,2), 0, 2);
                        printer.printCustom("", 1, 1);
                        printer.printCustom("FORMA PAGO: " + (comprobante.formapago==0?"CREDITO":"EFECTIVO"), 0, 0);
                        printer.printCustom("", 1, 1);
                        if(!comprobante.cliente.nip.equals("9999999999999")) {
                            printer.printCustom("Descargue su factura electrónica en: https://comprobantes.sanisidrosa.com/. Utilice como usuario y contraseña su número de indentificación: ".concat(comprobante.cliente.nip), 0, 0);
                            printer.printCustom("", 0, 1);
                        }
                        //printer.printText(printer.leftRightAlign("Fecha: " + dateTime[0], dateTime[1]));
                        printer.printCustom("Usuario: " + SQLite.usuario.Usuario,0,0);
                        printer.printCustom("Fecha impresión: " + Utils.getDateFormat("yyyy-MM-dd HH:mm:ss"),0,0);
                        //tarea.Fecha = dateTime[0] + " " + dateTime[1];
                        printer.printNewLine();
                        printer.printCustom(strTipo, 0, 1);
                        printer.printUnicode();
                        printer.printNewLine();
                        printer.printNewLine();
                        printer.flush();
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
            Log.d(TAG, e.getMessage());
            fImp = false;
        }
        return fImp;
    }

    private void ConsultarDeudaCliente(Context context, Integer idpersona) {
        try{
            rbEfectivo.setChecked(true);
            rbCredito.setEnabled(false);
            rbCredito.setText("Crédito (Disp:$0.00)");
            cliente.montocredito = 0d;
            cliente.deudatotal = 0d;
            cliente.montodisponible = 0d;

            ICliente miInterface = retrofit.create(ICliente.class);

            Call<JsonObject> call = miInterface.getDeudaCliente(SQLite.usuario.Usuario,SQLite.usuario.Clave, idpersona);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonObject jsonCliente = obj.getAsJsonObject("persona");
                                if(jsonCliente!=null){
                                    cliente.montocredito = jsonCliente.get("montocredito").getAsDouble();
                                    cliente.deudatotal = jsonCliente.get("deudatotal").getAsDouble();
                                    cliente.montodisponible = jsonCliente.get("montodisponible").getAsDouble();
                                    cliente.plazomaximo = jsonCliente.get("plazomaximo").getAsInt();
                                    rbCredito.setEnabled(cliente.montodisponible > 0);
                                    rbCredito.setText("Crédito (Disp:" + Utils.FormatoMoneda(cliente.montodisponible,2)+")");

                                    ContentValues values = new ContentValues();
                                    values.put("montocredito", cliente.montocredito);
                                    values.put("deudatotal", cliente.deudatotal);
                                    values.put("plazomaximo", cliente.plazomaximo);
                                    Cliente.Update(cliente.idcliente, values);
                                }
                            }
                        }
                    }catch (Exception e){
                        Log.d(TAG,"onResponse(): " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.d(TAG, "onFailure(): " + t.getMessage());
                    call.cancel();
                }
            });
        }catch (Exception e){
            Log.d(TAG, "ConsultaDeuda(): " + e.getMessage());
        }
    }

    public void setSubtotales(Double total, Double subtotal, Double subtotaliva) {
        lblSubtotales.setText("Subtotal 0%:    " + Utils.FormatoMoneda(subtotal,2) +
                "\nSubtotal 12%:    " + Utils.FormatoMoneda(subtotaliva ,2) +
                "\nIVA 12%:    " + Utils.FormatoMoneda((total - subtotaliva - subtotal),2));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_BUSQUEDA:
                    detalleAdapter.visualizacion=false;
                    for(DetalleComprobante miP:productBusqueda) {
                        boolean agregar = true;
                        for(DetalleComprobante miD:detalleAdapter.detalleComprobante){
                            if(miP.producto.idproducto.equals(miD.producto.idproducto)) {
                                miD.cantidad += miP.cantidad;
                                agregar = false;
                                break;
                            }
                        }
                        if(agregar)
                            detalleAdapter.detalleComprobante.add(miP);
                    }
                    comprobante.detalle.clear();
                    comprobante.detalle.addAll(detalleAdapter.detalleComprobante);
                    comprobante.getTotal();
                    this.setSubtotales(comprobante.total, comprobante.subtotal, comprobante.subtotaliva);
                    detalleAdapter.CambiarPrecio(cliente.categoria.equals("")?"0":cliente.categoria, rbCredito.isChecked());
                    detalleAdapter.CalcularTotal();
                    detalleAdapter.notifyDataSetChanged();
                    btnCambiaEstablecimiento.setVisibility(View.GONE);
                    break;
                case REQUEST_CLIENTE:
                    Integer idcliente = data.getExtras().getInt("idcliente",0);
                    if(idcliente>0){
                        cliente = Cliente.get(idcliente);
                        comprobante.cliente = cliente;
                        txtCliente.setText(cliente.razonsocial);
                        if(cliente.codigosistema > 0)
                            ConsultarDeudaCliente(ComprobanteActivity.this, cliente.codigosistema);
                        detalleAdapter.categoria = cliente.categoria;
                        detalleAdapter.isCredito = rbCredito.isChecked();
                        detalleAdapter.CambiarPrecio(cliente.categoria, rbCredito.isChecked());
                    }
                    break;
                case DeviceList.REQUEST_CONNECT_BT:
                    try{
                        btsocket = DeviceList.getSocket();
                        if(btsocket!=null) {
                            Utils.showMessageShort(this,"Imprimiendo comprobante");
                            imprimirFactura(idcomprobante==0?"* ORIGINAL CLIENTE *": "* REIMPRESIÓN DE FACTURA *",
                                    idcomprobante>0);
                            Log.d(TAG, "IMPRESORA SELECCIONADA");
                        }
                    }catch (Exception e){
                        Log.d(TAG,e.getMessage());
                    }
                    break;
                case REQUEST_BUSQUEDA_COMPROBANTE:
                    idcomprobante = data.getExtras().getInt("idcomprobante",0);
                    if(idcomprobante>0) {
                        BuscaComprobante(idcomprobante);
                        toolbar.getMenu().findItem(R.id.option_reimprimir).setVisible(true);
                        toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                    }
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        toolbar.setTitle("Nueva factura");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setBackgroundColor(getResources().getColor(R.color.black_overlay));
        //toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
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
            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Desea salir de la ventana de facturación?");
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_check_white);
            ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
            ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(v -> finish());

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
