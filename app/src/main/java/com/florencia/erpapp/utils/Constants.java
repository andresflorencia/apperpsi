package com.florencia.erpapp.utils;


public class Constants {

    public static final String PING_Server = "ping -c 1 -w 2 ";
    //public static final String WEBSERVICE = "http://192.168.0.9/erpproduccion/index.php/wsmovil2/";
    //public static final String BASEURL = "192.168.0.9";
    public static final String WEBSERVICE = "http://192.168.100.146/erpproduccion/index.php/wsmovil2/";
    public static final String BASEURL = "192.168.100.146";
    //public static final String WEBSERVICE = "https://pruebas.sanisidrosa.com/index.php/wsmovil2/";
    //public static final String BASEURL = "sanisidrosa.com";
    public static final String HTTP = "http://";
    public static final String HTTPs = "https://";
    public static final String ENDPOINT = "/index.php/wsmovil2/";

    //Mensajes
    public static final String MSG_COMPROBAR_CONEXION_INTERNET= "Comprueba tu conexión a internet";
    public static final String MSG_USUARIO_CLAVE_INCORRECTO = "Usuario o contraseña incorrecta.";
    public static final String MSG_USUARIO_NO_ASIGNADO = "Usuario no asignado.";
    public static final String MSG_FALTAN_CAMPOS= "Faltan campos por completar.";
    public static final String MSG_SERVIDOR_NO_RESPONDE = "El servidor no responde. Intente nuevamente";
    public static final String MSG_DATOS_GUARDADOS="Datos guardados correctamente.";
    public static final String MSG_DATOS_NO_GUARDADOS="Ocurrió un error al guardar los datos.";
    public static final String MSG_PROCESO_COMPLETADO="Proceso de sincronización realizado con éxito.";
    public static final String MSG_PROCESO_NO_COMPLETADO="El proceso no se completó en su totalidad";
    public static final String LINEAS_FIRMA = "F: ____________________";
    public static final String FORMATO_FECHA_IMPRESION = "____/____/________  ____:____";

    //NOMBRES DE OPCIONES
    public static final String PUNTO_VENTA = "in/puntoventa";
    public static final String PEDIDO = "cc/gestiondepedidos";
    public static final String REGISTRO_CLIENTE = "pe/personas";
    public static final String RECEPCION_INVENTARIO = "in/recepcion";
    public static final String TRANSFERENCIA_INVENTARIO = "in/transferencia";
    public static final String PEDIDO_INVENTARIO = "in/pedido";

    public static final String CLAVE_SEGURIDAD = "T3cn0l0g14_";
    public static final String URL_DOWNLOAD_APK = "downloadapk";
    public static final String ACTION_INSTALL_COMPLETE = "com.florencia.erpapp.INSTALL_COMPLETE";


    //RUTA IMAGENES
    public static final String FOLDER_FILES = "ImageSI"; //DIRECTORIO PRINCIPAL
    public static final String FOLDER_IMAGES = "imagenes"; // CARPETA DONDE SE GUARDAN LAS IMAGENES
    public static final String PATH_IMAGES = FOLDER_FILES + FOLDER_IMAGES; //RUTA CARPETA
}
