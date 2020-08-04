package br.com.hioktec.temporizadorremoto;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void paraModoDisplay(View view){
        Intent intentModoDisplay = new Intent(this,ModoDisplay.class);
        startActivity(intentModoDisplay);
        /* termina activity - para não poder voltar mais para ela */
        this.finish();
    }

    public void paraModoControle(View view){
        Intent intentModoControle = new Intent(this, ModoControle.class);
        startActivity(intentModoControle);
        /* termina activity - para não poder voltar mais para ela */
        this.finish();
    }
}
