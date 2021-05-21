package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.florencia.erpapp.R;
import com.florencia.erpapp.interfaces.IUsuario;
import com.florencia.erpapp.models.Configuracion;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shasin.notificationbanner.Banner;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ConfigActivity extends AppCompatActivity {

    private static final String TAG = "TAG_CONFIGACTIVITY";
    View rootView;
    Toolbar toolbar;
    EditText txtURLBase;
    CheckBox ckSSL;
    private OkHttpClient okHttpClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        rootView = findViewById(android.R.id.content);

        init();
    }

    private void init() {
        okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        txtURLBase = findViewById(R.id.txtUrlBase);
        ckSSL = findViewById(R.id.ckSSL);

        if(SQLite.configuracion!=null){
            txtURLBase.setText(SQLite.configuracion.urlbase);
            ckSSL.setChecked(SQLite.configuracion.hasSSL);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        menu.findItem(R.id.option_newdocument).setVisible(false);
        menu.findItem(R.id.option_listdocument).setVisible(false);
        menu.findItem(R.id.option_reimprimir).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_save:
                ValidarDatos();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ValidarDatos() {
        try{
            if(txtURLBase.getText().toString().trim().equals("")){
                Banner.make(rootView,this,Banner.ERROR, "Debe especificar la URL válida.",Banner.BOTTOM, 3000).show();
                txtURLBase.requestFocus();
                return;
            }else {//if(!Utils.isOnlineNet(txtURLBase.getText().toString().trim())){
                String url_temp = (ckSSL.isChecked()?Constants.HTTPs:Constants.HTTP)
                        + txtURLBase.getText().toString().trim()
                        + (ckSSL.isChecked()?"":"/erpproduccion")
                        + Constants.ENDPOINT;
                Gson gson = new GsonBuilder()
                        .setLenient()
                        .create();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(url_temp)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .client(okHttpClient)
                        .build();
                IUsuario miInterface = retrofit.create(IUsuario.class);

                Call<String> call = miInterface.verificaconexion();
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (!response.isSuccessful()) {
                            Banner.make(rootView,ConfigActivity.this,Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.",Banner.BOTTOM, 3000).show();
                            return;
                        }
                        try {
                            if (response.body() != null) {
                                String resp = response.body();
                                if(resp.equalsIgnoreCase("OK"))
                                    GuardarDatos();
                                else
                                    Banner.make(rootView,ConfigActivity.this,Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.",Banner.BOTTOM, 3000).show();
                            }else
                                Banner.make(rootView,ConfigActivity.this,Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.",Banner.BOTTOM, 3000).show();
                        } catch (Exception e) {
                            Banner.make(rootView,ConfigActivity.this,Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.",Banner.BOTTOM, 3000).show();
                            Log.d(TAG, e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.d(TAG, t.getMessage());
                        Banner.make(rootView,ConfigActivity.this,Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.",Banner.BOTTOM, 3000).show();
                        call.cancel();
                    }
                });
            }

        }catch (Exception e){
            Banner.make(rootView,this,Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.",Banner.BOTTOM, 3000).show();
            Log.d(TAG, "ValidarDatos(): " + e.getMessage());
        }
    }

    private void GuardarDatos(){
        try{
            Configuracion newConfig = new Configuracion();
            newConfig.urlbase = txtURLBase.getText().toString().trim();
            newConfig.hasSSL = ckSSL.isChecked();
            newConfig.url_ws = (newConfig.hasSSL?Constants.HTTPs:Constants.HTTP)
                    + newConfig.urlbase
                    + (SQLite.configuracion.hasSSL?"":"/erpproduccion")
                    + Constants.ENDPOINT;
            if(newConfig.Save()){
                SQLite.configuracion = newConfig;
                //Utils.showSuccessDialog(this, "Configuración",Constants.MSG_DATOS_GUARDADOS,true,false);
                //Banner.make(rootView, ConfigActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS, Banner.BOTTOM,2000).show();
                Utils.showMessage(ConfigActivity.this, Constants.MSG_DATOS_GUARDADOS);
                onBackPressed();
                overridePendingTransition(R.anim.right_in, R.anim.right_out);
            }else{
                //Utils.showErrorDialog(this, "Error",Constants.MSG_DATOS_NO_GUARDADOS);
                Banner.make(rootView, this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM,3000).show();
            }
        }catch (Exception e){
            Log.d(TAG, "GuardarDatos(): " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        toolbar.setTitle("Configuración");
        toolbar.setTitleTextColor(Color.WHITE);
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.right_in, R.anim.right_out);
    }
}
