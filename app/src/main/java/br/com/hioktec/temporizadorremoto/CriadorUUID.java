package br.com.hioktec.temporizadorremoto;

import java.util.UUID;

public class CriadorUUID {
    public static void main(String[] args) {

        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();

        System.out.println(uuidString);
    }
}
