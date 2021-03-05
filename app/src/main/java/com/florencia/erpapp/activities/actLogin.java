package com.florencia.erpapp.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.florencia.erpapp.MainActivity;
import com.florencia.erpapp.R;
import com.florencia.erpapp.interfaces.UsuarioInterface;
import com.florencia.erpapp.models.Canton;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.Configuracion;
import com.florencia.erpapp.models.Parroquia;
import com.florencia.erpapp.models.PedidoInventario;
import com.florencia.erpapp.models.Permiso;
import com.florencia.erpapp.models.Provincia;
import com.florencia.erpapp.models.Sucursal;
import com.florencia.erpapp.models.Usuario;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utilidades;
import com.florencia.erpapp.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.shasin.notificationbanner.Banner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class actLogin extends AppCompatActivity {

    EditText etUser, etPassword;
    Button btnLogin;
    ImageButton btnConfig;
    private SharedPreferences sPreferencesSesion;
    private OkHttpClient okHttpClient;
    private ProgressDialog pbProgreso;
    View rootView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act_login);

        SQLite.sqlDB = new SQLite(getApplicationContext());
        Utilidades.createdb(this);
        pbProgreso = new ProgressDialog(this);

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnConfig = findViewById(R.id.btnConfig);
        btnLogin.setOnClickListener(onClick);
        btnConfig.setOnClickListener(onClick);

        rootView = findViewById(android.R.id.content);

        Utils.verificarPermisos(this);

        okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        SQLite.configuracion = Configuracion.GetLast();
        if(SQLite.configuracion!=null) {
            SQLite.configuracion.url_ws = (SQLite.configuracion.hasSSL ? Constants.HTTPs : Constants.HTTP)
                    + SQLite.configuracion.urlbase
                    + (SQLite.configuracion.hasSSL ? "" : "/erpproduccion")
                    + Constants.ENDPOINT;
        }
        sPreferencesSesion = getSharedPreferences("DatosSesion", MODE_PRIVATE);
        if(sPreferencesSesion != null){
            int id = sPreferencesSesion.getInt("idUser",0);
            if(id > 0)
                this.LoginLocal(id);
        }

        etPassword.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    IniciarSesion(v.getContext());
                    return true;
                }
                return false;
            }
        });
    }

    private void ConsultaConfig() {
        try{
            AlertDialog.Builder dialog = new AlertDialog.Builder(actLogin.this);
            dialog.setIcon(getResources().getDrawable(R.drawable.ic_settings));
            dialog.setTitle("Configuración");
            dialog.setMessage("Debe especificar la configuración para continuar");
            dialog.setPositiveButton("Configurar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(actLogin.this, ConfigActivity.class);
                    startActivity(i);
                    dialog.dismiss();
                }
            });
            dialog.show();
        }catch (Exception e) {
            Log.d("TAGLOGIN", e.getMessage());
        }
    }

    private View.OnClickListener onClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnLogin:
                    IniciarSesion(v.getContext());
                    break;
                case R.id.btnConfig:
                    Intent i = new Intent(actLogin.this,ConfigActivity.class);
                    startActivity(i);
                    break;
            }
        }
    };

    private void LoginLocal(Integer id) {
        try {
            Usuario user = Usuario.getUsuario(id);
            if(user != null) {
                SQLite.usuario = user;
                SQLite.usuario.GuardarSesionLocal(this);
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                finish();
            }
        }catch (Exception e){
            Utils.showErrorDialog(actLogin.this, "Error", e.getMessage());
        }
    }

    private void IniciarSesion(final Context context){
        try{
            Usuario miUser = new Usuario();

            String User = etUser.getText().toString().trim();
            String Clave = etPassword.getText().toString();
            if (User.equals("")) {
                etUser.setError("Ingrese el usuario");
                return;
            }
            if(Clave.equals("")){
                etPassword.setError("Ingrese la contraseña");
                return;
            }

            if(!Utils.isOnlineNet(SQLite.configuracion.urlbase)) {
                miUser = Usuario.Login(User, Clave);
                if(miUser == null){
                    Banner.make(rootView, actLogin.this, Banner.ERROR, "Usuario o contraseña incorrecta.", Banner.BOTTOM, 2000).show();
                    return;
                }else{
                    SQLite.usuario = miUser;
                    SQLite.usuario.GuardarSesionLocal(context);
                    Utils.showMessage(context, "Bienvenido...");
                    Intent i = new Intent(actLogin.this, MainActivity.class);
                    startActivity(i);
                    finish();
                }
                return;
            }

            pbProgreso.setTitle("Iniciando sesión");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            Log.d("TAGLOGIN", SQLite.configuracion.url_ws);
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(SQLite.configuracion.url_ws)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();
            UsuarioInterface miInterface = retrofit.create(UsuarioInterface.class);

            Call<JsonObject> call=null;
            call=miInterface.IniciarSesion(User,Clave,"");
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if(!response.isSuccessful()){
                        Toast.makeText(context,"Code:" + response.code(),Toast.LENGTH_SHORT).show();
                        pbProgreso.dismiss();
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            if (!obj.get("haserror").getAsBoolean()) {
                                Usuario usuario = new Usuario();
                                JsonObject jsonUsuario = obj.getAsJsonObject("usuario");
                                usuario.IdUsuario = jsonUsuario.get("idpersona").getAsInt();
                                usuario.RazonSocial = jsonUsuario.get("razonsocial").getAsString();
                                usuario.Usuario = jsonUsuario.get("usuario").getAsString();
                                usuario.Clave = etPassword.getText().toString();
                                usuario.Perfil = jsonUsuario.get("perfil").getAsInt();
                                usuario.Autorizacion = jsonUsuario.get("auth").getAsInt();
                                usuario.sucursal = Sucursal.AsignaDatos(jsonUsuario.getAsJsonObject("sucursal"));
                                usuario.ParroquiaID = jsonUsuario.get("parroquiaid").getAsInt();
                                usuario.nombrePerfil = jsonUsuario.has("nombreperfil")?jsonUsuario.get("nombreperfil").getAsString():"";

                                JsonArray jsonPermisos = jsonUsuario.get("permisos").getAsJsonArray();
                                //usuario.permisos = new Gson().fromJson(jsonPermisos, usuario.permisos.getClass());
                                if(jsonPermisos!=null){
                                    for(JsonElement element:jsonPermisos){
                                        JsonObject per = element.getAsJsonObject();
                                        Permiso mipermiso = new Permiso();
                                        mipermiso.nombreopcion = per.get("nombreopcion").getAsString();
                                        mipermiso.opcionid = per.get("opcionid").getAsInt();
                                        mipermiso.perfilid = per.get("perfilid").getAsInt();
                                        mipermiso.permisoescritura = per.get("permisoescritura").getAsString();
                                        mipermiso.permisoimpresion = per.get("permisoimpresion").getAsString();
                                        mipermiso.permisomodificacion = per.get("permisomodificacion").getAsString();
                                        mipermiso.permisoborrar = per.get("permisoborrar").getAsString();
                                        mipermiso.rutaopcion = per.get("rutaopcion").getAsString();
                                        usuario.permisos.add(mipermiso);
                                    }
                                }

                                if(usuario.permisos == null || usuario.permisos.size()==0){
                                    Banner.make(rootView, actLogin.this, Banner.ERROR, "Su perfil no tiene permisos asignados. Contacte a soporte.", Banner.BOTTOM, 2000).show();
                                    return;
                                }

                                if(usuario.Guardar()) {
                                    //ACTUALIZAR EL SECUENCIAL DE FACTURAS
                                    Comprobante comprobante;
                                    if(jsonUsuario.has("secuencial_fa") && Usuario.numDocNoSincronizados(usuario.IdUsuario, "01") == 0) {
                                        Integer secuencial_fa = jsonUsuario.get("secuencial_fa").getAsInt();
                                        comprobante = new Comprobante();
                                        comprobante.secuencial = secuencial_fa - 1;
                                        comprobante.establecimientoid = usuario.sucursal.IdEstablecimiento;
                                        comprobante.codigoestablecimiento = usuario.sucursal.CodigoEstablecimiento;
                                        comprobante.puntoemision = usuario.sucursal.PuntoEmision;
                                        comprobante.tipotransaccion = "01";
                                        comprobante.actualizasecuencial();
                                    }

                                    //ACTUALIZAR EL SECUENCIAL DE PEDIDOS CLIENTE
                                    if(jsonUsuario.has("secuencial_pe") && Usuario.numDocNoSincronizados(usuario.IdUsuario, "PC") == 0) {
                                        Integer secuencial_pe = jsonUsuario.get("secuencial_pe").getAsInt();
                                        comprobante = new Comprobante();
                                        comprobante.secuencial = secuencial_pe - 1;
                                        comprobante.establecimientoid = usuario.sucursal.IdEstablecimiento;
                                        comprobante.codigoestablecimiento = usuario.sucursal.CodigoEstablecimiento;
                                        comprobante.puntoemision = usuario.sucursal.PuntoEmision;
                                        comprobante.tipotransaccion = "PC";
                                        comprobante.actualizasecuencial();
                                    }

                                    //ACTUALIZAR EL SECUENCIAL DE PEDIDOS INVENTARIO
                                    if(jsonUsuario.has("secuencial_pi") && Usuario.numDocNoSincronizados(usuario.IdUsuario, "PI") == 0) {
                                        Integer secuencial_pi = jsonUsuario.get("secuencial_pi").getAsInt();
                                        PedidoInventario pedidoinv = new PedidoInventario();
                                        pedidoinv.secuencial = secuencial_pi - 1;
                                        pedidoinv.establecimientoid = usuario.sucursal.IdEstablecimiento;
                                        pedidoinv.tipotransaccion = "PI";
                                        pedidoinv.actualizasecuencial();
                                    }

                                    List<Provincia> listProvincia = new ArrayList<>();
                                    JsonArray jsonProvincias = obj.get("provincias").getAsJsonArray();
                                    for (JsonElement ele : jsonProvincias) {
                                        JsonObject prov = ele.getAsJsonObject();
                                        Provincia miProvincia = new Provincia();
                                        miProvincia.idprovincia = prov.get("idprovincia").getAsInt();
                                        miProvincia.nombreprovincia = prov.get("nombreprovincia").getAsString();
                                        listProvincia.add(miProvincia);
                                    }
                                    Provincia.SaveLista(listProvincia);

                                    JsonArray jsonCantones = obj.get("cantones").getAsJsonArray();
                                    List<Canton> cantones = new ArrayList<>();
                                    for (JsonElement ele : jsonCantones) {
                                        JsonObject prov = ele.getAsJsonObject();
                                        Canton miCanton = new Canton();
                                        miCanton.idcanton= prov.get("idcanton").getAsInt();
                                        miCanton.nombrecanton= prov.get("nombrecanton").getAsString();
                                        miCanton.provinciaid = prov.get("provinciaid").getAsInt();
                                        cantones.add(miCanton);
                                    }
                                    Canton.SaveLista(cantones);

                                    JsonArray jsonParroquias = obj.get("parroquias").getAsJsonArray();
                                    List<Parroquia> parroquias = new ArrayList<>();
                                    for (JsonElement ele : jsonParroquias) {
                                        JsonObject prov = ele.getAsJsonObject();
                                        Parroquia miParroquia = new Parroquia();
                                        miParroquia.idparroquia= prov.get("idparroquia").getAsInt();
                                        miParroquia.nombreparroquia= prov.get("nombreparroquia").getAsString();
                                        miParroquia.cantonid= prov.get("cantonid").getAsInt();
                                        parroquias.add(miParroquia);
                                    }
                                    Parroquia.SaveLista(parroquias);

                                    SQLite.usuario = usuario;
                                    SQLite.usuario.GuardarSesionLocal(context);
                                    pbProgreso.dismiss();
                                    Intent i = new Intent(actLogin.this, MainActivity.class);
                                    startActivity(i);
                                    finish();
                                }else
                                    Banner.make(rootView, actLogin.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 2000).show();
                            } else
                                Utils.showErrorDialog(actLogin.this,"Error", obj.get("message").getAsString());
                        } else
                            Banner.make(rootView, actLogin.this, Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM, 2000).show();

                    }catch (JsonParseException ex){
                        Log.d("TAG", ex.getMessage());
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Utils.showErrorDialog(actLogin.this, "Error",t.getMessage());
                    Log.d("TAG", t.getMessage());
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

    @Override
    protected void onResume(){

        SQLite.configuracion = Configuracion.GetLast();
        if(SQLite.configuracion==null || SQLite.configuracion.urlbase.equals(""))
        {
            btnLogin.setEnabled(false);
            Intent i = new Intent(actLogin.this, ConfigActivity.class);
            startActivity(i);
        }else{
            btnLogin.setEnabled(true);
            SQLite.configuracion.url_ws = (SQLite.configuracion.hasSSL?Constants.HTTPs:Constants.HTTP)
                    + SQLite.configuracion.urlbase
                    + (SQLite.configuracion.hasSSL?"":"/erpproduccion")
                    + Constants.ENDPOINT;
        }
        super.onResume();
    }

}
