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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.models.Lote;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.models.Regla;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class InfoItemDialogFragment extends AppCompatDialogFragment {

    private View view;
    private TextView txtNombre, txtInfo;
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

        txtNombre = (TextView)view.findViewById(R.id.txtNombre);
        txtInfo = (TextView)view.findViewById(R.id.txtInfo);
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
                                String text = "";

                                text = text.concat("<strong>Código: </strong> ").concat(producto.codigoproducto).concat("<br>")
                                    .concat("<strong>Tipo:</strong> "+ (producto.tipo.equalsIgnoreCase("S")?"SERVICIO":"PRODUCTO").concat("<br>"))
                                    .concat("<strong>Clasificación: </strong> ").concat(producto.nombreclasificacion).concat("<br>")
                                    .concat("<strong>Precio Referencia: </strong> ").concat(Utils.FormatoMoneda(producto.pvp,2).concat("<br>"))
                                    .concat(producto.unidadesporcaja > 0?"<strong>U/Caja:</strong> ".concat(producto.unidadesporcaja.toString()):"").concat("<br>")
                                    .concat(producto.tipo.equalsIgnoreCase("S")?"":"<strong>Stock: </strong> ".concat(producto.stock.toString()).concat("<br>"));

                                if(producto.lotes.size()>0){
                                    text += "<br><h5>Lotes</h5><ol>";
                                    for(Lote lote:producto.lotes)
                                        text = text.concat("<li><strong>N°:</strong> " + (lote.numerolote.equals("")?"Sin Lote":lote.numerolote) + " <strong> Venc.: </strong>" + lote.fechavencimiento + " <strong> Stock: </strong>" + lote.stock);
                                    text += "</ol>";
                                }
                                if(producto.reglas.size()>0){
                                    text += "<br><h5>Regla de Precios</h5><ol>";
                                    for(Regla regla:producto.reglas)
                                        text = text.concat("<li><strong>Cant ≥ </strong> " + regla.cantidad + " <strong> PVP: </strong>" + Utils.FormatoMoneda(regla.precio,2) + " <strong> Fec. Max. </strong>" + regla.fechamaxima);
                                    text += "</ol>";
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    txtInfo.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
                                else
                                    txtInfo.setText(Html.fromHtml(text));
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
