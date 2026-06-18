package egovframework.let.pmc.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 해시/토큰 유틸. API 키는 평문 저장하지 않고 sha-256 해시만 보관한다.
 */
public final class Hashing {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Hashing() {
    }

    /** sha-256 hex */
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 랜덤 토큰(hex) */
    public static String randomToken(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return toHex(b);
    }

    private static String toHex(byte[] b) {
        char[] out = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
