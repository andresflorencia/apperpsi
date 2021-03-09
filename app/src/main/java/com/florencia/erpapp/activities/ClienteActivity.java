package com.florencia.erpapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.models.Canton;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.models.Parroquia;
import com.florencia.erpapp.models.Provincia;
import com.florencia.erpapp.models.TipoIdentificacion;
import com.florencia.erpapp.services.GPSTracker;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.shasin.notificationbanner.Banner;

import java.util.ArrayList;
import java.util.List;

public class ClienteActivity extends AppCompatActivity implements View.OnFocusChangeListener, View.OnClickListener {

    Spinner cbTipoDocumento, cbProvincia, cbCanton, cbParroquia;;
    EditText txtNIP, txtRazonSocial, txtNombreComercial, txtLatitud, txtLongitud, txtDireccion,
            txtFono1, txtFono2, txtCorreo, txtObservacion;
    ImageButton btnObtenerDireccion;
    Cliente miCliente;
    View rootView;
    boolean isReturn = false, band = false;

    TextView lblMessage, lblTitle, lblInfoPersonal, lblInfoContacto;
    LinearLayout lyInfoPersonal, lyInfoContacto;
    BottomSheetDialog btsDialog;
    Button btnPositive, btnNegative;
    View viewSeparator;
    String tipoAccion="";
    Toolbar toolbar;
    List<Provincia> provincias = new ArrayList<>();
    List<Canton> cantones = new ArrayList<>();
    List<Parroquia> parroquias = new ArrayList<>();
    Provincia provActual = new Provincia();
    Canton canActual = new Canton();

    public ClienteActivity(){}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente);

        toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootView = findViewById(android.R.id.content);
        init();

        if(getIntent().getExtras()!=null){
            Integer idcliente =  getIntent().getExtras().getInt("idcliente",0);
            if(idcliente>0){
                BuscarDatos(idcliente,"");
            }
            this.isReturn = getIntent().getExtras().getBoolean("nuevo_cliente",false);
        }
    }

    private void BuscarDatos(Integer id, String nip){
        try{
            Thread th = new Thread(){
                @Override
                public void run(){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(id>0)
                                miCliente = Cliente.get(id);
                            else if(!nip.equals(""))
                                miCliente = Cliente.get(nip);

                            if(miCliente != null){

                                band = true;
                                Parroquia miparroquia = Parroquia.get(miCliente.parroquiaid);
                                canActual = Canton.get(miparroquia.cantonid);
                                provActual = Provincia.get(canActual.provinciaid);

                                for(Provincia miP:provincias){
                                    if(provActual.idprovincia.equals(miP.idprovincia)){
                                        cbProvincia.setSelection(provincias.indexOf(miP));
                                        break;
                                    }
                                }
                                LlenarComboCantones(provActual.idprovincia, canActual.idcanton);
                                LlenarComboParroquias(canActual.idcanton, miparroquia.idparroquia);

                                txtNIP.setTag(miCliente.idcliente);
                                txtNIP.setText(miCliente.nip);
                                txtRazonSocial.setText(miCliente.razonsocial);
                                txtNombreComercial.setText(miCliente.nombrecomercial);
                                txtLatitud.setText(miCliente.lat.toString());
                                txtLongitud.setText(miCliente.lon.toString());
                                txtDireccion.setText(miCliente.direccion);
                                txtFono1.setText(miCliente.fono1);
                                txtFono2.setText(miCliente.fono2);
                                txtCorreo.setText(miCliente.email);
                                txtObservacion.setText(miCliente.observacion);

                                if(miCliente.tiponip != null){
                                    for(int i =0; i < cbTipoDocumento.getCount();i++){
                                        TipoIdentificacion ti = (TipoIdentificacion) cbTipoDocumento.getItemAtPosition(i);
                                        if(ti.getCodigo().equals(miCliente.tiponip)){
                                            cbTipoDocumento.setSelection(i,true);
                                            break;
                                        }
                                    }
                                }

                                toolbar.setTitle("Modificación");
                            }
                        }
                    });
                }
            };
            th.start();
        }catch (Exception e){
            Log.d("TAGCLIENTE", "BuscarDatos" + e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPositive:
                if (tipoAccion.equals("MESSAGE")) {
                    btnNegative.setVisibility(View.VISIBLE);
                    viewSeparator.setVisibility(View.VISIBLE);
                    lblTitle.setVisibility(View.VISIBLE);
                    btsDialog.dismiss();
                }
                break;
            case R.id.btnNegative:
                btsDialog.dismiss();
                break;
            case R.id.btnObtenerDireccion:
                //SQLite.gpsTracker = new GPSTracker(v.getContext());
                ObtenerCoordenadas(true);
                break;
            case R.id.lblInfoPersonal:
                Utils.EfectoLayout(lyInfoPersonal, lblInfoPersonal);
                break;
            case R.id.lblInfoContacto:
                Utils.EfectoLayout(lyInfoContacto, lblInfoContacto);
                break;
        }
    }

    void init(){
        cbTipoDocumento = findViewById(R.id.spTipoDocumento);
        txtNIP = findViewById(R.id.txtNIP);
        txtRazonSocial = findViewById(R.id.txtRazonSocial);
        txtNombreComercial = findViewById(R.id.txtNombreComercial);
        btnObtenerDireccion = findViewById(R.id.btnObtenerDireccion);
        txtLatitud = findViewById(R.id.txtLatitud);
        txtLongitud = findViewById(R.id.txtLongitud);
        txtDireccion = findViewById(R.id.txtDireccion);
        txtFono1 = findViewById(R.id.txtfono1);
        txtFono2 = findViewById(R.id.txtfono2);
        txtCorreo = findViewById(R.id.txtCorreo);
        txtObservacion = findViewById(R.id.txtObservacion);
        lblInfoPersonal = findViewById(R.id.lblInfoPersonal);
        lblInfoContacto = findViewById(R.id.lblInfoContacto);
        lyInfoPersonal = findViewById(R.id.lyInfoPersonal);
        lyInfoContacto = findViewById(R.id.lyInfoContacto);
        cbProvincia = findViewById(R.id.cbProvincia);
        cbCanton = findViewById(R.id.cbCanton);
        cbParroquia = findViewById(R.id.cbParroquia);
        LlenarTipoNIP();
        txtNIP.setOnFocusChangeListener(this);
        btnObtenerDireccion.setOnClickListener(this::onClick);
        lblInfoPersonal.setOnClickListener(this::onClick);
        lblInfoContacto.setOnClickListener(this::onClick);

        if(SQLite.gpsTracker==null)
            SQLite.gpsTracker = new GPSTracker(this);
        if (!SQLite.gpsTracker.checkGPSEnabled())
            SQLite.gpsTracker.showSettingsAlert(ClienteActivity.this);

        ObtenerCoordenadas(false);

        LlenarComboProvincias(0);
        LlenarComboCantones(0,-1);
        LlenarComboParroquias(0,-1);

        cbProvincia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(cbProvincia.getAdapter()!=null) {
                    Provincia provincia = ((Provincia)cbProvincia.getItemAtPosition(position));
                    if(provincia.idprovincia!=provActual.idprovincia)
                        band=false;
                    if(!band)
                        LlenarComboCantones(provincia.idprovincia,-1);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        cbCanton.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(cbCanton.getAdapter()!=null) {
                    Canton canton = ((Canton)cbCanton.getItemAtPosition(position));
                    if(!band)
                        LlenarComboParroquias(canton.idcanton,-1);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void ObtenerCoordenadas(boolean alertar){
        try {
            if (SQLite.gpsTracker.checkGPSEnabled()) {
                SQLite.gpsTracker.updateGPSCoordinates();
                SQLite.gpsTracker.getLastKnownLocation();
                txtLatitud.setText(String.valueOf(SQLite.gpsTracker.getLatitude()));
                txtLongitud.setText(String.valueOf(SQLite.gpsTracker.getLongitude()));
            } else if(alertar)
                SQLite.gpsTracker.showSettingsAlert(ClienteActivity.this);
        }catch (Exception e){
            Log.d("TAG_CLIENTEACTIVITY", e.getMessage());
        }
    }

    void LlenarTipoNIP(){
        ArrayList<TipoIdentificacion> tipoIdentificaciones = new ArrayList<>();
        tipoIdentificaciones.add(new TipoIdentificacion("00", "SIN IDENTIFICACIÓN"));
        tipoIdentificaciones.add(new TipoIdentificacion("05", "CÉDULA"));
        tipoIdentificaciones.add(new TipoIdentificacion("04", "RUC"));
        tipoIdentificaciones.add(new TipoIdentificacion("06", "PASAPORTE"));
        tipoIdentificaciones.add(new TipoIdentificacion("07", "ID. EXTERIOR"));
        ArrayAdapter<TipoIdentificacion> adapter = new ArrayAdapter<TipoIdentificacion>(
                this, android.R.layout.simple_spinner_item, tipoIdentificaciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cbTipoDocumento.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save, menu);
        menu.findItem(R.id.option_newdocument).setVisible(false);
        menu.findItem(R.id.option_reimprimir).setVisible(false);
        menu.findItem(R.id.option_listdocument).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_save:
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                        (ConstraintLayout) findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((TextView)view.findViewById(R.id.lblTitle)).setText("Guardar cliente");
                ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea guardar los datos del cliente?");
                ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_save);
                ((Button)view.findViewById(R.id.btnCancel)).setText("Cancelar");
                ((Button)view.findViewById(R.id.btnConfirm)).setText("Si");
                final AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnConfirm).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GuardarDatos();
                        alertDialog.dismiss();
                    }
                });

                view.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { alertDialog.dismiss();}
                });

                if(alertDialog.getWindow()!=null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
                break;
            case R.id.option_newclient:
                LimpiarDatos();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void GuardarDatos() {
        try {
            if(miCliente == null)
                miCliente = new Cliente();
            if(!ValidarDatos()) return;

            miCliente.tiponip = ((TipoIdentificacion)cbTipoDocumento.getSelectedItem()).getCodigo();
            miCliente.nip = txtNIP.getText().toString().trim();
            miCliente.razonsocial = txtRazonSocial.getText().toString().trim();
            miCliente.nombrecomercial = txtNombreComercial.getText().toString().trim();

            SQLite.gpsTracker.getLastKnownLocation();
            miCliente.lat = SQLite.gpsTracker.getLatitude();
            miCliente.lon = SQLite.gpsTracker.getLongitude();

            miCliente.direccion = txtDireccion.getText().toString().trim();
            miCliente.fono1 = txtFono1.getText().toString().trim();
            miCliente.fono2 = txtFono2.getText().toString().trim();
            miCliente.email = txtCorreo.getText().toString().trim();
            miCliente.observacion = txtObservacion.getText().toString().trim();
            miCliente.usuarioid = SQLite.usuario.IdUsuario;
            miCliente.actualizado = 1;
            miCliente.establecimientoid = SQLite.usuario.sucursal.IdEstablecimiento;
            miCliente.parroquiaid = ((Parroquia) cbParroquia.getSelectedItem()).idparroquia;
            if(miCliente.idcliente == 0) {
                miCliente.fecharegistro = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
                miCliente.longdater = Utils.longDate(Utils.getDateFormat("yyyy-MM-dd"));
            }
            miCliente.fechamodificacion = Utils.getDateFormat("yyyy-MM-dd HH:mm:ss");
            miCliente.longdatem = Utils.longDate(Utils.getDateFormat("yyyy-MM-dd"));
            if(miCliente.Save()) {
                Utils.showMessageShort(this, Constants.MSG_DATOS_GUARDADOS);
                //Banner.make(rootView, this, Banner.SUCCESS, Constants.MSG_DATOS_GUARDADOS, Banner.BOTTOM, 3000).show();
                if(this.isReturn)
                    setResult(Activity.RESULT_OK,new Intent().putExtra("idcliente",miCliente.idcliente));
                finish();
                //this.LimpiarDatos();
            }else
                Banner.make(rootView,this,Banner.ERROR,Constants.MSG_DATOS_NO_GUARDADOS, Banner.BOTTOM,3500).show();

        }catch (Exception e){
            Log.d("TAGCLIENTE", e.getMessage());
            Utils.showErrorDialog(this,"Error: ", e.getMessage());
        }
    }

    private boolean ValidarDatos() throws Exception{
        if(miCliente.idcliente==0
            && !SQLite.usuario.VerificaPermiso(this,Constants.REGISTRO_CLIENTE, "escritura")){
                Banner.make(rootView, this, Banner.ERROR,"No tiene permisos para registrar nuevos clientes.", Banner.BOTTOM, 3000).show();
                return false;
        }else if(miCliente.idcliente>0
            && !SQLite.usuario.VerificaPermiso(this,Constants.REGISTRO_CLIENTE, "modificacion")){
            Banner.make(rootView, this, Banner.ERROR,"No tiene permisos para modificar datos.", Banner.BOTTOM, 3000).show();
                return false;
        }
        if(((TipoIdentificacion)cbTipoDocumento.getSelectedItem()).getCodigo().equals("00")){
            Banner.make(rootView,this, Banner.ERROR,"Especifique el tipo de identificación.", Banner.BOTTOM, 3000).show();
            return false;
        }
        if(txtNIP.getText().toString().trim().equals("")){
            txtNIP.setError("Ingrese una identificación.");
            Banner.make(rootView,this, Banner.ERROR,"Ingrese una identificación.", Banner.BOTTOM, 3000).show();
            return false;
        }
        if(txtRazonSocial.getText().toString().trim().equals("")){
            txtRazonSocial.setError("Ingrese el nombre del cliente.");
            Banner.make(rootView, this, Banner.ERROR,"Ingrese el nombre del cliente.", Banner.BOTTOM, 3000).show();
            return false;
        }
        if(txtDireccion.getText().toString().trim().equals("")){
            txtRazonSocial.setError("Ingrese la dirección del cliente.");
            Banner.make(rootView, this, Banner.ERROR,"Ingrese la dirección del cliente.",Banner.BOTTOM, 3000).show();
            return false;
        }
        if(txtFono1.getText().toString().trim().equals("") && txtFono2.getText().toString().trim().equals("")){
            Banner.make(rootView,this,Banner.ERROR,"Especifique al menos un número de contacto.", Banner.BOTTOM, 3000).show();
            return false;
        }

        if(Double.parseDouble(txtLatitud.getText().toString()) == 0 && Double.parseDouble(txtLongitud.getText().toString()) == 0){
            Banner.make(rootView,this,Banner.ERROR,"Debe obtener las coordenadas. Verifique si está activado el GPS.", Banner.BOTTOM, 3000).show();
            return false;
        }
        return true;
    }

    private void LimpiarDatos(){
        try {
            miCliente = new Cliente();
            cbTipoDocumento.setSelection(0, true);
            txtNIP.setText("");
            txtNIP.setTag(0);
            txtRazonSocial.setText("");
            txtNombreComercial.setText("");
            txtLatitud.setText("");
            txtLongitud.setText("");
            txtDireccion.setText("");
            txtFono1.setText("");
            txtFono2.setText("");
            txtCorreo.setText("");
            txtDireccion.setText("");
            txtObservacion.setText("");
            toolbar.setTitle("Nuevo Registro");
        }catch (Exception e){
            Log.d("TAGCLIENTE", "LimpiarDatos(): " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        String titulo=miCliente == null? "Nuevo registro" : "Modificación";
        toolbar.setTitle(titulo);
        toolbar.setTitleTextColor(Color.WHITE);
        //toolbar.setBackgroundColor(getResources().getColor(R.color.colorBlue));
        //toolbar.getNavigationIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_confirmation_dialog,
                    (ConstraintLayout) findViewById(R.id.lyDialogContainer));
            builder.setView(view);
            ((TextView)view.findViewById(R.id.lblTitle)).setText("Cerrar");
            ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Desea salir de la ventana de cliente?");
            ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_check_white);
            ((Button)view.findViewById(R.id.btnCancel)).setText("Cancelar");
            ((Button)view.findViewById(R.id.btnConfirm)).setText("Si");
            final AlertDialog alertDialog = builder.create();
            view.findViewById(R.id.btnConfirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            view.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { alertDialog.dismiss();}
            });

            if(alertDialog.getWindow()!=null)
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            alertDialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void crearBottonSheet() {
        if(btsDialog==null){
            View view = LayoutInflater.from(this).inflate(R.layout.bottonsheet_message,null);
            btnPositive = view.findViewById(R.id.btnPositive);
            btnNegative = view.findViewById(R.id.btnNegative);
            lblMessage = view.findViewById(R.id.lblMessage);
            lblTitle = view.findViewById(R.id.lblTitle);
            viewSeparator = view.findViewById(R.id.vSeparator);
            btnPositive.setOnClickListener(this::onClick);
            btnNegative.setOnClickListener(this::onClick);

            btsDialog = new BottomSheetDialog(this, R.style.AlertDialogTheme);
            btsDialog.setContentView(view);
        }
    }

    public void showError(String message){
        crearBottonSheet();
        lblMessage.setText(message);
        btnNegative.setVisibility(View.GONE);
        viewSeparator.setVisibility(View.GONE);
        lblTitle.setVisibility(View.GONE);
        tipoAccion = "MESSAGE";
        if(btsDialog.getWindow()!=null)
            btsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        btsDialog.show();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        try{
            switch (v.getId()){
                case R.id.txtNIP:
                    if(!hasFocus && !txtNIP.getText().toString().trim().equals(""))
                        BuscarDatos(0,txtNIP.getText().toString().trim());
                    break;
            }
        }catch (Exception e){
            Log.d("TAGCLIENTE", "onFocusChange(): " + e.getMessage());
        }
    }

    private void LlenarComboProvincias(Integer idprovincia){
        try{
            provincias = Provincia.getList();
            ArrayAdapter<Provincia> adapterProvincia = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, provincias);
            adapterProvincia.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cbProvincia.setAdapter(adapterProvincia);
            int position = 0;
            for(int i=0; i<provincias.size(); i++){
                if(provincias.get(i).idprovincia == idprovincia){
                    position = i;
                    break;
                }
            }
            if(idprovincia>=0)
                cbProvincia.setSelection(position,true);
        }catch (Exception e){
            Log.d("TAG_CLIENTEACT", "LlenarComboProvincias(): " +e.getMessage());
        }
    }

    private void LlenarComboCantones(Integer idprovincia, Integer idcanton){
        try{
            cantones.clear();
            cantones = Canton.getList(idprovincia);
            ArrayAdapter<Canton> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cantones);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cbCanton.setAdapter(adapter);
            int position = 0;
            for(int i=0; i<cantones.size(); i++){
                if(cantones.get(i).idcanton.equals(idcanton)){
                    position = i;
                    break;
                }
            }
            if(idcanton>=0)
                cbCanton.setSelection(position, true);
        }catch (Exception e){
            Log.d("TAG_CLIENTEACT", "LlenarComboCantones(): " +e.getMessage());
        }
    }

    private void LlenarComboParroquias(Integer idcanton, Integer idparroquia){
        try{
            parroquias.clear();
            parroquias = Parroquia.getList(idcanton);
            ArrayAdapter<Parroquia> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, parroquias);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cbParroquia.setAdapter(adapter);
            int position = 0;
            for(int i=0; i<parroquias.size(); i++){
                if(parroquias.get(i).idparroquia.equals(idparroquia)){
                    position = i;
                    break;
                }
            }
            if(idparroquia>=0) {
                cbParroquia.setSelection(position, true);
                //band = false;
            }
        }catch (Exception e){
            Log.d("TAG_CLIENTEACT", "LlenarComboProvincias(): " +e.getMessage());
        }
    }
}
