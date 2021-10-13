package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.florencia.erpapp.MainActivity;
import com.florencia.erpapp.R;
import com.florencia.erpapp.interfaces.IUsuario;
import com.florencia.erpapp.models.Configuracion;
import com.florencia.erpapp.models.Empresa;
import com.florencia.erpapp.models.Usuario;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utilidades;
import com.florencia.erpapp.utils.Utils;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shasin.notificationbanner.Banner;

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

public class ConfigActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TAG_CONFIGACTIVITY";
    View rootView;
    Toolbar toolbar;
    EditText txtURLBase;
    CheckBox ckSSL, ckRestablecer, ckManual;
    Spinner cbEmpresas;
    List<Empresa> listEmpresa = new ArrayList<>();
    SpinnerAdapter spinnerAdapter;
    ProgressBar pbCargando;
    ImageButton btnRefresh;
    TextView lblRuc, lblLeyendaR;
    Button btnRestaurar, btnGuardar;
    LinearLayout lyRestablecer, lyManual, lyCombo;
    MaterialCardView cvRestablecer;
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
        cbEmpresas = findViewById(R.id.cbEmpresas);
        pbCargando = findViewById(R.id.pbCargando);
        btnRefresh = findViewById(R.id.btnRefresh);
        lblRuc = findViewById(R.id.lblRuc);
        btnRestaurar = findViewById(R.id.btnRestaurar);
        ckRestablecer = findViewById(R.id.ckRestablecer);
        lyRestablecer = findViewById(R.id.lyRestablecer);
        btnGuardar = findViewById(R.id.btnGuardar);
        lblLeyendaR = findViewById(R.id.lblLeyendaR);
        cvRestablecer = findViewById(R.id.cvRestablecer);
        ckManual = findViewById(R.id.ckManual);
        lyManual = findViewById(R.id.lyManual);
        lyCombo = findViewById(R.id.lyCombo);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            lblLeyendaR.setText(Html.fromHtml(getResources().getString(R.string.leyendaRestablecer), Html.FROM_HTML_MODE_LEGACY));
        } else {
            lblLeyendaR.setText(Html.fromHtml(getResources().getString(R.string.leyendaRestablecer)));
        }

        btnRefresh.setOnClickListener(this::onClick);
        btnRestaurar.setOnClickListener(this::onClick);
        btnGuardar.setOnClickListener(this::onClick);

        if (SQLite.configuracion != null) {
            txtURLBase.setText(SQLite.configuracion.urlbase);
            ckSSL.setChecked(SQLite.configuracion.hasSSL);
        }

        cbEmpresas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Empresa empresa = (Empresa) parent.getItemAtPosition(position);
                lblRuc.setText("RUC: 1234567890001\nURL: www.empresa.com");
                if (empresa.ruc != "") {
                    lblRuc.setText(
                            "RUC: ".concat(empresa.ruc)
                                    .concat("\n")
                                    .concat("URL: ").concat(empresa.hostname)
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ckRestablecer.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked)
                        lyRestablecer.setVisibility(View.VISIBLE);
                    else
                        lyRestablecer.setVisibility(View.GONE);
                }
        );

        ckManual.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        lyManual.setVisibility(View.VISIBLE);
                        lyCombo.setVisibility(View.GONE);
                        DialogoIngresoManual();
                    } else {
                        lyManual.setVisibility(View.GONE);
                        lyCombo.setVisibility(View.VISIBLE);
                    }
                }
        );

        if (Usuario.numUsuarios() > 0)
            cvRestablecer.setVisibility(View.VISIBLE);

        LlenarEmpresas();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newclient).setVisible(false);
        menu.findItem(R.id.option_newdocument).setVisible(false);
        menu.findItem(R.id.option_listdocument).setVisible(false);
        menu.findItem(R.id.option_reimprimir).setVisible(false);
        menu.findItem(R.id.option_save).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void ValidarDatos(boolean ssl, String url) {
        try {
            if (ckManual.isChecked() && txtURLBase.getText().toString().trim().equals("")) {
                Banner.make(rootView, this, Banner.ERROR, "Debe especificar la URL de la empresa.", Banner.BOTTOM, 3000).show();
                txtURLBase.requestFocus();
                return;
            } else if (!ckManual.isChecked() && cbEmpresas.getSelectedItemPosition() == 0) {
                Banner.make(rootView, this, Banner.ERROR, "Debe especificar la empresa.", Banner.BOTTOM, 3000).show();
                return;
            } else {
                String url_temp = (ssl ? Constants.HTTPs : Constants.HTTP)
                        + url
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
                            if (!ssl)
                                Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                            else
                                ValidarDatos(false, url);
                            return;
                        }
                        try {
                            if (response.body() != null) {
                                String resp = response.body();
                                if (resp.equalsIgnoreCase("OK"))
                                    GuardarDatos(ssl, url);
                                else {
                                    if (!ssl)
                                        Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                                    else
                                        ValidarDatos(false, url);
                                }
                            } else {
                                if (!ssl)
                                    Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                                else
                                    ValidarDatos(false, url);
                            }
                        } catch (Exception e) {
                            if (!ssl)
                                Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                            else
                                ValidarDatos(false, url);
                            Log.d(TAG, e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.d(TAG, t.getMessage());
                        if (!ssl)
                            Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                        else
                            ValidarDatos(false, url);
                        call.cancel();
                    }
                });
            }

        } catch (Exception e) {
            if (!ssl)
                Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible conectar con la URL ingresada. Verifique su conexión.", Banner.BOTTOM, 3000).show();
            else
                ValidarDatos(false, url);
            Log.d(TAG, "ValidarDatos(): " + e.getMessage());
        }
    }

    private void GuardarDatos(boolean ssl, String url) {
        try {
            Configuracion newConfig = new Configuracion();
            newConfig.urlbase = url;
            newConfig.hasSSL = ssl;//ckSSL.isChecked();
            newConfig.url_ws = (newConfig.hasSSL ? Constants.HTTPs : Constants.HTTP)
                    + newConfig.urlbase
                    + Constants.ENDPOINT;
            if (newConfig.Save()) {
                SQLite.configuracion = newConfig;
                //Utils.showSuccessDialog(this, "Configuración",Constants.MSG_DATOS_GUARDADOS,true,false);
                //Banner.make(rootView, ConfigActivity.this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS, Banner.BOTTOM,2000).show();
                Utils.showMessage(ConfigActivity.this, Constants.MSG_DATOS_GUARDADOS);
                onBackPressed();
                overridePendingTransition(R.anim.right_in, R.anim.right_out);
            } else {
                //Utils.showErrorDialog(this, "Error",Constants.MSG_DATOS_NO_GUARDADOS);
                Banner.make(rootView, this, Banner.ERROR, Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM, 3000).show();
            }
        } catch (Exception e) {
            Log.d(TAG, "GuardarDatos(): " + e.getMessage());
        }
    }

    private void LlenarEmpresas() {
        try {

            pbCargando.setVisibility(View.VISIBLE);
            btnRefresh.setVisibility(View.GONE);

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.WS_EMPRESAS)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();
            IUsuario miInterface = retrofit.create(IUsuario.class);

            Call<JsonObject> call = miInterface.getEmpresas();
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful()) {
                        Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible obtener las empresas. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                        pbCargando.setVisibility(View.GONE);
                        btnRefresh.setVisibility(View.VISIBLE);
                        return;
                    }
                    try {
                        if (response.body() != null) {
                            JsonObject obj = response.body();
                            cbEmpresas.setAdapter(null);
                            listEmpresa.clear();
                            if (!obj.get("haserror").getAsBoolean()) {
                                JsonArray jsonEmpresas = obj.getAsJsonArray("data");
                                if (jsonEmpresas != null) {
                                    Empresa miempresa;
                                    for (JsonElement ele : jsonEmpresas) {
                                        JsonObject trans = ele.getAsJsonObject();
                                        miempresa = new Empresa();
                                        miempresa.ruc = trans.get("ruc").getAsString();
                                        miempresa.razonsocial = trans.get("razonsocial").getAsString();
                                        miempresa.alias = trans.get("alias").getAsString();
                                        miempresa.hostname = trans.get("hostname").getAsString();
                                        listEmpresa.add(miempresa);
                                    }
                                }
                            } else
                                Banner.make(rootView, ConfigActivity.this, Banner.ERROR, obj.get("message").getAsString(), Banner.BOTTOM, 3000).show();
                            Empresa miempresa = new Empresa();
                            miempresa.alias = "- Escoja una empresa -";
                            listEmpresa.add(0, miempresa);
                            spinnerAdapter = new ArrayAdapter<>(ConfigActivity.this, android.R.layout.simple_spinner_dropdown_item, listEmpresa);
                            cbEmpresas.setAdapter(spinnerAdapter);
                            cbEmpresas.setSelection(0, true);

                            if (SQLite.configuracion != null) {
                                for (int i = 0; i < listEmpresa.size(); i++) {
                                    if (listEmpresa.get(i).hostname.equals(SQLite.configuracion.urlbase)) {
                                        cbEmpresas.setSelection(i, true);
                                        break;
                                    }
                                }
                            }
                        } else
                            Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible obtener las empresas. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                    } catch (Exception e) {
                        Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible obtener las empresas. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                        Log.d(TAG, e.getMessage());
                    }
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.d(TAG, t.getMessage());
                    Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "No es posible obtener las empresas. Verifique su conexión.", Banner.BOTTOM, 3000).show();
                    call.cancel();
                    pbCargando.setVisibility(View.GONE);
                    btnRefresh.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception ex) {
            pbCargando.setVisibility(View.GONE);
            btnRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void DialogoRestaurar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                (ConstraintLayout) findViewById(R.id.lyDialogContainer));
        builder.setView(view);
        ((TextView) view.findViewById(R.id.lblTitle)).setText("Confirmación");
        ((TextView) view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea realizar esta acción?");
        ((ImageView) view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
        ((Button) view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
        ((Button) view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            RestaurarDatos();
            alertDialog.dismiss();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

        if (alertDialog.getWindow() != null)
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        alertDialog.show();
    }

    private void RestaurarDatos() {
        if (Utilidades.deletedb(this)) {
            if (Usuario.CerrarSesionLocal(this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(this).inflate(R.layout.layout_success_dialog,
                        (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView) view.findViewById(R.id.lblTitle)).setText("Aplicación restablecida");
                ((TextView) view.findViewById(R.id.lblMessage)).setText("El proceso de restablecimiento se completó correctamente.\nDebe cerrar y volver a abrir la aplicación para continuar.");
                ((ImageView) view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
                ((Button) view.findViewById(R.id.btnAction)).setText("Cerrar APP");
                final AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnAction).setOnClickListener(v -> {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("EXIT", true);
                    startActivity(intent);
                });
                if (alertDialog.getWindow() != null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
            } else
                Banner.make(rootView, this, Banner.ERROR, "Ocurrió un error al eliminar los datos de usuario.", Banner.BOTTOM, 3000).show();
        } else
            Banner.make(rootView, this, Banner.ERROR, "Ocurrió un error al eliminar la base de datos.", Banner.BOTTOM, 3000).show();
    }

    private void DialogoIngresoManual() {
        txtURLBase.setEnabled(false);
        EditText txtPassword;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_seguridad_dialog,
                (ConstraintLayout) findViewById(R.id.lyDialogContainer));
        builder.setView(view);
        txtPassword = view.findViewById(R.id.txtPassword);
        ((TextView) view.findViewById(R.id.lblTitle)).setText("Clave seguridad");
        ((TextView) view.findViewById(R.id.lblMessage)).setText("Para realizar el ingreso manual debe confirmar la clave de seguridad.");
        ((ImageView) view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_lock);
        ((Button) view.findViewById(R.id.btnCancel)).setText(getResources().getString(R.string.Cancel));
        ((Button) view.findViewById(R.id.btnConfirm)).setText(getResources().getString(R.string.Confirm));
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            if (txtPassword.getText().toString().equals(Constants.CLAVE_SEGURIDAD)) {
                txtURLBase.setEnabled(true);
                alertDialog.dismiss();
                txtURLBase.requestFocus();
            } else {
                Banner.make(rootView, ConfigActivity.this, Banner.ERROR, "Clave incorrecta, intente nuevamente.", Banner.BOTTOM, 2000).show();
            }
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            ckManual.setChecked(false);
            alertDialog.dismiss();
        });

        if (alertDialog.getWindow() != null)
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        alertDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRefresh:
                LlenarEmpresas();
                break;
            case R.id.btnRestaurar:
                DialogoRestaurar();
                break;
            case R.id.btnGuardar:
                String url = "";
                if (ckManual.isChecked())
                    url = txtURLBase.getText().toString().toLowerCase().trim();
                else if (cbEmpresas.getSelectedItemPosition() != 0) {
                    Empresa emp = (Empresa) cbEmpresas.getSelectedItem();
                    url = emp.hostname;
                }
                ValidarDatos(true, url);
                break;
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
