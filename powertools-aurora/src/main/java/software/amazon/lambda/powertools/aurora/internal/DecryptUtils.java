package software.amazon.lambda.powertools.aurora.internal;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoInputStream;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import software.amazon.awssdk.utils.IoUtils;

import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class DecryptUtils {

    private static AwsCrypto crypto = AwsCrypto.builder().withCommitmentPolicy(CommitmentPolicy.RequireEncryptAllowDecrypt).build();

    public static byte[] decrypt(final byte[] decoded, final byte[] decodedDataKey) throws IOException {

        final JceMasterKey masterKey = JceMasterKey.getInstance(
                new SecretKeySpec(decodedDataKey, "AES"),
                "BC",
                "DataKey",
                "AES/GCM/NoPadding");

        try (final CryptoInputStream<JceMasterKey> decryptingStream = crypto.createDecryptingStream(masterKey,
                new ByteArrayInputStream(decoded)); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IoUtils.copy(decryptingStream, out);
            return decompress(out.toByteArray());
        }
    }

    private static byte[] decompress(final byte[] src) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(src);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        return IoUtils.toByteArray(gzipInputStream);
    }
}
