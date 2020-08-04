package br.com.hioktec.temporizadorremoto;

public class Dispositivo {

    public String nome;
    public String endereco;
    public String encontrado;

    public Dispositivo(String nome, String endereco, String encontrado) {
        this.nome = nome;
        this.endereco = endereco;
        this.encontrado = encontrado;
    }

    public String getNome() {
        return nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getEncontrado(){
        return encontrado;
    }
}
