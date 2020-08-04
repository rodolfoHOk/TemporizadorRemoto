package br.com.hioktec.temporizadorremoto;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DispositivosAdapter extends RecyclerView.Adapter<DispositivosHolder> {

    private List<Dispositivo> dispositivos;

    public DispositivosAdapter(List<Dispositivo> dispositivos) {
        this.dispositivos = dispositivos;
    }

    @NonNull
    @Override
    public DispositivosHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DispositivosHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dispositivo, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DispositivosHolder holder, int position) {
        Dispositivo dispositivo = dispositivos.get(position);

        holder.textNomeDispositivo.setText(dispositivo.getNome());
        holder.textEnderecoDispositivo.setText(dispositivo.getEndereco());
        holder.textEncontrado.setText(dispositivo.getEncontrado());
    }

    @Override
    public int getItemCount() {
        return dispositivos != null ? dispositivos.size() : 0;
    }
}
