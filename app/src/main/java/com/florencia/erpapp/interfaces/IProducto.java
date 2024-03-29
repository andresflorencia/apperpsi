package com.florencia.erpapp.interfaces;

import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface IProducto {
    @FormUrlEncoded
    @POST("wsproducto")
    Call<JsonObject> GetProductos(@Field("usuario") String user, @Field("clave") String clave,
                                  @Field("establecimientoid") Integer establecimiento,
                                  @Field("fechaconsulta") String fechaconsulta);

    @POST("confirmauditoria")
    Call<JsonObject> ConfirmAuditoria(@Body Map<String, Object> data);
}
