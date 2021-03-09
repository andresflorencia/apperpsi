package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.DetallePedidoInvAdapter;
import com.florencia.erpapp.models.DetallePedidoInv;
import com.florencia.erpapp.models.PedidoInventario;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.shasin.notificationbanner.Banner;

import java.util.ArrayList;
import java.util.List;

public class PedidoInventarioActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int REQUEST_PRODUCTO = 1;
    public static final int REQUEST_BUSQUEDA_PEDIDO = 2;
    RecyclerView rvDetalle;
    CardView cvInformacion;
    TextView lblProducto,lblPedido, lblCodDocumento, lblFechaReg, lblEstado, lblObservacion;
    EditText tvObservacion;
    Button btnBuscaProducto;
    PedidoInventario pedido = new PedidoInventario();
    DetallePedidoInvAdapter detalleAdapter;
    List<DetallePedidoInv> detalleProductos = new ArrayList<>();
    public static final List<DetallePedidoInv> productBusqueda = new ArrayList<>();
    Integer idpedido=0;
    ProgressDialog pgCargando;
    Toolbar toolbar;
    View rootView;

    LinearLayout lyDatosInformativos, lyProductos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedido_inventario);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        init();
    }

    private void init() {

        rootView = findViewById(android.R.id.content);
        toolbar.setTitle("Nuevo pedido");

        pgCargando = new ProgressDialog(this);
        pgCargando.setTitle("Cargando comprobante");
        pgCargando.setMessage("Espere un momento...");
        pgCargando.setCancelable(false);

        btnBuscaProducto = findViewById(R.id.btnBuscarProducto);
        rvDetalle = findViewById(R.id.rvDetalleProductos);
        cvInformacion = findViewById(R.id.cvInformacion);
        lblProducto = findViewById(R.id.lblProducto);
        lblPedido = findViewById(R.id.lblPedido);
        lblCodDocumento = findViewById(R.id.lblCodDocumento);
        lblFechaReg = findViewById(R.id.lblFechaReg);
        lblEstado = findViewById(R.id.lblEstado);
        lyProductos = findViewById(R.id.lyProductos);
        lyDatosInformativos = findViewById(R.id.lyDatosInformativos);
        lblObservacion = findViewById(R.id.lblObservacion);
        tvObservacion = findViewById(R.id.tvObservacion);

        cvInformacion.setVisibility(View.GONE);
        lblProducto.setOnClickListener(this::onClick);
        lblPedido.setOnClickListener(this::onClick);
        lblObservacion.setOnClickListener(this::onClick);
        btnBuscaProducto.setOnClickListener(this::onClick);

        if(getIntent().getExtras()!=null){
            idpedido = getIntent().getExtras().getInt("idcomprobante",0);
        }

        detalleProductos = new ArrayList<>();
        detalleAdapter = new DetallePedidoInvAdapter(this, detalleProductos, idpedido > 0, "PI");
        rvDetalle.setAdapter(detalleAdapter);

        if(idpedido>0){
            BuscaPedido(idpedido);
        }
    }

    @Override
    public void onClick(View v) {
        try{
            switch(v.getId()){
                case R.id.btnBuscarProducto:
                    Intent i = new Intent(v.getContext(),ProductoBusquedaActivity.class);
                    i.putExtra("tipobusqueda", "PI");
                    startActivityForResult(i, REQUEST_PRODUCTO);
                    break;
                case R.id.lblProducto:
                    Utils.EfectoLayout(lyProductos, lblProducto);
                    break;
                case R.id.lblPedido:
                    Utils.EfectoLayout(lyDatosInformativos, lblPedido);
                    break;
                case R.id.lblObservacion:
                    Utils.EfectoLayout(tvObservacion, lblObservacion);
                    break;
            }
        }catch (Exception e){}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        menu.findItem(R.id.option_reimprimir).setVisible(false);
        if(idpedido==0) {
            //menu.findItem(R.id.option_reimprimir).setVisible(false);
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
                if(!SQLite.usuario.VerificaPermiso(this, Constants.PEDIDO_INVENTARIO, "escritura")){
                    //Utils.showMessage(this,"No tiene permisos para registrar pedidos.");
                    Banner.make(rootView,PedidoInventarioActivity.this,Banner.ERROR,"No tiene permisos para registrar pedidos.", Banner.BOTTOM,3000).show();
                    break;
                }

                if(detalleAdapter.detallePedido.size()==0) {
                    Banner.make(rootView,PedidoInventarioActivity.this,Banner.ERROR,"Agregue productos al pedido...", Banner.BOTTOM,3000).show();
                    break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                        (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView)view.findViewById(R.id.lblTitle)).setText("Guardar pedido");
                ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea guardar este pedido?");
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
                i.putExtra("tipobusqueda","PI");
                startActivityForResult(i, REQUEST_BUSQUEDA_PEDIDO);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void LimpiarDatos() {
        try{
            toolbar.setTitle("Nuevo pedido");
            toolbar.setSubtitle("");
            pedido = new PedidoInventario();
            pedido.tipotransaccion = "PI";
            pedido.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            toolbar.setSubtitle(pedido.getCodigoTransaccion());
            detalleAdapter.visualizacion = false;
            detalleAdapter.detallePedido.clear();
            detalleProductos.clear();
            productBusqueda.clear();
            detalleAdapter.notifyDataSetChanged();
            tvObservacion.setText("");
            tvObservacion.setEnabled(true);
            btnBuscaProducto.setVisibility(View.VISIBLE);
            cvInformacion.setVisibility(View.GONE);
            idpedido = 0;
        }catch (Exception e){
            Log.d("TAGPEDIDO_ACT", "LimpiarDatos(): " + e.getMessage());
        }
    }

    private boolean ValidaDatos() throws Exception{
        pedido.detalle.clear();
        pedido.detalle.addAll(detalleAdapter.detallePedido);
        if( SQLite.usuario.sucursal == null){
            //Utils.showMessage(this, "Datos incompletos de la Sucursal para emitir facturas.");
            Banner.make(rootView,PedidoInventarioActivity.this,Banner.ERROR,"Datos incompletos de la Sucursal para emitir facturas.", Banner.BOTTOM,3000).show();
            return false;
        }else if(SQLite.usuario.sucursal.CodigoEstablecimiento.equals("")) {
            //Utils.showMessage(this, "Datos incompletos de la Sucursal para emitir facturas.");
            Banner.make(rootView,PedidoInventarioActivity.this,Banner.ERROR,"Datos incompletos de la Sucursal para emitir facturas.", Banner.BOTTOM,3000).show();
            return false;
        }

        if(pedido.detalle.size()==0){
            Banner.make(rootView,PedidoInventarioActivity.this,Banner.ERROR,"Especifique un detalle para el pedido", Banner.BOTTOM,3000).show();
            return false;
        }else{
            for(DetallePedidoInv det:pedido.detalle) {
                if (det.cantidadpedida <= 0) {
                    Banner.make(rootView, PedidoInventarioActivity.this, Banner.ERROR, "La cantidad para «" + det.producto.nombreproducto + "» debe ser mayor a 0", Banner.BOTTOM, 3500).show();
                    return false;
                }
            }
        }
        return true;
    }

    private void GuardarDatos() {
        try{
            if(!ValidaDatos()) return;

            pedido.detalle.clear();
            pedido.detalle.addAll(detalleAdapter.detallePedido);
            pedido.tipotransaccion = "PI";
            pedido.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            pedido.getCodigoTransaccion();
            pedido.estadomovil = 1;
            pedido.estado = "P";
            pedido.fechahora = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            pedido.fecharegistro = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            pedido.usuarioid = SQLite.usuario.IdUsuario;
            pedido.longdate = Utils.longDate(Utils.getDateFormat("yyyy-MM-dd"));

            if (pedido.Save()) {
                LimpiarDatos();
                Banner.make(rootView,PedidoInventarioActivity.this,Banner.SUCCESS,Constants.MSG_DATOS_GUARDADOS,Banner.BOTTOM,3000).show();
            }else
                Banner.make(rootView,PedidoInventarioActivity.this,Banner.ERROR,Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM,3000).show();
        }catch (Exception e){
            Log.d("TAGPEDIDO", e.getMessage());
        }
    }

    private void BuscaPedido(Integer idpedido) {
        pgCargando.show();
        try {

            Thread th = new Thread() {
                @Override
                public void run() {
                    pedido = new PedidoInventario();
                    pedido = PedidoInventario.get(idpedido);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (pedido != null) {
                                toolbar.setTitle(pedido.codigopedido);
                                toolbar.setSubtitle("");
                                cvInformacion.setVisibility(View.VISIBLE);
                                btnBuscaProducto.setVisibility(View.GONE);
                                lblCodDocumento.setText(pedido.codigopedido);
                                lblFechaReg.setText(pedido.fecharegistro);
                                lblEstado.setText(pedido.estadomovil == 1?"No sincronizado":"Sincronizado");
                                lblEstado.setTextColor(pedido.estadomovil == 1?getResources().getColor(R.color.black_overlay):getResources().getColor(R.color.colorSuccess));
                                detalleAdapter.visualizacion = true;
                                detalleProductos.clear();
                                detalleProductos.addAll(pedido.detalle);
                                detalleAdapter.detallePedido.clear();
                                detalleAdapter.detallePedido.addAll(pedido.detalle);
                                detalleAdapter.notifyDataSetChanged();
                                tvObservacion.setText(pedido.observacion);
                                tvObservacion.setEnabled(false);
                            } else {
                                Banner.make(rootView,PedidoInventarioActivity.this, Banner.ERROR,"Ocurrió un error al obtener los datos para este pedido.", Banner.BOTTOM, 3000).show();
                            }
                            pgCargando.dismiss();
                        }
                    });
                }
            };
            th.start();
        }catch (Exception e){
            pgCargando.dismiss();
            Log.d("TAG", e.getMessage());
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PRODUCTO:
                    if(pedido==null)
                        pedido = new PedidoInventario();
                    detalleAdapter.visualizacion = false;
                    detalleAdapter.detallePedido.addAll(productBusqueda);
                    pedido.detalle.clear();
                    pedido.detalle.addAll(detalleAdapter.detallePedido);
                    detalleAdapter.notifyDataSetChanged();
                    Log.d("TAGPRODUCTO", String.valueOf(detalleAdapter.detallePedido.size()));
                    break;
                case REQUEST_BUSQUEDA_PEDIDO:
                    idpedido = data.getExtras().getInt("idcomprobante", 0);
                    if (idpedido > 0) {
                        BuscaPedido(idpedido);
                        toolbar.getMenu().findItem(R.id.option_save).setVisible(false);
                    }
                    break;
            }
        }
    }
    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setTitle("Nuevo pedido");
        if(pedido == null)
            pedido = new PedidoInventario();
        pedido.tipotransaccion = "PI";
        pedido.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
        toolbar.setSubtitle(pedido.getCodigoTransaccion());
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
            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Desea salir de la ventana de pedido?");
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

}
