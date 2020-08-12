package br.com.hioktec.temporizadorremoto;

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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.graphics.Color.rgb;

public class ModoSolo extends AppCompatActivity {

    // Atributos de campo

    private static TextView textStatusSolo;
    private static TextView textDuracaoSolo;
    private static TextView textTempoSolo;

    private static RecyclerView recyclerViewIniciados;
    private static IniciadosAdapter adapter;
    private static List<Iniciado> iniciados;

    private static SharedPreferences sharedPreferences;
    public static final String PREFS_NAME = "dados_salvos";

    private static boolean parado = true;

    private static Context context;

    private static int duracaoAlarme = 20;
    private static boolean multiAlarmes = false;

    public static final String idCanalSolo = "canalAlarmeSolo";
    private static final int idNotificacao = 1102;
    private static int contadorNotificacoes = 1;

    private static final int pendingIntentRequestCode = 1202;

    // Handler Modo Solo
    public Handler handlerSolo = new Handler();

    // BroadcastReceiver - para receber intent de um RecyclerView Adapter e para receber intent do AlarmManager
    public BroadcastReceiver mReceiverSolo = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // se vier se um AlarmManager
            if(intent.getAction().equals("tocarAlarmeSolo")){
                if(intent.getStringExtra("alarmeSolo") != null
                        && intent.getStringExtra("alarmeSolo").equals("notificar")) {
                    // cria uma notificação de alarme
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    String conteudoTexto = contadorNotificacoes * duracaoAlarme + " min";

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, idCanalSolo)
                            .setSmallIcon(R.drawable.ic_baseline_alarm_24)
                            .setContentTitle("Alarme Solo")
                            .setContentText(conteudoTexto)
                            .setOngoing(false)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setAutoCancel(true);

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

                        Uri notificacaoUri;
                        if (sharedPreferences.contains("uri_notificacao")) {
                            notificacaoUri = Uri.parse(sharedPreferences.getString("uri_notificacao", ""));
                        } else {
                            notificacaoUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        }

                        builder.setDefaults(Notification.DEFAULT_LIGHTS);
                        builder.setSound(notificacaoUri);
                        builder.setVibrate(new long[]{1000, 1000, 2000, 1000});
                    }

                    // quando tocar na notificação mostrar a ativity já aberta do app
                    final Intent intentAlvo = new Intent(context, ModoSolo.class);
                    intentAlvo.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentAlvo, 0);
                    builder.setContentIntent(pendingIntent);

                    notificationManager.notify(idNotificacao, builder.build());
                    if (multiAlarmes) {
                        contadorNotificacoes++;
                        iniciarAlarmeSolo();
                    }
                }
            }
            // se vier mensagem local de configurações
            else if(intent.getAction().equals("mensagemModoSolo")) {
                if (intent.getStringExtra("mensagemSolo") != null) {
                    String mensagemRecebida = intent.getStringExtra("mensagemSolo");
                    if (mensagemRecebida.startsWith("mudar")) {
                        duracaoAlarme = Integer.parseInt(mensagemRecebida.substring(mensagemRecebida.indexOf("para") + 5, mensagemRecebida.indexOf("min") - 1));
                        if (!parado && iniciados.size() > 0) {
                            contadorNotificacoes = (int) ((Calendar.getInstance().getTimeInMillis() - iniciados.get(0).getData()) / (duracaoAlarme * 60 * 1000)) + 1;
                            if (contadorNotificacoes == 1 || multiAlarmes) {
                                iniciarAlarmeSolo();
                            }
                        }
                        textDuracaoSolo.setText(duracaoAlarme + " min");
                        textStatusSolo.setText(duracaoAlarme + " min");
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("duracao", duracaoAlarme);
                        editor.commit();
                        Intent intentConfigNotify = new Intent("notificarConfig");
                        intentConfigNotify.putExtra("mensagem", mensagemRecebida);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentConfigNotify);
                    } else if (mensagemRecebida.equals("multiplos ligado")) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("multiplos", true);
                        editor.commit();
                        multiAlarmes = true;
                        Intent intentConfigNotify = new Intent("notificarConfig");
                        intentConfigNotify.putExtra("mensagem", mensagemRecebida);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentConfigNotify);
                        textStatusSolo.setText(mensagemRecebida);
                    } else if (mensagemRecebida.equals("multiplos desligado")) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("multiplos", false);
                        editor.commit();
                        multiAlarmes = false;
                        Intent intentConfigNotify = new Intent("notificarConfig");
                        intentConfigNotify.putExtra("mensagem", mensagemRecebida);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentConfigNotify);
                        textStatusSolo.setText(mensagemRecebida);
                    }
                }
            }
        }
    };

    // cria canal de notificação necessário android 8+
    public void criarCanalDeNotificacaoSolo(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence nomeCanal = "Alarme Solo";
            String descricaoCanal = "Duração do temporizador solo atingida";
            int importancia = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(idCanalSolo, nomeCanal, importancia);
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
                textTempoSolo.setText(String.format("%02d:%02d", minutos, segundos));

                handlerSolo.postDelayed(tempoAproximado, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modo_solo);

        // Muda Título da Activity
        this.setTitle("Temporizador Remoto - Modo Solo");

        // iniciar componentes da view
        textStatusSolo = findViewById(R.id.textStatusSolo);
        textDuracaoSolo = findViewById(R.id.textDuracaoSolo);
        textTempoSolo = findViewById(R.id.textTempoSolo);
        textTempoSolo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!textStatusSolo.getText().toString().equals("Temporizador parado!") &&
                        !textStatusSolo.getText().toString().equals("Temporizador local parado!")) {
                    new AlertDialog.Builder(ModoSolo.this)
                            .setMessage("Você deseja parar o temporizador?")
                            .setCancelable(false)
                            .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parado = true;
                                    cancelarAlarme();
                                    textStatusSolo.setText("Temporizador local parado!");
                            }})
                            .setNegativeButton("Não", null)
                            .show();
                }
            }
        });

        context = this;

        // iniciar lista iniciados
        recyclerViewIniciados = findViewById(R.id.recyclerViewIniciados);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewIniciados.setLayoutManager(layoutManager);

        // Popular lista iniciados
        iniciados = new ArrayList<>();
        // com JSON SharedPreferences
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

        // listar iniciados
        adapter = new IniciadosAdapter(iniciados, sharedPreferences);
        recyclerViewIniciados.setAdapter(adapter);
        recyclerViewIniciados.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // criar canal de notificação (alarme do temporizador)
        criarCanalDeNotificacaoSolo();

        // BroadcastReceiver para filtrar notificação de alarme
        IntentFilter intentFilter1 = new IntentFilter("tocarAlarmeSolo");
        context.registerReceiver(mReceiverSolo, intentFilter1);
        // Local broadcast para filtar mensagem das configurações
        IntentFilter intentFilter2 = new IntentFilter("mensagemModoSolo");
        LocalBroadcastManager.getInstance(context).registerReceiver(mReceiverSolo, intentFilter2);

        // recuperar outros dados salvos
        if(sharedPreferences.contains("multiplos")){
            multiAlarmes = sharedPreferences.getBoolean("multiplos", false);
        }

        if(sharedPreferences.contains("duracao")){
            duracaoAlarme = sharedPreferences.getInt("duracao", 20);
            textDuracaoSolo.setText(duracaoAlarme + " min");
        }
        textStatusSolo.setText(duracaoAlarme + " min");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // recupera duração salva
        if(sharedPreferences.contains("duracao")){
            duracaoAlarme = sharedPreferences.getInt("duracao", 20);
            textDuracaoSolo.setText(duracaoAlarme + " min");
        }
    }

    // iniciar gerenciador de alarme cria um alarme que dispara o broadcast que cria uma notificação
    private void iniciarAlarmeSolo(){
        if(iniciados.size() > 0){
            long dataIniciado = iniciados.get(0).getData();
            long duracaoMilis = contadorNotificacoes * duracaoAlarme * 60 * 1000;
            long alarmeData = dataIniciado + duracaoMilis;

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intentBroadcast = new Intent("tocarAlarmeSolo");
            intentBroadcast.putExtra("alarmeSolo", "notificar");
            PendingIntent alarmeIntent = PendingIntent.getBroadcast(context, pendingIntentRequestCode, intentBroadcast, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmeData, alarmeIntent);

            parado = false;
            tempoAproximado.run();
        }
    }

    // cancela alarme definido
    private void cancelarAlarme(){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentBroadcast = new Intent("tocarAlarmeSolo");
        intentBroadcast.putExtra("alarmeSolo", "notificar");
        PendingIntent alarmeIntent = PendingIntent.getBroadcast(context, pendingIntentRequestCode, intentBroadcast, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(alarmeIntent);
    }

    // Métodos Botões

    // pede senha e abre activity de configuração
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
                        // resgatar senha salva
                        String senha;
                        if (sharedPreferences.contains("senha")) {
                            senha = sharedPreferences.getString("senha", null);
                        } else {
                            senha = "0000";
                        }
                        // testar autenticidade
                        if(senhaDigitada.getText().toString().equals(senha)){
                            Intent intentConfig = new Intent(view.getContext(), Configuracoes.class);
                            intentConfig.putExtra("modoSolo", true);
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

    // iniciar ou reiniciar o temporizador remotamente */
    public void iniciarSolo(View view) {
        new AlertDialog.Builder(this)
                .setMessage("Você desejar iniciar ou reiniciar tempo?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        iniciarLocal();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    // inicia ou reinicia temporizador solo
    private void iniciarLocal() {
        Iniciado iniciado = criarIniciadoAgora();
        iniciados.add(0, iniciado);
        adapter.notifyDataSetChanged();
        salvarLista();
        contadorNotificacoes = 1;
        iniciarAlarmeSolo();
    }

    // cria um objeto Iniciado com a data e hora do momento
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

    // limpa lista de iniciados
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

    // Métodos outros

    // salva a lista de iniciados
    private void salvarLista() {
        // montar um JSON Array da lista de iniciados para salvar
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
        // salvar JSON Array iniciados no sharedPreferences
        String jsonArrayString = jsonArray.toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("iniciados",jsonArrayString);
        editor.commit();
    }

    // pede confirmação para sair do aplicativo
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Você deseja fechar o aplicativo?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ModoSolo.super.onBackPressed();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        // necessário desregistar o broadcastreceiver e cancelar alarme
        unregisterReceiver(mReceiverSolo);
        cancelarAlarme();
        super.onDestroy();
    }
}