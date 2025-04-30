package de.shurablack;

import org.apache.logging.log4j.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class SSLBuilder {

    /*
    private SSLBuilder() { }

    public static SSLContext createSSLContext(final Logger logger) {
        SSLContext context;
        char[] keyPassword = "keyPassword".toCharArray();
        try {
            context = SSLContext.getInstance("TLSv1.2");
            char[] password = "password".toCharArray();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream("resources/certificate.jks");
            keyStore.load(fis, password);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, "password".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);

            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return context;
        } catch (Exception e) {
            logger.error("Failed to create SSL context: {}", e.getMessage());
            return null;
        }
    }

     */

    private SSLBuilder() { }

    public static SSLContext createSSLContext(final Logger logger) {
        SSLContext context;
        char[] keyPassword = "keyPassword".toCharArray();
        try {
            context = SSLContext.getInstance("TLSv1.2");

            byte[] certBytes = parseDERFromPEM(Files.readAllBytes(new File("certificate/cert.pem").toPath()),
                    "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");

            byte[] keyBytes = parseDERFromPEM(Files.readAllBytes(new File("certificate/privkey.pem").toPath()),
                    "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");

            X509Certificate cert = generateCertificateFromDER(certBytes);
            PrivateKey key = generatePrivateKeyFromDER(keyBytes);

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("cert-alias", cert);
            keystore.setKeyEntry("key-alias", key, keyPassword, new Certificate[]{cert});

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, keyPassword);

            KeyManager[] km = kmf.getKeyManagers();

            context.init(km, null, null);
            return context;
        } catch (Exception e) {
            logger.error("Failed to create SSL context: {}", e.getMessage());
            return null;
        }
    }

    protected static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    protected static PrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory factory = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return factory.generatePrivate(spec);
    }

    protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }
}
