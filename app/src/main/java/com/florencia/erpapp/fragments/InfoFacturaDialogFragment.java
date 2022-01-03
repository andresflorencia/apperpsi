package com.florencia.erpapp.fragments;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.florencia.erpapp.BuildConfig;
import com.florencia.erpapp.R;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.DetalleComprobante;
import com.florencia.erpapp.models.DetallePedido;
import com.florencia.erpapp.models.DetalleRetencion;
import com.florencia.erpapp.models.Ingreso;
import com.florencia.erpapp.models.Pedido;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.models.Usuario;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.internal.Util;

public class InfoFacturaDialogFragment extends AppCompatDialogFragment {

    private View view;
    private TextView txtNumFactura, txtInfoRight, txtInfoLeft, lblCant, lblDetalle, lblPUnit, lblSubtotal,
            lblTotalesLeft, lblTotalesRight, lblLeyenda, lblEmpresa, lblDesc;
    private ProgressBar pbCargando;
    private ImageButton btnCerrar, btnShareW, btnShare;
    LinearLayout lyLotes, lyReglasPrecio, lyContent;
    CardView cvContent;
    ImageView imgFoto;
    Comprobante comprobante = new Comprobante();
    Pedido pedido = new Pedido();
    Ingreso ingreso = new Ingreso();
    String ExternalDirectory = "";
    String tipotransaccion = "";
    Activity activity;

    public static String TAG = "TAGFACTURAFRAGMENT";

    public InfoFacturaDialogFragment(Activity activity) {
        this.activity = activity;
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
        lyLotes = view.findViewById(R.id.lyLotes);
        lyReglasPrecio = view.findViewById(R.id.lyReglasPrecio);
        imgFoto = view.findViewById(R.id.imgFoto);
        lyContent = view.findViewById(R.id.lyContent);
        cvContent = view.findViewById(R.id.cvContent);
        btnShareW = view.findViewById(R.id.btnShareW);
        btnShare = view.findViewById(R.id.btnShare);
        lblLeyenda = view.findViewById(R.id.txtLeyenda);
        lblEmpresa = view.findViewById(R.id.lblEmpresa);
        lblDesc = view.findViewById(R.id.lblDesc);

        File miFile = new File(activity.getExternalMediaDirs()[0], Constants.FOLDER_FILES);
        if (!miFile.exists())
            miFile.mkdirs();

        ExternalDirectory = activity.getExternalMediaDirs()[0] + File.separator + Constants.FOLDER_FILES;

        String datosEmpresa = "";
        datosEmpresa = SQLite.usuario.sucursal.NombreComercial.concat("\n")
                .concat(SQLite.usuario.sucursal.RazonSocial).concat("\n")
                .concat("RUC: ".concat(SQLite.usuario.sucursal.RUC)).concat("\n")
                .concat(SQLite.usuario.sucursal.Direcion);

        lblEmpresa.setText(datosEmpresa);

        if (!getArguments().isEmpty()) {
            int id = getArguments().getInt("id", 0);
            tipotransaccion = getArguments().getString("tipobusqueda", "");
            if (id > 0) {
                switch (tipotransaccion) {
                    case "01":
                    case "PR":
                        lyLotes.setVisibility(View.VISIBLE);
                        lyReglasPrecio.setVisibility(View.VISIBLE);
                        BuscarDatosFactura(id);
                        break;
                    case "PC":
                        lyLotes.setVisibility(View.VISIBLE);
                        lyReglasPrecio.setVisibility(View.VISIBLE);
                        BuscarDatosPedido(id);
                        break;
                    case "DE":
                        imgFoto.setVisibility(View.VISIBLE);
                        BuscarDatosDeposito(id);
                        break;
                }
            }
        }

        btnCerrar.setOnClickListener(v -> getDialog().dismiss());
        btnShareW.setOnClickListener(v -> GenerarImagen(false));
        btnShare.setOnClickListener(v -> GenerarImagen(true));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            lblLeyenda.setText(
                    Html.fromHtml(
                            "<strong>Usuario: </strong> " + SQLite.usuario.RazonSocial +
                                    "           <strong>Generado: </strong> " + Utils.getDateFormat("yyyy-MM-dd HH:mm:ss")
                            , Html.FROM_HTML_MODE_COMPACT));
        } else {
            lblLeyenda.setText(
                    Html.fromHtml(
                            "<strong>Usuario: </strong> " + SQLite.usuario.RazonSocial +
                                    "           <strong>Generado: </strong> " + Utils.getDateFormat("yyyy-MM-dd HH:mm:ss")));
        }

        if (getDialog().getWindow() != null)
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return view;
    }

    private void BuscarDatosFactura(int id) {
        try {
            Thread th = new Thread() {
                @Override
                public void run() {
                    pbCargando.setVisibility(View.VISIBLE);
                    comprobante = Comprobante.get(id, true);
                    getActivity().runOnUiThread(
                            () -> {
                                if (comprobante != null) {
                                    txtNumFactura.setText(comprobante.codigotransaccion);
                                    String textLeft = "", textRight = "";

                                    textLeft = textLeft.concat("Cliente:\n")
                                            .concat("CI/RUC:\n")
                                            .concat((comprobante.tipotransaccion.equals("PR") ? "Proforma" : "Recibo") + " #:\n")
                                            .concat("Fecha:\n")
                                            .concat("Estado:\n")
                                            .concat("Forma Pago:\n")
                                            .concat("ID:");

                                    textRight = textRight.concat(comprobante.cliente.razonsocial).concat("\n")
                                            .concat(comprobante.cliente.nip).concat("\n")
                                            .concat(comprobante.codigotransaccion).concat("\n")
                                            .concat(comprobante.fechacelular).concat("\n")
                                            .concat(comprobante.estado == 0 && comprobante.codigosistema == 0 ? "No sincronizado" : "Sincronizado").concat("\n")
                                            .concat(comprobante.formapago == 0 ? "Crédito" : "Efectivo").concat("\n")
                                            .concat(comprobante.idcomprobante.toString()).concat("\n");

                                    txtInfoLeft.setText(textLeft);
                                    txtInfoRight.setText(textRight);

                                    //lblDetalle.setBackground(getResources().getDrawable(R.drawable.bg_btn_gps));
                                    Double desc0 = 0d, desc12 = 0d;
                                    for (DetalleComprobante detalle : comprobante.detalle) {
                                        lblCant.setText(lblCant.getText().toString().concat(detalle.cantidad.toString()).concat("\n"));
                                        lblDetalle.setText(lblDetalle.getText().toString().concat(
                                                (detalle.producto.porcentajeiva > 0 ? "** " : "")
                                                        + (detalle.producto.nombreproducto.length() > 25
                                                        ? detalle.producto.nombreproducto.substring(0, 20) + "..." + detalle.producto.nombreproducto.substring(detalle.producto.nombreproducto.length() - 5)
                                                        : detalle.producto.nombreproducto)).concat("\n"));
                                        lblPUnit.setText(lblPUnit.getText().toString().concat(Utils.FormatoMoneda(detalle.precio, 2)).concat("\n"));
                                        lblDesc.setText(lblDesc.getText().toString().concat(Utils.FormatoMoneda(detalle.descuento, 2)).concat("\n"));
                                        lblSubtotal.setText(lblSubtotal.getText().toString().concat(Utils.FormatoMoneda(detalle.Subtotal() - detalle.descuento, 2)).concat("\n"));
                                        if(detalle.producto.porcentajeiva>0)
                                            desc12 += detalle.descuento;
                                        else
                                            desc0 += detalle.descuento;
                                    }

                                    //lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL:\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL 0% (+):\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL 12% (+):\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("DESCUENTO (-):\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("IVA 12% (+):\n\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("TOTAL:\n"));
                                    if(comprobante.retencion != null && comprobante.retencion.detalle.size()>0){
                                        lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("RETENCION (-):\n"));
                                        lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("A PAGAR:\n"));
                                    }

                                    //lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.subtotal + comprobante.subtotaliva, 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.subtotal, 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.subtotaliva, 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.descuento, 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda((comprobante.total - comprobante.subtotal - comprobante.subtotaliva + comprobante.descuento), 2).concat("\n\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.total, 2).concat("\n")));
                                    if(comprobante.retencion != null && comprobante.retencion.detalle.size()>0){
                                        Double totret = 0d;
                                        for(DetalleRetencion det:comprobante.retencion.detalle)
                                            totret += det.valorretenido + det.valorretenidoiva;
                                        lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(totret, 2).concat("\n")));
                                        lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(comprobante.total - totret, 2).concat("\n")));
                                    }

                                    lblLeyenda.setVisibility(View.VISIBLE);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        lblLeyenda.setText(Html.fromHtml((comprobante.tipotransaccion.equals("PR") ? getResources().getString(R.string.leyendaProforma) : getResources().getString(R.string.leyendaFactura))
                                                        + "<br><strong>Vendedor: </strong> " + SQLite.usuario.RazonSocial + "           <strong>Generado: </strong> " + Utils.getDateFormat("yyyy-MM-dd HH:mm:ss"),
                                                Html.FROM_HTML_MODE_COMPACT));
                                    } else {
                                        lblLeyenda.setText(Html.fromHtml((comprobante.tipotransaccion.equals("PR") ? getResources().getString(R.string.leyendaProforma) : getResources().getString(R.string.leyendaFactura))
                                                + "<br><strong>Vendedor: </strong> " + SQLite.usuario.RazonSocial + "           <strong>Generado: </strong> " + Utils.getDateFormat("yyyy-MM-dd HH:mm:ss")
                                        ));
                                    }
                                    pbCargando.setVisibility(View.GONE);
                                }
                            }
                    );
                }
            };
            th.start();
        } catch (Exception e) {
            pbCargando.setVisibility(View.GONE);
            Log.d(TAG, e.getMessage());
        }
    }

    private void BuscarDatosPedido(int id) {
        try {
            Thread th = new Thread() {
                @Override
                public void run() {
                    pbCargando.setVisibility(View.VISIBLE);
                    pedido = Pedido.get(id);
                    getActivity().runOnUiThread(() -> {
                                if (pedido != null) {
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
                                            .concat(pedido.estado >= 0 && pedido.codigosistema == 0 ? "No sincronizado" : "Sincronizado").concat("\n")
                                            .concat(pedido.observacion);

                                    txtInfoLeft.setText(textLeft);
                                    txtInfoRight.setText(textRight);

                                    for (DetallePedido detalle : pedido.detalle) {
                                        lblCant.setText(lblCant.getText().toString().concat(detalle.cantidad.toString()).concat("\n"));
                                        lblDetalle.setText(lblDetalle.getText().toString().concat(
                                                (detalle.producto.porcentajeiva > 0 ? "** " : "")
                                                        + (detalle.producto.nombreproducto.length() > 25
                                                        ? detalle.producto.nombreproducto.substring(0, 20) + "..." + detalle.producto.nombreproducto.substring(detalle.producto.nombreproducto.length() - 5)
                                                        : detalle.producto.nombreproducto)).concat("\n"));
                                        lblPUnit.setText(lblPUnit.getText().toString().concat(Utils.FormatoMoneda(detalle.precio, 2)).concat("\n"));
                                        //lblSubtotal.setText(lblSubtotal.getText().toString().concat(Utils.FormatoMoneda(detalle.Subtotal(), 2)).concat("\n"));
                                        lblDesc.setText(lblDesc.getText().toString().concat(Utils.FormatoMoneda(detalle.descuento, 2)).concat("\n"));
                                        lblSubtotal.setText(lblSubtotal.getText().toString().concat(Utils.FormatoMoneda(detalle.Subtotal() - detalle.descuento, 2)).concat("\n"));
                                    }


                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL:\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL 0% (+):\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("SUBTOTAL 12% (+):\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("DESCUENTO (-):\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("IVA 12% (+):\n"));
                                    lblTotalesLeft.setText(lblTotalesLeft.getText().toString().concat("TOTAL:\n"));

                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(pedido.subtotal + pedido.subtotaliva, 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(pedido.subtotal, 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(pedido.subtotaliva, 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(pedido.descuento, 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda((pedido.total - pedido.subtotal - pedido.subtotaliva + pedido.descuento), 2).concat("\n")));
                                    lblTotalesRight.setText(lblTotalesRight.getText().toString().concat(Utils.FormatoMoneda(pedido.total, 2).concat("\n")));
                                }
                                pbCargando.setVisibility(View.GONE);
                            }
                    );
                }
            };
            th.start();
        } catch (Exception e) {
            pbCargando.setVisibility(View.GONE);
            Log.d(TAG, e.getMessage());
        }
    }

    private void BuscarDatosDeposito(int id) {
        try {
            Thread th = new Thread() {
                @Override
                public void run() {
                    pbCargando.setVisibility(View.VISIBLE);
                    ingreso = Ingreso.get(id);
                    getActivity().runOnUiThread(() -> {
                                if (ingreso != null) {
                                    txtNumFactura.setText(ingreso.secuencialdocumento);
                                    String textLeft = "", textRight = "";

                                    textLeft = textLeft.concat("Nombres:\n")
                                            .concat("CI/RUC:\n")
                                            .concat("Doc. #:\n")
                                            .concat("F. Ventas:\n")
                                            .concat("F. Doc.:\n")
                                            .concat("Tipo:\n")
                                            .concat("Entidad:\n")
                                            .concat("# Comprob.:\n")
                                            .concat("Monto:\n")
                                            .concat("F. Registro:\n")
                                            .concat("Estado:\n")
                                            .concat("Concepto:\n");

                                    textRight = textRight.concat(ingreso.detalle.get(0).razonsocialtitular).concat("\n")
                                            .concat(ingreso.detalle.get(0).niptitular).concat("\n")
                                            .concat(ingreso.secuencialdocumento).concat("\n")
                                            .concat(ingreso.fechadiario).concat("\n")
                                            .concat(ingreso.detalle.get(0).fechadocumento).concat("\n")
                                            .concat(ingreso.detalle.get(0).tipodocumento == 4 ? "Depósito" : "Transferencia").concat("\n")
                                            .concat(ingreso.detalle.get(0).entidadfinanciera.nombrecatalogo + (ingreso.detalle.get(0).tipodecuenta.equals("A") ? " - Ahorro" : " - Corriente")).concat("\n")
                                            .concat(ingreso.detalle.get(0).numerodocumentoreferencia).concat("\n")
                                            .concat(Utils.FormatoMoneda(ingreso.totalingreso, 2)).concat("\n")
                                            .concat(ingreso.fechacelular).concat("\n")
                                            .concat(ingreso.estado >= 0 && ingreso.codigosistema == 0 ? "No sincronizado" : "Sincronizado").concat("\n")
                                            .concat(ingreso.observacion);

                                    txtInfoLeft.setText(textLeft);
                                    txtInfoRight.setText(textRight);

                                    if (ingreso.fotos != null && ingreso.fotos.size() > 0) {
                                        for (int i = 0; i < ingreso.fotos.size(); i++) {
                                            try {
                                                File miFile = new File(ExternalDirectory, ingreso.fotos.get(i).name);
                                                Uri path = Uri.fromFile(miFile);
                                                ingreso.fotos.get(i).bitmap = MediaStore.Images.Media.getBitmap(
                                                        activity.getContentResolver(),
                                                        path);
                                            } catch (IOException e) {
                                                Log.d(TAG, "NotFound(): " + e.getMessage());
                                            }
                                        }
                                        if (ingreso.fotos.get(0).bitmap != null)
                                            imgFoto.setImageBitmap(ingreso.fotos.get(0).bitmap);
                                        else if (ingreso.fotos.get(0).uriFoto != null)
                                            imgFoto.setImageURI(ingreso.fotos.get(0).uriFoto);
                                    }
                                }
                                pbCargando.setVisibility(View.GONE);
                            }
                    );
                }
            };
            th.start();
        } catch (Exception e) {
            pbCargando.setVisibility(View.GONE);
            Log.d(TAG, e.getMessage());
        }
    }

    private void GenerarImagen(boolean other) {

        try {
            btnCerrar.setVisibility(View.INVISIBLE);
            btnShare.setVisibility(View.GONE);
            btnShareW.setVisibility(View.GONE);
            cvContent.setDrawingCacheEnabled(true);
            cvContent.buildDrawingCache(true);
            Bitmap bitmap = Bitmap.createBitmap(cvContent.getDrawingCache());
            cvContent.setDrawingCacheEnabled(false);

            String nameimg = "";
            String numberphone = "";
            switch (tipotransaccion) {
                case "01":
                    nameimg = "FAC-" + comprobante.codigotransaccion + ".png";
                    if (comprobante.cliente.fono1.length() > 0)
                        numberphone = comprobante.cliente.fono1;
                    else if (comprobante.cliente.fono2.length() > 0)
                        numberphone = comprobante.cliente.fono2;
                    break;
                case "PC":
                    nameimg = "PED-" + pedido.secuencialpedido + ".png";
                    if (pedido.cliente.fono1.length() > 0)
                        numberphone = pedido.cliente.fono1;
                    else if (pedido.cliente.fono2.length() > 0)
                        numberphone = pedido.cliente.fono2;
                    break;
                case "DE":
                    nameimg = "DEP-" + ingreso.secuencialdocumento + ".png";
                    break;
            }

            File imageFile = new File(ExternalDirectory + File.separator + nameimg);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            Utils.showMessage(getContext(), "Generando imagen, espere un momento...");

            String ni = nameimg;
            final String number = numberphone;
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                btnCerrar.setVisibility(View.VISIBLE);
                btnShareW.setVisibility(View.VISIBLE);
                btnShare.setVisibility(View.VISIBLE);
                if (other)
                    sendImage(imageFile);
                else
                    sendImageWhatsApp(number, ni);
            }, 3000);

        } catch (Throwable e) {
            Utils.showMessage(getContext(), "ERROR al generar imagen .png");
            Log.d("TAGIMAGEN", e.getMessage());
        }
    }

    //PERMITE COMPARTIR IMAGEN VIA WHATSAPP
    private void sendImageWhatsApp(String phoneNumber, String nombreImagen) {
        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(ExternalDirectory + File.separator + nombreImagen));
            intent.putExtra(Intent.EXTRA_TEXT,
                    tipotransaccion.equals("01") ? getResources().getString(R.string.leyendaFactura2) : "Comprobante generado desde SI Móvil");
            intent.putExtra("jid", phoneNumber + "@s.whatsapp.net"); //numero telefonico sin prefijo "+"!
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Utils.showMessage(getContext(), "Whatsapp no esta instalado.");
            Log.d("TAGIMAGE", ex.getMessage());
        }
    }

    //PERMITE COMPARTIR IMAGEN MEDIANTE APLICACIONES MULTIMEDIA
    private void sendImage(File fileImage) {
        try {
            String PACKAGE_NAME = BuildConfig.APPLICATION_ID + ".services.GenericFileProvider";

            Uri contentUri = FileProvider.getUriForFile(getContext(), PACKAGE_NAME, fileImage);

            if (contentUri != null) {

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, getContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                        tipotransaccion.equals("01") ? getResources().getString(R.string.leyendaFactura2) : "Comprobante generado desde SI Movil");
                startActivity(Intent.createChooser(shareIntent, "Elige una aplicación:"));

            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }
}
