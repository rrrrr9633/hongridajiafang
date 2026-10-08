package top.sama.haode.order.domain;

import java.security.SecureRandom;

public final class InviteCodes {
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCodes() {
    }

    public static String random() {
        char[] chars = new char[6];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
