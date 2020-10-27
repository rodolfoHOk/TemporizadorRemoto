package br.com.hioktec.temporizadorremoto;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class IniciadosHolder extends RecyclerView.ViewHolder {

    public TextView textDia;
    public TextView textHora;
    public ImageButton btiExcluir;

    public IniciadosHolder(@NonNull View itemView) {
        super(itemView);
        textDia = itemView.findViewById(R.id.textDia);
        textHora = itemView.findViewById(R.id.textHora);
        btiExcluir = itemView.findViewById(R.id.btiExcluir);
    }
}