package cn.tazumesans.xinmeiti.common;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 口令哈希：PBKDF2WithHmacSHA256（JDK 内置，无需第三方依赖）
 * 存储格式：pbkdf2$迭代次数$salt(base64)$hash(base64)
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LEN = 256;
    private static final int SALT_LEN = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String plain) {
        byte[] salt = new byte[SALT_LEN];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plain.toCharArray(), salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + b64(salt) + "$" + b64(hash);
    }

    public static boolean matches(String plain, String stored) {
        if (plain == null || stored == null || !stored.startsWith("pbkdf2$")) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        int iter = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = pbkdf2(plain.toCharArray(), salt, iter);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(char[] chars, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_LEN);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = f.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("口令加密失败", e);
        }
    }

    private static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    /** 生成随机初始密码，去掉易混淆字符 */
    public static String randomPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
