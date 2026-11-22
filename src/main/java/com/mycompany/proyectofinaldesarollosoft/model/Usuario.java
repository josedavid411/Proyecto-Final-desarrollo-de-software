package com.mycompany.proyectofinaldesarollosoft.model;

import com.mycompany.proyectofinaldesarollosoft.enums.Rol;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public abstract class Usuario {
    private static final int ITERACIONES_HASH = 120_000;
    private static final int LONGITUD_HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String id;
    private final String nombre;
    private final String username;
    private final String credencial;
    private final Rol rol;

    protected Usuario(String id, String nombre, String username, String password, Rol rol) {
        this(id, nombre, username, hashPassword(textoRequerido(password, "password")), rol, true);
    }

    protected Usuario(String id, String nombre, String username, String credencial, Rol rol, boolean credencialHasheada) {
        this.id = textoRequerido(id, "id");
        this.nombre = textoRequerido(nombre, "nombre");
        this.username = textoRequerido(username, "username");
        this.credencial = textoRequerido(credencial, "credencial");
        this.rol = Objects.requireNonNull(rol, "rol");
    }

    private static String textoRequerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }
        return valor.trim();
    }

    public boolean autenticar(String username, String password) {
        if (!this.username.equals(username) || password == null) {
            return false;
        }
        try {
            String[] partes = credencial.split(":", 2);
            if (partes.length != 2) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(partes[0]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[1]);
            byte[] hashRecibido = derivarHash(password.toCharArray(), salt);
            return MessageDigest.isEqual(hashEsperado, hashRecibido);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String hashPassword(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derivarHash(password.toCharArray(), salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] derivarHash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERACIONES_HASH, LONGITUD_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo proteger la credencial del usuario.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsername() {
        return username;
    }

    public String getCredencialPersistida() {
        return credencial;
    }

    public Rol getRol() {
        return rol;
    }
}
