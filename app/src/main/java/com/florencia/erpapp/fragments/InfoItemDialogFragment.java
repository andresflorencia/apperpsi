package com.florencia.erpapp.fragments;


import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.models.Lote;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.models.Regla;
import com.florencia.erpapp.services.GPSTracker;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class InfoItemDialogFragment extends AppCompatDialogFragment {

    private View view;
    private TextView txtNombre, txtInfoRight, txtInfoLeft, lblNumLote, lblFecVenc, lblStock, lblReglasPrecio,
            lblCant, lblValido, lblPrecio;
    private LinearLayout lyLotes, lyReglasPrecio;
    private ProgressBar pbCargando;
    private ImageButton btnCerrar;
    Producto producto = new Producto();
    public InfoItemDialogFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_info_item_dialog, container, false);

        txtNombre = view.findViewById(R.id.txtNombre);
        txtInfoRight = view.findViewById(R.id.txtInfoRight);
        txtInfoLeft = view.findViewById(R.id.txtInfoLeft);
        lblNumLote = view.findViewById(R.id.lblNumLote);
        lblFecVenc = view.findViewById(R.id.lblFecVenc);
        lblStock = view.findViewById(R.id.lblStock);
        lblReglasPrecio = view.findViewById(R.id.lblReglasPrecio);
        lblCant = view.findViewById(R.id.lblCant);
        lblValido = view.findViewById(R.id.lblValido);
        lblPrecio = view.findViewById(R.id.lblPrecio);
        lyLotes = view.findViewById(R.id.lyLotes);
        lyReglasPrecio = view.findViewById(R.id.lyReglasPrecio);
        pbCargando = view.findViewById(R.id.pbCargando);
        btnCerrar = view.findViewById(R.id.btnCerrar);

        if(!getArguments().isEmpty()) {
            int id = getArguments().getInt("idproducto",0);
            if(id>0)
                BuscarDatos(id);
        }

        btnCerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        return view;
    }

    private void BuscarDatos(int id) {
        try{
            Thread th = new Thread(){
                @Override
                public void run(){
                    pbCargando.setVisibility(View.VISIBLE);
                    producto = Producto.get(id, SQLite.usuario.sucursal.IdEstablecimiento);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(producto != null){
                                txtNombre.setText(producto.nombreproducto);
                                String textLeft = "", textRight = "";

                                textLeft = textLeft.concat("Código:\n")
                                    .concat("Tipo:\n")
                                    .concat("Clasificación:\n")
                                    .concat("PVP Normal:\n")
                                    .concat("IVA:\n")
                                    .concat(producto.unidadesporcaja > 0?"U/Caja:\n":"")
                                    .concat(producto.tipo.equalsIgnoreCase("S")?"":"Stock:\n")
                                    .concat(producto.lotes.size()>0?"\n\nLOTES:":"");

                                textRight = textRight.concat(producto.codigoproducto).concat("\n")
                                        .concat(producto.tipo.equalsIgnoreCase("S")?"SERVICIO":"PRODUCTO").concat("\n")
                                        .concat(producto.nombreclasificacion).concat("\n")
                                        .concat(Utils.FormatoMoneda(producto.getPrecioSugerido(),2).concat("\n"))
                                        .concat(producto.porcentajeiva>0?"Si":"No").concat("\n")
                                        .concat(producto.unidadesporcaja > 0?producto.unidadesporcaja.toString()+"\n":"")
                                        .concat(producto.tipo.equalsIgnoreCase("S")?"":producto.stock.toString());

                                txtInfoLeft.setText(textLeft);
                                txtInfoRight.setText(textRight);

                                if(producto.lotes.size()>0){
                                    for(Lote lote:producto.lotes) {
                                        lblNumLote.setText(lblNumLote.getText().toString().concat(lote.numerolote.equals("")?"Sin Lote":lote.numerolote).concat("\n"));
                                        lblFecVenc.setText(lblFecVenc.getText().toString().concat(lote.fechavencimiento).concat("\n"));
                                        lblStock.setText(lblStock.getText().toString().concat(Utils.RoundDecimal(lote.stock,2).toString()).concat("\n"));
                                    }
                                }else
                                    lyLotes.setVisibility(View.GONE);

                                if(producto.reglas.size()>0){
                                    for(Regla regla:producto.reglas) {
                                        lblCant.setText(lblCant.getText().toString().concat(Utils.RoundDecimal(regla.cantidad,2).toString()).concat("\n"));
                                        lblValido.setText(lblValido.getText().toString().concat(regla.fechamaxima).concat("\n"));
                                        lblPrecio.setText(lblPrecio.getText().toString().concat(Utils.FormatoMoneda(regla.precio,2)).concat("\n"));
                                    }
                                }else {
                                    lyReglasPrecio.setVisibility(View.GONE);
                                    lblReglasPrecio.setVisibility(View.GONE);
                                }
                            }
                            pbCargando.setVisibility(View.GONE);
                        }
                    });
                }
            };
            th.start();
        }catch (Exception e){
            pbCargando.setVisibility(View.GONE);
            Log.d("TAGPRODUCTOFRAGMENT", e.getMessage());
        }
    }
}
