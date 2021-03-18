package com.florencia.erpapp.fragments;


import android.os.Bundle;

import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.DetalleComprobante;
import com.florencia.erpapp.models.DetallePedido;
import com.florencia.erpapp.models.Pedido;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;

public class InfoFacturaDialogFragment extends AppCompatDialogFragment {

    private View view;
    private TextView txtNumFactura, txtInfoRight, txtInfoLeft, lblCant, lblDetalle, lblPUnit, lblSubtotal,
            lblTotalesLeft, lblTotalesRight;
    private ProgressBar pbCargando;
    private ImageButton btnCerrar;
    Comprobante comprobante = new Comprobante();
    Pedido pedido = new Pedido();

    public static String TAG = "TAGFACTURAFRAGMENT";
    public InfoFacturaDialogFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_info_factura_dialog, container, false);

        txtNumFactura = view.findViewById(R.id.txtNumFactura);
        txtInfoLeft = view.findViewById(R.id.txtInfoLeft);
        txtInfoRight = view.findViewById(R.id.txtInfoRight);
        lblCant = view.findViewById(R.id.lblCant);
        lblDetalle = view.findViewById(R.id.lblDetalle);
        lblPUnit = view.findViewById(R.id.lblPUnit);
        lblSubtotal = view.findViewById(R.id.lblSubtotal);
        lblTotalesLeft = view.findViewById(R.id.lblTotalesLeft);
        lblTotalesRight = view.findViewById(R.id.lblTotalesRight);
        pbCargando = view.findViewById(R.id.pbCargando);
        btnCerrar = view.findViewById(R.id.btnCerrar);

        if(!getArguments().isEmpty()) {
            int id = getArguments().getInt("id",0);
            String tipo = getArguments().getString("tipobusqueda","");
            if(id>0) {
                switch (tipo){
                    case "01":
                        BuscarDatosFactura(id);
                        break;
                    case "PC":
                        BuscarDatosPedido(id);
                        break;
                }
            }
        }

        btnCerrar.setOnClickListener(v -> getDialog().dismiss());
        return view;
    }

    private void BuscarDatosFactura(int id) {
        try{
            Thread th = new Thread(){
                @Override
                public void run(){
                    pbCargando.setVisibility(View.VISIBLE);
                    comprobante = Comprobante.get(id);
                    getActivity().runOnUiThread(() -> {
                            if(comprobante != null){
                                txtNumFactura.setText(comprobante.codigotransaccion);
                                String textLeft = "", textRight = "";

                                textLeft = textLeft.concat("Cliente:\n")
                                        .concat("CI/RUC:\n")
                                        .concat("Factura #:\n")
                                        .concat("Fecha:\n")
                                        .concat("Núm. Aut.:\n\n")
                                        .concat("Estado:");

                                textRight = textRight.concat(comprobante.cliente.razonsocial).concat("\n")
                                        .concat(comprobante.cliente.nip).concat("\n")
                                        .concat(comprobante.codigotransaccion).concat("\n")
                                        .concat(comprobante.fechadocumento).concat("\n")
                                        .concat(comprobante.claveacceso).concat("\n")
                                        .concat(comprobante.estado == 0 && comprobante.codigosistema == 0?"Sincronizado":"No sincronizado").concat("\n");

                                txtInfoLeft.setText(textLeft);
                                txtInfoRight.setText(textRight);

                                for(DetalleComprobante detalle:comprobante.detalle) {
                                    lblCant.setText(lblCant.getText().toString().concat(detalle.cantidad.toString()).concat("\n"));
                                    lblDetalle.setText(lblDetalle.getText().toString().concat((detalle.producto.porcentajeiva>0?"** ":"")+ detalle.producto.nombreproducto).concat("\n"));
                                    lblPUnit.setText(lblPUnit.getText().toString().concat(Utils.FormatoMoneda(detalle.precio,2)).concat("\n"));
                                    lblSubtotal.setText(lblSubtotal.getText().toString().concat(Utils.FormatoMoneda(detalle.Subtotal(),2)).concat("\n"));
                                }

                                lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL 0%\n"));
                                lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL 12%\n"));
                                lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("IVA 12%\n"));
                                lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("TOTAL\n"));

                                lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.subtotal,2).concat("\n")));
                                lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.subtotaliva,2).concat("\n")));
                                lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda((comprobante.total -comprobante.subtotal - comprobante.subtotaliva),2).concat("\n")));
                                lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.total,2).concat("\n")));
                            }
                            pbCargando.setVisibility(View.GONE);
                        }
                    );
                }
            };
            th.start();
        }catch (Exception e){
            pbCargando.setVisibility(View.GONE);
            Log.d(TAG, e.getMessage());
        }
    }

    private void BuscarDatosPedido(int id) {
        try{
            Thread th = new Thread(){
                @Override
                public void run(){
                    pbCargando.setVisibility(View.VISIBLE);
                    pedido = Pedido.get(id);
                    getActivity().runOnUiThread(() -> {
                                if(pedido != null){
                                    txtNumFactura.setText(pedido.secuencialpedido);
                                    String textLeft = "", textRight = "";

                                    textLeft = textLeft.concat("Cliente:\n")
                                            .concat("CI/RUC:\n")
                                            .concat("Pedido #:\n")
                                            .concat("Cód. Sist.:\n")
                                            .concat("F. Reg:\n")
                                            .concat("F. Pedido:\n")
                                            .concat("Estado:\n")
                                            .concat("Observ.:\n");

                                    textRight = textRight.concat(pedido.cliente.razonsocial).concat("\n")
                                            .concat(pedido.cliente.nip).concat("\n")
                                            .concat(pedido.secuencialpedido).concat("\n")
                                            .concat(pedido.secuencialsistema).concat("\n")
                                            .concat(pedido.fechacelular).concat("\n")
                                            .concat(pedido.fechapedido).concat("\n")
                                            .concat(pedido.estado == 0 && pedido.codigosistema == 0?"Sincronizado":"No sincronizado").concat("\n")
                                            .concat(pedido.observacion);

                                    txtInfoLeft.setText(textLeft);
                                    txtInfoRight.setText(textRight);

                                    for(DetallePedido detalle:pedido.detalle) {
                                        lblCant.setText(lblCant.getText().toString().concat(detalle.cantidad.toString()).concat("\n"));
                                        lblDetalle.setText(lblDetalle.getText().toString().concat((detalle.producto.porcentajeiva>0?"** ":"")+ detalle.producto.nombreproducto).concat("\n"));
                                        lblPUnit.setText(lblPUnit.getText().toString().concat(Utils.FormatoMoneda(detalle.precio,2)).concat("\n"));
                                        lblSubtotal.setText(lblSubtotal.getText().toString().concat(Utils.FormatoMoneda(detalle.Subtotal(),2)).concat("\n"));
                                    }

                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL 0%\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL 12%\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("IVA 12%\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("TOTAL\n"));

                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(pedido.subtotal,2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(pedido.subtotaliva,2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda((pedido.total - pedido.subtotal - pedido.subtotaliva),2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(pedido.total,2).concat("\n")));
                                }
                                pbCargando.setVisibility(View.GONE);
                            }
                    );
                }
            };
            th.start();
        }catch (Exception e){
            pbCargando.setVisibility(View.GONE);
            Log.d(TAG, e.getMessage());
        }
    }
}
