package com.university.sms.core.verify;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * NOVEL FEATURE - tamper-proof marksheets (Anjay: Unit 3 streams + Unit 2 exceptions).
 *
 * <p>Every official marksheet carries a 16-character verification code such as
 * {@code 7F3A-91C2-0BDE-44A8}. The code is an <b>HMAC-SHA256</b> of the marksheet's content
 * (roll number, every subject total and grade, SGPA) keyed with a server secret, so:</p>
 * <ul>
 *   <li>an employer can type roll + code on the public {@code /verify} page and see the real result;</li>
 *   <li>an edited PDF (changed marks) produces a different code → reported as MISMATCH;</li>
 *   <li>nobody can create a valid code without the secret key.</li>
 * </ul>
 * The canonical bytes are written with a {@link DataOutputStream} into a {@link ByteArrayOutputStream},
 * so the same data always gives the same bytes (sorted subjects, fixed number formats).
 */
public class MarksheetFingerprint {

    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKeySpec key;

    public MarksheetFingerprint(String secret) {
        if (secret == null || secret.length() < 8) {
            throw new IllegalArgumentException("Verification secret must be at least 8 characters");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** Builds the canonical byte representation with data streams. */
    static byte[] canonicalBytes(String rollNumber, List<MarksheetLine> lines, double sgpa) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(rollNumber.trim().toUpperCase());
            List<MarksheetLine> sorted = lines.stream()
                    .sorted(Comparator.comparing(MarksheetLine::subjectCode)).toList();
            out.writeInt(sorted.size());
            for (MarksheetLine l : sorted) {
                out.writeUTF(l.subjectCode());
                out.writeUTF(String.format("%.1f", l.total()));
                out.writeUTF(l.grade());
            }
            out.writeUTF(String.format("%.2f", sgpa));
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            // cannot happen for in-memory streams, but the checked exception must be handled
            throw new IllegalStateException("Could not serialise marksheet", e);
        }
    }

    /** Returns the code formatted as XXXX-XXXX-XXXX-XXXX. */
    public String codeFor(String rollNumber, List<MarksheetLine> lines, double sgpa) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal(canonicalBytes(rollNumber, lines, sgpa));
            String hex = HexFormat.of().withUpperCase().formatHex(digest, 0, 8);
            return hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC not available", e);
        }
    }

    /** Constant-time comparison so the check does not leak how many characters matched. */
    public boolean matches(String expectedCode, String providedCode) {
        if (providedCode == null) {
            return false;
        }
        String clean = providedCode.trim().toUpperCase().replace(" ", "");
        return MessageDigest.isEqual(expectedCode.getBytes(StandardCharsets.UTF_8),
                clean.getBytes(StandardCharsets.UTF_8));
    }
}
