package com.example.geonex;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecurityManager {

    private static final String TAG = "SecurityManager";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "geonex_master_key";
    private static final String PREFS_NAME = "secure_prefs";
    private static final String ENCRYPTED_DATA_PREFIX = "ENC:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final Context context;
    private final SharedPreferences securePrefs;
    private KeyStore keyStore;
    private SecretKey secretKey;

    public SecurityManager(Context context) {
        this.context = context;
        this.securePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initKeyStore();
    }

    /**
     * Initialize Android Keystore
     */
    private void initKeyStore() {
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            // Check if key exists
            if (keyStore.containsAlias(KEY_ALIAS)) {
                // Load existing key
                secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null))
                        .getSecretKey();
                Log.d(TAG, "Loaded existing encryption key");
            } else {
                // Generate new key
                generateKey();
            }
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException |
                 UnrecoverableEntryException e) {
            Log.e(TAG, "Error initializing keystore: " + e.getMessage());
        }
    }

    /**
     * Generate new encryption key in Android Keystore
     */
    private void generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);

            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(false) // Set to true if you want biometric auth
                    .build();

            keyGenerator.init(keyGenParameterSpec);
            secretKey = keyGenerator.generateKey();

            Log.d(TAG, "Generated new encryption key");
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                 | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "Error generating key: " + e.getMessage());
        }
    }

    // ===== ENCRYPTION =====

    /**
     * Encrypt sensitive data
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] iv = cipher.getIV();
            byte[] encryption = cipher.doFinal(plainText.getBytes());

            // Combine IV and encrypted data
            byte[] combined = new byte[GCM_IV_LENGTH + encryption.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(encryption, 0, combined, GCM_IV_LENGTH, encryption.length);

            return ENCRYPTED_DATA_PREFIX + Base64.encodeToString(combined, Base64.DEFAULT);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                 | InvalidKeyException | IllegalBlockSizeException
                 | BadPaddingException e) {
            Log.e(TAG, "Encryption error: " + e.getMessage());
            return plainText; // Fallback to plain text on error
        }
    }

    /**
     * Decrypt sensitive data
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()
                || !encryptedText.startsWith(ENCRYPTED_DATA_PREFIX)) {
            return encryptedText;
        }

        try {
            String base64Data = encryptedText.substring(ENCRYPTED_DATA_PREFIX.length());
            byte[] combined = Base64.decode(base64Data, Base64.DEFAULT);

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryption = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encryption, 0, encryption.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encryption);
            return new String(decrypted);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                 | InvalidKeyException | IllegalBlockSizeException
                 | BadPaddingException | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "Decryption error: " + e.getMessage());
            return encryptedText; // Return as is on error
        }
    }

    // ===== SECURE STORAGE =====

    /**
     * Store sensitive data securely
     */
    public void putSecureString(String key, String value) {
        String encrypted = encrypt(value);
        securePrefs.edit().putString(key, encrypted).apply();
    }

    /**
     * Retrieve sensitive data securely
     */
    public String getSecureString(String key, String defaultValue) {
        String encrypted = securePrefs.getString(key, null);
        if (encrypted == null) {
            return defaultValue;
        }
        return decrypt(encrypted);
    }

    /**
     * Store boolean securely
     */
    public void putSecureBoolean(String key, boolean value) {
        securePrefs.edit().putBoolean(key, value).apply();
    }

    /**
     * Retrieve boolean securely
     */
    public boolean getSecureBoolean(String key, boolean defaultValue) {
        return securePrefs.getBoolean(key, defaultValue);
    }

    // ===== INPUT VALIDATION =====

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    /**
     * Validate location coordinates
     */
    public boolean isValidLocation(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    /**
     * Sanitize input to prevent injection
     */
    public String sanitizeInput(String input) {
        if (input == null) return "";

        // Remove any potential harmful characters
        return input.replaceAll("[<>\"'%;()&+]", "")
                .trim();
    }

    /**
     * Validate radius (prevent negative or too large)
     */
    public boolean isValidRadius(float radius) {
        return radius >= 100 && radius <= 5000;
    }

    /**
     * Validate title length
     */
    public boolean isValidTitle(String title) {
        return title != null && title.length() >= 3 && title.length() <= 100;
    }

    // ===== BIOMETRIC AUTHENTICATION =====

    /**
     * Check if biometric authentication is available
     */
    public boolean isBiometricAvailable() {
        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG);

        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Show biometric authentication prompt
     */
    public void authenticateWithBiometrics(FragmentActivity activity,
                                           String title,
                                           String subtitle,
                                           BiometricAuthCallback callback) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancel")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                ContextCompat.getMainExecutor(activity),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        callback.onError(errorCode, errString.toString());
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        callback.onFailed();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Callback interface for biometric authentication
     */
    public interface BiometricAuthCallback {
        void onSuccess();
        void onError(int errorCode, String errorMessage);
        void onFailed();
    }

    // ===== SECURE DELETE =====

    /**
     * Securely delete file by overwriting before deletion
     */
    public boolean secureDeleteFile(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                return true;
            }

            long length = file.length();

            // Overwrite with zeros
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw");
            raf.seek(0);

            byte[] zeros = new byte[1024];
            for (long written = 0; written < length; written += zeros.length) {
                raf.write(zeros, 0, (int) Math.min(zeros.length, length - written));
            }
            raf.close();

            // Delete the file
            return file.delete();

        } catch (Exception e) {
            Log.e(TAG, "Secure delete failed: " + e.getMessage());
            return false;
        }
    }
}