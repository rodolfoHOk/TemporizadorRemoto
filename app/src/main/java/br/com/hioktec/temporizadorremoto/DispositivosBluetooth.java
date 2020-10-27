package br.com.hioktec.temporizadorremoto;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DispositivosBluetooth extends AppCompatActivity {

    private TextView textRecyclerTitulo;
    private RecyclerView recyclerViewDisp;
    private DispositivosAdapter adapter;
    private List<Dispositivo> dispositivos;
    private BroadcastReceiver receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos_bluetooth);

        /* Muda Título da Activity */
        this.setTitle("Temporizador Remoto - Modo Controle");

        textRecyclerTitulo = findViewById(R.id.textRecyclerTitulo);
        recyclerViewDisp = findViewById(R.id.recyclerViewDisp);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewDisp.setLayoutManager(layoutManager);

        dispositivos = new ArrayList<>();

        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        textRecyclerTitulo.setText("Dispositivos Encontrados");

        /* Dispositivos Pareados */

        Set<BluetoothDevice> dispositivosPareados = bluetoothAdapter.getBondedDevices();

        if(dispositivosPareados.size() > 0){
            for(BluetoothDevice dispositivo : dispositivosPareados){
                dispositivos.add(new Dispositivo(dispositivo.getName(), dispositivo.getAddress(), "pareado"));
            }
        }

        /* Dispositivos Descobertos */

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001 );
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice dispositivoDescoberto = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    assert dispositivoDescoberto != null;
                    dispositivos.add(new Dispositivo(dispositivoDescoberto.getName(), dispositivoDescoberto.getAddress(), "descoberto"));
                    adapter.notifyDataSetChanged();
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        /* Listar Dispositivos */

        adapter = new DispositivosAdapter(dispositivos);
        recyclerViewDisp.setAdapter(adapter);
        recyclerViewDisp.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        /* Para Selecionar um dispositivo */

        recyclerViewDisp.addOnItemTouchListener(new RecyclerItemClickListener(getApplicationContext(), recyclerViewDisp, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                Dispositivo item = dispositivos.get(position);
                String nome = item.getNome();
                String endereco = item.getEndereco();

                Intent intentRetorno = new Intent();
                intentRetorno.putExtra("nomeDispositivo", nome);
                intentRetorno.putExtra("enderecoDispositivo", endereco);
                setResult(RESULT_OK, intentRetorno);
                finish();
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(receiver != null) {
            unregisterReceiver(receiver);
        }
    }
}
