package org.scoula.deposit.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 계좌번호 암복호화 (AES-256-GCM).
 *
 * <p>키는 DB 밖(환경변수)에 두어, DB 접근 권한만으로는 복호화할 수 없게 한다.
 * 매 호출마다 새 IV를 생성해 같은 계좌번호도 저장값이 달라진다.
 *
 * <p>저장 형식 : Base64( IV(12) + 암호문 + 인증태그(16) )
 */
public final class AccountCrypto {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private static final SecretKeySpec KEY = loadKey();
    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountCrypto() {
    }

    private static SecretKeySpec loadKey() {
        String raw = System.getenv("DEPOSIT_ENC_KEY");

        if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "환경변수 DEPOSIT_ENC_KEY 가 없거나 32바이트 미만입니다. "
                            + "톰캣 실행 구성의 Environment variables 를 확인해주세요.");
        }

        byte[] key = new byte[32];
        System.arraycopy(raw.getBytes(StandardCharsets.UTF_8), 0, key, 0, 32);
        return new SecretKeySpec(key, "AES");
    }

    /** 평문 → Base64 암호문. null 이나 빈 값은 그대로 통과시킨다. */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LEN];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("계좌번호 암호화에 실패했습니다", e);
        }
    }

    /** Base64 암호문 → 평문. 복호화 실패 시 null 을 돌려 화면이 죽지 않게 한다. */
    public static String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return encoded;
        }
        try {
            byte[] all = Base64.getDecoder().decode(encoded);

            byte[] iv = new byte[IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));

            byte[] plain = cipher.doFinal(all, IV_LEN, all.length - IV_LEN);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 마이그레이션 전 평문이 남아 있거나 키가 바뀐 경우
            return null;
        }
    }
}