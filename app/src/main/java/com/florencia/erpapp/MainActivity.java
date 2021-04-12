package com.florencia.erpapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.florencia.erpapp.activities.ComprobanteActivity;
import com.florencia.erpapp.activities.ConfigActivity;
import com.florencia.erpapp.activities.PedidoActivity;
import com.florencia.erpapp.activities.PedidoInventarioActivity;
import com.florencia.erpapp.activities.RecepcionActivity;
import com.florencia.erpapp.activities.TransferenciaActivity;
import com.florencia.erpapp.activities.actLogin;
import com.florencia.erpapp.fragments.ClienteFragment;
import com.florencia.erpapp.fragments.PrincipalFragment;
import com.florencia.erpapp.interfaces.ICliente;
import com.florencia.erpapp.interfaces.IComprobante;
import com.florencia.erpapp.interfaces.IProducto;
import com.florencia.erpapp.interfaces.IUsuario;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.Ingreso;
import com.florencia.erpapp.models.Pedido;
import com.florencia.erpapp.models.PedidoInventario;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.models.Ubicacion;
import com.florencia.erpapp.services.GPSTracker;
import com.florencia.erpapp.services.JobServiceGPS;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.shasin.notificationbanner.Banner;

import java.io.File;
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

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    private DrawerLayout drawerLayout;
    public Fragment fragment;
    private FragmentTransaction fragmentTransaction;
    private FragmentManager fragmentManager;
    public static List<String> listaFragments = new ArrayList<String>();
    private SharedPreferences sPreferencesSesion;
    private TextView txt_Usuario, txtInfo, txtUltimaConexion, txtSucursal, txtEmpresa, txtPerfil;
    private NavigationView navigation;
    private Gson gson = new Gson();
    Retrofit retrofit;
    private ProgressDialog pbProgreso;
    private OkHttpClient okHttpClient;
    private final static int ID_SERVICE_LOCATION =1000;
    View rootView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Inicio");
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        rootView = findViewById(android.R.id.content);

        sPreferencesSesion = getSharedPreferences("DatosSesion", MODE_PRIVATE);

        navigation= findViewById(R.id.navigation_view);
        txt_Usuario=navigation.getHeaderView(0).findViewById(R.id.txt_Usuario);
        txtInfo = navigation.getHeaderView(0).findViewById(R.id.txtInfo);
        txtSucursal = navigation.getHeaderView(0).findViewById(R.id.txtSucursal);
        txtEmpresa = navigation.getHeaderView(0).findViewById(R.id.txtEmpresa);
        txtPerfil = navigation.getHeaderView(0).findViewById(R.id.txtPerfil);
        txtUltimaConexion =  navigation.findViewById(R.id.txtUltimoAcceso);
        try{
            Log.d("TAG", SQLite.configuracion.urlbase);
            if(sPreferencesSesion!=null){
                String ultimoacceso = sPreferencesSesion.getString("ultimaconexion","");
                String url =(SQLite.configuracion.hasSSL?Constants.HTTPs:Constants.HTTP)+SQLite.configuracion.urlbase;
                txtUltimaConexion.setText((ultimoacceso.length()>0? "Último acceso: " + ultimoacceso + "\n":"") + url);
            }
            txtEmpresa.setText(SQLite.usuario.sucursal.RazonSocial.toUpperCase());
            txt_Usuario.setText(SQLite.usuario.RazonSocial);
            txtPerfil.setText("Perfil: " + SQLite.usuario.nombrePerfil);
            txtInfo.setText("Establecimiento: "+ SQLite.usuario.sucursal.CodigoEstablecimiento
                    + " - Punto Emisión: " + SQLite.usuario.sucursal.PuntoEmision );
            txtSucursal.setText(SQLite.usuario.sucursal.NombreSucursal);

            Cliente cliente_cf = new Cliente();
            cliente_cf.nip = "9999999999999";
            cliente_cf.razonsocial = "CONSUMIDOR FINAL";
            cliente_cf.direccion = "N/D";
            cliente_cf.categoria = "0";
            cliente_cf.actualizado = 0;
            cliente_cf.codigosistema = 0;
            cliente_cf.usuarioid = SQLite.usuario.IdUsuario;
            cliente_cf.Save();

        }catch (Exception e){
            Log.d("TAGNAV", e.getMessage());
        }
        navigation.inflateMenu(R.menu.menu_navigation);
        initNavigationDrawer();
        VerificarPermisos();

        fragmentManager = getSupportFragmentManager();
        fragment = new PrincipalFragment();
        String backStateName = fragment.getClass().getName();
        //agregaFragment(backStateName);
        fragment.setArguments(getIntent().getExtras());
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, fragment)
                .commit();
        listaFragments.add(backStateName);

        pbProgreso = new ProgressDialog(this);

        Utils.verificarPermisos(this);

        okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        gson = new GsonBuilder()
                .setLenient()
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(SQLite.configuracion.url_ws)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();

        if(SQLite.gpsTracker==null)
            SQLite.gpsTracker = new GPSTracker(this);

        if (!SQLite.gpsTracker.checkGPSEnabled()) {
            SQLite.gpsTracker.showSettingsAlert(this);
        }

        //SUBIR LA LISTA DE UBICACIONES PENDIENTES
        Thread th = new Thread(){
            @Override
            public void run() {
                loadUbicacion();}
        };
        th.start();
        IniciarServicio();
    }

    public void agregaFragment(String backStateName){
        fragment.setArguments(getIntent().getExtras());
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, fragment)
                .addToBackStack(null)
                .commit();
        if(!listaFragments.contains(backStateName)) {
            /*fragmentTransaction.replace(R.id.fragment, fragment)
                    .addToBackStack(backStateName)
                    .commit();*/
            listaFragments.add(backStateName);
        }else{
            /*fragmentTransaction.replace(R.id.fragment, fragment)
                    .addToBackStack(null)
                    .commit();*/
        }
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    public void initNavigationDrawer() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);

        navigationView.setNavigationItemSelectedListener(
            (menuItem)-> {
                boolean retorno=true;
                int id = menuItem.getItemId();
                //getSupportActionBar().setTitle(menuItem.getTitle());
                String backStateName;
                Intent i;

                switch (id) {
                    case R.id.nav_home:
                    case R.id.nav_cliente:
                        menuItem.setChecked(true);
                        if(id == R.id.nav_home)
                            fragment = new PrincipalFragment();
                        else if(id == R.id.nav_cliente)
                            fragment = new ClienteFragment();

                        backStateName = fragment.getClass().getName();
                        agregaFragment(backStateName);
                        break;
                    case R.id.nav_factura:
                        i = new Intent(MainActivity.this, ComprobanteActivity.class);
                        startActivity(i);
                        break;
                    case R.id.nav_pedido:
                        i = new Intent(MainActivity.this, PedidoActivity.class);
                        startActivity(i);
                        break;
                    case R.id.nav_recepcion:
                        i = new Intent(MainActivity.this, RecepcionActivity.class);
                        startActivity(i);
                        break;
                    case R.id.nav_transferencia:
                        i = new Intent(MainActivity.this, TransferenciaActivity.class);
                        startActivity(i);
                        break;
                    case R.id.nav_pedidoinv:
                        i = new Intent(MainActivity.this, PedidoInventarioActivity.class);
                        startActivity(i);
                        break;
                    case R.id.nav_config:
                        i = new Intent(MainActivity.this, ConfigActivity.class);
                        startActivity(i);
                        break;
                    case R.id.nav_cerrarsesion:
                        if(SQLite.usuario != null) {
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme);
                            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_confirmation_dialog,
                                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                            builder.setView(view);
                            ((TextView)view.findViewById(R.id.lblTitle)).setText("Salir");
                            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea cerrar sesión?");
                            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_exit);
                            ((Button)view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
                            ((Button)view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
                            final android.app.AlertDialog alertDialog = builder.create();
                            view.findViewById(R.id.btnConfirm).setOnClickListener(
                                v -> {
                                    if(SQLite.usuario.CerrarSesionLocal(getApplicationContext())) {
                                        DetenerServicio();
                                        Intent in = new Intent(MainActivity.this, actLogin.class);
                                        startActivity(in);
                                        alertDialog.dismiss();
                                        finish();
                                    }
                                }
                            );

                            view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

                            if(alertDialog.getWindow()!=null)
                                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                            alertDialog.show();
                        }
                        break;

                }
                return retorno;
            }
        );

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View v) {
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);
        menu.findItem(R.id.option_sincronizacomprobantes)
                .setVisible(SQLite.usuario.VerificaPermiso(this,Constants.PUNTO_VENTA,"escritura"));
        menu.findItem(R.id.option_sincronizapedidos)
                .setVisible(SQLite.usuario.VerificaPermiso(this,Constants.PEDIDO,"escritura"));
        menu.findItem(R.id.option_sincronizapedidos_inv)
                .setVisible(SQLite.usuario.VerificaPermiso(this,Constants.PEDIDO_INVENTARIO,"escritura"));
        menu.findItem(R.id.option_sincronizadepositos)
                .setVisible(SQLite.usuario.VerificaPermiso(this,Constants.PUNTO_VENTA,"escritura"));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent i;
        switch (item.getItemId()){
            case R.id.option_descargaproductos:
                descargaProductos(getApplicationContext());
                break;
            case R.id.option_sincronizaclientes:
                sincronizaClientes(getApplicationContext());
                break;
            case R.id.option_sincronizacomprobantes:
                sincronizaComprobantes(getApplicationContext());
                break;
            case R.id.option_sincronizapedidos:
                sincronizaPedidos(getApplicationContext());
                break;
            case R.id.option_sincronizapedidos_inv:
                sincronizaPedidos_Inv(getApplicationContext());
                break;
            case R.id.option_sincronizadepositos:
                sincronizaDepositos(getApplicationContext());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sincronizaClientes(final Context context) {
        try{
            /*if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                Banner.make(rootView, MainActivity.this,Banner.ERROR, Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM, 3000).show();
                return;
            }*/

            List<Cliente> listClientes = Cliente.getClientesSC(SQLite.usuario.IdUsuario);
            if(listClientes == null) {
                listClientes = new ArrayList<>();
            }

            pbProgreso.setTitle("Sincronizando clientes");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            ICliente miInterface = retrofit.create(ICliente.class);

            Map<String,Object> post = new HashMap<>();
            post.put("usuario",SQLite.usuario.Usuario);
            post.put("clave",SQLite.usuario.Clave);
            post.put("clientes", listClientes);
            Call<JsonObject> call = miInterface.LoadCliente2(post);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        Banner.make(rootView, MainActivity.this,Banner.ERROR,"Error: " + response.code() + " - " + response.message(), Banner.BOTTOM,3000).show();
                        pbProgreso.dismiss();
                        return;
                    }
                    try {

                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonClientesUpdate = obj.getAsJsonArray("clientesupdate");
                                if(jsonClientesUpdate!=null){
                                    for(JsonElement ele:jsonClientesUpdate){
                                        JsonObject cli = ele.getAsJsonObject();
                                        ContentValues values = new ContentValues();
                                        values.put("codigosistema", cli.get("codigosistema").getAsInt());
                                        values.put("actualizado",0);
                                        Cliente.Update(cli.get("idcliente").getAsInt(),values);
                                    }
                                }

                                JsonArray jsonClientes = obj.getAsJsonArray("clientes");
                                if(jsonClientes!=null){
                                    int numClientUpdate = 0;
                                    Cliente.removeClientes(SQLite.usuario.IdUsuario);
                                    for(JsonElement ele:jsonClientes){
                                        JsonObject clie = ele.getAsJsonObject();
                                        Cliente miCliente = new Cliente();
                                        miCliente.codigosistema = clie.has("idpersona")?clie.get("idpersona").getAsInt():0;
                                        miCliente.tiponip = clie.get("tiponip").isJsonNull()?"00":clie.get("tiponip").getAsString();
                                        miCliente.nip = clie.get("nip").isJsonNull()?"":clie.get("nip").getAsString();
                                        miCliente.razonsocial = clie.get("razonsocial").isJsonNull()?"":clie.get("razonsocial").getAsString();
                                        miCliente.nombrecomercial = clie.get("nombrecomercial").isJsonNull()?"":clie.get("nombrecomercial").getAsString();
                                        miCliente.direccion = clie.get("direccion").isJsonNull()?"":clie.get("direccion").getAsString();
                                        miCliente.lat = clie.get("lat").isJsonNull()?0:clie.get("lat").getAsDouble();
                                        miCliente.lon = clie.get("lon").isJsonNull()?0:clie.get("lon").getAsDouble();
                                        miCliente.fono1 = clie.get("fono1").isJsonNull()?"":clie.get("fono1").getAsString();
                                        miCliente.fono2 = clie.get("fono2").isJsonNull()?"":clie.get("fono2").getAsString();
                                        miCliente.usuarioid = SQLite.usuario.IdUsuario;//clie.get("usuarioid").isJsonNull()?SQLite.usuario.IdUsuario:clie.get("usuarioid").getAsInt();
                                        miCliente.categoria = clie.get("categoria").isJsonNull()?"":clie.get("categoria").getAsString();
                                        miCliente.email = clie.get("email").isJsonNull()?"":clie.get("email").getAsString();
                                        miCliente.observacion = clie.get("observacion").isJsonNull()?"":clie.get("observacion").getAsString();
                                        miCliente.ruc = clie.get("ruc").isJsonNull()?"":clie.get("ruc").getAsString();
                                        miCliente.parroquiaid = clie.get("parroquiaid").isJsonNull()?0:clie.get("parroquiaid").getAsInt();
                                        miCliente.fecharegistro = clie.has("fechareg")?clie.get("fechareg").getAsString():"";
                                        miCliente.longdater = Utils.longDate(miCliente.fecharegistro);
                                        miCliente.nombrecategoria = clie.has("nombrecategoria")?clie.get("nombrecategoria").getAsString():"";

                                        if(miCliente.Save() || miCliente.nip.equals("9999999999999"))
                                            numClientUpdate++;
                                    }
                                    if(numClientUpdate == jsonClientes.size()) {
                                        Banner.make(rootView, MainActivity.this, Banner.SUCCESS, Constants.MSG_PROCESO_COMPLETADO + "\nSe sincronizó " + numClientUpdate + " registro(s). " + obj.get("message").getAsString(), Banner.BOTTOM, 3000).show();
                                        try {
                                            List<Fragment> fragments = fragmentManager.getFragments();
                                            if (fragments != null) {
                                                for (Fragment f : fragments) {
                                                    Log.d("TAGMAIN", "Fls: " + f.getClass().getSimpleName());
                                                    if (f.getClass().getSimpleName().equalsIgnoreCase("clientefragment") && f.isVisible()) {
                                                        fragment = f;
                                                        break;
                                                    }else if (f.getClass().getSimpleName().equalsIgnoreCase("principalfragment") && f.isVisible()) {
                                                        fragment = f;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (fragment != null) {
                                                if (fragment.getClass().getSimpleName().equalsIgnoreCase("clientefragment") && fragment.isVisible())
                                                    ((ClienteFragment) fragment).CargarDatos(false);
                                                else if (fragment.getClass().getSimpleName().equalsIgnoreCase("principalfragment") && fragment.isVisible())
                                                    ((PrincipalFragment) fragment).BuscaResumen("");
                                            }
                                        }catch (Exception e){
                                            Log.d("TAGMAIN", e.getMessage());
                                        }
                                    }else
                                        Banner.make(rootView, MainActivity.this, Banner.WARNING, Constants.MSG_PROCESO_NO_COMPLETADO + "\nSe sincronizó " + numClientUpdate +"/" + jsonClientes.size() +" registro(s). " + obj.get("message").getAsString(), Banner.BOTTOM,3500).show();
                                }
                            } else
                                Utils.showErrorDialog(MainActivity.this,"Error", obj.get("message").getAsString());
                        } else {
                            Banner.make(rootView, MainActivity.this, Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM,3000).show();
                        }
                    }catch (JsonParseException ex){
                        Log.d("TAG", ex.getMessage());
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(MainActivity.this, "Error",t.getMessage());
                    Log.d("TAG", t.getMessage());
                    call.cancel();
                    pbProgreso.dismiss();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.d("TAG", e.getMessage());
            Utils.showErrorDialog(MainActivity.this, "Error",e.getMessage());
            pbProgreso.dismiss();
        }
    }

    private void sincronizaComprobantes(final Context context) {
        try{
            /*if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                Banner.make(rootView, this, Banner.ERROR, Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM, 3000).show();
                return;
            }*/

            List<Comprobante> listComprobantes = Comprobante.getPorSincronizar(SQLite.usuario.IdUsuario);
            if(listComprobantes == null)
                listComprobantes = new ArrayList<>();

            if(listComprobantes.size() == 0 ){
                Banner.make(rootView,this, Banner.INFO,"No hay comprobantes por sincronizar.", Banner.BOTTOM, 2000).show();
                return;
            }

            pbProgreso.setTitle("Sincronizando comprobantes");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Map<String,Object> post = new HashMap<>();
            post.put("usuario",SQLite.usuario.Usuario);
            post.put("clave",SQLite.usuario.Clave);
            post.put("comprobantes", listComprobantes);
            post.put("establecimientoid", SQLite.usuario.sucursal.IdEstablecimiento);
            String json = post.toString();
            Log.d("TAGJSON", json);
            Call<JsonObject> call=null;
            call=miInterface.LoadComprobantes(post);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        Banner.make(rootView, MainActivity.this, Banner.ERROR,"Código:" + response.code() + " - " + response.message(), Banner.BOTTOM,2500).show();
                        pbProgreso.dismiss();
                        return;
                    }
                    try {

                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonComprobantesUpdate = obj.getAsJsonArray("comprobantesupdate");

                                if(obj.has("productos")) {
                                    JsonArray jsonProductos = obj.getAsJsonArray("productos");
                                    if (jsonProductos != null) {
                                        Producto.Delete(SQLite.usuario.sucursal.IdEstablecimiento);
                                        int num = 1;
                                        for (JsonElement pro : jsonProductos) {
                                            JsonObject prod = pro.getAsJsonObject();
                                            Producto miProducto = new Gson().fromJson(prod, Producto.class);
                                            if (miProducto != null) {
                                                if (miProducto.Save()) {
                                                    num++;
                                                    Log.d("TAG", prod.get("nombreproducto").getAsString());
                                                }
                                            }
                                        }
                                    }
                                }

                                if(jsonComprobantesUpdate!=null){
                                    int numUpdate = 0;
                                    ContentValues values;
                                    for(JsonElement ele:jsonComprobantesUpdate){
                                        JsonObject upd = ele.getAsJsonObject();
                                        //ACTUALIZAR EL CLIENTE
                                        values = new ContentValues();
                                        values.put("codigosistema", upd.get("codigosistema_cliente").getAsInt());
                                        values.put("actualizado",0);
                                        Cliente.Update(upd.get("idcliente").getAsInt(),values);

                                        values = new ContentValues();
                                        values.put("codigosistema", upd.get("codigosistema_comprobante").getAsInt());
                                        values.put("estado", 1);
                                        if(Comprobante.Update(upd.get("idcomprobante").getAsInt(), values))
                                            numUpdate++;
                                    }

                                    if(numUpdate == jsonComprobantesUpdate.size()) {
                                        Banner.make(rootView, MainActivity.this, Banner.SUCCESS, Constants.MSG_PROCESO_COMPLETADO
                                                + "\nSe sincronizó " + numUpdate + " comprobante(s)."
                                                + "\n" + obj.get("message").getAsString(), Banner.BOTTOM, 3000).show();

                                        List<Fragment> fragments = fragmentManager.getFragments();
                                        if (fragments != null) {
                                            for (Fragment f : fragments) {
                                                Log.d("TAGMAIN", "Fls: " + f.getClass().getSimpleName());
                                                if(f.getClass().getSimpleName().equalsIgnoreCase("principalfragment") && f.isVisible()) {
                                                    fragment = f;
                                                    break;
                                                }
                                            }
                                        }
                                        if(fragment!=null) {
                                            if (fragment.getClass().getSimpleName().equalsIgnoreCase("principalfragment") ||
                                                    (listaFragments.size() > 0 && listaFragments.get(listaFragments.size() - 1).toLowerCase().contains("principalfragment"))) {
                                                ((PrincipalFragment) fragment).BuscaResumen("");
                                            }
                                        }
                                    }else
                                        Banner.make(rootView, MainActivity.this, Banner.ERROR, Constants.MSG_PROCESO_NO_COMPLETADO
                                                + "\nSe sincronizó " + numUpdate + "/" + jsonComprobantesUpdate.size() + " comprobante(s)."
                                                + "\n"+ obj.get("message").getAsString(), Banner.BOTTOM, 3500).show();


                                }

                            } else
                                Utils.showErrorDialog(MainActivity.this,"Error", obj.get("message").getAsString());
                        } else {
                            Banner.make(rootView, MainActivity.this, Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM, 3000).show();
                        }
                    }catch (JsonParseException ex){
                        Log.d("TAG", ex.getMessage());
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(MainActivity.this, "Error",t.getMessage());
                    Log.d("TAG", t.getMessage());
                    call.cancel();
                    pbProgreso.dismiss();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.d("TAG", e.getMessage());
            Utils.showErrorDialog(this, "Error",e.getMessage());
            pbProgreso.dismiss();
        }
    }

    private void sincronizaPedidos(final Context context) {
        try{
            /*if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                Banner.make(rootView, this,Banner.ERROR, Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM,3000).show();
                return;
            }*/

            List<Pedido> listPedidos = Pedido.getPorSincronizar(SQLite.usuario.IdUsuario);
            if(listPedidos == null)
                listPedidos = new ArrayList<>();

            if(listPedidos.size() == 0 ){
                Banner.make(rootView,this, Banner.INFO,"No hay pedidos por sincronizar.", Banner.BOTTOM,2000).show();
                return;
            }

            pbProgreso.setTitle("Sincronizando pedidos");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Map<String,Object> post = new HashMap<>();
            post.put("usuario",SQLite.usuario.Usuario);
            post.put("clave",SQLite.usuario.Clave);
            post.put("pedidos", listPedidos);
            String json = post.toString();
            Log.d("TAGJSON", json);
            Call<JsonObject> call=null;
            call=miInterface.LoadPedidos(post);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        Banner.make(rootView,MainActivity.this,Banner.ERROR,"Código: " + response.code() + " - " + response.message(), Banner.BOTTOM,3000).show();
                        pbProgreso.dismiss();
                        return;
                    }
                    try {

                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonPedidosUpdate = obj.getAsJsonArray("pedidosupdate");
                                if(jsonPedidosUpdate!=null){
                                    int numUpdate = 0;
                                    ContentValues values;
                                    for(JsonElement ele:jsonPedidosUpdate){
                                        JsonObject upd = ele.getAsJsonObject();
                                        //ACTUALIZAR EL CLIENTE
                                        values = new ContentValues();
                                        values.put("codigosistema", upd.get("codigosistema_cliente").getAsInt());
                                        values.put("actualizado",0);
                                        Cliente.Update(upd.get("idcliente").getAsInt(),values);

                                        values = new ContentValues();
                                        values.put("codigosistema", upd.get("codigosistema_pedido").getAsInt());
                                        values.put("estado", upd.get("codigosistema_pedido").getAsInt());
                                        values.put("secuencialsistema", upd.get("secuencialsistema").getAsString());
                                        if(Pedido.Update(upd.get("idpedido").getAsInt(), values))
                                            numUpdate++;
                                    }

                                    if(obj.has("secuencial_pe")){
                                        Integer secuencial_pe = obj.get("secuencial_pe").getAsInt();
                                        Comprobante comprobante = new Comprobante();
                                        comprobante.secuencial = secuencial_pe;
                                        comprobante.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
                                        comprobante.codigoestablecimiento = SQLite.usuario.sucursal.CodigoEstablecimiento;
                                        comprobante.puntoemision = SQLite.usuario.sucursal.PuntoEmision;
                                        comprobante.tipotransaccion = "PC";
                                        comprobante.actualizasecuencial();
                                    }
                                    if(numUpdate == jsonPedidosUpdate.size()) {
                                        Banner.make(rootView,MainActivity.this,Banner.SUCCESS, Constants.MSG_PROCESO_COMPLETADO
                                                + "\nSe sincronizó " + numUpdate + " pedido(s)."
                                                + "\n" + obj.get("message").getAsString(), Banner.BOTTOM, 3000).show();

                                        try {
                                            List<Fragment> fragments = fragmentManager.getFragments();
                                            if (fragments != null) {
                                                for (Fragment f : fragments) {
                                                    Log.d("TAGMAIN", "Fls: " + f.getClass().getSimpleName());
                                                    if(f.getClass().getSimpleName().equalsIgnoreCase("principalfragment") && f.isVisible()) {
                                                        fragment = f;
                                                        break;
                                                    }
                                                }
                                            }
                                            if(fragment!=null) {
                                                Log.d("TAGMAIN1", fragment.getClass().getSimpleName());
                                                if (fragment.getClass().getSimpleName().equalsIgnoreCase("principalfragment") ||
                                                        (listaFragments.size() > 0 && listaFragments.get(listaFragments.size() - 1).toLowerCase().contains("principalfragment"))) {
                                                    ((PrincipalFragment) fragment).BuscaResumen("");
                                                }
                                            }
                                        }catch (Exception e){
                                            Log.d("TAGMAIN", e.getMessage());
                                        }
                                    }else {
                                        Banner.make(rootView, MainActivity.this, Banner.SUCCESS, Constants.MSG_PROCESO_NO_COMPLETADO
                                                + "\nSe sincronizó " + numUpdate + "/" + jsonPedidosUpdate.size() + " pedido(s)."
                                                + "\n" + obj.get("message").getAsString(), Banner.BOTTOM,3500).show();
                                    }
                                }

                            } else
                                Utils.showErrorDialog(MainActivity.this,"Error", obj.get("message").getAsString());
                        } else {
                            Banner.make(rootView,MainActivity.this,Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM,3000).show();
                        }
                    }catch (JsonParseException ex){
                        Log.d("TAG", ex.getMessage());
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(MainActivity.this,"Error",t.getMessage());
                    Log.d("TAG", t.getMessage());
                    call.cancel();
                    pbProgreso.dismiss();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.d("TAG", e.getMessage());
            Utils.showErrorDialog(this, "Error",e.getMessage());
            pbProgreso.dismiss();
        }
    }

    private void contarproductos(Context context) {
        try {
            //Log.d("TAG", String.valueOf(Producto.getAll(SQLite.usuario.sucursal.IdEstablecimiento).size()));
        }catch (Exception e){
            Log.d("TAG",e.getMessage());
        }
    }

    private void descargaProductos(final Context context){
        try{
            /*if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                Banner.make(rootView,MainActivity.this,Banner.ERROR, Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM,3000).show();
                return;
            }*/

            pbProgreso.setTitle("Descargando productos");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            IProducto miInterface = retrofit.create(IProducto.class);

            Call<JsonObject> call=null;
            call=miInterface.GetProductos(SQLite.usuario.Usuario,SQLite.usuario.Clave,SQLite.usuario.sucursal.IdEstablecimiento);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        Banner.make(rootView,MainActivity.this,Banner.ERROR,"Código: " + response.code() + " - " + response.message(), Banner.BOTTOM,3000).show();
                        pbProgreso.dismiss();
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonProductos = obj.getAsJsonArray("productos");
                                if(jsonProductos!=null){
                                    int numProd = 0;
                                    Producto.Delete(SQLite.usuario.sucursal.IdEstablecimiento);
                                    for (JsonElement ele : jsonProductos) {
                                        JsonObject prod = ele.getAsJsonObject();
                                        Producto miProducto = new Gson().fromJson(prod, Producto.class);
                                    /*miProducto.idproducto = prod.get("idproducto").getAsInt();
                                    miProducto.codigoproducto = prod.get("codigoproducto").getAsString();
                                    miProducto.nombreproducto = prod.get("nombreproducto").getAsString();
                                    miProducto.pvp = prod.get("pvp").getAsDouble();
                                    miProducto.unidadid = prod.get("unidadid").getAsInt();
                                    miProducto.unidadesporcaja = prod.get("unidadesporcaja").getAsInt();
                                    miProducto.iva = prod.get("iva").getAsInt();
                                    miProducto.ice = prod.get("ice").getAsInt();
                                    miProducto.factorconversion = prod.get("factorconversion").getAsDouble();
                                    miProducto.pvp1 = prod.get("pvp1").getAsDouble();
                                    miProducto.pvp2 = prod.get("pvp2").getAsDouble();
                                    miProducto.pvp3 = prod.get("pvp3").getAsDouble();
                                    miProducto.pvp4 = prod.get("pvp4").getAsDouble();
                                    miProducto.pvp5 = prod.get("pvp5").getAsDouble();
                                    miProducto.stock = prod.get("stock").getAsDouble();
                                    miProducto.porcentajeiva = prod.get("porcentajeiva").getAsDouble();
                                    JsonArray jsonLotes = prod.has("lotes")?prod.get("lotes").getAsJsonArray():null;
                                    if(jsonLotes != null){
                                        miProducto.lotes = new Gson().fromJson(jsonLotes,miProducto.lotes.getClass());
                                    }*/
                                        if (miProducto != null) {
                                            if (miProducto.Save())
                                                numProd++;
                                            Log.d("TAG", prod.get("nombreproducto").getAsString());
                                        }
                                    }
                                    if (numProd == jsonProductos.size())
                                        Banner.make(rootView,MainActivity.this,Banner.SUCCESS, Constants.MSG_PROCESO_COMPLETADO + (numProd > 0?"\nSe descargó " + numProd + " producto(s)":""), Banner.BOTTOM,3000).show();
                                    else
                                        Banner.make(rootView,MainActivity.this,Banner.ERROR, Constants.MSG_PROCESO_NO_COMPLETADO, Banner.BOTTOM,3000).show();
                                }
                            } else
                                Banner.make(rootView,MainActivity.this,Banner.ERROR, obj.get("message").getAsString(), Banner.BOTTOM,3000).show();
                        } else
                            Banner.make(rootView,MainActivity.this,Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM,3000).show();
                    }catch (JsonParseException ex){
                        Log.d("TAG", ex.getMessage());
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(MainActivity.this,"Error",t.getMessage());
                    Log.d("TAG", t.getMessage());
                    call.cancel();
                    pbProgreso.dismiss();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.d("TAG", e.getMessage());
            Utils.showErrorDialog(this,"Error",e.getMessage());
            pbProgreso.dismiss();
        }
    }

    private void sincronizaPedidos_Inv(final Context context) {
        try{
            /*if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                Banner.make(rootView, this,Banner.ERROR, Constants.MSG_COMPROBAR_CONEXION_INTERNET, Banner.BOTTOM,3000).show();
                return;
            }*/

            List<PedidoInventario> listPedidos = PedidoInventario.getPorSincronizar(SQLite.usuario.IdUsuario);
            if(listPedidos == null)
                listPedidos = new ArrayList<>();

            if(listPedidos.size() == 0 ){
                Banner.make(rootView,this, Banner.INFO,"No hay pedidos por sincronizar.", Banner.BOTTOM,2000).show();
                return;
            }

            pbProgreso.setTitle("Sincronizando pedidos");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Map<String,Object> post = new HashMap<>();
            post.put("usuario",SQLite.usuario.Usuario);
            post.put("clave",SQLite.usuario.Clave);
            post.put("pedidos", listPedidos);
            post.put("periodo", SQLite.usuario.sucursal.periodo.toString() + SQLite.usuario.sucursal.mesactual);
            post.put("codigoestablecimiento", SQLite.usuario.sucursal.IdSucursal);
            Call<JsonObject> call = miInterface.LoadPedidosInv(post);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        Banner.make(rootView,MainActivity.this,Banner.ERROR,"Código: " + response.code() + " - " + response.message(), Banner.BOTTOM,3000).show();
                        pbProgreso.dismiss();
                        return;
                    }
                    try {

                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonPedidosUpdate = obj.getAsJsonArray("pedidosupdate");
                                if(jsonPedidosUpdate!=null){
                                    int numUpdate = 0;
                                    ContentValues values;
                                    for(JsonElement ele:jsonPedidosUpdate){
                                        JsonObject upd = ele.getAsJsonObject();

                                        values = new ContentValues();
                                        values.put("codigosistema", upd.get("codigosistema_pedido").getAsInt());
                                        values.put("estadomovil", upd.get("codigosistema_pedido").getAsInt());
                                        if(PedidoInventario.Update(upd.get("idpedido").getAsInt(), values))
                                            numUpdate++;
                                    }

                                    if(obj.has("secuencial_pi")){
                                        Integer secuencial_pi = obj.get("secuencial_pi").getAsInt();
                                        PedidoInventario pedido = new PedidoInventario();
                                        pedido.secuencial = secuencial_pi;
                                        pedido.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
                                        pedido.tipotransaccion = "PI";
                                        pedido.actualizasecuencial();
                                    }
                                    if(numUpdate == jsonPedidosUpdate.size()) {
                                        Banner.make(rootView,MainActivity.this,Banner.SUCCESS, Constants.MSG_PROCESO_COMPLETADO
                                                + "\nSe sincronizó " + numUpdate + " pedido(s)."
                                                + "\n" + obj.get("message").getAsString(), Banner.BOTTOM, 3000).show();

                                        try {
                                            List<Fragment> fragments = fragmentManager.getFragments();
                                            if (fragments != null) {
                                                for (Fragment f : fragments) {
                                                    Log.d("TAGMAIN", "Fls: " + f.getClass().getSimpleName());
                                                    if(f.getClass().getSimpleName().equalsIgnoreCase("principalfragment") && f.isVisible()) {
                                                        fragment = f;
                                                        break;
                                                    }
                                                }
                                            }
                                            if(fragment!=null) {
                                                Log.d("TAGMAIN1", fragment.getClass().getSimpleName());
                                                if (fragment.getClass().getSimpleName().equalsIgnoreCase("principalfragment") ||
                                                        (listaFragments.size() > 0 && listaFragments.get(listaFragments.size() - 1).toLowerCase().contains("principalfragment"))) {
                                                    ((PrincipalFragment) fragment).BuscaResumen("");
                                                }
                                            }
                                        }catch (Exception e){
                                            Log.d("TAGMAIN", e.getMessage());
                                        }
                                    }else {
                                        Banner.make(rootView, MainActivity.this, Banner.SUCCESS, Constants.MSG_PROCESO_NO_COMPLETADO
                                                + "\nSe sincronizó " + numUpdate + "/" + jsonPedidosUpdate.size() + " pedido(s)."
                                                + "\n" + obj.get("message").getAsString(), Banner.BOTTOM,3500).show();
                                    }
                                }

                            } else
                                Utils.showErrorDialog(MainActivity.this,"Error", obj.get("message").getAsString());
                        } else {
                            Banner.make(rootView,MainActivity.this,Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM,3000).show();
                        }
                    }catch (JsonParseException ex){
                        Log.d("TAG", ex.getMessage());
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(MainActivity.this,"Error",t.getMessage());
                    Log.d("TAG", t.getMessage());
                    call.cancel();
                    pbProgreso.dismiss();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.d("TAG", e.getMessage());
            Utils.showErrorDialog(this, "Error",e.getMessage());
            pbProgreso.dismiss();
        }
    }

    private void sincronizaDepositos(final Context context){
        try{
            List<Ingreso> listIngresos = Ingreso.getPorSincronizar(SQLite.usuario.IdUsuario);
            if(listIngresos == null || listIngresos.size()==0){
                Banner.make(rootView,this, Banner.INFO,"No hay comprobantes de depósitos por sincronizar.", Banner.BOTTOM,2000).show();
                return;
            }
            for(Ingreso ingreso: listIngresos) {
                if (ingreso.fotos == null)
                    continue;
                for (int i = 0; i < ingreso.fotos.size(); i++) {
                    try {
                        String ExternalDirectory = getExternalMediaDirs()[0] + File.separator + Constants.FOLDER_FILES;
                        File miFile = new File(ExternalDirectory, ingreso.fotos.get(i).name);
                        Uri path = Uri.fromFile(miFile);
                        ingreso.fotos.get(i).bitmap = MediaStore.Images.Media.getBitmap(
                                MainActivity.this.getContentResolver(),
                                path);
                        ingreso.fotos.get(i).image_base = Utils.convertImageToString(ingreso.fotos.get(i).bitmap);
                    } catch (IOException e) {
                        Log.d("TAGMAINACTIVITY", "NotFound(): " + e.getMessage());
                    }
                }
            }

            pbProgreso.setTitle("Sincronizando depósitos");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            IComprobante miInterface = retrofit.create(IComprobante.class);

            Map<String,Object> post = new HashMap<>();
            post.put("usuario",SQLite.usuario.Usuario);
            post.put("clave",SQLite.usuario.Clave);
            post.put("depositos", listIngresos);
            post.put("periodo", SQLite.usuario.sucursal.periodo.toString() + SQLite.usuario.sucursal.mesactual);
            post.put("periodoactual", SQLite.usuario.sucursal.periodo);
            post.put("mesactual", SQLite.usuario.sucursal.mesactual);
            post.put("establecimientoid", SQLite.usuario.sucursal.IdEstablecimiento);
            Call<JsonObject> call = miInterface.LoadDepositos(post);

            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        Banner.make(rootView,MainActivity.this,Banner.ERROR,"Código: " + response.code() + " - " + response.message(), Banner.BOTTOM,3000).show();
                        pbProgreso.dismiss();
                        return;
                    }

                    try {

                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonDepositosUpdate = obj.getAsJsonArray("depositosupdate");
                                if(jsonDepositosUpdate!=null){
                                    int numUpdate = 0;
                                    ContentValues values;
                                    for(JsonElement ele:jsonDepositosUpdate){
                                        JsonObject upd = ele.getAsJsonObject();
                                        values = new ContentValues();
                                        values.put("codigosistema", upd.get("codigosistema_deposito").getAsInt());
                                        values.put("estado", upd.get("codigosistema_deposito").getAsInt());
                                        if(Ingreso.Update(upd.get("idingreso").getAsInt(), values))
                                            numUpdate++;
                                    }

                                    if(obj.has("secuencial_dep")){
                                        Integer secuencial_pe = obj.get("secuencial_dep").getAsInt();
                                        Ingreso comprobante = new Ingreso();
                                        comprobante.secuencial = secuencial_pe;
                                        comprobante.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
                                        comprobante.actualizasecuencial();
                                    }

                                    if(numUpdate == jsonDepositosUpdate.size()) {
                                        Banner.make(rootView,MainActivity.this,Banner.SUCCESS, Constants.MSG_PROCESO_COMPLETADO
                                                + "\nSe sincronizó " + numUpdate + " deposito(s)."
                                                + "\n" + obj.get("message").getAsString(), Banner.BOTTOM, 3000).show();

                                        try {
                                            List<Fragment> fragments = fragmentManager.getFragments();
                                            if (fragments != null) {
                                                for (Fragment f : fragments) {
                                                    if(f.getClass().getSimpleName().equalsIgnoreCase("principalfragment") && f.isVisible()) {
                                                        fragment = f;
                                                        break;
                                                    }
                                                }
                                            }
                                            if(fragment!=null) {
                                                if (fragment.getClass().getSimpleName().equalsIgnoreCase("principalfragment") ||
                                                        (listaFragments.size() > 0 && listaFragments.get(listaFragments.size() - 1).toLowerCase().contains("principalfragment"))) {
                                                    ((PrincipalFragment) fragment).BuscaResumen("");
                                                }
                                            }
                                        }catch (Exception e){
                                            Log.d("TAGMAIN", e.getMessage());
                                        }
                                    }else {
                                        Banner.make(rootView, MainActivity.this, Banner.SUCCESS, Constants.MSG_PROCESO_NO_COMPLETADO
                                                + "\nSe sincronizó " + numUpdate + "/" + jsonDepositosUpdate.size() + " deposito(s)."
                                                + "\n" + obj.get("message").getAsString(), Banner.BOTTOM,3500).show();
                                    }
                                }

                            } else
                                Utils.showErrorDialog(MainActivity.this,"Error", obj.get("message").getAsString());
                        } else {
                            Banner.make(rootView,MainActivity.this,Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM,3000).show();
                        }
                    }catch (JsonParseException ex){
                        Log.d("TAG", ex.getMessage());
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(MainActivity.this,"Error",t.getMessage());
                    Log.d("TAG", t.getMessage());
                    call.cancel();
                    pbProgreso.dismiss();
                }
            });

        }catch (Exception e){
            e.printStackTrace();
            Log.d("TAG", e.getMessage());
            Utils.showErrorDialog(this, "Error",e.getMessage());
            pbProgreso.dismiss();
        }
    }

    private void VerificarPermisos() {
        try {
            for (int i = 0; i < navigation.getMenu().size(); i++) {
                MenuItem menuItem = navigation.getMenu().getItem(i);
                if (menuItem.hasSubMenu()) {
                    /*for (int j = 0; j < menuItem.getSubMenu().size(); j++) {
                        MenuItem menuSubItem = menuItem.getSubMenu().getItem(j);
                        menuSubItem.setVisible(SQLite.usuario.VerificaPermiso(this,menuItem.getTitleCondensed().toString(),"lectura"));
                        break;
                    }*/
                } else {
                    menuItem.setVisible(SQLite.usuario.VerificaPermiso(this, menuItem.getTitleCondensed().toString().toUpperCase(),"lectura"));
                }
            }
            navigation.getMenu().findItem(R.id.nav_home).setVisible(true);

        }catch (Exception e){
            Log.d("TAGPERMISO", "VerificarPermisos(): " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
    }

    private static long presionado;
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(fragmentManager.getBackStackEntryCount()> 0) {
                try {
                    listaFragments.remove(listaFragments.size() - 1);
                    Log.d("TAGMAIN", "onBackPressed: " + fragment.getClass().getSimpleName());
                    List<Fragment> fragments = fragmentManager.getFragments();
                    if (fragments != null) {
                        for (Fragment f : fragments)
                            Log.d("TAGMAIN", "F: " + f.getClass().getSimpleName());
                    }
                    for (String n : listaFragments)
                        Log.d("TAGMAIN", "N: " + n);
                } catch (Exception e) {
                    Log.d("TAGMAIN", e.getMessage());
                }
                super.onBackPressed();
            }else{
                if (presionado + 2000 > System.currentTimeMillis())
                    super.onBackPressed();
                else
                    Utils.showMessage(this, "Vuelve a presionar para salir");
                presionado = System.currentTimeMillis();
            }
        }
    }

    private void IniciarServicio(){
        try {
            ComponentName componentName = new ComponentName(getApplicationContext(), JobServiceGPS.class);
            JobInfo info;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                info = new JobInfo.Builder(ID_SERVICE_LOCATION, componentName)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPersisted(true)
                        .setMinimumLatency(5*1000)
                        .build();
            }else{
                info = new JobInfo.Builder(ID_SERVICE_LOCATION, componentName)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPersisted(true)
                        .setPeriodic(5*1000)
                        .build();
            }
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            int result = scheduler.schedule(info);
            if(result == JobScheduler.RESULT_SUCCESS)
                Log.d("TAG", "Completado correctamente");
            else
                Log.d("TAG", "Ha ocurrido un error en el job!!!");
        }catch (Exception e){
            Log.d("TAG", e.getMessage());
        }
    }

    private void DetenerServicio(){
        try {
            JobScheduler schedule = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            schedule.cancel(ID_SERVICE_LOCATION);
            Log.d("TAG", "Job Cancelado por el usuario!");
        }catch (Exception e){
            Log.d("TAG",e.getMessage());
        }
    }

    private boolean VerificaServicio(){
        boolean result = false;
        try {
            JobScheduler schedule = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            JobInfo jInfo = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                jInfo = schedule.getPendingJob(ID_SERVICE_LOCATION);
            }
            if(jInfo!=null){
                result = true;
                Log.d("TAG","EL SERVICIO ESTA INICIADO");
            }else
                Log.d("TAG","EL SERVICIO NO ESTÀ INICIADO");
        }catch (Exception e){
            Log.d("TAG",e.getMessage());
        }
        return result;
    }

    private void loadUbicacion(){
        try{
            Map<String,Object> datos = new HashMap<>();
            List<Ubicacion> ubicaciones = Ubicacion.getListSC(SQLite.usuario.IdUsuario);
            if(ubicaciones == null) {
                Log.d("TAGMAIN", "La lista es nula");
                return;
            }
            if(ubicaciones.size()==0) {
                Log.d("TAGMAIN", "La lista está vacía");
                return;
            }
            datos.put("ubicaciones",ubicaciones);

            IUsuario miInterface = retrofit.create(IUsuario.class);

            Call<JsonObject> call = null;
            call = miInterface.loadUbicacion(datos);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful()) {
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonUpdate = obj.getAsJsonArray("ubicacionesupdate");
                                if (jsonUpdate != null) {
                                    int numUpdate = 0;
                                    ContentValues values;
                                    for (JsonElement ele : jsonUpdate) {
                                        JsonObject upd = ele.getAsJsonObject();

                                        values = new ContentValues();
                                        values.put("estado", upd.get("codigosistema").getAsInt());
                                        if (Ubicacion.Update(upd.get("idubicacion").getAsInt(), values))
                                            numUpdate++;
                                    }
                                    Log.d("TAGMAIN", "Se subieron " + numUpdate + "/" + jsonUpdate.size() + " ubicaciones");
                                }else
                                    Log.d("TAGMAIN", "Error: El webservice no devolvió valores");
                            }else
                                Log.d("TAGMAIN", "Error: " + obj.get("message").getAsString());
                        }
                    } catch (Exception e) {
                        Log.d("TAGMAIN1", e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.d("TAGMAIN2", t.getMessage());
                }
            });
        }catch (Exception e){
            Log.d("TAGMAIN3", e.getMessage());
        }
    }

    private void verificarVersion(final Context context) {
        try {
            IUsuario miInterface = retrofit.create(IUsuario.class);

            Call<JsonObject> call = null;
            call = miInterface.getLastVersion();
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful()) {
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (obj.has("versionapp")) {
                                if (!obj.get("versionapp").getAsString().equals(BuildConfig.VERSION_NAME)) {
                                    SQLite.newversion = obj.get("versionapp").getAsString();
                                    SQLite.linkdescarga = obj.get("linkapp").getAsString();
                                    ShowModalUpdate(SQLite.linkdescarga);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.d("TAG", e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.d("TAG", t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.d("TAG", e.getMessage());
        }
    }

    public void ShowModalUpdate(String url){
        try{
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            builder.setCancelable(false);
            ((TextView)view.findViewById(R.id.lblTitle)).setText("Actualización");
            ((TextView)view.findViewById(R.id.lblMessage)).setText("Necesita actualizar la aplicación para continuar");
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_check_white);
            ((Button)view.findViewById(R.id.btnCancel)).setText("Cerrar");
            ((Button)view.findViewById(R.id.btnConfirm)).setText("Descargar");
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(v -> Utils.DescargaApk(MainActivity.this, url));

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> {alertDialog.dismiss(); finish();});

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
        }catch (Exception e){
            Log.d("TAG", e.getMessage());
        }
    }
}
