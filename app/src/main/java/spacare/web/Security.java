package spacare.web;

import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public final class Security {

  public static String token() {
    byte[] b = new byte[32];
    new SecureRandom().nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public static String hash(String pw) throws Exception {
    String salt = token();
    return salt + "$" + Base64.getEncoder().encodeToString(derive(pw, salt));
  }

  static byte[] derive(String pw, String salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(
      pw.toCharArray(),
      salt.getBytes(java.nio.charset.StandardCharsets.UTF_8),
      210000,
      256
    );
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(spec)
        .getEncoded();
    } finally {
      spec.clearPassword();
    }
  }

  public static boolean verify(String pw, String hash) throws Exception {
    String[] p = hash.split("\\$");
    return (
      p.length == 2 &&
      MessageDigest.isEqual(derive(pw, p[0]), Base64.getDecoder().decode(p[1]))
    );
  }
}
