package br.com.hioktec.temporizadorremoto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectThread extends Thread {

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    private static final String MY_UUID = "8f5d5489-73b7-405a-ad7c-769718917e51";

    private Handler handler;

    /* Constructor */
    public BluetoothConnectThread(Handler handler, String deviceAddress) {

        this.handler = handler;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothSocket tmpSocket = null;
        BluetoothDevice tmpDevice = null;

        try{
            tmpDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
        } catch (Exception e){
            e.printStackTrace();
            handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Adquidir dispositivo remoto!".getBytes()).sendToTarget();
        }
        mmDevice = tmpDevice;

        try{
            assert mmDevice != null;
            tmpSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
        } catch (IOException e){
            e.printStackTrace();
            handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Criação cliente!".getBytes()).sendToTarget();
        }
        mmSocket = tmpSocket;
    }

    /* get */
    public BluetoothSocket getSocket() {
        return mmSocket;
    }

    /* run on start */
    @Override
    public void run() {

        /* Obtain connection */

        bluetoothAdapter.cancelDiscovery();

        try {
            mmSocket.connect();
        } catch (IOException connectE) {
            connectE.printStackTrace();
            handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Conectar ao servidor!".getBytes()).sendToTarget();
            try {
                mmSocket.close();
            } catch (IOException closeE) {
                closeE.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fechar Socket!".getBytes()).sendToTarget();
            }
            return;
        }
        handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_STATUS, -1, -1, "Conectado".getBytes()).sendToTarget();
    }

    /* cancel connect thread */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fechar Socket!".getBytes()).sendToTarget();
        }
    }
}