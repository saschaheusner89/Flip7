// Signiert eine APK mit APK Signature Scheme v2 (apksig) und prüft das Ergebnis.
// v2 reicht, weil die App ohnehin Android 7.0+ (minSdk 24) voraussetzt – dort wird v2 geprüft.
// Aufruf: java -cp apksig.jar Sign.java <in.apk> <out.apk> <keystore.p12> <passwort>
import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

public class Sign {
    public static void main(String[] a) throws Exception {
        File in = new File(a[0]), out = new File(a[1]);
        char[] pw = a[3].toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream f = new FileInputStream(a[2])) { ks.load(f, pw); }
        String alias = ks.aliases().nextElement();
        PrivateKey key = (PrivateKey) ks.getKey(alias, pw);
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        ApkSigner.SignerConfig sc = new ApkSigner.SignerConfig.Builder("FLIP7", key, Collections.singletonList(cert)).build();
        new ApkSigner.Builder(Collections.singletonList(sc))
                .setInputApk(in).setOutputApk(out).setMinSdkVersion(24)
                .setV1SigningEnabled(false).setV2SigningEnabled(true)
                .build().sign();
        ApkVerifier.Result r = new ApkVerifier.Builder(out).build().verify();
        System.out.println("signiert: verified=" + r.isVerified() + " v2=" + r.isVerifiedUsingV2Scheme());
        for (Object e : r.getErrors()) System.out.println("FEHLER " + e);
        for (Object w : r.getWarnings()) System.out.println("Warnung " + w);
        if (!r.isVerified()) System.exit(1);
    }
}
