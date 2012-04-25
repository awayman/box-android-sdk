package com.box.onecloud.android;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Crypto class.
 * 
 */
public final class Crypto {

    /** Use the BouncyCastle provider. */
    private static final String PROVIDER = "BC";

    /** Length of salt (in bytes). */
    private static final int SALT_LENGTH = 20;

    /** Length of initialization vector. */
    private static final int IV_LENGTH = 16;

    /** Iteration count when generating a secret key. */
    private static final int PBE_ITERATION_COUNT = 100;

    /** Algorithm for generating random bytes. Used to generate an initialization vector, and generate a salt. */
    private static final String RANDOM_ALGORITHM = "SHA1PRNG";

    /** Encryption algorithm. */
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    /** Algorithm for generating a secret key. */
    private static final String SECRET_KEY_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";

    /** Algorithm for generating a secret key. */
    private static final String SECRET_KEY_SPEC_ALGORITHM = "AES";

    /** Key length. */
    private static final int KEY_LENGTH = 256;

    /** Hex chars (used in asHex()). */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * Private constructor.
     */
    private Crypto() {
    }

    /**
     * Get an output stream to which will encrypt data as it is being written out.
     * 
     * @param streamOut
     *            OutputStream to which data will be written.
     * @param key
     *            Encryption key.
     * @param salt
     *            Salt. Use Crypto.generateSalt().
     * @return An output stream.
     * @throws CryptoException
     *             Exception thrown if there are any problems.
     */
    public static OutputStream getOutputStream(final OutputStream streamOut, final String key, final String salt) throws CryptoException {
        try {
            byte[] iv = generateIv();
            SecretKey secretKey = getSecretKey(key, salt);
            Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
            encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            streamOut.write(iv);
            return new CipherOutputStream(streamOut, encryptionCipher);
        }
        catch (Exception e) {
            throw new CryptoException();
        }
    }

    /**
     * Get an input stream that will decrypt data as it is being read in.
     * 
     * @param streamIn
     *            The inputstream pointing to the encrypted data.
     * @param key
     *            Encryption key.
     * @param salt
     *            Salt. This was set when the data was encrypted so you have to know what it was.
     * @return An input stream that returns decrypted data as the stream is read from.
     * @throws CryptoException
     *             Exception thrown if there are any problems.
     */
    public static InputStream getInputStream(final InputStream streamIn, final String key, final String salt) throws CryptoException {
        try {
            byte[] iv = new byte[IV_LENGTH];
            streamIn.read(iv);

            SecretKey secretKey = getSecretKey(key, salt);
            Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
            encryptionCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new CipherInputStream(streamIn, encryptionCipher);
        }
        catch (Exception e) {
            throw new CryptoException();
        }
    }

    /**
     * Default exception thrown for crypto errors.
     * 
     */
    public static class CryptoException extends Exception {

        /** Serial version id. */
        private static final long serialVersionUID = 1L;
    }

    /**
     * Get a SecretKey from a password and a salt.
     * 
     * @param password
     *            Password.
     * @param salt
     *            Salt.
     * @return A SecretKey that can be used for crypto.
     * @throws CryptoException
     *             Exception thrown if there are any problems.
     */
    private static SecretKey getSecretKey(final String password, final String salt) throws CryptoException {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), PBE_ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM, PROVIDER);
            SecretKey tmp = factory.generateSecret(pbeKeySpec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_SPEC_ALGORITHM);
            return secret;
        }
        catch (Exception e) {
            throw new CryptoException();
        }
    }

    /**
     * Generate an initialization vector.
     * 
     * @return An array of bytes which can be used as an initialization vector.
     * @throws NoSuchAlgorithmException
     *             Thrown if RANDOM_ALGORITHM could not be used.
     * @throws NoSuchProviderException
     *             Thrown if BC provider was not available.
     */
    private static byte[] generateIv() throws NoSuchAlgorithmException, NoSuchProviderException {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    /**
     * Generate a salt.
     * 
     * @return A string salt.
     * @throws CryptoException
     *             Exception thrown if there are any problems.
     */
    public static String generateSalt() throws CryptoException {
        try {
            SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            String saltHex = asHex(salt);
            return saltHex;
        }
        catch (Exception e) {
            throw new CryptoException();
        }
    }

    /**
     * Converts an byte array to a hex string. http://forums.xkcd.com/viewtopic.php?f=11&t=16666&p=553936#p459685
     * 
     * @param buf
     *            Array of bytes.
     * @return hex string
     */
    private static String asHex(final byte[] buf) {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }
}
