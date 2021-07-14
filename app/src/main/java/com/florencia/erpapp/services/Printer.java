package com.florencia.erpapp.services;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.florencia.erpapp.utils.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;


public class Printer {
    byte FONT_TYPE;
    public static BluetoothSocket btsocket;
    private static OutputStream outputStream;

    public Printer(Activity activity) {
        if (btsocket != null) {
            OutputStream opstream = null;
            try {
                opstream = btsocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = opstream;
            try {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                outputStream = btsocket.getOutputStream();
                byte[] printformat = new byte[]{0x1B, 0x21, 0x03};
                outputStream.write(printformat);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void flush() throws IOException {
        outputStream.flush();
    }

    public void printCustom(String msg, int size, int align) throws IOException {
        //Print config "mode"
        byte[] cc = new byte[]{0x1B, 0x21, 0x03};  // 0- normal size text
        //byte[] cc1 = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
        byte[] bb = new byte[]{0x1B, 0x21, 0x08};  // 1- only bold text
        byte[] bb2 = new byte[]{0x1B, 0x21, 0x20}; // 2- bold with medium text
        byte[] bb3 = new byte[]{0x1B, 0x21, 0x10}; // 3- bold with large text
        switch (size) {
            case 0:
                outputStream.write(cc);
                break;
            case 1:
                outputStream.write(bb);
                break;
            case 2:
                outputStream.write(bb2);
                break;
            case 3:
                outputStream.write(bb3);
                break;
        }

        switch (align) {
            case 0:
                //left align
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                break;
            case 1:
                //center align
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                break;
            case 2:
                //right align
                outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                break;
        }
        outputStream.write(stringABytes(msg));
        outputStream.write(PrinterCommands.LF);
    }

    public void printCustom(byte[] msg, int size, int align) throws IOException {
        //Print config "mode"
        byte[] cc = new byte[]{0x1B, 0x21, 0x03};  // 0- normal size text
        //byte[] cc1 = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
        byte[] bb = new byte[]{0x1B, 0x21, 0x08};  // 1- only bold text
        byte[] bb2 = new byte[]{0x1B, 0x21, 0x20}; // 2- bold with medium text
        byte[] bb3 = new byte[]{0x1B, 0x21, 0x10}; // 3- bold with large text
        switch (size) {
            case 0:
                outputStream.write(cc);
                break;
            case 1:
                outputStream.write(bb);
                break;
            case 2:
                outputStream.write(bb2);
                break;
            case 3:
                outputStream.write(bb3);
                break;
        }

        switch (align) {
            case 0:
                //left align
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                break;
            case 1:
                //center align
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                break;
            case 2:
                //right align
                outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                break;
        }
        outputStream.write(msg);
        outputStream.write(PrinterCommands.LF);
    }

    public void print(String msg, int size, int align) throws IOException {
        //Print config "mode"
        byte[] cc = new byte[]{0x1B, 0x21, 0x03};  // 0- normal size text
        //byte[] cc1 = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
        byte[] bb = new byte[]{0x1B, 0x21, 0x08};  // 1- only bold text
        byte[] bb2 = new byte[]{0x1B, 0x21, 0x20}; // 2- bold with medium text
        byte[] bb3 = new byte[]{0x1B, 0x21, 0x10}; // 3- bold with large text
        switch (size) {
            case 0:
                outputStream.write(cc);
                break;
            case 1:
                outputStream.write(bb);
                break;
            case 2:
                outputStream.write(bb2);
                break;
            case 3:
                outputStream.write(bb3);
                break;
        }
        switch (align) {
            case 0:
                //left align
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                break;
            case 1:
                //center align
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                break;
            case 2:
                //right align
                outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                break;
        }
        outputStream.write(stringABytes(msg));
    }

    //print photo
    public void printPhoto(Activity activity, int img) throws IOException {
        Bitmap bmp = BitmapFactory.decodeResource(activity.getResources(),
                img);
        if (bmp != null) {
            byte[] command = Utils.decodeBitmap(bmp);
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
            printText(command);
        } else {
            Log.e("Print Photo error", "the file isn't exists");
        }
    }

    //print unicode
    public void printUnicode() throws IOException {
        outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
        printText(Utils.UNICODE_TEXT);
    }


    //print new line
    public void printNewLine() throws IOException {
        outputStream.write(PrinterCommands.FEED_LINE);
    }

    public static void resetPrint() {
        try {
            outputStream.write(PrinterCommands.ESC_FONT_COLOR_DEFAULT);
            outputStream.write(PrinterCommands.FS_FONT_ALIGN);
            outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
            outputStream.write(PrinterCommands.ESC_CANCEL_BOLD);
            outputStream.write(PrinterCommands.LF);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //print text
    public boolean printText(String msg) throws IOException {
        outputStream.write(msg.getBytes());
        return false;
    }

    //print byte[]
    public boolean printText(byte[] msg) throws IOException {
        outputStream.write(msg);
        printNewLine();
        return true;
    }

    public void printArray(Data[] pV, int size, int align) throws IOException {
        String str = "";
        for (Data item : pV) {
            str = str.concat(" " + item.toString());
        }
        this.printCustom(str, size, align);
    }


    public String leftRightAlign(String str1, String str2) {
        String ans = str1 + str2;
        if (ans.length() < 31) {
            int n = (31 - str1.length() + str2.length());
            ans = str1 + new String(new char[n]).replace("\0", " ") + str2;
        }
        return ans;
    }

    public String leftRightAlign2(String str1) {
        String ans = str1;
        if (ans.length() < 31) {
            int n = (31 - str1.length());
            ans = str1 + new String(new char[n]).replace("\0", " ");
        }
        return ans;
    }

    public String[] getDateTime() {
        final Calendar c = Calendar.getInstance();
        String dateTime[] = new String[3];
        dateTime[0] = String.valueOf(c.get(Calendar.DAY_OF_MONTH)) + "/" + String.valueOf(c.get(Calendar.MONTH) + 1) + "/" + String.valueOf(c.get(Calendar.YEAR));
        dateTime[1] = String.valueOf(c.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(c.get(Calendar.MINUTE));
        return dateTime;
    }

    public static String getFecha() {
        final Calendar c = Calendar.getInstance();
        return String.valueOf(c.get(Calendar.YEAR)) + "-" + String.format("%02d", c.get(Calendar.MONTH) + 1) + "-" + String.format("%02d", c.get(Calendar.DAY_OF_MONTH)) + " " + String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", c.get(Calendar.MINUTE)) + ":" + String.format("%02d", c.get(Calendar.SECOND));
    }

    public String[] getDateTime2() {
        final Calendar c = Calendar.getInstance();
        String dateTime[] = new String[1];
        dateTime[0] = String.valueOf(c.get(Calendar.DAY_OF_MONTH)) + "/" + String.valueOf(c.get(Calendar.MONTH) + 1) + "/" + String.valueOf(c.get(Calendar.YEAR));
//        dateTime[1] = String.valueOf(c.get(Calendar.HOUR_OF_DAY)) +":"+ String.valueOf(c.get(Calendar.MINUTE));
        return dateTime;
    }

    public static byte[] stringABytes(String s) {
        byte[] cad = new byte[s.length()];
        int i = 0;
        for (char c : s.toCharArray()) {
            switch (c) {
                case 'á':
                    cad[i] = -96;
                    break;
                case 'Á':
                    cad[i] = -113;
                    break;
                case 'é':
                    cad[i] = -126;
                    break;
                case 'É':
                    cad[i] = -112;
                    break;
                case 'í':
                    cad[i] = -95;
                    break;
                case 'Í':
                    cad[i] = -114;
                    break;
                case 'ó':
                    cad[i] = -94;
                    break;
                case 'Ó':
                    cad[i] = -94;
                    break;
                case 'ú':
                    cad[i] = -93;
                    break;
                case 'Ú':
                    cad[i] = -93;
                    break;
                case 'ñ':
                    cad[i] = -92;
                    break;
                case 'Ñ':
                    cad[i] = -91;
                    break;
                case '$':
                    cad[i] = 4;
                    break;
                default:
                    cad[i] = String.valueOf(c).getBytes()[0];
            }
            i++;
        }
        return cad;
    }

    public static class Data {
        public int Ancho;
        public String Valor;
        public int Align;

        public Data(int Ancho, String Valor, int Align) {
            this.Ancho = Ancho;
            this.Valor = Valor;
            this.Align = Align;
        }

        @Override
        public String toString() {
            String str;
            if (Valor.length() < Ancho)
                if (Align == 0)
                    str = Valor.concat(new String(new char[Ancho - 1 - Valor.length()]).replace("\0", " "));
                else
                    str = new String(new char[Ancho - Valor.length()]).replace("\0", " ").concat(Valor);
            else
                str = Valor.substring(0, Ancho - 1);
            return str;
        }
    }
}
