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
import android.widget.TextView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.ClasificacionAdapter;
import com.florencia.erpapp.adapters.ClienteAdapter;
import com.florencia.erpapp.adapters.ProductoAdapter;
import com.florencia.erpapp.models.Categoria;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;

public class ProductoBusquedaActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static String TAG = "TAGPRODUCTO_BUSQUEDA_ACT";
    RecyclerView rvProductos, rvCategorias;
    List<Producto> lstProductos = new ArrayList<>();
    List<Categoria> categorias = new ArrayList<>();
    public ProductoAdapter productoAdapter;
    ClasificacionAdapter clasificacionAdapter;
    SearchView svBusqueda;
    LinearLayout lyContainer, lyLoading;
    ProgressDialog pgCargando;
    ProgressBar pbCargando;
    public Toolbar toolbar;
    String tipobusqueda = "01";
    public FloatingActionButton btnConfirmar;
    public TextView txtCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producto_busqueda);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init();
        if (getIntent().getExtras() != null) {
            tipobusqueda = getIntent().getExtras().getString("tipobusqueda", "01");
        }

        CargarDatos();

    }

    private void init() {
        pgCargando = new ProgressDialog(this);
        rvProductos = findViewById(R.id.rvProductos);
        rvCategorias = findViewById(R.id.rvCategorias);
        lyContainer = findViewById(R.id.lyContainer);
        svBusqueda = findViewById(R.id.svBusqueda);
        svBusqueda.setOnQueryTextListener(this);
        pbCargando = findViewById(R.id.pbCargando);
        lyLoading = findViewById(R.id.lyLoading);
        btnConfirmar = findViewById(R.id.btnConfirmar);
        txtCounter = findViewById(R.id.txtCounter);

        pgCargando.setTitle("Cargando productos");
        pgCargando.setMessage("Espere un momento...");
        pgCargando.setCancelable(false);

        btnConfirmar.setOnClickListener(v -> Confirmar());
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

    private void CargarDatos() {
        try {
            //pgCargando.show();
            pbCargando.setVisibility(View.VISIBLE);
            lyLoading.setVisibility(View.VISIBLE);
            svBusqueda.setVisibility(View.GONE);
            Thread th = new Thread() {
                @Override
                public void run() {
                    categorias = Producto.getCategorias(SQLite.usuario.sucursal.IdEstablecimiento, tipobusqueda);
                    switch (tipobusqueda) {
                        case "01":
                        case "PC":
                        case "PI":
                        case "PR":
                            lstProductos = Producto.getAll(SQLite.usuario.sucursal.IdEstablecimiento, tipobusqueda.equals("01"));
                            break;
                        case "4,20":
                        case "20,4":
                            lstProductos = Producto.getForTransferencia(SQLite.usuario.sucursal.IdEstablecimiento);
                            break;
                    }
                    runOnUiThread(
                            () -> {
                                if (lstProductos == null || lstProductos.size() == 0) {
                                    lyLoading.setVisibility(View.VISIBLE);
                                    lyContainer.setVisibility(View.GONE);
                                    ((TextView) findViewById(R.id.lblMessage)).setText("No hay productos disponibles. \nIntente descargar los productos.");
                                } else {
                                    productoAdapter = new ProductoAdapter(toolbar, lstProductos, tipobusqueda, ProductoBusquedaActivity.this);
                                    rvProductos.setAdapter(productoAdapter);
                                    clasificacionAdapter = new ClasificacionAdapter(ProductoBusquedaActivity.this, categorias);
                                    rvCategorias.setAdapter(clasificacionAdapter);
                                    lyLoading.setVisibility(View.GONE);
                                    lyContainer.setVisibility(View.VISIBLE);
                                    svBusqueda.setVisibility(View.VISIBLE);
                                    svBusqueda.setEnabled(true);
                                }
                                //pgCargando.dismiss();
                                pbCargando.setVisibility(View.GONE);
                            }
                    );
                }
            };
            th.start();
        } catch (Exception e) {
            pbCargando.setVisibility(View.GONE);
            //pgCargando.dismiss();
            Log.d(TAG, e.getMessage());
        }
    }

    private void Confirmar() {
        if (tipobusqueda.equals("01") || tipobusqueda.equals("PR")) {
            ComprobanteActivity.productBusqueda.clear();
            ComprobanteActivity.productBusqueda.addAll(productoAdapter.productosSelected);
        } else if (tipobusqueda.equals("PC")) {
            PedidoActivity.productBusqueda.clear();
            PedidoActivity.productBusqueda.addAll(productoAdapter.productosSelectedP);
        } else if (tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")) {
            TransferenciaActivity.productBusqueda.clear();
            TransferenciaActivity.productBusqueda.addAll(productoAdapter.productosSelected);
        } else if (tipobusqueda.equals("PI")) {
            PedidoInventarioActivity.productBusqueda.clear();
            PedidoInventarioActivity.productBusqueda.addAll(productoAdapter.productosSelectedPI);
        }
        setResult(RESULT_OK);
        onBackPressed();
        overridePendingTransition(R.anim.zoom_back_in, R.anim.zoom_back_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.zoom_back_in, R.anim.zoom_back_out);
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        String titulo = "Productos";
        toolbar.setTitle(titulo);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorDate));
        //toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_product, menu);
        menu.findItem(R.id.option_select).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_select:
                Confirmar();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
