package br.com.hioktec.temporizadorremoto;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import static android.graphics.Color.rgb;

public class Configuracoes extends AppCompatActivity {

    private static TextView textStatusConfig;
    private static TextView textMACDispositivo;
    private static EditText editTextDuracao;
    private static Switch switchMultiplos;

    private static final int SELECIONAR_DISPOSITIVO = 2;
    private static final int SELECIONAR_SOM_ALARME = 3;
    private static final int SELECIONAR_SOM_NOTIFICACAO = 4;

    private static String macDispositivo;

    private static int duracao;

    private static BluetoothManagerThread gerenciadorConexao;

    private static Context context;

    private static SharedPreferences sharedPreferences;
    private static boolean doSharedPref = false;

    private static boolean modoSolo = false;

    /* BroadcastReceiver - para receber intent do modo controle */
    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mensagem = intent.getStringExtra("mensagem");
            textStatusConfig.setText(mensagem);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);
        this.setTitle("Temporizador Remoto - Configurações");

        // resgata valor modoSolo
        modoSolo = getIntent().getBooleanExtra("modoSolo", false);

        // iniciar componentes da view
        textStatusConfig = findViewById(R.id.textStatusConfig);
        textMACDispositivo = findViewById(R.id.textMACDispositivo);

        editTextDuracao = findViewById(R.id.editTextDuracao);

        editTextDuracao.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(modoSolo){
                        mudarDuracao(editTextDuracao.getText().toString());
                    } else {
                        enviarDuracao(editTextDuracao.getText().toString());
                    }
                }
                return false;
            }
        });

        switchMultiplos = findViewById(R.id.switchMultiplos);

        switchMultiplos.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String enviar;
                if(!doSharedPref){
                    if (switchMultiplos.isChecked()) {
                        if(!modoSolo) {
                            if (gerenciadorConexao != null) {
                                enviar = "multiplosTrue";
                                gerenciadorConexao.write(enviar.getBytes());
                            } else {
                                switchMultiplos.setChecked(false);
                                toastNaoConectado();
                            }
                        } else {
                            String mensagem = "multiplos ligado";
                            Intent intentMensagemSolo = new Intent("mensagemModoSolo");
                            intentMensagemSolo.putExtra("mensagemSolo", mensagem);
                            System.out.println("config: " + mensagem);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intentMensagemSolo);
                        }
                    } else if (!switchMultiplos.isChecked()){
                        if(!modoSolo) {
                            if (gerenciadorConexao != null) {
                                enviar = "multiplosFalse";
                                gerenciadorConexao.write(enviar.getBytes());
                            } else {
                                switchMultiplos.setChecked(true);
                                toastNaoConectado();
                            }
                        } else {
                            String mensagem = "multiplos desligado";
                            Intent intentMensagemSolo = new Intent("mensagemModoSolo");
                            intentMensagemSolo.putExtra("mensagemSolo", mensagem);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intentMensagemSolo);
                        }
                    }
                } else {
                    doSharedPref = false;
                }
            }
        });

        /* iniciar atributos */
        context = this;
        gerenciadorConexao = ModoControle.gerenciadorConexao;

        /* LocalBroadcastManager para filtar mensagem do modo controle */
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("notificarConfig"));

        /* recuperar endereco MAC do dispositivo salvo */
        sharedPreferences = getSharedPreferences(ModoControle.PREFS_NAME, MODE_PRIVATE);
        if (sharedPreferences.contains("MAC_dispositivo")) {
            macDispositivo = sharedPreferences.getString("MAC_dispositivo", null);
        } else {
            macDispositivo = "00:00:00:00:00:00";
        }
        textMACDispositivo.setText(macDispositivo);

        /* recuperar duração salva */
        if (sharedPreferences.contains("duracao")) {
            duracao = sharedPreferences.getInt("duracao", 20);
        } else {
            duracao = 20;
        }
        editTextDuracao.setHint(String.valueOf(duracao));

        /* recuperar multiplos salvo */
        if(sharedPreferences.contains("multiplos")){
            boolean multiAlarme = sharedPreferences.getBoolean("multiplos", false);
            if(multiAlarme == true) {
                doSharedPref = true;
            }
            switchMultiplos.setChecked(multiAlarme);
        }
    }

    // no retorno de resultado de outras activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        switch (requestCode){
            case SELECIONAR_DISPOSITIVO:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    String MAC = data.getStringExtra("enderecoDispositivo");
                    textStatusConfig.setText("Você selecionou " + data.getStringExtra("nomeDispositivo")
                            + "\n" + MAC);
                    textMACDispositivo.setText(MAC);
                    // salvar MAC do dispositivo selecionado
                    editor.putString("MAC_dispositivo", MAC);
                    editor.commit();
                } else {
                    textStatusConfig.setText("Nenhum dispositivo selecionado!");
                }
                break;

            case SELECIONAR_SOM_ALARME:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    Uri uriAlarme = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uriAlarme != null) {
                        editor.putString("uri_alarme", uriAlarme.toString());
                        editor.commit();
                        Toast.makeText(this, "Som de alarme salvo!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case SELECIONAR_SOM_NOTIFICACAO:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    Uri uriNotificacao = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uriNotificacao != null) {
                        editor.putString("uri_notificacao", uriNotificacao.toString());
                        editor.commit();
                        Toast.makeText(this, "Som de notificação salvo!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    /**
     * envia mensagem para mudar o tempo de duração do temporizador
     * @param duracao
     */
    private void enviarDuracao(final String duracao) {
        new AlertDialog.Builder(this)
                .setMessage("Confirma mudar duração para " + duracao + "min?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (gerenciadorConexao != null) {
                            String enviar = "duracao =" + duracao;
                            gerenciadorConexao.write(enviar.getBytes());
                        } else {
                            toastNaoConectado();
                        }
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    /**
     * pede para o modo solo mudar a duração do temporizador
     * @param duracao
     */
    private void mudarDuracao(final String duracao) {
        new AlertDialog.Builder(this)
                .setMessage("Confirma mudar duração para " + duracao + "min?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String mensagem = "mudar duração para " + duracao + " min!";
                        Intent intentMensagemSolo = new Intent("mensagemModoSolo");
                        intentMensagemSolo.putExtra("mensagemSolo", mensagem);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentMensagemSolo);
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    /**
     * toast(pop up) de aviso não conectado a um display
     * fundo vermelho e no centro da tela
     */
    private void toastNaoConectado(){
        Toast toast = Toast.makeText(Configuracoes.this, "não conectado", Toast.LENGTH_LONG);
        View view = toast.getView();
        view.getBackground().setColorFilter(rgb(255,0,0), PorterDuff.Mode.SRC_IN);
        TextView textView = view.findViewById(android.R.id.message);
        textView.setTextColor(rgb(255,255,255));
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }

    // métodos dos Botões

    /**
     * buscar dispositivos bluetooth
     * @param view
     */
    public void buscarDispositivos(View view) {
        Intent buscarPareadosIntent = new Intent(this, DispositivosBluetooth.class);
        startActivityForResult(buscarPareadosIntent, SELECIONAR_DISPOSITIVO);
    }

    /**
     * mudar senha
     * @param view
     */
    public void mudarSenha(View view){
        View mudarSenhaView = LayoutInflater.from(this).inflate(R.layout.senha_mudar, null);
        AlertDialog.Builder mudarSenhaDialog = new AlertDialog.Builder(this).setView(mudarSenhaView);

        final EditText mudarSenhaDigitada = mudarSenhaView.findViewById(R.id.editTextSenhaMudar);
        final EditText confirmarSenhaDigitada = mudarSenhaView.findViewById(R.id.editTextSenhaConfirmar);

        mudarSenhaDialog.setCancelable(false)
                .setMessage("Digite nova senha:")
                .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!mudarSenhaDigitada.getText().toString().equals("")){
                            if(mudarSenhaDigitada.getText().toString().equals(confirmarSenhaDigitada.getText().toString())){
                                SharedPreferences sharedPreferences = getSharedPreferences(ModoControle.PREFS_NAME, MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("senha", mudarSenhaDigitada.getText().toString());
                                editor.commit();
                            } else {
                                mudarSenhaDigitada.setText("");
                                confirmarSenhaDigitada.setText("");
                                Toast toast = Toast.makeText(context, "Campos digitados não são iguais",Toast.LENGTH_LONG);
                                View view = toast.getView();
                                view.getBackground().setColorFilter(rgb(255,0,0), PorterDuff.Mode.SRC_IN);
                                TextView textView = view.findViewById(android.R.id.message);
                                textView.setTextColor(rgb(255,255,255));
                                toast.setGravity(Gravity.CENTER,0,0);
                                toast.show();
                            }
                        } else{
                            mudarSenhaDigitada.setText("");
                            confirmarSenhaDigitada.setText("");
                            Toast toast = Toast.makeText(context, "Senha não pode ser nula!",Toast.LENGTH_LONG);
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

    /**
     * selecionar som do alarme do display
     * @param view
     */
    public void somAlarme(View view){
        Intent intentSomAlarme = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intentSomAlarme.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intentSomAlarme.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Selecione som de alarme do display");
        intentSomAlarme.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        this.startActivityForResult(intentSomAlarme, SELECIONAR_SOM_ALARME);
    }

    /**
     * selecionar som do notificação do controle
     * @param view
     */
    public void somNotificacao(View view){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, ModoControle.idCanal);
            startActivity(intent);
        } else {
            Intent intentSomNotificacao = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intentSomNotificacao.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intentSomNotificacao.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Selecione som de notificação do controle");
            intentSomNotificacao.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
            this.startActivityForResult(intentSomNotificacao, SELECIONAR_SOM_NOTIFICACAO);
        }
    }

    /**
     * reseta todas as configurações
     * @param view
     */
    public void resetConfig(View view){
        SharedPreferences sharedPreferences = getSharedPreferences(ModoControle.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * botão voltar
     * @param view
     */
    public void voltar(View view){
        super.onBackPressed();
    }
}