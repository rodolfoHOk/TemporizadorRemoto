package br.com.hioktec.temporizadorremoto;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothManagerThread extends Thread {

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private Handler handler;

    private boolean isRunning;

    /* Constructor */
    public BluetoothManagerThread(BluetoothSocket mmSocket, Handler handler) {

        this.mmSocket = mmSocket;
        this.handler = handler;

        isRunning = true;

        /* Adquirir fluxo de entrada e fluxo de saída bluetooth */

        InputStream tmpInStream = null;
        OutputStream tmpOutStream = null;

        try {
            tmpInStream = mmSocket.getInputStream();
        } catch (IOException e){
            e.printStackTrace();
            handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Adquidir fluxo entrada!".getBytes()).sendToTarget();
        }

        try {
            tmpOutStream = mmSocket.getOutputStream();
        } catch (IOException e){
            e.printStackTrace();
            handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Adquidir fluxo saída!".getBytes()).sendToTarget();
        }

        mmInStream = tmpInStream;
        mmOutStream = tmpOutStream;
    }

    /* run on start */
    @Override
    public void run() {

        byte[] mmBuffer = new byte[1024];
        int numBytes;

        /* Listen inputStream */
        while(isRunning){
            try{
                numBytes = mmInStream.read(mmBuffer);
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_LIDA, numBytes, -1, mmBuffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_STATUS, -1, -1, "Desconectado!".getBytes()).sendToTarget();
                isRunning = false;
                break;
            }
        }
    }

    /* write data */
    public void write(byte[] data){
        if(mmOutStream != null) {
            try {
                mmOutStream.write(data);
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ESCRITA, -1, -1, data).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Enviar mensagem!".getBytes()).sendToTarget();
            }
        } else {
            handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fluxo saída nulo!".getBytes()).sendToTarget();
        }
    }

    /* cancel BluetoothManager */
    public void cancel() {

        isRunning = false;

        if (mmSocket != null) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fechar Socket!".getBytes()).sendToTarget();
            }
        }

        if (mmInStream != null) {
            try {
                mmInStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fechar fluxo entrada!".getBytes()).sendToTarget();
            }
        }

        if (mmOutStream != null) {
            try {
                mmOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                handler.obtainMessage(BluetoothMessageConstants.MENSAGEM_ERRO, -1, -1, "Erro: Fechar fluxo saída!".getBytes()).sendToTarget();
            }
        }
    }
}
