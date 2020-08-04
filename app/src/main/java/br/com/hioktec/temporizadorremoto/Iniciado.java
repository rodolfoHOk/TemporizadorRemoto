package br.com.hioktec.temporizadorremoto;

import java.io.Serializable;

public class Iniciado implements Serializable {

    private String dia;
    private String hora;
    private Long data;

    public Iniciado(String dia, String hora, Long data) {
        this.dia = dia;
        this.hora = hora;
        this.data = data;
    }

    public String getDia() {
        return dia;
    }

    public void setDia(String dia) {
        this.dia = dia;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public Long getData() {
        return data;
    }

    public void setData(Long data) {
        this.data = data;
    }
}
