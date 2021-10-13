package com.florencia.erpapp.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.DetallePedidoAdapter;
import com.florencia.erpapp.fragments.InfoDialogFragment;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.models.DetallePedido;
import com.florencia.erpapp.models.Pedido;
import com.florencia.erpapp.services.DeviceList;
import com.florencia.erpapp.services.GPSTracker;
import com.florencia.erpapp.services.Printer;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.shasin.notificationbanner.Banner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.florencia.erpapp.services.Printer.btsocket;

public class PedidoActivity extends AppCompatActivity {
    private static String TAG = "TAGPEDIDO_ACT";
    public static final int REQUEST_BUSQUEDA = 1;
    public static final int REQUEST_CLIENTE = 2;
    public static final int REQUEST_BUSQUEDA_PEDIDO = 3;
    Button btnBuscarProducto, btnFecha;
    EditText txtCliente;
    RecyclerView rvDetalle;
    Cliente cliente = new Cliente();
    Pedido pedido = new Pedido();
    DetallePedidoAdapter detalleAdapter;
    List<DetallePedido> detalleProductos = new ArrayList<>();
    public static final List<DetallePedido> productBusqueda = new ArrayList<>();
    public TextView lblTotal, lblSubtotales;
    public LinearLayout lySubtotales;
    Integer idpedido = 0;
    ProgressDialog pgCargando;
    Toolbar toolbar;
    ImageButton btViewSubtotales, btnObservacion;
    TextView lblLeyendaCF, lblMessage, lblTitle, lblCliente, lblProducto, lblTitleFecha;
    LinearLayout lyCliente, lyProductos, lyBotones;
    DatePickerDialog dtpDialog;
    Calendar calendar;

    BottomSheetDialog btsDialog;
    Button btnPositive, btnNegative;
    View viewSeparator, rootView;
    String tipoAccion = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comprobante);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        rootView = findViewById(android.R.id.content);
        toolbar.setTitle("Nuevo pedido");
        init();

        crearBottonSheet();

        txtCliente.setOnKeyListener(
                (v, keyCode, event) -> {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        Intent i = new Intent(v.getContext(), ClienteBusquedaActivity.class);
                        i.putExtra("busqueda", txtCliente.getText().toString().trim());
                        i.putExtra("tipobusqueda", "PC");
                        startActivityForResult(i, REQUEST_CLIENTE);
                        overridePendingTransition(R.anim.left_in, R.anim.left_out);
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
                            detalleAdapter.categoria = "0";
                            detalleAdapter.CambiarPrecio("0");
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (s.length() > 0 && idpedido == 0)
                        txtCliente.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_user, 0, R.drawable.ic_close, 0);
                    else
                        txtCliente.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_user, 0, 0, 0);
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        });
    }

    private void crearBottonSheet() {
        if (btsDialog == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.bottonsheet_message, null);
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

    private void init() {
        pgCargando = new ProgressDialog(this);
        pgCargando.setTitle("Cargando comprobante");
        pgCargando.setMessage("Espere un momento...");
        pgCargando.setCancelable(false);

        btnBuscarProducto = findViewById(R.id.btnBuscarProducto);
        txtCliente = findViewById(R.id.txtCliente);
        rvDetalle = findViewById(R.id.rvDetalleProductos);
        lblTotal = findViewById(R.id.lblTotal);
        lblSubtotales = findViewById(R.id.tvsubtotales);
        lySubtotales = findViewById(R.id.lySubtotales);
        btViewSubtotales = findViewById(R.id.btViewSubtotales);
        lblLeyendaCF = findViewById(R.id.lblLeyendaCF);
        btnFecha = findViewById(R.id.btnFechaDocumento);
        btnObservacion = findViewById(R.id.btnObservacion);
        lblTitleFecha = findViewById(R.id.lblTitleFecha);
        btnObservacion.setVisibility(View.VISIBLE);
        lblLeyendaCF.setVisibility(View.GONE);
        btnFecha.setVisibility(View.VISIBLE);
        lblTitleFecha.setVisibility(View.VISIBLE);
        EstablecerFecha("");
        //btnFecha.setText(Utils.getDateFormat("yyyy-MM-dd"));
        lblCliente = findViewById(R.id.lblCliente);
        lblProducto = findViewById(R.id.lblProducto);
        lyCliente = findViewById(R.id.lyCliente);
        lyProductos = findViewById(R.id.lyProductos);
        lyBotones = findViewById(R.id.lyBotones);
        findViewById(R.id.lyFormaPago).setVisibility(View.GONE);

        lblTotal.setOnClickListener(onClick);
        lySubtotales.setOnClickListener(onClick);
        btViewSubtotales.setOnClickListener(onClick);
        btnBuscarProducto.setOnClickListener(onClick);
        btnFecha.setOnClickListener(onClick);
        lblCliente.setOnClickListener(onClick);
        lblProducto.setOnClickListener(onClick);
        btnObservacion.setOnClickListener(onClick);

        if (SQLite.gpsTracker == null)
            SQLite.gpsTracker = new GPSTracker(this);
        if (!SQLite.gpsTracker.checkGPSEnabled())
            SQLite.gpsTracker.showSettingsAlert(PedidoActivity.this);

        btnObservacion.setTag("");

        if (getIntent().getExtras() != null) {
            int idcliente = getIntent().getExtras().getInt("idcliente", 0);
            if (idcliente > 0) {
                cliente = Cliente.get(idcliente);
                pedido.cliente = cliente;
                txtCliente.setText(cliente.razonsocial);
            }
            idpedido = getIntent().getExtras().getInt("idcomprobante", 0);
        }

        detalleProductos = new ArrayList<>();
        detalleAdapter = new DetallePedidoAdapter(this, detalleProductos, cliente.categoria.equals("") ? "0" : cliente.categoria, idpedido > 0);
        rvDetalle.setAdapter(detalleAdapter);

        if (idpedido > 0) {
            BuscaPedido(idpedido);
        }
    }

    private View.OnClickListener onClick = v -> {
        switch (v.getId()) {
            case R.id.lblTotal:
            case R.id.lySubtotales:
            case R.id.btViewSubtotales:
                Utils.EfectoLayout(lySubtotales);
                break;
            case R.id.btnBuscarProducto:
                Intent i = new Intent(v.getContext(), ProductoBusquedaActivity.class);
                i.putExtra("tipobusqueda", "PC");
                startActivityForResult(i, REQUEST_BUSQUEDA);
                overridePendingTransition(R.anim.zoom_back_in, R.anim.zoom_back_out);
                break;
            case R.id.btnFechaDocumento:
                showDatePickerDialog(v);
                break;
            case R.id.btnPositive:
                if (tipoAccion.equals("ERROR")) {
                    btnNegative.setVisibility(View.VISIBLE);
                    viewSeparator.setVisibility(View.VISIBLE);
                    lblTitle.setVisibility(View.VISIBLE);
                    btsDialog.dismiss();
                } else if (tipoAccion.equals("GUARDAR")) {
                    btnNegative.setVisibility(View.GONE);
                    viewSeparator.setVisibility(View.GONE);
                    lblTitle.setText("");
                    lblTitle.setVisibility(View.GONE);
                    btsDialog.dismiss();
                    GuardarDatos();
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
            case R.id.btnObservacion:
                IngresaObservacion(btnObservacion.getTag().toString());
                break;
        }
    };

    void IngresaObservacion(String observacion) {
        try {

            EditText txtObservacion;
            Button btnOk;
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PedidoActivity.this,
                    R.style.AlertDialogTheme);
            View view = LayoutInflater.from(PedidoActivity.this).inflate(R.layout.layout_observacion_dialog,
                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            txtObservacion = view.findViewById(R.id.txtObservacion);
            btnOk = (Button) view.findViewById(R.id.btnConfirm);
            ((Button) view.findViewById(R.id.btnCancel)).setText("Cancelar");
            btnOk.setText("Confirmar");
            txtObservacion.setText(observacion);
            txtObservacion.setSelectAllOnFocus(true);

            txtObservacion.setEnabled(idpedido == 0);

            final android.app.AlertDialog alertDialog = builder.create();
            btnOk.setOnClickListener(
                    v -> {
                        btnObservacion.setTag(txtObservacion.getText().toString());
                        alertDialog.dismiss();
                    }
            );

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

            if (alertDialog.getWindow() != null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

    }

    public void showDatePickerDialog(View v) {
        Locale l = new Locale("ES-es");
        calendar = Calendar.getInstance(l);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        String[] fecha = btnFecha.getTag().toString().split("-");
        day = Integer.valueOf(fecha[2]);
        month = Integer.valueOf(fecha[1]) - 1;
        year = Integer.valueOf(fecha[0]);
        dtpDialog = new DatePickerDialog(v.getContext(),
                (vi, y, m, d) -> {
                    String dia = (d >= 0 && d < 10 ? "0" + (d) : String.valueOf(d));
                    String mes = (m >= 0 && m < 9 ? "0" + (m + 1) : String.valueOf(m + 1));

                    String mitextoU = y + "-" + mes + "-" + dia;
                    btnFecha.setTag(mitextoU);
                    btnFecha.setText(dia + "-" + Utils.getMes(m, true) + "-" + y);
                }
                , year, month, day);
        dtpDialog.show();
    }

    private void MostrarInfoDialog(Integer idcliente) {
        DialogFragment dialogFragment = new InfoDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("idcliente", idcliente);
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(), "dialog");
    }

    private void BuscaPedido(Integer idpedido) {

        txtCliente.setEnabled(false);
        btnBuscarProducto.setVisibility(View.GONE);
        pgCargando.show();

        try {

            Thread th = new Thread() {
                @Override
                public void run() {
                    pedido = new Pedido();
                    pedido = Pedido.get(idpedido);
                    runOnUiThread(() -> {
                        if (pedido != null) {
                            toolbar.setTitle(pedido.secuencialpedido);
                            toolbar.setSubtitle("Registro: " + pedido.fechacelular);
                            txtCliente.setText(pedido.cliente.razonsocial);
                            EstablecerFecha(pedido.fechapedido);
                            btnFecha.setEnabled(false);
                            detalleAdapter.visualizacion = true;
                            detalleProductos.clear();
                            detalleProductos.addAll(pedido.detalle);
                            detalleAdapter.detallePedido.clear();
                            detalleAdapter.detallePedido.addAll(pedido.detalle);
                            detalleAdapter.CalcularTotal();
                            pedido.getTotal();
                            detalleAdapter.notifyDataSetChanged();
                            setSubtotales(pedido.total, pedido.subtotal, pedido.subtotaliva, pedido.descuento);
                            btnObservacion.setTag(pedido.observacion);
                        } else {
                            //Utils.showMessage(PedidoActivity.this, "Ocurrió un error al obtener los datos para este comprobante.");
                            Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "Ocurrió un error al obtener los datos para este pedido.", Banner.BOTTOM, 3000).show();
                        }
                        pgCargando.dismiss();
                    });
                }
            };
            th.start();
        } catch (Exception e) {
            pgCargando.dismiss();
            Log.d(TAG, e.getMessage());
        }

    }

    public void showError(String message) {
        crearBottonSheet();
        lblMessage.setText(message);
        btnNegative.setVisibility(View.GONE);
        viewSeparator.setVisibility(View.GONE);
        lblTitle.setVisibility(View.GONE);
        tipoAccion = "ERROR";
        if (btsDialog.getWindow() != null)
            btsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        btsDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        menu.findItem(R.id.option_reimprimir).setVisible(false);
        if (idpedido == 0) {
            //menu.findItem(R.id.option_reimprimir).setVisible(false);
        } else {
            menu.findItem(R.id.option_save).setVisible(false);
            menu.findItem(R.id.option_newdocument).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_save:
                if (!SQLite.usuario.VerificaPermiso(this, Constants.PEDIDO, "escritura")) {
                    //Utils.showMessage(this,"No tiene permisos para registrar pedidos.");
                    Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "No tiene permisos para registrar pedidos.", Banner.BOTTOM, 3000).show();
                    break;
                }

                if (detalleAdapter.detallePedido.size() == 0) {
                    Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "Agregue productos al pedido...", Banner.BOTTOM, 3000).show();
                    break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                        (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView) view.findViewById(R.id.lblTitle)).setText("Guardar pedido");
                ((TextView) view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea guardar este pedido?");
                ((ImageView) view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
                ((Button) view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
                ((Button) view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
                final AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                    GuardarDatos();
                    alertDialog.dismiss();
                });

                view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

                if (alertDialog.getWindow() != null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
                break;
            /*case R.id.option_reimprimir:
                try {
                    if (Printer.btsocket == null) {
                        Utils.showMessage(this, "Emparejar la impresora...");
                        Intent BTIntent = new Intent(this.getApplicationContext(), DeviceList.class);
                        startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
                        return true;
                    }
                }catch (Exception e){
                    Log.d("TAGIMPRIMIR3", e.getMessage());
                }
                imprimirFactura("* REIMPRESIÓN DE PEDIDO *",true);
                break;*/
            case R.id.option_newdocument:
                LimpiarDatos();
                toolbar.getMenu().findItem(R.id.option_reimprimir).setVisible(false);
                toolbar.getMenu().findItem(R.id.option_save).setVisible(true);
                break;
            case R.id.option_listdocument:
                Intent i = new Intent(this, ListaComprobantesActivity.class);
                i.putExtra("tipobusqueda", "PC");
                startActivityForResult(i, REQUEST_BUSQUEDA_PEDIDO);
                overridePendingTransition(R.anim.left_in, R.anim.left_out);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void GuardarDatos() {
        try {
            if (!ValidaDatos()) return;

            pedido.detalle.clear();
            pedido.detalle.addAll(detalleAdapter.detallePedido);
            pedido.cliente = cliente;
            pedido.tipotransaccion = "PC";
            pedido.getTotal();
            pedido.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            pedido.codigoestablecimiento = SQLite.usuario.sucursal.CodigoEstablecimiento;
            pedido.puntoemision = SQLite.usuario.sucursal.PuntoEmision;
            pedido.getCodigoTransaccion();
            pedido.estado = 1;
            pedido.nip = cliente.nip;
            pedido.porcentajeiva = pedido.subtotaliva > 0 ? 12d : 0d;
            pedido.fechacelular = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            pedido.fechapedido = btnFecha.getTag().toString().trim().equals("") ? Utils.getDateFormat("yyyy-MM-dd") : btnFecha.getTag().toString().trim();
            pedido.usuarioid = SQLite.usuario.IdUsuario;
            pedido.categoria = cliente.categoria.equals("") ? "0" : cliente.categoria;
            pedido.parroquiaid = SQLite.usuario.ParroquiaID;
            pedido.longdate = Utils.longDate(Utils.getDateFormat("yyyy-MM-dd"));
            pedido.observacion = btnObservacion.getTag().toString();

            SQLite.gpsTracker.getLastKnownLocation();
            pedido.lat = SQLite.gpsTracker.getLatitude();
            pedido.lon = SQLite.gpsTracker.getLongitude();

            if (pedido.Save()) {

                /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Imprimir");
                builder.setMessage("¿Desea imprimir el comprobante?");
                builder.setIcon(R.drawable.ic_printer);
                builder.setPositiveButton(getResources().getString(R.string.Confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (Printer.btsocket == null) {
                            Utils.showMessage(getApplicationContext(), "Emparejar la impresora...");
                            Intent BTIntent = new Intent(getApplicationContext(), DeviceList.class);
                            startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
                            return;
                        }else {
                            imprimirFactura("* ORIGINAL CLIENTE *", false);
                        }
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { LimpiarDatos();}
                });
                builder.show();*/
                LimpiarDatos();
                //Utils.showMessage(this, Constants.MSG_DATOS_GUARDADOS);
                Banner.make(rootView, PedidoActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS, Banner.BOTTOM, 3000).show();
            } else
                Banner.make(rootView, PedidoActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    private void LimpiarDatos() {
        try {
            toolbar.setTitle("Nuevo pedido");
            toolbar.setSubtitle("");
            toolbar.setSubtitle("");
            pedido = new Pedido();
            cliente = new Cliente();
            txtCliente.setText("");
            txtCliente.setEnabled(true);
            detalleAdapter.visualizacion = false;
            detalleAdapter.detallePedido.clear();
            detalleProductos.clear();
            productBusqueda.clear();
            detalleAdapter.CalcularTotal();
            detalleAdapter.notifyDataSetChanged();
            btnBuscarProducto.setVisibility(View.VISIBLE);
            idpedido = 0;
            btnObservacion.setTag("");
            EstablecerFecha("");
            btnFecha.setEnabled(true);
        } catch (Exception e) {
            Log.d(TAG, "LimpiarDatos(): " + e.getMessage());
        }
    }

    private void EstablecerFecha(String fecha) {
        if (fecha.equals(""))
            fecha = Utils.getDateFormat("yyyy-MM-dd");
        btnFecha.setTag(fecha);
        btnFecha.setText(Utils.fechaMes(fecha));
    }

    private boolean ValidaDatos() throws Exception {
        pedido.detalle.clear();
        pedido.detalle.addAll(detalleAdapter.detallePedido);
        pedido.getTotal();

        if (cliente == null || cliente.nip.equals("")) {
            //Utils.showMessage(this, "Debe especificar un cliente.");
            Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "Debe especificar un cliente.", Banner.BOTTOM, 3000).show();
            return false;
        }
        if (SQLite.usuario.sucursal == null) {
            //Utils.showMessage(this, "Datos incompletos de la Sucursal para emitir facturas.");
            Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "Datos incompletos de la Sucursal para emitir facturas.", Banner.BOTTOM, 3000).show();
            return false;
        } else if (SQLite.usuario.sucursal.CodigoEstablecimiento.equals("") || SQLite.usuario.sucursal.PuntoEmision.equals("")) {
            //Utils.showMessage(this, "Datos incompletos de la Sucursal para emitir facturas.");
            Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "Datos incompletos de la Sucursal para emitir facturas.", Banner.BOTTOM, 3000).show();
            return false;
        }
        if (pedido.detalle.size() == 0) {
            Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "Especifique un detalle para el pedido", Banner.BOTTOM, 3000).show();
            return false;
        } else {
            for (DetallePedido det : pedido.detalle) {
                if (det.cantidad <= 0) {
                    Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "Ingrese una cantidad mayor a 0 para " + det.producto.nombreproducto, Banner.BOTTOM, 3500).show();
                    return false;
                }
            }
        }
        if (pedido.total == 0) {
            //Utils.showMessage(this, "El total del pedido debe ser mayor que $0.");
            Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "El total del pedido debe ser mayor que $0.", Banner.BOTTOM, 3000).show();
            return false;
        }

        if (cliente.nip.contains("99999999")) {
            //Utils.showMessage(this, "No está permitido el registro de pedido a CONSUMIDOR FINAL");
            Banner.make(rootView, PedidoActivity.this, Banner.ERROR, "No está permitido el registro de pedido a CONSUMIDOR FINAL", Banner.BOTTOM, 3000).show();
            return false;
        }

        return true;
    }

    private boolean imprimirFactura(String strTipo, boolean reimpresion) {
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
                        printer.printCustom("Cliente: ".concat(pedido.cliente.razonsocial), 0, 0);
                        printer.printCustom("CI|RUC:  ".concat(pedido.cliente.nip), 0, 0);
                        printer.printCustom("Fec. Pedido:   ".concat(pedido.fechapedido), 0, 0);
                        printer.printCustom("Fec. Registro: ".concat(pedido.fechapedido), 0, 0);
                        printer.printCustom("Pedido #:      ".concat(pedido.secuencialpedido), 0, 0);
                        printer.printCustom("------------------------------------------", 0, 1);
                        printer.printCustom(" Cant. |     Detalle    | P. Uni | S. Tot", 0, 0);
                        printer.printCustom("------------------------------------------", 0, 1);
                        for (DetallePedido midetalle : pedido.detalle) {
                            Printer.Data[] Datos = new Printer.Data[]{
                                    new Printer.Data(6, midetalle.cantidad.toString(), 0),
                                    new Printer.Data(15, (midetalle.producto.porcentajeiva > 0 ? "** " : "") + midetalle.producto.nombreproducto, 0),
                                    new Printer.Data(8, Utils.FormatoMoneda(midetalle.precio, 2), 1),
                                    new Printer.Data(11, Utils.FormatoMoneda(midetalle.Subtotal(), 2), 1),
                            };
                            printer.printArray(Datos, 0, 0);
                        }
                        printer.printCustom("------------------------------------------", 0, 1);
                        printer.printCustom("SUB TOTAL 0%: " + Utils.FormatoMoneda(pedido.subtotal, 2), 0, 2);
                        printer.printCustom("SUB TOTAL 12%: " + Utils.FormatoMoneda(pedido.subtotaliva, 2), 0, 2);
                        printer.printCustom("IVA 12%: " + Utils.FormatoMoneda((pedido.total - pedido.subtotal - pedido.subtotaliva), 2), 0, 2);
                        printer.printCustom("TOTAL: " + Utils.FormatoMoneda(pedido.total, 2), 0, 2);
                        printer.printCustom("", 1, 1);
                        //printer.printText(printer.leftRightAlign("Fecha: " + dateTime[0], dateTime[1]));
                        printer.printCustom("Usuario: " + SQLite.usuario.Usuario, 0, 0);
                        printer.printCustom("Fecha impresión: " + Utils.getDateFormat("yyyy-MM-dd HH:mm:ss"), 0, 0);
                        //tarea.Fecha = dateTime[0] + " " + dateTime[1];
                        printer.printNewLine();
                        printer.printCustom(strTipo, 0, 1);
                        printer.printUnicode();
                        printer.printNewLine();
                        printer.printNewLine();
                        printer.flush();
                        if (!reimpresion)
                            this.LimpiarDatos();
                    } catch (IOException e) {
                        fImp = false;
                        e.printStackTrace();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

            } else fImp = false;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            fImp = false;
        }
        return fImp;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_BUSQUEDA:
                    detalleAdapter.visualizacion = false;
                    for (DetallePedido miP : productBusqueda) {
                        boolean agregar = true;
                        for (DetallePedido miD : detalleAdapter.detallePedido) {
                            if (miP.producto.idproducto.equals(miD.producto.idproducto)) {
                                miD.cantidad += miP.cantidad;
                                agregar = false;
                                break;
                            }
                        }
                        if (agregar)
                            detalleAdapter.detallePedido.add(miP);
                    }
                    pedido.detalle.clear();
                    pedido.detalle.addAll(detalleAdapter.detallePedido);
                    pedido.getTotal();
                    this.setSubtotales(pedido.total, pedido.subtotal, pedido.subtotaliva, pedido.descuento);
                    detalleAdapter.CambiarPrecio(cliente.categoria.equals("") ? "0" : cliente.categoria);
                    detalleAdapter.CalcularTotal();
                    detalleAdapter.notifyDataSetChanged();
                    Log.d(TAG, String.valueOf(detalleAdapter.detallePedido.size()));
                    break;
                case REQUEST_CLIENTE:
                    Integer idcliente = data.getExtras().getInt("idcliente", 0);
                    if (idcliente > 0) {
                        cliente = Cliente.get(idcliente);
                        pedido.cliente = cliente;
                        txtCliente.setText(cliente.razonsocial);
                        detalleAdapter.categoria = cliente.categoria;
                        detalleAdapter.CambiarPrecio(cliente.categoria);
                    }
                    break;
                case DeviceList.REQUEST_CONNECT_BT:
                    try {
                        btsocket = DeviceList.getSocket();
                        if (btsocket != null) {
                            Utils.showMessageShort(this, "Imprimiendo comprobante");
                            imprimirFactura(idpedido == 0 ? "* ORIGINALCLIENTE *" : "* REIMPRESIÓN DE PEDIDO *",
                                    idpedido > 0);
                            Log.d(TAG, "IMPRESORA SELECCIONADA");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                    }
                    break;
                case REQUEST_BUSQUEDA_PEDIDO:
                    idpedido = data.getExtras().getInt("idcomprobante", 0);
                    if (idpedido > 0) {
                        BuscaPedido(idpedido);
                        //toolbar.getMenu().findItem(R.id.option_reimprimir).setVisible(true);
                        toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                    }
                    break;
            }
        }
    }

    public void setSubtotales(Double total, Double subtotal, Double subtotaliva, Double descuento) {
        lblSubtotales.setText(
                "Subtotal 0%:    " + Utils.FormatoMoneda(subtotal, 2) +
                "\nSubtotal 12%:    " + Utils.FormatoMoneda(subtotaliva, 2) +
                "\nDescuento:    " + Utils.FormatoMoneda(descuento, 2) +
                "\nIVA 12%:    " + Utils.FormatoMoneda((total - subtotaliva - subtotal + descuento), 2));
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setTitle("Nuevo pedido");
        //toolbar.setBackgroundColor(getResources().getColor(R.color.colorMoradito));
        //toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
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
            ((TextView) view.findViewById(R.id.lblMessage)).setText("¿Desea salir de la ventana de pedido?");
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
