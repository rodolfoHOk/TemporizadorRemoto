package br.com.hioktec.temporizadorremoto;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DispositivosHolder extends RecyclerView.ViewHolder {

    public TextView textNomeDispositivo;
    public TextView textEnderecoDispositivo;
    public TextView textEncontrado;

    public DispositivosHolder(@NonNull View itemView) {
        super(itemView);
        textNomeDispositivo = itemView.findViewById(R.id.textNomeDispositivo);
        textEnderecoDispositivo = itemView.findViewById(R.id.textEnderecoDispositivo);
        textEncontrado = itemView.findViewById(R.id.textEncontrado);
    }
}
