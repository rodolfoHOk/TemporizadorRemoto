package br.com.hioktec.temporizadorremoto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.graphics.Color.rgb;

public class ModoControle extends AppCompatActivity {

    /* Atributos de campo */

    private static TextView textStatusControle;
    private static TextView textDuracao;
    private static TextView textTempoRecebido;
    private static TextView textTempoAproximado;

    private static ToggleButton bttConexao;
    private static Button btIniciar;
    private static Button btPedirTempo;

    private static RecyclerView recyclerViewIniciados;
    private static IniciadosAdapter adapter;
    private static List<Iniciado> iniciados;

    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothConnectThread conexao;
    public static BluetoothManagerThread gerenciadorConexao;

    private static final int HABILITAR_BLUETOOTH = 1;

    private static SharedPreferences sharedPreferences;
    public static final String PREFS_NAME = "dados_salvos";

    private static boolean parado = true;
    public static boolean foiPedidoIniciado = false;

    private static Context context;

    private static int duracaoAlarme = 20;
    private static boolean multiAlarmes = false;

    public static final String idCanal = "canalAlarme";
    private static final int idNotificacao = 1101;
    private static int contadorNotificacoes = 1;

    private static final int pendingIntentRequestCode = 1201;

    /* Handler Bluetooth do Modo Controle*/
    public Handler handlerControle = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            byte[] buffer = (byte[]) msg.obj;
            String mensagem;

            switch (msg.what) {
                case BluetoothMessageConstants.MENSAGEM_STATUS:
                    mensagem = new String(buffer);
                    textStatusControle.setText(mensagem);
                    if (mensagem.equals(("Conectado"))) {
                        /* iniciar gerenciadorBuetooth e pedir último iniciado */
                        gerenciadorConexao = new BluetoothManagerThread(conexao.getSocket(), handlerControle);
                        gerenciadorConexao.start();
                        gerenciadorConexao.write("pedir último iniciado".getBytes());
                        foiPedidoIniciado = true;
                    }
                    if (mensagem.equals("Desconectado!")) {
                        /* muda botão conexão para false fazendo isso já chama o método desconectar */
                        bttConexao.setChecked(false);
                    }
                    break;

                case BluetoothMessageConstants.MENSAGEM_ERRO:
                    mensagem = new String(buffer);
                    textStatusControle.setText(mensagem);
                    break;

                case BluetoothMessageConstants.MENSAGEM_LIDA:
                    mensagem = new String(buffer, 0, msg.arg1);
                    if (mensagem.startsWith("tempo=")) {
                        String tempo = mensagem.substring(mensagem.lastIndexOf("=") + 1);
                        textTempoRecebido.setText(tempo);
                        textStatusControle.setText("recebido: tempo");
                    }
                    else if (mensagem.startsWith("dura")){
                        textStatusControle.setText(mensagem);
                        if(mensagem.contains("mudou")) {
                            duracaoAlarme = Integer.parseInt(mensagem.substring(mensagem.indexOf("para") +5, mensagem.indexOf("min") -1));
                            if(!parado && iniciados.size() > 0) {
                                contadorNotificacoes = (int) ((Calendar.getInstance().getTimeInMillis() - iniciados.get(0).getData()) / (duracaoAlarme * 60 * 1000)) + 1;
                                if (contadorNotificacoes == 1 || multiAlarmes) {
                                    iniciarAlarme();
                                }
                            }
                            textDuracao.setText(duracaoAlarme + " min");
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt("duracao", duracaoAlarme);
                            editor.commit();
                            Intent intent = new Intent("notificarConfig");
                            intent.putExtra("mensagem", mensagem);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }
                        textStatusControle.setText("recebido: " + mensagem);
                    }
                    else if(mensagem.equals("multiplos ligado")){
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("multiplos", true);
                        editor.commit();
                        multiAlarmes = true;
                        Intent intent = new Intent("notificarConfig");
                        intent.putExtra("mensagem", mensagem);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        textStatusControle.setText("recebido: " + mensagem);
                    }
                    else if(mensagem.equals("multiplos desligado")){
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("multiplos", false);
                        editor.commit();
                        multiAlarmes = false;
                        Intent intent = new Intent("notificarConfig");
                        intent.putExtra("mensagem", mensagem);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        textStatusControle.setText("recebido: " + mensagem);
                    }
                    else if (mensagem.equals("iniciado nulo")){
                        foiPedidoIniciado = false;
                    }
                    else if (mensagem.equals("parado")){
                        parado = true;
                        cancelarAlarme();
                        textStatusControle.setText("Temporizador parado!");
                    }
                    else {
                        /* restaurar de Byte Array para objeto Iniciado */
                        ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
                        ObjectInput oi = null;
                        try {
                            oi = new ObjectInputStream(bis);
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                        Iniciado iniciado = null;
                        try {
                            assert oi != null;
                            iniciado = (Iniciado) oi.readObject();
                        } catch (ClassNotFoundException | IOException e) {
                            e.printStackTrace();
                        }
                        /* adiciona iniciado não existente na lista */
                        if (!foiPedidoIniciado){
                            iniciados.add(0, iniciado);
                            adapter.notifyDataSetChanged();
                            salvarLista();
                            contadorNotificacoes = 1;
                            iniciarAlarme();
                        } else {
                            assert iniciado != null;
                            if (iniciados.size() > 0 && iniciado.getData().equals(iniciados.get(0).getData())){
                                foiPedidoIniciado = false;
                                iniciarAlarme();
                            } else {
                                iniciados.add(0, iniciado);
                                adapter.notifyDataSetChanged();
                                salvarLista();
                                foiPedidoIniciado = false;
                                contadorNotificacoes = (int) ((Calendar.getInstance().getTimeInMillis() - iniciados.get(0).getData()) / (duracaoAlarme * 60 * 1000)) + 1;
                                if(contadorNotificacoes == 1 || multiAlarmes) {
                                    iniciarAlarme();
                                }
                                Toast toast = Toast.makeText(context,"Recebido novo item na lista", Toast.LENGTH_LONG);
                                View view = toast.getView();
                                view.getBackground().setColorFilter(rgb(255,0,0), PorterDuff.Mode.SRC_IN);
                                TextView textView = view.findViewById(android.R.id.message);
                                textView.setTextColor(rgb(255,255,255));
                                toast.setGravity(Gravity.CENTER,0,0);
                                toast.show();
                            }
                        }
                        if (gerenciadorConexao != null) {
                            String duracao;
                            if (sharedPreferences.contains("duracao")) {
                                duracao = String.valueOf(sharedPreferences.getInt("duracao", 20));
                            } else {
                                duracao = "20";
                            }
                            String enviar = "duracao =" + duracao;
                            gerenciadorConexao.write(enviar.getBytes());
                        }
                    }
                    break;

                case BluetoothMessageConstants.MENSAGEM_ESCRITA:
                    mensagem = new String(buffer);
                    if(!mensagem.startsWith("pedir")) {
                        textStatusControle.setText("enviado: " + mensagem);
                    }
                    break;
            }
            return false;
        }
    });

    /* BroadcastReceiver - para receber intent de um RecyclerView Adapter e para receber intent do AlarmManager*/
    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // se vier se um AlarmManager
            if(intent.getStringExtra("alarme") != null
                    && intent.getStringExtra("alarme").equals("notificar")){
                // cria uma notificação de alarme
                NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                String conteudoTexto = contadorNotificacoes * duracaoAlarme + " min";

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, idCanal)
                        .setSmallIcon(R.drawable.ic_baseline_alarm_24)
                        .setContentTitle("Alarme")
                        .setContentText(conteudoTexto)
                        .setOngoing(false)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(true);

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){

                    Uri notificacaoUri;
                    if(sharedPreferences.contains("uri_notificacao")){
                        notificacaoUri = Uri.parse(sharedPreferences.getString("uri_notificacao", ""));
                    } else{
                        notificacaoUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    }

                    builder.setDefaults(Notification.DEFAULT_LIGHTS);
                    builder.setSound(notificacaoUri);
                    builder.setVibrate(new long[]{1000, 1000, 2000, 1000});
                }

                // quando tocar na notificação mostrar a ativity já aberta do app
                final Intent intentAlvo = new Intent(context, ModoControle.class);
                intentAlvo.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentAlvo, 0);
                builder.setContentIntent(pendingIntent);

                notificationManager.notify(idNotificacao, builder.build());
                if(multiAlarmes) {
                    contadorNotificacoes++;
                    iniciarAlarme();
                }
            }
        }
    };

    // cria canal de notificação necessário android 8+
    public void criarCanalDeNotificacao(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence nomeCanal = "Alarme";
            String descricaoCanal = "Duração do temporizador atingida";
            int importancia = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(idCanal, nomeCanal, importancia);
            notificationChannel.setDescription(descricaoCanal);

            notificationChannel.setShowBadge(false);

            notificationChannel.setLightColor(Color.WHITE);
            notificationChannel.enableLights(true);

            notificationChannel.setVibrationPattern(new long[] {1000, 1000, 2000, 1000});
            notificationChannel.enableVibration(true);

            Uri notificacaoUri;
            if(sharedPreferences.contains("uri_notificacao")){
                notificacaoUri = Uri.parse(sharedPreferences.getString("uri_notificacao", ""));
            } else{
                notificacaoUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            notificationChannel.setSound(notificacaoUri, audioAttributes);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    /* Thread do tempo aproximado */
    private Runnable tempoAproximado = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            if (!parado && iniciados.size() > 0) {
                long iniciadoData = iniciados.get(0).getData();
                long agoraData = Calendar.getInstance().getTimeInMillis();
                long tempo = (agoraData - iniciadoData) / 1000;
                int minutos = (int) tempo / 60;
                int segundos = (int) tempo % 60;
                textTempoAproximado.setText(String.format("%02d:%02d", minutos, segundos));

                handlerControle.postDelayed(tempoAproximado, 1000);
            }
        }
    };

    /* Métodos de Activity */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modo_controle);

        /* Muda Título da Activity */
        this.setTitle("Temporizador Remoto - Modo Controle");

        /* iniciar componentes da view */
        textStatusControle = findViewById(R.id.textStatusControle);
        textDuracao = findViewById(R.id.textDuracao);
        textTempoRecebido = findViewById(R.id.textTempoRecebido);
        textTempoAproximado = findViewById(R.id.textTempoAproximado);
        textTempoAproximado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!textStatusControle.getText().toString().equals("Temporizador parado!") &&
                        !textStatusControle.getText().toString().equals("Temporizador local parado!")) {
                    new AlertDialog.Builder(ModoControle.this)
                            .setMessage("Você deseja parar o temporizador?")
                            .setCancelable(false)
                            .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (gerenciadorConexao != null) {
                                        gerenciadorConexao.write("parar".getBytes());
                                    } else {
                                        new AlertDialog.Builder(ModoControle.this)
                                                .setMessage("Desconectado!!! Deseja parar somente neste dispositivo?")
                                                .setCancelable(false)
                                                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        parado = true;
                                                        cancelarAlarme();
                                                        textStatusControle.setText("Temporizador local parado!");
                                                    }
                                                })
                                                .setNegativeButton("Não", null)
                                                .show();
                                    }
                                }
                            })
                            .setNegativeButton("Não", null)
                            .show();
                }
            }
        });

        bttConexao = findViewById(R.id.bttConexao);
        btIniciar = findViewById(R.id.btIniciar);
        btPedirTempo = findViewById(R.id.btPedirTempo);

        bttConexao.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(!foiPedidoIniciado){
                        conectar(buttonView);
                    }
                } else {
                    cancelarConexao(buttonView);
                }
            }
        });

        context = this;

        /* iniciar lista iniciados */
        recyclerViewIniciados = findViewById(R.id.recyclerViewIniciados);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewIniciados.setLayoutManager(layoutManager);

        /* Popular lista iniciados */
        iniciados = new ArrayList<>();
            /* com JSON SharedPreferences */
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (sharedPreferences.contains("iniciados")) {
            JSONArray jsonArray = null;
            try {
                jsonArray = new JSONArray(sharedPreferences.getString("iniciados", null));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject jsonObject;
            Iniciado iniciado;
            String dia;
            String hora;
            long data;
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        jsonObject = jsonArray.getJSONObject(i);
                        dia = jsonObject.getString("dia");
                        hora = jsonObject.getString("hora");
                        data = jsonObject.getLong("data");
                        iniciado = new Iniciado(dia, hora, data);
                        iniciados.add(iniciado);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /* listar iniciados */
        adapter = new IniciadosAdapter(iniciados, sharedPreferences);
        recyclerViewIniciados.setAdapter(adapter);
        recyclerViewIniciados.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // criar canal de notificação (alarme do temporizador)
        criarCanalDeNotificacao();

        /* BroadcastReceiver para filtar, remover item da lista, do RecyclerView Adapter e filtrar notificação de alarme */
        IntentFilter intentFilter = new IntentFilter("tocarAlarme");
        context.registerReceiver(mReceiver, intentFilter);

        /* obter adaptador bluetooth */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            textStatusControle.setText("Hardware bluetooth não está funcionando!");
        } else {
            textStatusControle.setText("Hardware bluetooth está funcionando!)");
        }

        /* habilitar bluetooth */
        if (!bluetoothAdapter.isEnabled()) {
            Intent habilitarBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(habilitarBtIntent, HABILITAR_BLUETOOTH);
            textStatusControle.setText("Solicitando ativação do bluetooth...");
        } else {
            textStatusControle.setText("Bluetooth já está ativado!");
        }

        /* verifica se já está conectado e faz as ações de acordo */
        if (conexao != null && gerenciadorConexao != null) {
            btIniciar.setEnabled(true);
            btPedirTempo.setEnabled(true);
            foiPedidoIniciado = true;
            bttConexao.setChecked(true);
            textStatusControle.setText("Conectado");
            gerenciadorConexao.write("pedir último iniciado".getBytes());
        } else {
            btIniciar.setEnabled(false);
            btPedirTempo.setEnabled(false);
        }

        /* recuperar outros dados salvos */
        if(sharedPreferences.contains("multiplos")){
            multiAlarmes = sharedPreferences.getBoolean("multiplos", false);
        }

        if(sharedPreferences.contains("duracao")){
            duracaoAlarme = sharedPreferences.getInt("duracao", 20);
            textDuracao.setText(duracaoAlarme + " min");
        }
    }

    /*  no retorno de resultado de outras activity */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* retorno pedido de habilitação bluetooth */
        if (requestCode == HABILITAR_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                textStatusControle.setText("Bluetooth ativado!");
            } else {
                textStatusControle.setText("Bluetooth não ativado!");
            }
        }
    }

    /* Métodos Bluetooth */

    public void desconectar() {

        if (gerenciadorConexao != null) {
            gerenciadorConexao.cancel();
            gerenciadorConexao = null;
        }
        if (conexao != null) {
            conexao.cancel();
            conexao = null;
        }

        btIniciar.setEnabled(false);
        btPedirTempo.setEnabled(false);
    }

    // iniciar gerenciador de alarme cria um alarme que dispara o broadcast que cria uma notificação
    private void iniciarAlarme(){
        if(iniciados.size() > 0){
            long dataIniciado = iniciados.get(0).getData();
            long duracaoMilis = contadorNotificacoes * duracaoAlarme * 60 * 1000;
            long alarmeData = dataIniciado + duracaoMilis;

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intentBroadcast = new Intent("tocarAlarme");
            intentBroadcast.putExtra("alarme", "notificar");
            PendingIntent alarmeIntent = PendingIntent.getBroadcast(context, pendingIntentRequestCode, intentBroadcast, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmeData, alarmeIntent);

            parado = false;
            tempoAproximado.run();
        }
    }

    // cancela alarme definido
    private void cancelarAlarme(){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentBroadcast = new Intent("tocarAlarme");
        intentBroadcast.putExtra("alarme", "notificar");
        PendingIntent alarmeIntent = PendingIntent.getBroadcast(context, pendingIntentRequestCode, intentBroadcast, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(alarmeIntent);
    }

    /* Métodos Botões */

    public void conectar(View view){
        /* recupera endereço do ultimo dispositivo selecionado */
        String macDispositivo;
        if (sharedPreferences.contains("MAC_dispositivo")) {
            macDispositivo = sharedPreferences.getString("MAC_dispositivo", null);
        } else {
            macDispositivo = "00:00:00:00:00:00";
        }
        /* verifica endereço válido e conecta */
        assert macDispositivo != null;
        if(macDispositivo.equals("00:00:00:00:00:00")){
            textStatusControle.setText("Selecione dispositivo nas configurações!");
            bttConexao.setChecked(false);
        } else {
            conexao = new BluetoothConnectThread(handlerControle, macDispositivo);
            conexao.start();
        }
        /* quando conectado habilita e desabilita botões de acordo */
        if (conexao != null) {
            btIniciar.setEnabled(true);
            btPedirTempo.setEnabled(true);
        }
    }

    public void cancelarConexao(View view) {
        desconectar();
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
                            intentConfig.putExtra("modoSolo", false);
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

    /* pede para iniciar ou reiniciar o temporizador remotamente */
    public void iniciarRemoto(View view) {
        new AlertDialog.Builder(this)
                .setMessage("Você desejar iniciar ou reiniciar tempo?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(gerenciadorConexao != null) {
                            String enviar = "iniciar";
                            gerenciadorConexao.write(enviar.getBytes());
                        } else {
                            Toast.makeText(ModoControle.this, "não conectado", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    /* pede tempo atual ao temporizador */
    public void pedirTempo(View view) {
        if(gerenciadorConexao != null) {
            String enviar = "tempo";
            gerenciadorConexao.write(enviar.getBytes());
        } else {
            Toast.makeText(this, "não conectado", Toast.LENGTH_LONG).show();
        }
    }

    /* limpa lista de iniciados */
    public void limparLista(View view) {
        new AlertDialog.Builder(this)
                .setMessage("Você deseja limpar a lista?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int tamanho = iniciados.size();
                        if (tamanho > 0) {
                            for (int i = 0; i < tamanho; i++) {
                                iniciados.remove(0);
                                adapter.notifyItemRemoved(0);
                            }
                        }
                        if (sharedPreferences.contains("iniciados")) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove("iniciados");
                            editor.commit();
                        }
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    /* Métodos outros */

    /* salva a lista de iniciados*/
    private void salvarLista() {
        /* montar um JSON Array da lista de iniciados para salvar*/
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject;
        for(Iniciado iniciado : iniciados){
            jsonObject = new JSONObject();
            try {
                jsonObject.put("dia", iniciado.getDia());
                jsonObject.put("hora", iniciado.getHora());
                jsonObject.put("data", iniciado.getData());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        /* salvar JSON Array iniciados no sharedPreferences */
        String jsonArrayString = jsonArray.toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("iniciados",jsonArrayString);
        editor.commit();
    }

    /* pede confirmação para sair do aplicativo */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Você deseja fechar o aplicativo?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ModoControle.super.onBackPressed();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        // necessário desregistar o broadcastreceiver
        unregisterReceiver(mReceiver);
        cancelarAlarme();
        super.onDestroy();
    }
}