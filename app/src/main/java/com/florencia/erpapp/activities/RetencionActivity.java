package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Selection;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.adapters.DetalleRetencionAdapter;
import com.florencia.erpapp.models.Catalogo;
import com.florencia.erpapp.models.DetalleRetencion;
import com.florencia.erpapp.models.Retencion;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.google.gson.Gson;
import com.shasin.notificationbanner.Banner;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import br.com.sapereaude.maskedEditText.MaskedEditText;

public class RetencionActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public static final String TAG = "TAGRETENCION_ACTIVITY";
    Toolbar toolbar;
    View rootView;
    EditText txtNumAutorizacion, txtMonto;
    MaskedEditText txtNumDocumento;
    TextView lblTotal;
    Button btnFechaDoc, btnAgregar, btnLimpiar, btnConfirmar;
    RadioGroup rgTipoRet;
    RadioButton rbRetFuente, rbRetIVA;
    SearchableSpinner spTipoDoc;
    RecyclerView rvDetalle;
    DetalleRetencionAdapter detalleRetencionAdapter;
    Calendar calendar;
    DatePickerDialog dtpDialog;

    Retencion miRetencion = new Retencion();

    public Double baseimponible = 0d, baseiva = 0d;
    public String ruccliente = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retencion);
        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootView = findViewById(android.R.id.content);
        toolbar.setTitle("RETENCION");

        init();
    }

    private void init() {
        try {
            txtNumDocumento = findViewById(R.id.txtNumRetencion);
            txtNumAutorizacion = findViewById(R.id.txtNumAutorizacion);
            txtMonto = findViewById(R.id.txtMonto);
            btnFechaDoc = findViewById(R.id.btnFechaDoc);
            btnAgregar = findViewById(R.id.btnAgregar);
            rgTipoRet = findViewById(R.id.rgTipoRetencion);
            rbRetFuente = findViewById(R.id.rbRetFuente);
            rbRetIVA = findViewById(R.id.rbRetIVA);
            spTipoDoc = (SearchableSpinner) findViewById(R.id.spTipoDocumento);
            rvDetalle = findViewById(R.id.rvDetalle);
            lblTotal = findViewById(R.id.lblTotal);
            btnLimpiar = findViewById(R.id.btnLimpiar);
            btnConfirmar = findViewById(R.id.btnConfirmar);

            spTipoDoc.setTitle("Especifique una opción");
            spTipoDoc.setPositiveButton("Aceptar");

            if (getIntent().getExtras() != null) {
                Log.d(TAG, "si");
                baseimponible = Utils.RoundDecimal(getIntent().getDoubleExtra("baseimponible", 0d), 2);
                baseiva = Utils.RoundDecimal(getIntent().getDoubleExtra("baseiva", 0d), 2);
                ruccliente = getIntent().getExtras().getString("ruccliente", "");
                miRetencion = new Gson().fromJson(getIntent().getStringExtra("retencion"), Retencion.class);
                if (miRetencion == null)
                    miRetencion = new Retencion();

                txtNumDocumento.setText(miRetencion.numerodocumento);
                txtNumAutorizacion.setText(miRetencion.numeroautorizacion);
                btnFechaDoc.setText(miRetencion.fechahora);

            }

            if (miRetencion.idretencion > 0) {
                txtNumDocumento.setEnabled(false);
                txtNumAutorizacion.setEnabled(false);
                btnFechaDoc.setEnabled(false);
                rbRetFuente.setEnabled(false);
                rbRetIVA.setEnabled(false);
                spTipoDoc.setEnabled(false);
                txtMonto.setEnabled(false);
                btnAgregar.setVisibility(View.INVISIBLE);
                btnLimpiar.setVisibility(View.INVISIBLE);
                btnConfirmar.setVisibility(View.INVISIBLE);
            }

            detalleRetencionAdapter = new DetalleRetencionAdapter(RetencionActivity.this, miRetencion.detalle, miRetencion.idretencion > 0);
            rvDetalle.setAdapter(detalleRetencionAdapter);

            updateTotal();

            btnAgregar.setOnClickListener(this::onClick);
            btnFechaDoc.setOnClickListener(this::onClick);
            btnLimpiar.setOnClickListener(this::onClick);
            btnConfirmar.setOnClickListener(this::onClick);

            rbRetIVA.setOnCheckedChangeListener(this::onCheckedChanged);
            rbRetFuente.setOnCheckedChangeListener(this::onCheckedChanged);

            txtNumAutorizacion.setOnFocusChangeListener((view, b) -> {
                if (b)
                    GeneraAut();
            });

            txtMonto.setOnFocusChangeListener((view, b) -> {
                if (b) {
                    if (rbRetIVA.isChecked())
                        txtMonto.setText(baseiva.toString());
                    else if (rbRetFuente.isChecked())
                        txtMonto.setText(baseimponible.toString());
                    txtMonto.setSelection(0, txtMonto.getText().toString().length());
                }
            });
        } catch (Exception ex) {
            Log.d(TAG, "init(): " + ex.getMessage());
        }
    }

    private void GeneraAut() {
        if (!btnFechaDoc.getText().toString().equals("") && txtNumDocumento.getText().toString().length() == 17) {
            String f = btnFechaDoc.getText().toString();
            f = f.split("-")[2] + f.split("-")[1] + f.split("-")[0];
            String c = txtNumDocumento.getText().toString();
            c = c.split("-")[0] + c.split("-")[1] + c.split("-")[2];
            txtNumAutorizacion.setText(f + "07" + ruccliente + "2" + c + "123456781");
            txtNumAutorizacion.setSelection(0, txtNumAutorizacion.getText().toString().length());
        }
    }

    public void updateTotal() {
        Double total = 0d;
        for (DetalleRetencion det : detalleRetencionAdapter.detalleRetencion) {
            total += det.valorretenido + det.valorretenidoiva;
        }
        lblTotal.setText("Total: " + Utils.FormatoMoneda(total, 2));
    }

    private void LlenarComboFuente() {
        Thread th = new Thread() {
            @Override
            public void run() {
                List<Catalogo> entidades = new ArrayList<>();
                entidades.add(new Catalogo(-1, "", "NONE", "-Seleccione-", 0));
                entidades.addAll(Catalogo.getCatalogo("RETFUENTE"));
                ArrayAdapter<Catalogo> adapter = new ArrayAdapter<>(RetencionActivity.this, android.R.layout.simple_spinner_dropdown_item, entidades);
                runOnUiThread(() -> spTipoDoc.setAdapter(adapter));
            }
        };
        th.start();
    }

    private void LlenarComboIVA() {
        Thread th = new Thread() {
            @Override
            public void run() {
                List<Catalogo> entidades = new ArrayList<>();
                entidades.add(new Catalogo(-1, "", "NONE", "-Seleccione-", 0));
                entidades.add(new Catalogo(-1, "8", "RETIVA", "0% No aplica", 0));
                entidades.add(new Catalogo(-1, "7", "RETIVA", "0%", 0));
                entidades.add(new Catalogo(-1, "9", "RETIVA", "10%", 0));
                entidades.add(new Catalogo(-1, "10", "RETIVA", "20%", 0));
                entidades.add(new Catalogo(-1, "1", "RETIVA", "30%", 0));
                entidades.add(new Catalogo(-1, "11", "RETIVA", "50%", 0));
                entidades.add(new Catalogo(-1, "2", "RETIVA", "70%", 0));
                entidades.add(new Catalogo(-1, "3", "RETIVA", "100%", 0));
                ArrayAdapter<Catalogo> adapter = new ArrayAdapter<>(RetencionActivity.this, android.R.layout.simple_spinner_dropdown_item, entidades);
                runOnUiThread(() -> {
                    spTipoDoc.setAdapter(null);
                    spTipoDoc.setAdapter(adapter);
                    Log.d(TAG, "can: " + entidades.size());
                });
            }
        };
        th.start();
    }

    private boolean Validar() {
        try {
            if (txtNumDocumento.getText().toString().trim().equals("") || txtNumDocumento.getText().toString().trim().length() != 17) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe ingresar un número de documento válido.", Banner.BOTTOM, 3000).show();
                return false;
            }

            if (txtNumAutorizacion.getText().toString().trim().length() < 10) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "El número de autorización debe contener al menos 10 caracteres.", Banner.BOTTOM, 3000).show();
                return false;
            }

            if (txtNumAutorizacion.getText().toString().trim().length() > 10 && txtNumAutorizacion.getText().toString().trim().length() < 49) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "El número de autorización para retención electrónica debe contener 49 caracteres.", Banner.BOTTOM, 3000).show();
                return false;
            }

            if (btnFechaDoc.getText().toString().trim().equals("")) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe especificar la fecha del documento.", Banner.BOTTOM, 3000).show();
                return false;
            }
            if (!rbRetFuente.isChecked() && !rbRetIVA.isChecked()) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe especificar el tipo de retención.", Banner.BOTTOM, 3000).show();
                return false;
            }
            if (txtMonto.getText().toString().trim().equals("") || Double.valueOf(txtMonto.getText().toString()) <= 0) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe ingresar un valor válido en la base imponible para retener.", Banner.BOTTOM, 3000).show();
                return false;
            }
            if (rbRetFuente.isChecked() && Double.valueOf(txtMonto.getText().toString()) > baseimponible) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "La base imponible no debe ser mayor a " + Constants.COMILLA_ABRE + Utils.FormatoMoneda(baseimponible, 2) + Constants.COMILLA_CIERRA, Banner.BOTTOM, 3000).show();
                return false;
            }
            if (rbRetFuente.isChecked() && spTipoDoc.getSelectedItemPosition() == 0) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe especificar el tipo de retención a la  " + Constants.COMILLA_ABRE + "FUENTE" + Constants.COMILLA_CIERRA, Banner.BOTTOM, 3000).show();
                return false;
            }
            if (rbRetIVA.isChecked() && Double.valueOf(txtMonto.getText().toString()) > baseiva) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "La base imponible no debe ser mayor a " + Constants.COMILLA_ABRE + Utils.FormatoMoneda(baseiva, 2) + Constants.COMILLA_CIERRA, Banner.BOTTOM, 3000).show();
                return false;
            }
            if (rbRetIVA.isChecked() && spTipoDoc.getSelectedItemPosition() == 0) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe especificar el tipo de retención al  " + Constants.COMILLA_ABRE + "IVA" + Constants.COMILLA_CIERRA, Banner.BOTTOM, 3000).show();
                return false;
            }
            Catalogo codigo = (Catalogo) spTipoDoc.getSelectedItem();
            String codigoret = rbRetIVA.isChecked() ? "322" : codigo.nombrecatalogo.split("-")[0].trim();
            if (detalleRetencionAdapter.detalleRetencion.size() > 0) {
                for (DetalleRetencion det : detalleRetencionAdapter.detalleRetencion) {
                    if (det.codigoretencion.equals(codigoret)) {
                        Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                                "La retención " + Constants.COMILLA_ABRE + (rbRetIVA.isChecked() ? "al IVA" : "a la FUENTE") + Constants.COMILLA_CIERRA
                                        + " con código " + Constants.COMILLA_ABRE + codigoret + Constants.COMILLA_CIERRA + " ya ha sido agregada.", Banner.BOTTOM, 3000).show();
                        return false;
                    }
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Validar(): " + ex.getMessage());
            return false;
        }
        return true;
    }

    private void Agregar() {
        if (!Validar())
            return;

        try {
            Catalogo codigo = (Catalogo) spTipoDoc.getSelectedItem();
            DetalleRetencion miDetalle = new DetalleRetencion();
            miDetalle.tipo = rbRetFuente.isChecked() ? "FUENTE" : "IVA";
            miDetalle.codigo = miDetalle.tipo.equals("IVA") ? codigo.codigocatalogo : "0";
            miDetalle.codigoretencion = miDetalle.tipo.equals("IVA") ? "322" : codigo.nombrecatalogo.split("-")[0].trim();
            miDetalle.porcentajeretener = miDetalle.tipo.equals("IVA") ? 0 : Double.parseDouble(codigo.codigocatalogo);
            miDetalle.porcentajereteneriva = miDetalle.tipo.equals("IVA") ? Double.parseDouble(codigo.nombrecatalogo.split("%")[0]) : 0;
            miDetalle.baseimponible = miDetalle.tipo.equals("IVA") ? 0 : Double.parseDouble(txtMonto.getText().toString());
            miDetalle.baseimponibleiva = miDetalle.tipo.equals("IVA") ? Double.parseDouble(txtMonto.getText().toString()) : 0;
            miDetalle.valorretenido = miDetalle.baseimponible * (miDetalle.porcentajeretener / 100);
            miDetalle.valorretenidoiva = miDetalle.baseimponibleiva * (miDetalle.porcentajereteneriva / 100);
            miDetalle.fechaemisiondocsustento = btnFechaDoc.getText().toString();
            miDetalle.coddocsustento = "01";

            baseimponible -= miDetalle.baseimponible;
            baseiva -= miDetalle.baseimponibleiva;

            baseimponible = Utils.RoundDecimal(baseimponible, 2);
            baseiva = Utils.RoundDecimal(baseiva, 2);

            txtMonto.setText(Utils.RoundDecimal(baseimponible, 2).toString());
            if (miDetalle.tipo.equals("IVA"))
                txtMonto.setText(Utils.RoundDecimal(baseiva, 2).toString());

            detalleRetencionAdapter.detalleRetencion.add(miDetalle);
            detalleRetencionAdapter.notifyDataSetChanged();
            updateTotal();
        } catch (Exception e) {
            Log.d(TAG, "Agregar(): " + e.getMessage());
        }
    }

    private boolean ValidarRetencion() {
        try {
            if (txtNumDocumento.getText().toString().trim().equals("") || txtNumDocumento.getText().toString().trim().length() != 17) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe ingresar un número de documento válido.", Banner.BOTTOM, 3000).show();
                return false;
            }

            if (txtNumAutorizacion.getText().toString().trim().length() < 10 || txtNumAutorizacion.getText().toString().trim().length() > 49) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "El número de autorización debe tener una longitud entre 10 y 49 caracteres.", Banner.BOTTOM, 3000).show();
                return false;
            }
            if (btnFechaDoc.getText().toString().trim().equals("")) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe especificar la fecha del documento.", Banner.BOTTOM, 3000).show();
                return false;
            }

            if (detalleRetencionAdapter.detalleRetencion == null || detalleRetencionAdapter.detalleRetencion.size() == 0) {
                Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                        "Debe agregar al menos un detalle para la retención: "
                                + Constants.COMILLA_ABRE + "FUENTE" + Constants.COMILLA_CIERRA + " o "
                                + Constants.COMILLA_ABRE + "IVA" + Constants.COMILLA_CIERRA, Banner.BOTTOM, 3000).show();
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "ValidarRetencion(): " + e.getMessage());
            return false;
        }
        return true;
    }

    private void GuardarRetencion() {
        if (!ValidarRetencion())
            return;
        try {
            String f = btnFechaDoc.getText().toString();
            miRetencion.usuarioid = SQLite.usuario.IdUsuario;
            miRetencion.numerodocumento = txtNumDocumento.getText().toString();
            miRetencion.numeroautorizacion = txtNumAutorizacion.getText().toString();
            miRetencion.periodofiscal = f.split("-")[1] + "/" + f.split("-")[0];
            miRetencion.puntoemision = miRetencion.numerodocumento.split("-")[1];
            miRetencion.establecimiento = miRetencion.numerodocumento.split("-")[0];
            miRetencion.fechahora = f;

            if (miRetencion.detalle.size() == 0)
                miRetencion.detalle.addAll(detalleRetencionAdapter.detalleRetencion);

            setResult(Activity.RESULT_OK, new Intent().putExtra("retencion", new Gson().toJson(miRetencion, Retencion.class)));
            onBackPressed();
            overridePendingTransition(R.anim.zoom_back_in, R.anim.zoom_back_out);
        } catch (Exception e) {
            Log.d(TAG, "GuardarRetencion(): " + e.getMessage());
        }
    }

    public void showDatePickerDialog(View v) {
        Locale l = new Locale("ES-es");
        calendar = Calendar.getInstance(l);
        String[] fecha = (((Button) v).getText().toString().equals("") ? Utils.getDateFormat("yyyy-MM-dd") : ((Button) v).getText().toString()).split("-");
        int day = Integer.valueOf(fecha[2]);
        int month = Integer.valueOf(fecha[1]) - 1;
        int year = Integer.valueOf(fecha[0]);
        dtpDialog = new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                String dia = (day >= 0 && day < 10 ? "0" + (day) : String.valueOf(day));
                String mes = (month >= 0 && month < 9 ? "0" + (month + 1) : String.valueOf(month + 1));

                String miFecha = year + "-" + mes + "-" + dia;
                ((Button) v).setText(miFecha);
                if (Utils.longDate(miFecha) > Utils.longDate(Utils.getDateFormat("yyyy-MM-dd"))) {
                    Banner.make(rootView, RetencionActivity.this, Banner.ERROR,
                            "Debe elegir una fecha menor o igual a la fecha actual",
                            Banner.BOTTOM, 3000).show();
                    return;
                }
            }
        }, year, month, day);
        dtpDialog.show();
    }

    private void LimpiarDatos() {
        try {
            txtNumDocumento.setText("");
            txtNumAutorizacion.setText("");
            txtMonto.setText("");
            for (DetalleRetencion det : detalleRetencionAdapter.detalleRetencion) {
                if (det.tipo.equals("FUENTE"))
                    baseimponible += det.baseimponible;
                if (det.tipo.equals("IVA"))
                    baseiva += det.baseimponibleiva;
            }
            detalleRetencionAdapter.detalleRetencion.clear();
            miRetencion = new Retencion();
            detalleRetencionAdapter.notifyDataSetChanged();
            btnFechaDoc.setText("");
            rgTipoRet.clearCheck();

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAgregar:
                Agregar();
                break;
            case R.id.btnFechaDoc:
                showDatePickerDialog(v);
                break;
            case R.id.btnLimpiar:
                AlertDialog.Builder builder = new AlertDialog.Builder(RetencionActivity.this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(RetencionActivity.this).inflate(R.layout.layout_warning_dialog,
                        (ConstraintLayout) RetencionActivity.this.findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView) view.findViewById(R.id.lblTitle)).setText("LIMPIAR DATOS");
                ((TextView) view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea limpiar los datos?");
                ((ImageView) view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_delete2);
                ((Button) view.findViewById(R.id.btnCancel)).setText(RetencionActivity.this.getResources().getString(R.string.Cancel));
                ((Button) view.findViewById(R.id.btnYes)).setText(RetencionActivity.this.getResources().getString(R.string.Confirm));
                final AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnYes).setOnClickListener((vi) -> {
                    LimpiarDatos();
                    alertDialog.dismiss();
                });

                view.findViewById(R.id.btnCancel).setOnClickListener(vi -> alertDialog.dismiss());

                if (alertDialog.getWindow() != null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
                break;
            case R.id.btnConfirmar:
                GuardarRetencion();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.rbRetFuente:
                if (isChecked) {
                    txtMonto.setText(baseimponible.toString());
                    LlenarComboFuente();
                }
                break;
            case R.id.rbRetIVA:
                if (isChecked) {
                    txtMonto.setText(baseiva.toString());
                    LlenarComboIVA();
                }
                break;
        }
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search_product, menu);
        return super.onCreateOptionsMenu(menu);
    }*/

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_select:
                GuardarRetencion();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        toolbar.setTitle("RETENCION");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setBackgroundColor(getResources().getColor(R.color.black_overlay));
        //toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        super.onResume();
    }
}
