package com.florencia.erpapp.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Toast;

import com.florencia.erpapp.MainActivity;
import com.florencia.erpapp.R;
import com.florencia.erpapp.interfaces.IUsuario;
import com.florencia.erpapp.models.Canton;
import com.florencia.erpapp.models.Catalogo;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.Configuracion;
import com.florencia.erpapp.models.Ingreso;
import com.florencia.erpapp.models.Parroquia;
import com.florencia.erpapp.models.PedidoInventario;
import com.florencia.erpapp.models.Permiso;
import com.florencia.erpapp.models.Provincia;
import com.florencia.erpapp.models.Sucursal;
import com.florencia.erpapp.models.Usuario;
import com.florencia.erpapp.models.VersionApp;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.provider.Settings.Secure;

public class LoginActivity extends AppCompatActivity {

    private static String TAG = "TAGLOGIN_ACT";
    EditText etUser, etPassword;
    Button btnLogin;
    ImageButton btnConfig;
    private SharedPreferences sPreferencesSesion;
    private OkHttpClient okHttpClient;
    private ProgressDialog pbProgreso;
    View rootView;
    Retrofit retrofit;
    Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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

        gson = new GsonBuilder()
                .setLenient()
                .create();


        SQLite.version = VersionApp.getLast();
        SQLite.configuracion = Configuracion.GetLast();
        if (SQLite.configuracion != null) {
            SQLite.configuracion.url_ws = (SQLite.configuracion.hasSSL ? Constants.HTTPs : Constants.HTTP)
                    + SQLite.configuracion.urlbase
                    + (SQLite.configuracion.hasSSL ? "" : "/erpproduccion")
                    + Constants.ENDPOINT;
        }

        sPreferencesSesion = getSharedPreferences("DatosSesion", MODE_PRIVATE);
        if (sPreferencesSesion != null) {
            int id = sPreferencesSesion.getInt("idUser", 0);
            if (id > 0)
                this.LoginPreferences(id);
        }

        etPassword.setOnKeyListener(
                (v, keyCode, event) -> {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        LoginRemot(v.getContext(), etUser.getText().toString().trim(), etPassword.getText().toString());
                        return true;
                    }
                    return false;
                }
        );

        etPassword.setOnTouchListener(
                (v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        btnLogin.requestFocus();
                        ScrollView sv = findViewById(R.id.svLogin);
                        sv.scrollTo(0, sv.getBottom());
                    }
                    return false;
                }
        );
    }

    private void ConsultaConfig() {
        try {
            AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this);
            dialog.setIcon(getResources().getDrawable(R.drawable.ic_settings));
            dialog.setTitle("Configuración");
            dialog.setMessage("Debe especificar la configuración para continuar");
            dialog.setPositiveButton("Configurar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(LoginActivity.this, ConfigActivity.class);
                    startActivity(i);
                    overridePendingTransition(R.anim.left_in, R.anim.left_out);
                    dialog.dismiss();
                }
            });
            dialog.show();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    private View.OnClickListener onClick =
            v -> {
                switch (v.getId()) {
                    case R.id.btnLogin:
                        LoginRemot(v.getContext(), etUser.getText().toString().trim(), etPassword.getText().toString());
                        break;
                    case R.id.btnConfig:
                        Intent i = new Intent(LoginActivity.this, ConfigActivity.class);
                        startActivity(i);
                        overridePendingTransition(R.anim.left_in, R.anim.left_out);
                        break;
                }
            };

    //INGRESA SIN SOLICITAR USUARIO Y CONTRASEÑA, UTILIZANDO EL ID DEL USUARIO DESDE SHARED PREFERENCES
    private void LoginPreferences(Integer id) {
        try {
            Usuario user = Usuario.getUsuario(id);
            if (user != null) {
                SQLite.usuario = user;
                SQLite.usuario.establecimiento_fact = sPreferencesSesion.getInt("establecimiento_fac", SQLite.usuario.sucursal.IdEstablecimiento);
                SQLite.usuario.GuardarSesionLocal(this);
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.left_in, R.anim.left_out);
                finish();
            }
        } catch (Exception e) {
            Utils.showErrorDialog(LoginActivity.this, "Error", e.getMessage());
        }
    }

    private void ActualizarPermisos() {
        try {
            IUsuario iUsuario = retrofit.create(IUsuario.class);
            Call<JsonObject> call = iUsuario.getPermisos(SQLite.usuario.Usuario, SQLite.usuario.Clave);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject obj = response.body();
                        if (!obj.get("haserror").getAsBoolean()) {
                            JsonArray jsonPermisos = obj.get("permisos").getAsJsonArray();
                            if (jsonPermisos != null) {
                                SQLite.usuario.permisos.clear();
                                for (JsonElement element : jsonPermisos) {
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
                                    SQLite.usuario.permisos.add(mipermiso);
                                }
                                Permiso.SaveLista(SQLite.usuario.permisos);

                                if (obj.has("newperfil")) {
                                    ContentValues values = new ContentValues();
                                    values.put("perfilid", obj.get("newperfil").getAsInt());
                                    Usuario.Update(SQLite.usuario.IdUsuario, values);
                                }
                                SQLite.usuario.GuardarSesionLocal(LoginActivity.this);
                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(i);
                                overridePendingTransition(R.anim.left_in, R.anim.left_out);
                                finish();
                            }
                        } else {
                            Banner.make(rootView, LoginActivity.this, Banner.ERROR,
                                    obj.get("message").getAsString(), Banner.BOTTOM, 3000).show();
                        }
                    } else {
                        SQLite.usuario.GuardarSesionLocal(LoginActivity.this);
                        Intent i = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(i);
                        overridePendingTransition(R.anim.left_in, R.anim.left_out);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    call.cancel();
                    SQLite.usuario.GuardarSesionLocal(LoginActivity.this);
                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(i);
                    overridePendingTransition(R.anim.left_in, R.anim.left_out);
                    finish();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    //INGRESA SOLICITANDO USUARIO Y CONTRASEÑA, VERIFICANDO EN LA BASE LOCAL DEL DISPOSITIVO
    private void LoginLocal(String User, String Clave) {
        try {
            Usuario miUser = Usuario.Login(User, Clave);
            if (miUser == null) {
                Banner.make(rootView, LoginActivity.this, Banner.ERROR, "Usuario o contraseña incorrecta.", Banner.BOTTOM, 2000).show();
                return;
            } else {
                SQLite.usuario = miUser;
                SQLite.usuario.GuardarSesionLocal(LoginActivity.this);
                Utils.showMessage(LoginActivity.this, "Bienvenido...");
                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.left_in, R.anim.left_out);
                finish();
            }
        } catch (Exception e) {
            Banner.make(rootView, LoginActivity.this, Banner.ERROR, "Ocurrió un error al tratar de iniciar sesión", Banner.BOTTOM, 2000).show();
            Log.d(TAG, "IniciarSesionLocal(): " + e.getMessage());
        }
    }

    //INGRESA SOLICITANDO USUARIO Y CONTRASEÑA, VERIFICANDO DESDE EL WEBSERVICE
    private void LoginRemot(final Context context, String User, String Clave) {
        try {
            if (User.equals("")) {
                Banner.make(rootView, LoginActivity.this, Banner.ERROR, "Debe ingresar el usuario...", Banner.TOP, 2000).show();
                return;
            }
            if (Clave.equals("")) {
                Banner.make(rootView, LoginActivity.this, Banner.ERROR, "Debe ingresar la contraseña...", Banner.TOP, 2000).show();
                return;
            }

            pbProgreso.setTitle("Iniciando sesión");
            pbProgreso.setMessage("Espere un momento...");
            pbProgreso.setCancelable(false);
            pbProgreso.show();

            IUsuario miInterface = retrofit.create(IUsuario.class);

            String device = Build.MANUFACTURER.concat(" ").concat(Build.MODEL).concat(" - Android: ").concat(Build.VERSION.RELEASE);

            Call<JsonObject> call = miInterface.IniciarSesion(User, Clave, device);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful()) {
                        Toast.makeText(context, "Code:" + response.code(), Toast.LENGTH_SHORT).show();
                        pbProgreso.dismiss();
                        LoginLocal(User, Clave);
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
                                usuario.Clave = Clave;
                                usuario.Perfil = jsonUsuario.get("perfil").getAsInt();
                                usuario.Autorizacion = jsonUsuario.get("auth").getAsInt();

                                Sucursal.DeleteByUser(usuario.IdUsuario);
                                usuario.sucursal = Sucursal.AsignaDatos(jsonUsuario.getAsJsonObject("sucursal"));
                                usuario.ParroquiaID = jsonUsuario.get("parroquiaid").getAsInt();
                                usuario.nombrePerfil = jsonUsuario.has("nombreperfil") ? jsonUsuario.get("nombreperfil").getAsString() : "";
                                usuario.nip = jsonUsuario.has("nip") ? jsonUsuario.get("nip").getAsString() : "";
                                usuario.establecimiento_fact = usuario.sucursal.IdEstablecimiento;

                                usuario.establecimientos.clear();
                                usuario.establecimientos.add(usuario.sucursal);
                                if (jsonUsuario.has("establecimientos")) {
                                    JsonArray jsonEstablecimientos = jsonUsuario.get("establecimientos").getAsJsonArray();
                                    if (jsonEstablecimientos != null) {
                                        for (JsonElement est : jsonEstablecimientos) {
                                            usuario.establecimientos.add((Sucursal.AsignaDatos(est.getAsJsonObject())));
                                        }
                                    }
                                }

                                JsonArray jsonPermisos = jsonUsuario.get("permisos").getAsJsonArray();
                                //usuario.permisos = new Gson().fromJson(jsonPermisos, usuario.permisos.getClass());
                                if (jsonPermisos != null) {
                                    for (JsonElement element : jsonPermisos) {
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

                                if (usuario.permisos == null || usuario.permisos.size() == 0) {
                                    Banner.make(rootView, LoginActivity.this, Banner.ERROR, "Su perfil no tiene permisos asignados. Contacte a soporte.", Banner.BOTTOM, 2000).show();
                                    return;
                                }

                                if (usuario.Guardar()) {
                                    Catalogo.Delete("ENTIDADFINANCIE");
                                    JsonArray jsonCatalogo = obj.get("catalogos").getAsJsonArray();
                                    List<Catalogo> listCatalogo = new ArrayList<>();
                                    for (JsonElement ele : jsonCatalogo) {
                                        JsonObject cata = ele.getAsJsonObject();
                                        Catalogo miCatalogo = new Catalogo();
                                        miCatalogo.idcatalogo = cata.get("idcatalogo").getAsInt();
                                        miCatalogo.nombrecatalogo = cata.get("nombrecatalogo").getAsString();
                                        miCatalogo.codigocatalogo = cata.get("codigocatalogo").getAsString();
                                        miCatalogo.codigopadre = cata.get("codigopadre").getAsString();
                                        miCatalogo.cuentaid = cata.get("cuentaid").isJsonNull() ? 0 : cata.get("cuentaid").getAsInt();
                                        miCatalogo.entidadfinancieracodigo = cata.has("entidadfinancieracodigo") ? cata.get("entidadfinancieracodigo").getAsString() : "";
                                        listCatalogo.add(miCatalogo);
                                    }
                                    Catalogo.SaveLista(listCatalogo);
                                    //ACTUALIZAR EL SECUENCIAL DE FACTURAS
                                    Comprobante comprobante;
                                    if (jsonUsuario.has("secuencial_fa") && Usuario.numDocNoSincronizados(usuario.IdUsuario, "01", "", usuario.sucursal.IdEstablecimiento) == 0) {
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
                                    if (jsonUsuario.has("secuencial_pe") && Usuario.numDocNoSincronizados(usuario.IdUsuario, "PC", "", 0) == 0) {
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
                                    if (jsonUsuario.has("secuencial_pi") && Usuario.numDocNoSincronizados(usuario.IdUsuario, "PI", "", 0) == 0) {
                                        Integer secuencial_pi = jsonUsuario.get("secuencial_pi").getAsInt();
                                        PedidoInventario pedidoinv = new PedidoInventario();
                                        pedidoinv.secuencial = secuencial_pi - 1;
                                        pedidoinv.establecimientoid = usuario.sucursal.IdEstablecimiento;
                                        pedidoinv.tipotransaccion = "PI";
                                        pedidoinv.actualizasecuencial();
                                    }

                                    //ACTUALIZAR EL SECUENCIAL DE ORDEN DE PAGO(DEPOSITOS - DIARIO DE VENTA)
                                    if (jsonUsuario.has("secuencial_op") && Usuario.numDocNoSincronizados(usuario.IdUsuario, "DE", "", usuario.sucursal.IdEstablecimiento) == 0) {
                                        Integer secuencial_op = jsonUsuario.get("secuencial_op").getAsInt();
                                        Ingreso ingreso = new Ingreso();
                                        ingreso.secuencial = secuencial_op;
                                        ingreso.establecimientoid = usuario.sucursal.IdEstablecimiento;
                                        ingreso.actualizasecuencial();
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
                                        miCanton.idcanton = prov.get("idcanton").getAsInt();
                                        miCanton.nombrecanton = prov.get("nombrecanton").getAsString();
                                        miCanton.provinciaid = prov.get("provinciaid").getAsInt();
                                        cantones.add(miCanton);
                                    }
                                    Canton.SaveLista(cantones);

                                    JsonArray jsonParroquias = obj.get("parroquias").getAsJsonArray();
                                    List<Parroquia> parroquias = new ArrayList<>();
                                    for (JsonElement ele : jsonParroquias) {
                                        JsonObject prov = ele.getAsJsonObject();
                                        Parroquia miParroquia = new Parroquia();
                                        miParroquia.idparroquia = prov.get("idparroquia").getAsInt();
                                        miParroquia.nombreparroquia = prov.get("nombreparroquia").getAsString();
                                        miParroquia.cantonid = prov.get("cantonid").getAsInt();
                                        parroquias.add(miParroquia);
                                    }
                                    Parroquia.SaveLista(parroquias);

                                    SQLite.usuario = usuario;
                                    SQLite.usuario.GuardarSesionLocal(context);
                                    pbProgreso.dismiss();
                                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(i);
                                    overridePendingTransition(R.anim.left_in, R.anim.left_out);
                                    finish();
                                } else
                                    Banner.make(rootView, LoginActivity.this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 2000).show();
                            } else
                                Utils.showErrorDialog(LoginActivity.this, "Error", obj.get("message").getAsString());
                        } else
                            Banner.make(rootView, LoginActivity.this, Banner.ERROR, Constants.MSG_USUARIO_CLAVE_INCORRECTO, Banner.BOTTOM, 2000).show();

                    } catch (JsonParseException ex) {
                        Log.d(TAG, ex.getMessage());
                        LoginLocal(User, Clave);
                    }
                    pbProgreso.dismiss();
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    //Utils.showErrorDialog(LoginActivity.this, "Error",t.getMessage());
                    Log.d(TAG, t.getMessage());
                    pbProgreso.dismiss();
                    LoginLocal(User, Clave);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
            Utils.showErrorDialog(this, "Error: ", e.getMessage());
            pbProgreso.dismiss();
        }
    }

    @Override
    protected void onResume() {

        SQLite.configuracion = Configuracion.GetLast();
        if (SQLite.configuracion == null || SQLite.configuracion.urlbase.equals("")) {
            btnLogin.setEnabled(false);
            Intent i = new Intent(LoginActivity.this, ConfigActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.left_in, R.anim.left_out);
        } else {
            btnLogin.setEnabled(true);
            SQLite.configuracion.url_ws = (SQLite.configuracion.hasSSL ? Constants.HTTPs : Constants.HTTP)
                    + SQLite.configuracion.urlbase
                    + (SQLite.configuracion.hasSSL ? "" : "/erpproduccion")
                    + Constants.ENDPOINT;

            //VOLVEMOS A INSTANCIAR EL OBJETO RETROFIT CON LA NUEVA URL
            retrofit = new Retrofit.Builder()
                    .baseUrl(SQLite.configuracion.url_ws)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();
        }
        super.onResume();
    }

}
