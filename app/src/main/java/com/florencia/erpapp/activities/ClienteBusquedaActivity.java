package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.ClienteBusquedaAdapter;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.services.SQLite;

import java.util.ArrayList;
import java.util.List;

public class ClienteBusquedaActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    ImageView imgFondo;
    RecyclerView rvCliente;
    List<Cliente> listClientes = new ArrayList<>();
    ClienteBusquedaAdapter clienteAdapter;
    SearchView svBusqueda;
    LinearLayout lyContainer;
    ProgressDialog pgCargando;
    String tipobusqueda = "";
    public static final int REQUEST_NEW_CLIENTE = 2;
    private static String TAG = "TAGCLIENTE_BUSQUEDA_ACT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente_busqueda);

        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pgCargando = new ProgressDialog(this);
        rvCliente = findViewById(R.id.rvClientes);
        imgFondo = findViewById(R.id.imgFondo);
        lyContainer = findViewById(R.id.lyContainer);
        svBusqueda = findViewById(R.id.svBusqueda);
        svBusqueda.setOnQueryTextListener(this);

        pgCargando.setTitle("Cargando clientes");
        pgCargando.setMessage("Espere un momento...");
        pgCargando.setCancelable(false);

        CargarDatos();

        if (getIntent().getExtras() != null) {
            svBusqueda.setQuery(getIntent().getExtras().getString("busqueda", ""), true);
            tipobusqueda = getIntent().getExtras().getString("tipobusqueda", "");
            if (clienteAdapter != null)
                clienteAdapter.tipobusqueda = tipobusqueda;
        }
    }

    private void CargarDatos() {
        try {
            listClientes = Cliente.getClientes(SQLite.usuario.IdUsuario, "");
            if (listClientes == null || listClientes.size() == 0) {
                imgFondo.setVisibility(View.VISIBLE);
                lyContainer.setVisibility(View.GONE);
            } else {
                imgFondo.setVisibility(View.GONE);
                lyContainer.setVisibility(View.VISIBLE);
                clienteAdapter = new ClienteBusquedaAdapter(ClienteBusquedaActivity.this, listClientes, tipobusqueda);
                rvCliente.setAdapter(clienteAdapter);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_cliente, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.option_addclient:
                i = new Intent(this, ClienteActivity.class);
                i.putExtra("nuevo_cliente", true);
                startActivityForResult(i, REQUEST_NEW_CLIENTE);
                overridePendingTransition(R.anim.zoom_back_in, R.anim.zoom_back_out);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_NEW_CLIENTE:
                    setResult(Activity.RESULT_OK, data);
                    finish();
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        String titulo = "Búsqueda de cliente";
        toolbar.setTitle(titulo);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorGreenLight));
        super.onResume();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (clienteAdapter != null)
            clienteAdapter.filter(newText);
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.right_in, R.anim.right_out);
    }
}
