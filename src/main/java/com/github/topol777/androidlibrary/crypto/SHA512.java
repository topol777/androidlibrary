package com.github.topol777.androidlibrary.crypto;

import java.security.MessageDigest;

public class SHA512 {
    public static byte[] digest(byte[] buf) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update(buf);
            return digest.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
