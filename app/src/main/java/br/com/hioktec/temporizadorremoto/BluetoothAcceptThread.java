package br.com.hioktec.temporizadorremoto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.util.UUID;

public class BluetoothAcceptThread extends Thread {

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothServerSocket mmServerSocket;
    private static BluetoothSocket mmSocket;

    private static final String MY_UUID = "8f5d5489-73b7-405a-ad7c-769718917e51";

    private Handler handler;

    /* Constructor */
    public BluetoothAcceptThread (Handler handler) {

        this.handler = handler;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothServerSocket tmpServerSocket = null;

        try{
            tmpServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothEx", UUID.fromString(MY_UUID));
        } catch (IOException e){
            e.printStackTrace();
            handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Criação servidor!".getBytes()).sendToTarget();
        }
        mmServerSocket = tmpServerSocket;
    }

    /* get */

    public static BluetoothSocket getSocket() {
        return mmSocket;
    }

    /* run on start */
    @Override
    public void run() {

        /* Obtain connection */

        BluetoothSocket socket = null;
        handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_STATUS, -1, -1, "Aguardando conexão...".getBytes()).sendToTarget();

        while (true) {

            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Aceitar conexão!".getBytes()).sendToTarget();
                break;
            }

            if (socket != null) {
                try {
                    mmServerSocket.close();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fechar ServerSocket!".getBytes()).sendToTarget();
                    break;
                }
            }
        }
        mmSocket = socket;
        handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_STATUS, -1, -1, "Conectado".getBytes()).sendToTarget();

    }

    /* cancel accept thread */
    public void cancel() {

        if (mmServerSocket != null) {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fechar ServerSocket!".getBytes()).sendToTarget();
            }
        }

        if (mmSocket != null) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fechar Socket!".getBytes()).sendToTarget();
            }
        }
    }
}