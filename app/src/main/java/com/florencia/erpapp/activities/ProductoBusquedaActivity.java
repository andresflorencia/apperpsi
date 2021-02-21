package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.ClienteAdapter;
import com.florencia.erpapp.adapters.ProductoAdapter;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ProductoBusquedaActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    ImageView imgFondo;
    RecyclerView rvProductos;
    List<Producto> lstProductos= new ArrayList<>();
    ProductoAdapter productoAdapter;
    SearchView svBusqueda;
    LinearLayout lyContainer;
    ProgressDialog pgCargando;
    ProgressBar pbCargando;
    public Toolbar toolbar;
    String tipobusqueda = "01";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producto_busqueda);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pgCargando = new ProgressDialog(this);
        rvProductos = findViewById(R.id.rvProductos);
        imgFondo = findViewById(R.id.imgFondo);
        lyContainer = findViewById(R.id.lyContainer);
        svBusqueda = findViewById(R.id.svBusqueda);
        svBusqueda.setOnQueryTextListener(this);
        pbCargando = findViewById(R.id.pbCargando);

        pgCargando.setTitle("Cargando productos");
        pgCargando.setMessage("Espere un momento...");
        pgCargando.setCancelable(false);

        if(getIntent().getExtras()!=null){
            tipobusqueda = getIntent().getExtras().getString("tipobusqueda","01");
        }

        CargarDatos();

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        productoAdapter.filter(newText);
        return false;
    }

    private void CargarDatos(){
        try {
            //pgCargando.show();
            pbCargando.setVisibility(View.VISIBLE);
            Thread th = new Thread(){
                @Override
                public void run(){
                    switch (tipobusqueda) {
                        case "01":
                        case "PC":
                            lstProductos = Producto.getAll(SQLite.usuario.sucursal.IdEstablecimiento);
                            break;
                        case "4,20":
                        case "20,4":
                            lstProductos = Producto.getForTransferencia(SQLite.usuario.sucursal.IdEstablecimiento);
                            break;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (lstProductos == null || lstProductos.size() == 0) {
                                imgFondo.setVisibility(View.VISIBLE);
                                lyContainer.setVisibility(View.GONE);
                            } else {
                                imgFondo.setVisibility(View.GONE);
                                lyContainer.setVisibility(View.VISIBLE);
                                productoAdapter = new ProductoAdapter(toolbar,lstProductos, tipobusqueda, ProductoBusquedaActivity.this);
                                rvProductos.setAdapter(productoAdapter);
                            }
                            //pgCargando.dismiss();
                            pbCargando.setVisibility(View.GONE);
                        }
                    });
                }
            };
            th.start();
        }catch (Exception e){
            pbCargando.setVisibility(View.GONE);
            //pgCargando.dismiss();
            Log.d("TAGCLIENTE",e.getMessage());
        }
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        String titulo="Productos" ;
        toolbar.setTitle(titulo);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorGreenLight));
        //toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_product,menu);
        menu.findItem(R.id.option_select).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_select:
                if(tipobusqueda.equals("01")) {
                    ComprobanteActivity.productBusqueda.clear();
                    ComprobanteActivity.productBusqueda.addAll(productoAdapter.productosSelected);
                }else if(tipobusqueda.equals("PC")){
                    PedidoActivity.productBusqueda.clear();
                    PedidoActivity.productBusqueda.addAll(productoAdapter.productosSelectedP);
                }else if(tipobusqueda.equals("4,20") ||tipobusqueda.equals("20,4")){
                    TransferenciaActivity.productBusqueda.clear();
                    TransferenciaActivity.productBusqueda.addAll(productoAdapter.productosSelected);
                }
                setResult(RESULT_OK);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
