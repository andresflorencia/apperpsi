package com.florencia.erpapp.interfaces;

import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface IComprobante {

    @POST("loadcomprobantes")
    Call<JsonObject> LoadComprobantes(@Body Map<String, Object> comprobantes);

    @POST("loadpedidos")
    Call<JsonObject> LoadPedidos(@Body Map<String, Object> pedidos);

    @FormUrlEncoded
    @POST("transaccionesinventario")
    Call<JsonObject> getTransferencias(@Field("usuario") String user, @Field("clave") String clave,
                                       @Field("establecimientoid") Integer establecimientoid,
                                       @Field("poraceptar") Integer poraceptar);

    @FormUrlEncoded
    @POST("detalletransferencia")
    Call<JsonObject> getDetalleTransferencia(@Field("idtransaccion") Integer idtransaccion);

    @POST("saverecepcion")
    Call<JsonObject> saveRecepcion(@Body Map<String, Object> transacciones);

    @FormUrlEncoded
    @POST("getsucursalestransferencia")
    Call<JsonObject> getEstablecimientos(@Field("usuario") String user, @Field("clave") String clave,
                                         @Field("establecimientoid") Integer establecimientoid);

    @POST("savetransferencia")
    Call<JsonObject> saveTransferencia(@Body Map<String, Object> transacciones);

    @POST("loadpedidosinv")
    Call<JsonObject> LoadPedidosInv(@Body Map<String, Object> pedidos);

    @POST("loaddepositos")
    Call<JsonObject> LoadDepositos(@Body Map<String, Object> depositos);

    @POST("estadoresponsabletransf")
    Call<JsonObject> estadoResponsableTransf(@Body Map<String, Object> transferencia);
}
