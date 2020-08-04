package br.com.hioktec.temporizadorremoto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

import static android.graphics.Color.rgb;

public class ModoDisplay extends AppCompatActivity {

    /* Atributos de Campo */

    private static TextView textTemporizadorD;
    private static TextView textRelogio;
    private static TextView textTempo;
    private static TextView textStatusDisplay;

    private static ProgressBar progressBar;

    private static final long MILIS_POR_SEG = 1000L;
    private static final int SEG_POR_MIN = 60;
    private static int duracao = 20;
    private static long inicial;
    private static boolean multiplosAlarmes;
    private static int contadorAlarmes = 0;
    private static boolean pararDisplay = false;

    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothAcceptThread conexao;
    private static BluetoothManagerThread gerenciadorConexao;

    private static final int HABILITAR_BLUETOOTH = 1;

    private static Iniciado ultimoIniciado = null;

    private static Context context;

    private static SharedPreferences sharedPreferences;

    /* Getters e Setters */

    public static int getDuracao() {
        return duracao;
    }

    public static void setDuracao(int duracao) {
        ModoDisplay.duracao = duracao;
    }

    public static boolean isMultiplosAlarmes() {
        return multiplosAlarmes;
    }

    public static void setMultiplosAlarmes(boolean multiplosAlarmes) {
        ModoDisplay.multiplosAlarmes = multiplosAlarmes;
    }

    /* Handler Bluetooth do Modo Display */
    public Handler handlerDisplay = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            byte[] buffer= (byte[])msg.obj;
            String mensagem;

            switch (msg.what) {

                case BluetoothMessageConstants.MENSAGEM_STATUS:
                    mensagem = new String(buffer);
                    textStatusDisplay.setText(mensagem);
                    if(mensagem.equals(("Conectado"))){
                        /* inicia gerenciador bluetooth */
                        gerenciadorConexao = new BluetoothManagerThread(conexao.getSocket(), handlerDisplay);
                        gerenciadorConexao.start();
                    }
                    if(mensagem.equals("Desconectado!")){
                        desconectar();
                        esperarConexao();
                    }
                    break;

                case BluetoothMessageConstants.MENSAGEM_ERRO:
                    mensagem = new String(buffer);
                    textStatusDisplay.setText(mensagem);
                    break;

                case BluetoothMessageConstants.MENSAGEM_LIDA:
                    mensagem = new String(buffer, 0 , msg.arg1);
                    String enviar;
                    if(mensagem.equals("iniciar")){
                        /* inicia ou reinicia temporizador, cria e grava iniciado e tenta enviar para o controle remoto*/
                        iniciar();
                        Iniciado iniciado = criarIniciadoAgora();
                        ultimoIniciado = iniciado;
                        enviarIniciado(iniciado);
                    }
                    else if(mensagem.equals("tempo")){
                        /* envia o tempo atual registrado no temporizador */
                        enviar = ("tempo=" + textTemporizadorD.getText().toString() + " min ás " + textRelogio.getText().toString());
                        gerenciadorConexao.write(enviar.getBytes());
                    }
                    else if(mensagem.equals("pedir último iniciado")){
                        /* enviar ultimo pedido de iniciar quando pedido pelo remoto */
                        if(ultimoIniciado != null && !pararDisplay){
                            enviarIniciado(ultimoIniciado);
                        } else {
                            enviar = ("iniciado nulo".toString());
                            gerenciadorConexao.write(enviar.getBytes());
                        }
                    }
                    else if(mensagem.startsWith("duracao =")){
                        /* seta novo tempo de duração do temporizador quando pedido e envia mudação ocorrida*/
                        String novaDuracao = mensagem.substring(mensagem.lastIndexOf("=") + 1);
                        if(!String.valueOf(duracao).equals(novaDuracao)) {
                            setDuracao(Integer.parseInt(novaDuracao));
                            if (Integer.parseInt(textTemporizadorD.getText().toString()) < duracao) {
                                contadorAlarmes = 1;
                            }
                            textTempo.setText(getDuracao() + " min");
                            enviar = "duração mudou para " + novaDuracao + " min!";
                            gerenciadorConexao.write(enviar.getBytes());
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt("duracao", getDuracao());
                            editor.commit();
                        } else {
                            enviar = "duração " + duracao + " min";
                            gerenciadorConexao.write(enviar.getBytes());
                        }
                    }
                    else if(mensagem.equals("multiplosTrue")){
                        setMultiplosAlarmes(true);
                        enviar = "multiplos ligado";
                        gerenciadorConexao.write(enviar.getBytes());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("multiplos", true);
                        editor.commit();
                    }
                    else if(mensagem.equals("multiplosFalse")){
                        setMultiplosAlarmes(false);
                        enviar = "multiplos desligado";
                        gerenciadorConexao.write(enviar.getBytes());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("multiplos", false);
                        editor.commit();
                    }
                    else if(mensagem.equals("parar")){
                        pararDisplay = true;
                        gerenciadorConexao.write("parado".getBytes());
                }
                    break;

                case BluetoothMessageConstants.MENSAGEM_ESCRITA:
                    mensagem = new String(buffer);
                    if(mensagem.startsWith("dura")) {
                        textStatusDisplay.setText("enviado: " + mensagem);
                    }
                    else if (mensagem.startsWith("tempo")) {
                        textStatusDisplay.setText("enviado: tempo");
                    }
                    else if (mensagem.startsWith("mult")){
                        textStatusDisplay.setText("enviado: " + mensagem);
                    }
                    else if (mensagem.startsWith("iniciado")){
                        textStatusDisplay.setText("enviado: " + mensagem);
                    }
                    else if (mensagem.startsWith("parad")){
                        textStatusDisplay.setText("enviado: " + mensagem);
                }
                    else {
                        textStatusDisplay.setText("enviado: iniciado");
                    }
                    break;
            }
            return false;
        }
    });

    /* Thread do relógio */
    private Runnable relogio = new Runnable() {
        @Override
        public void run() {
            Calendar agora = Calendar.getInstance();
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat formatador = new SimpleDateFormat("HH:mm");
            textRelogio.setText(formatador.format(agora.getTime()));
            handlerDisplay.postDelayed(relogio, MILIS_POR_SEG);
        }
    };

    /* Thread Temporizador */
    private Runnable temporizador = new Runnable() {
        @Override
        public void run() {
            if(!pararDisplay) {
                long tempoPercorrido = (System.currentTimeMillis() - inicial) / MILIS_POR_SEG;
                int tempoPercorridoEmSeg = ((int) tempoPercorrido);
                progressBar.setProgress(tempoPercorridoEmSeg % SEG_POR_MIN);
                textTemporizadorD.setText(String.valueOf(tempoPercorridoEmSeg / SEG_POR_MIN));
                if (tempoPercorrido >= duracao * SEG_POR_MIN * contadorAlarmes) {
                    tocarAlarme();
                    textTemporizadorD.setTextColor(Color.parseColor("#FF3333"));
                    if (multiplosAlarmes) {
                        contadorAlarmes += 1;
                    } else {
                        contadorAlarmes = 200000;
                    }
                }
                handlerDisplay.postDelayed(temporizador, MILIS_POR_SEG);
            }
        }
    };

    /* Métodos de Activity*/

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide(); /* não mostra barra de título */
        setContentView(R.layout.activity_modo_display);

        /* mostra em tela inteira */
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        /* manter a tela sempre ligada */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /* Orientação Horizontal */
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);

        /* inicia componentes views */

        context = this;

        progressBar = findViewById(R.id.circularProgressBar);
        textTemporizadorD = findViewById(R.id.textTemporizadorD);
        textRelogio = findViewById(R.id.textRelogio);
        textTempo = findViewById(R.id.textTempo);
        textStatusDisplay = findViewById(R.id.textStatusDisplay);

        textTempo.setText(duracao + " min");

        /* obter adaptador bluetooth */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            textStatusDisplay.setText("Hardware bluetooth não está funcionando!");
        } else {
            textStatusDisplay.setText("Hardware bluetooth está funcionando!)");
        }

        /* habilitar bluetooth se já habilitado espera conexão do controle remoto */
        if(!bluetoothAdapter.isEnabled()){
            Intent habilitarBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(habilitarBtIntent, HABILITAR_BLUETOOTH);
            textStatusDisplay.setText("Solicitando ativação do bluetooth...");
        } else {
            textStatusDisplay.setText("Bluetooth já está ativado!");
            esperarConexao();
        }

        /* recuperar dados salvos */
        sharedPreferences = getSharedPreferences(ModoControle.PREFS_NAME, MODE_PRIVATE);

        if(sharedPreferences.contains("multiplos")){
            setMultiplosAlarmes(sharedPreferences.getBoolean("multiplos", false));
        }

        if(sharedPreferences.contains("duracao")){
            setDuracao(sharedPreferences.getInt("duracao", 20));
        }

        /* iniciar thread do relogio */
        relogio.run();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* retorno pedido de habilitação bluetooth se ok espera conexão do controle remoto */
        if(requestCode == HABILITAR_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                textStatusDisplay.setText("Bluetooth ativado!");
                esperarConexao();
            } else {
                textStatusDisplay.setText("Bluetooth não ativado!");
            }
        }
    }

    /* métodos do temporizador */

    /* inicia ou reinicia temporizador */
    private void iniciar() {
        inicial = System.currentTimeMillis();
        contadorAlarmes = 1;
        textTempo.setText(duracao + " min");
        textTemporizadorD.setTextColor(Color.parseColor("#00FF66"));
        pararDisplay = false;
        temporizador.run();
    }

    private void tocarAlarme(){
        Uri alarmeUri;
        if(sharedPreferences.contains("uri_alarme")){
            alarmeUri = Uri.parse(sharedPreferences.getString("uri_alarme", ""));
        } else {
            alarmeUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        Ringtone alarme = RingtoneManager.getRingtone(getApplicationContext(), alarmeUri);
        alarme.play();
    }

    /* cria um objeto Iniciado com a data e hora do momento */
    private Iniciado criarIniciadoAgora(){
        Calendar agora = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatadorDia = new SimpleDateFormat("dd/MM/yyyy");
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatadorHora = new SimpleDateFormat("HH:mm");
        String dia = formatadorDia.format(agora.getTime());
        String hora = formatadorHora.format(agora.getTime());
        return new Iniciado(dia, hora, agora.getTimeInMillis());
    }

    /* Métodos do Bluetooth */

    public void habilitarVisibilidade(View view) {
        Intent habilitarVisivilidadeIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        habilitarVisivilidadeIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivity(habilitarVisivilidadeIntent);
    }

    private void esperarConexao(){

        if(conexao == null){
            conexao = new BluetoothAcceptThread(handlerDisplay);
            conexao.start();
        }
    }

    private static void desconectar(){

        if (gerenciadorConexao != null) {
            gerenciadorConexao.cancel();
            gerenciadorConexao = null;
        }

        if(conexao != null){
            conexao.cancel();
            conexao = null;
        }
    }

    /* envia um objeto Iniciado para o controle remoto*/
    private void enviarIniciado(Iniciado iniciado){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(iniciado);
            oos.flush();
            byte[] iniciadoBytes = bos.toByteArray();
            gerenciadorConexao.write(iniciadoBytes);
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /* Métodos Botões */

    public void btIniciar(final View view){
        /* abre uma janela pedindo a senha se correta inicia tempoerizador */
        View senhaView = LayoutInflater.from(this).inflate(R.layout.senha_layout, null);
        AlertDialog.Builder senhaDialog = new AlertDialog.Builder(this, R.style.MyDialogTheme).setView(senhaView);

        final EditText senhaDigitada = senhaView.findViewById(R.id.editTextSenha);

        senhaDialog
                .setCancelable(false)
                .setMessage("Digite a senha para prosseguir:")
                .setPositiveButton("Ir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* resgatar senha salva */
                        String senha;
                        if (sharedPreferences.contains("senha")) {
                            senha = sharedPreferences.getString("senha", null);
                        } else {
                            senha = "0000";
                        }
                        /* testar autenticidade, se autentica inicia temporizador*/
                        if(senhaDigitada.getText().toString().equals(senha)){
                            iniciar();
                            ultimoIniciado = criarIniciadoAgora();
                            if(gerenciadorConexao != null){
                                enviarIniciado(ultimoIniciado);
                            }
                        } else{ /* se errada informa senha errada */
                            senhaDigitada.setText("");
                            Toast toast = Toast.makeText(context, "Senha errada!",Toast.LENGTH_LONG);
                            View view = toast.getView();
                            view.getBackground().setColorFilter(rgb(255,0,0), PorterDuff.Mode.SRC_IN);
                            TextView textView = view.findViewById(android.R.id.message);
                            textView.setTextColor(rgb(255,255,255));
                            toast.setGravity(Gravity.CENTER,0,0);
                            toast.show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /* pede senha e abre activity de configuração */
    public void config(final View view){
        View senhaView = LayoutInflater.from(this).inflate(R.layout.senha_layout, null);
        AlertDialog.Builder senhaDialog = new AlertDialog.Builder(this, R.style.MyDialogTheme).setView(senhaView);

        final EditText senhaDigitada = senhaView.findViewById(R.id.editTextSenha);

        senhaDialog
                .setCancelable(false)
                .setMessage("Digite a senha para prosseguir:")
                .setPositiveButton("Ir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* resgatar senha salva */
                        String senha;
                        if (sharedPreferences.contains("senha")) {
                            senha = sharedPreferences.getString("senha", null);
                        } else {
                            senha = "0000";
                        }
                        /* testar autenticidade */
                        if(senhaDigitada.getText().toString().equals(senha)){
                            Intent intentConfig = new Intent(view.getContext(), Configuracoes.class);
                            startActivity(intentConfig);
                        } else{
                            senhaDigitada.setText("");
                            Toast toast = Toast.makeText(context, "Senha errada!",Toast.LENGTH_LONG);
                            View view = toast.getView();
                            view.getBackground().setColorFilter(rgb(255,0,0), PorterDuff.Mode.SRC_IN);
                            TextView textView = view.findViewById(android.R.id.message);
                            textView.setTextColor(rgb(255,255,255));
                            toast.setGravity(Gravity.CENTER,0,0);
                            toast.show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /* pede confirmação para sair do aplicativo */
    @Override
    public void onBackPressed(){
        new AlertDialog.Builder(this)
                .setMessage("Você deseja fechar o aplicativo?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ModoDisplay.super.onBackPressed();
                    }
                })
                .setNegativeButton("Não",null)
                .show();
    }
}