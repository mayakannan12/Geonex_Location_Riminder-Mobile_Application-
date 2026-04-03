package com.example.geonex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceInputHelper {

    private static final String TAG = "VoiceInputHelper";
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private VoiceInputListener listener;
    private boolean isListening = false;

    public interface VoiceInputListener {
        void onPartialResult(String text);
        void onFinalResult(String text);
        void onError(String error);
        void onListeningStarted();
        void onListeningStopped();
    }

    public VoiceInputHelper(android.content.Context context, VoiceInputListener listener) {
        this.listener = listener;

        // Check if speech recognition is available
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Ready for speech");
                    if (listener != null) {
                        listener.onListeningStarted();
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Can be used for visual feedback
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "End of speech");
                    if (listener != null) {
                        listener.onListeningStopped();
                    }
                }

                @Override
                public void onError(int error) {
                    String errorMessage = getErrorText(error);
                    Log.e(TAG, "Speech recognition error: " + errorMessage);
                    isListening = false;
                    if (listener != null) {
                        listener.onError(errorMessage);
                        listener.onListeningStopped();
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    Log.d(TAG, "Got results");
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        if (listener != null) {
                            listener.onFinalResult(text);
                        }
                    }
                    isListening = false;
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        if (listener != null) {
                            listener.onPartialResult(text);
                        }
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });

            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());

        } else {
            Log.e(TAG, "Speech recognition not available");
            if (listener != null) {
                listener.onError("Speech recognition not available on this device");
            }
        }
    }

    public boolean checkPermission(android.content.Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void startListening() {
        if (speechRecognizer != null && !isListening) {
            try {
                speechRecognizer.startListening(speechRecognizerIntent);
                isListening = true;
            } catch (SecurityException e) {
                Log.e(TAG, "Permission error: " + e.getMessage());
                if (listener != null) {
                    listener.onError("Microphone permission required");
                }
            }
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public boolean isListening() {
        return isListening;
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match found";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognizer busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }
}