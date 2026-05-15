package com.example.chocolateria.controller;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// sha256 con salt, las viejas se migran solas al entrar
public class CifradoUtil {

    private static final String SEPARADOR = ":SHA256:";


    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            return saltB64 + SEPARADOR + hashB64;

        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar la contraseña: " + "Ocurrió un error. Intente de nuevo.", e);
        }
    }


    public static boolean verificarPassword(String passwordIngresado, String storedValue) {
        if (passwordIngresado == null || storedValue == null) return false;

        // si no tiene separador es clave vieja, comparacion directa
        if (!storedValue.contains(SEPARADOR)) {
            return passwordIngresado.equals(storedValue);
        }

        try {
            String[] partes = storedValue.split(SEPARADOR, 2);
            byte[] salt             = Base64.getDecoder().decode(partes[0]);
            byte[] hashAlmacenado   = Base64.getDecoder().decode(partes[1]);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashIngresado = md.digest(passwordIngresado.getBytes("UTF-8"));

            // isEqual compara byte por byte, mismo tiempo siempre
            return MessageDigest.isEqual(hashIngresado, hashAlmacenado);

        } catch (Exception e) {
            return false;
        }
    }

    // si tiene separador es hash, si no es texto plano
    public static boolean esHashSeguro(String storedValue) {
        return storedValue != null && storedValue.contains(SEPARADOR);
    }
}
