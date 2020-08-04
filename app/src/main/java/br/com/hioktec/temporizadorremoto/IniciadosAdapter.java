package br.com.hioktec.temporizadorremoto;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class IniciadosAdapter extends RecyclerView.Adapter<IniciadosHolder> {

    private List<Iniciado> iniciados;
    private SharedPreferences sharedPreferences;

    public IniciadosAdapter(List<Iniciado> iniciados, SharedPreferences sharedPreferences) {
        this.iniciados = iniciados;
        this.sharedPreferences = sharedPreferences;
    }

    @NonNull
    @Override
    public IniciadosHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IniciadosHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_iniciado, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull IniciadosHolder holder, final int position) {
        final Iniciado iniciado = iniciados.get(position);

        holder.textDia.setText(iniciado.getDia());
        holder.textHora.setText(iniciado.getHora());
        final Context context = holder.btiExcluir.getContext();
        holder.btiExcluir.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Você deseja excluir item?");
                builder.setCancelable(false);
                builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        iniciados.remove(iniciado);
                        notifyDataSetChanged();
                        salvarLista();
                    }
                });
                builder.setNegativeButton("Não", null);
                builder.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return iniciados != null ? iniciados.size() : 0;
    }

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
}