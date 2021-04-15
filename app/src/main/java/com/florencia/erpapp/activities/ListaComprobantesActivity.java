package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.ClienteBusquedaAdapter;
import com.florencia.erpapp.adapters.ComprobantesAdapter;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.Ingreso;
import com.florencia.erpapp.models.Pedido;
import com.florencia.erpapp.models.PedidoInventario;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.shasin.notificationbanner.Banner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ListaComprobantesActivity extends AppCompatActivity implements SearchView.OnQueryTextListener  {

    public static String TAG = "TAGLISTACOMPRO_ACT";
    ImageView imgFondo;
    RecyclerView rvComprobantes;
    List<Comprobante> listComprobantes = null;
    List<Pedido> listPedido = null;
    List<PedidoInventario> listPedidoInv = null;
    List<Ingreso> listIngresos = null;
    ComprobantesAdapter comprobanteAdapter;
    SearchView svBusqueda;
    LinearLayout lyContainer;
    ProgressDialog pgCargando;
    String tipobusqueda = "", fechadesde = "", fechahasta = "";
    Boolean retornar = true;
    Calendar calendar;
    DatePickerDialog dtpDialog;
    Toolbar toolbar;
    View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_comprobantes);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootView = findViewById(android.R.id.content);
        pgCargando = new ProgressDialog(this);
        rvComprobantes = findViewById(R.id.rvComprobantes);
        imgFondo = findViewById(R.id.imgFondo);
        lyContainer = findViewById(R.id.lyContainer);
        svBusqueda = findViewById(R.id.svBusqueda);
        svBusqueda.setOnQueryTextListener(this);

        pgCargando.setTitle("Cargando registros");
        pgCargando.setMessage("Espere un momento...");
        pgCargando.setCancelable(false);

        if(getIntent().getExtras()!=null){
            tipobusqueda = getIntent().getExtras().getString("tipobusqueda", "");
            fechadesde = getIntent().getExtras().getString("fechadesde","");
            fechahasta = getIntent().getExtras().getString("fechahasta","");
            retornar = getIntent().getExtras().getBoolean("retornar", true);
        }

    }

    private void CargarDatos(String tipobusqueda, String fechadesde, String fechahasta, Boolean retornar){
        try {
            Thread th = new Thread(){
                @Override
                public void run(){
                    runOnUiThread(
                        () -> {
                            try {
                                boolean band = false;
                                switch (tipobusqueda) {
                                    case "01":  //FACTURAS
                                    case "8,23":   //RECEPCION - RECEPDEVOLUCION
                                    case "23,8":   //RECEPDEVOLUCION - RECEPCION
                                    case "4,20":   //TRANSFERENCIA - DEVOLUCION
                                    case "20,4":   //DEVOLUCION - TRANSFERENCIA
                                        listComprobantes = Comprobante.getByUsuario(SQLite.usuario.IdUsuario, SQLite.usuario.sucursal.IdEstablecimiento, tipobusqueda, fechadesde, fechahasta);
                                        band = (listComprobantes != null && listComprobantes.size() > 0);
                                        break;
                                    case "PC":  //PEDIDOS
                                        listPedido = Pedido.getByUsuario(SQLite.usuario.IdUsuario, SQLite.usuario.sucursal.IdEstablecimiento, fechadesde, fechahasta);
                                        band = (listPedido != null && listPedido.size() > 0);
                                        break;
                                    case "PI":  //PEDIDOS INVENTARIO
                                        listPedidoInv = PedidoInventario.getByUsuario(SQLite.usuario.IdUsuario, SQLite.usuario.sucursal.IdEstablecimiento, fechadesde, fechahasta);
                                        band = (listPedidoInv != null && listPedidoInv.size() > 0);
                                        break;
                                    case "DE":  //COMPROBANTES DE DEPOSITO - DIARIO DE VENTA
                                        listIngresos = Ingreso.getByUsuario(SQLite.usuario.IdUsuario, SQLite.usuario.sucursal.IdEstablecimiento, fechadesde, fechahasta);
                                        band = (listIngresos != null && listIngresos.size() > 0);
                                        break;
                                }

                                if(band){
                                    imgFondo.setVisibility(View.GONE);
                                    lyContainer.setVisibility(View.VISIBLE);
                                    comprobanteAdapter = new ComprobantesAdapter(ListaComprobantesActivity.this, listComprobantes, listPedido, listPedidoInv, listIngresos, tipobusqueda, retornar);
                                    rvComprobantes.setAdapter(comprobanteAdapter);
                                    toolbar.getMenu().findItem(R.id.option_delete).setVisible(true);
                                }else{
                                    imgFondo.setVisibility(View.VISIBLE);
                                    lyContainer.setVisibility(View.GONE);
                                    toolbar.getMenu().findItem(R.id.option_delete).setVisible(false);
                                }
                            }catch (Exception e){
                                Log.d(TAG, e.getMessage());
                            }
                        }
                    );
                }
            };
            th.start();
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }

    public void showDatePickerDialog(boolean isDesde) {
        Locale l = new Locale("ES-es");
        calendar = Calendar.getInstance(l);
        int day=calendar.get(Calendar.DAY_OF_MONTH);
        int month=calendar.get(Calendar.MONTH);
        int year=calendar.get(Calendar.YEAR);
        if(isDesde) {
            String[] fechdes = this.fechadesde.trim().length() > 0 ? this.fechadesde.split("-") : Utils.getDateFormat("yyyy-MM-dd").split("-");
            day = Integer.valueOf(fechdes[2]);
            month = Integer.valueOf(fechdes[1]) - 1;
            year = Integer.valueOf(fechdes[0]);
        }else{
            String[] fechhas = this.fechahasta.trim().length() > 0 ? this.fechahasta.split("-") : Utils.getDateFormat("yyyy-MM-dd").split("-");
            day = Integer.valueOf(fechhas[2]);
            month = Integer.valueOf(fechhas[1]) - 1;
            year = Integer.valueOf(fechhas[0]);
        }
        dtpDialog = new DatePickerDialog(this,
            (v, y, m, d) -> {
                String dia = (d >= 0 && d < 10 ? "0" + (d) : String.valueOf(d));
                String mes = (m >= 0 && m < 9 ? "0" + (m + 1) : String.valueOf(m + 1));

                if (isDesde)
                    fechadesde = y + "-" + mes + "-" + dia;
                else
                    fechahasta = y + "-" + mes + "-" + dia;
                boolean buscar = true;
                if (!fechadesde.equals("") && !fechahasta.equals("")) {
                    if (Utils.longDate(fechadesde) > Utils.longDate(fechahasta)) {
                        Banner.make(rootView, ListaComprobantesActivity.this, Banner.WARNING, "La fecha inicio no puede ser mayor que la fecha fin", Banner.BOTTOM, 3000).show();
                        buscar = false;
                    }
                }
                if (buscar) {
                    toolbar.setSubtitle(fechadesde.equals(fechahasta) ? "Fecha: " + fechadesde : fechadesde + " || " + fechahasta);
                    CargarDatos(tipobusqueda, fechadesde, fechahasta, retornar);
                }
            },year,month,day);
        dtpDialog.show();
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        String documento = "";
        if(tipobusqueda.equals("01"))//FACTURAS
            documento = "Facturas";
        else if (tipobusqueda.equals("8,23") || tipobusqueda.equals("23,8"))
            documento = "Recepciones";
        else if(tipobusqueda.equals("PC"))
            documento = "Pedidos Cliente";
        else if (tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4"))
            documento = "Transferencias || Devoluciones";
        else if (tipobusqueda.equals("PI"))
            documento = "Pedidos Inventario";
        else if (tipobusqueda.equals("DE"))
            documento = "Depósitos";
        toolbar.setTitle(documento);

        if(!fechadesde.equals(""))
            toolbar.setSubtitle("Fecha: " + fechadesde);
        if(fechadesde.equals("") && fechahasta.equals(""))
            toolbar.setSubtitle("Todos");

        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorDate));
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_filter, menu);
        CargarDatos(tipobusqueda, fechadesde, fechahasta, retornar);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_date_init:
                showDatePickerDialog(true);
                break;
            case R.id.option_date_end:
                showDatePickerDialog(false);
                break;
            case R.id.option_delete:
                EliminarRegistros(fechadesde, fechahasta);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void EliminarRegistros(String fechadesde, String fechahasta) {
        try{
            String title = "¿Desea eliminar ";
            if(fechadesde.equals("") && fechahasta.equals(""))
                title += "todos los registros?";
            else if(fechadesde.equals(fechahasta))
                title += "los registros del día «" + fechadesde + "»?";
            else if(!fechadesde.equals("") && fechahasta.equals(""))
                title += "los registros desde el día «" + fechadesde + "» hasta la fecha actual?";
            else if(!fechahasta.equals("") && fechadesde.equals(""))
                title += "todos los registros hasta el día «" + fechahasta + "»?";
            else
                title += "los registros desde «" + fechadesde+ "» hasta «" + fechahasta + "»?";
            final CheckBox ckSincronizados = new CheckBox(this);
            ckSincronizados.setText("¿Solo registros sincronizados?");
            ckSincronizados.setChecked(true);
            AlertDialog.Builder builder = new AlertDialog.Builder(ListaComprobantesActivity.this);
            builder.setTitle("Eliminar registros");
            builder.setMessage(title +
                    "\nNota:\n1.-Después de eliminados no serán visibles.\n" +
                    "2.- Estos registros solo se eliminarán de su dispositivo.");
            builder.setIcon(R.drawable.ic_delete2);
            builder.setView(ckSincronizados);
            builder.setPositiveButton(getResources().getString(R.string.Confirm),
                (dialog, which) -> {
                    int eliminado = 0;
                    String numdoc = "";
                    switch (tipobusqueda) {
                        case "01":
                        case "4,20":
                        case "20,4":
                        case "8,23":
                        case "23,8":
                            eliminado = Comprobante.Delete(0, fechadesde, fechadesde, 0, ckSincronizados.isChecked());
                            if (eliminado > 0) {
                                comprobanteAdapter.listComprobantes.clear();
                                comprobanteAdapter.notifyDataSetChanged();
                            }
                            break;
                        case "PC":
                            eliminado = Pedido.Delete(0, fechadesde, fechadesde, 0, ckSincronizados.isChecked());
                            if (eliminado > 0) {
                                comprobanteAdapter.listPedidos.clear();
                                comprobanteAdapter.notifyDataSetChanged();
                            }
                            break;
                        case "PI":
                            eliminado = PedidoInventario.Delete(0, fechadesde, fechadesde, 0, ckSincronizados.isChecked());
                            if (eliminado > 0) {
                                comprobanteAdapter.listPedidosInv.clear();
                                comprobanteAdapter.notifyDataSetChanged();
                            }
                            break;
                        case "DE":
                            eliminado = Ingreso.Delete(0, fechadesde, fechadesde, 0, ckSincronizados.isChecked());
                            if (eliminado > 0) {
                                comprobanteAdapter.listIngresos.clear();
                                comprobanteAdapter.notifyDataSetChanged();
                            }
                            break;
                    }

                    if (eliminado > 0) {
                        CargarDatos(tipobusqueda, fechadesde, fechahasta, retornar);
                        Banner.make(rootView, ListaComprobantesActivity.this, Banner.SUCCESS, "Registros eliminados correctamente.", Banner.BOTTOM, 3000).show();
                    } else {
                        Banner.make(rootView, ListaComprobantesActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
                    }
                }
            );
            builder.setNegativeButton(getResources().getString(R.string.Cancel), null);
            builder.show();
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        comprobanteAdapter.filter(newText);
        return false;
    }
}
