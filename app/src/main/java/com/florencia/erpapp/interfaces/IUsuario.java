package com.florencia.erpapp.interfaces;

import com.florencia.erpapp.models.Usuario;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface IUsuario {

    @FormUrlEncoded
    @POST("registramovil")
    Call<JsonObject> IniciarSesion(@Field("usuario") String user, @Field("clave") String clave,
                                    @Field("phone") String phone);

    @POST("getlastversion")
    Call<JsonObject> getLastVersion();

    @POST("verificaconexion")
    Call<String> verificaconexion();

    @GET
    Call<ResponseBody> downloadApk(@Url String url);

    @POST("loadubicacion")
    Call<JsonObject> loadUbicacion(@Body Map<String,Object> ubicaciones);

    @FormUrlEncoded
    @POST("getpermisos")
    Call<JsonObject> getPermisos(@Field("usuario") String user, @Field("clave") String clave);
}