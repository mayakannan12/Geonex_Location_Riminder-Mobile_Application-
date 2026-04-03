package com.example.geonex;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

import com.google.android.material.button.MaterialButton;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppLockActivity extends AppCompatActivity {

    private SecurityManager securityManager;
    private boolean isUnlocked = false;
    private Executor executor = Executors.newSingleThreadExecutor();
    private TextView tvStatus;
    private MaterialButton btnCancel;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IMPORTANT: Set content view first
        setContentView(R.layout.activity_app_lock);

        // Initialize views
        tvStatus = findViewById(R.id.tvStatus);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);

        securityManager = new SecurityManager(this);

        // Check if biometric is enabled
        if (securityManager.getSecureBoolean("biometric_enabled", false)) {
            showBiometricPrompt();
        } else {
            // No lock required, proceed to main
            startMainActivity();
        }

        // Setup cancel button
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                finishAffinity(); // Close the app
            });
        }
    }

    private void showBiometricPrompt() {
        if (tvStatus != null) {
            tvStatus.setText("Authenticate to continue");
        }

        if (progressBar != null) {
            progressBar.setVisibility(ProgressBar.GONE);
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("App Lock")
                .setSubtitle("Authenticate to access Geonex")
                .setNegativeButtonText("Cancel")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        runOnUiThread(() -> {
                            isUnlocked = true;
                            Toast.makeText(AppLockActivity.this,
                                    "Authentication successful!",
                                    Toast.LENGTH_SHORT).show();
                            startMainActivity();
                        });
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        runOnUiThread(() -> {
                            Toast.makeText(AppLockActivity.this,
                                    "Authentication failed: " + errString,
                                    Toast.LENGTH_SHORT).show();

                            if (tvStatus != null) {
                                tvStatus.setText("Authentication failed. Try again.");
                            }
                        });
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        runOnUiThread(() -> {
                            Toast.makeText(AppLockActivity.this,
                                    "Fingerprint not recognized",
                                    Toast.LENGTH_SHORT).show();

                            if (tvStatus != null) {
                                tvStatus.setText("Fingerprint not recognized. Try again.");
                            }
                        });
                    }
                });

        biometricPrompt.authenticate(promptInfo);

        // Auto timeout after 30 seconds
        new Handler().postDelayed(() -> {
            if (!isUnlocked) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Authentication timeout", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                });
            }
        }, 30000);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent back button during authentication
        super.onBackPressed();
        Toast.makeText(this, "Please authenticate first", Toast.LENGTH_SHORT).show();
    }
}