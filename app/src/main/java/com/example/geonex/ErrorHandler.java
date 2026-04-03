package com.example.geonex;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofenceStatusCodes;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ErrorHandler {

    private static final String TAG = "ErrorHandler";
    private final Context context;

    public ErrorHandler(Context context) {
        this.context = context;
    }

    /**
     * Handle geofence errors
     */
    public String handleGeofenceError(Exception e) {
        String errorMessage = "Geofence error occurred";

        if (e instanceof ApiException) {
            ApiException apiException = (ApiException) e;
            int statusCode = apiException.getStatusCode();

            switch (statusCode) {
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    errorMessage = "Geofence service is not available";
                    break;
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    errorMessage = "Too many geofences. Maximum is 100.";
                    break;
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    errorMessage = "Too many pending intents";
                    break;
                default:
                    errorMessage = "Geofence error: " + statusCode;
                    break;
            }
        }

        Log.e(TAG, errorMessage + ": " + e.getMessage());
        return errorMessage;
    }

    /**
     * Handle location errors
     */
    public String handleLocationError(Exception e) {
        String errorMessage = "Location error occurred";

        if (e instanceof SecurityException) {
            errorMessage = "Location permission denied";
        } else if (e instanceof IOException) {
            errorMessage = "Network error while getting location";
        }

        Log.e(TAG, errorMessage + ": " + e.getMessage());
        return errorMessage;
    }

    /**
     * Handle network errors
     */
    public String handleNetworkError(Exception e) {
        String errorMessage = "Network error occurred";

        if (e instanceof UnknownHostException) {
            errorMessage = "No internet connection";
        } else if (e instanceof SocketTimeoutException) {
            errorMessage = "Network timeout";
        } else if (e instanceof IOException) {
            errorMessage = "Network connection failed";
        }

        Log.e(TAG, errorMessage + ": " + e.getMessage());
        return errorMessage;
    }

    /**
     * Handle database errors
     */
    public String handleDatabaseError(Exception e) {
        String errorMessage = "Database error occurred";

        if (e.getMessage() != null) {
            if (e.getMessage().contains("no such table")) {
                errorMessage = "Database schema error";
            } else if (e.getMessage().contains("UNIQUE constraint")) {
                errorMessage = "Duplicate entry";
            }
        }

        Log.e(TAG, errorMessage + ": " + e.getMessage());
        return errorMessage;
    }

    /**
     * Handle voice recognition errors
     */
    public String handleVoiceError(int errorCode) {
        String errorMessage;

        switch (errorCode) {
            case android.speech.SpeechRecognizer.ERROR_AUDIO:
                errorMessage = "Audio recording error";
                break;
            case android.speech.SpeechRecognizer.ERROR_CLIENT:
                errorMessage = "Client side error";
                break;
            case android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMessage = "Microphone permission required";
                break;
            case android.speech.SpeechRecognizer.ERROR_NETWORK:
                errorMessage = "Network error";
                break;
            case android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMessage = "Network timeout";
                break;
            case android.speech.SpeechRecognizer.ERROR_NO_MATCH:
                errorMessage = "No speech recognized";
                break;
            case android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMessage = "Speech recognizer busy";
                break;
            case android.speech.SpeechRecognizer.ERROR_SERVER:
                errorMessage = "Server error";
                break;
            case android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMessage = "No speech input";
                break;
            default:
                errorMessage = "Voice recognition error";
                break;
        }

        Log.e(TAG, errorMessage);
        return errorMessage;
    }

    /**
     * Show error toast
     */
    public void showErrorToast(String message) {
        Toast.makeText(context, "❌ " + message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show warning toast
     */
    public void showWarningToast(String message) {
        Toast.makeText(context, "⚠️ " + message, Toast.LENGTH_LONG).show();
    }

    /**
     * Show success toast
     */
    public void showSuccessToast(String message) {
        Toast.makeText(context, "✅ " + message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Log error with stack trace
     */
    public void logError(String tag, String message, Exception e) {
        Log.e(tag, message + ": " + e.getMessage());
        if (e.getStackTrace().length > 0) {
            Log.e(tag, "at " + e.getStackTrace()[0].toString());
        }
    }
}