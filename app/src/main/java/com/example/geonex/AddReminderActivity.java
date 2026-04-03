package com.example.geonex;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AddReminderActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "AddReminderActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int VOICE_PERMISSION_REQUEST_CODE = 200;

    // Views
    private MaterialToolbar toolbar;
    private GoogleMap mMap;
    private Marker selectedMarker;
    private LatLng selectedLatLng;
    private TextView tvSelectedLocation, tvCoordinates, tvRecurringDetail, tvSelectedRadius, tvRadiusHint;
    private TextInputEditText etTitle, etCustomRadius;
    private AutoCompleteTextView actvCategory;

    // Radius buttons
    private MaterialButton btnRadius100, btnRadius250, btnRadius500, btnRadius1000, btnRadius2000, btnRadius5000;
    private MaterialButton btnApplyCustomRadius;
    private MaterialCardView customRadiusCard, selectedRadiusCard;
    private View btnChangeRadius;

    // Other views
    private Button btnSave;
    private FloatingActionButton btnMyLocation;
    private MaterialCardView cardRecurring, voiceInputCard;

    // Radius variables
    private float selectedRadius = 500;
    private String radiusSource = "preset";

    // Recurring variables
    private String recurrenceRule = "never";
    private int customInterval = 0;
    private String customIntervalUnit = "";

    // Auto Category Detection
    private Handler autoDetectHandler = new Handler();
    private Runnable autoDetectRunnable = new Runnable() {
        @Override
        public void run() {
            autoDetectCategory();
        }
    };

    // Voice
    private VoiceInputHelper voiceInputHelper;
    private AlertDialog voiceDialog;
    private TextView tvRecognizedText, tvStatus, tvMicIcon;
    private View waveAnimation;
    private Button btnStartListening, btnStopListening, btnUseText, btnCancel;
    private String recognizedText = "";

    // ===== SECURITY VALIDATION =====
    private SecurityManager securityManager;
    private InputValidator inputValidator;

    private FusedLocationProviderClient fusedLocationClient;
    private ReminderRepository repository;
    private GeofenceHelper geofenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup window animations
        setupWindowAnimations();

        setContentView(R.layout.activity_add_reminder);

        initViews();
        setupToolbar();
        setupCategoryDropdown();
        setupRadiusButtons();
        setupClickListeners();
        setupAutoCategoryDetection();
        initVoiceInput();

        // Initialize security managers
        securityManager = new SecurityManager(this);
        inputValidator = new InputValidator();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        repository = ((GeonexApplication) getApplication()).getRepository();
        geofenceHelper = new GeofenceHelper(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Animate voice card after delay
        voiceInputCard.postDelayed(new Runnable() {
            @Override
            public void run() {
                animateVoiceCard();
            }
        }, 200);
    }

    private void setupWindowAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Fade fade = new Fade();
            fade.setDuration(300);
            getWindow().setEnterTransition(fade);
            getWindow().setReturnTransition(fade);
        }
    }

    private void animateVoiceCard() {
        voiceInputCard.setScaleX(0.95f);
        voiceInputCard.setScaleY(0.95f);
        voiceInputCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etTitle = findViewById(R.id.etTitle);
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        tvCoordinates = findViewById(R.id.tvCoordinates);
        actvCategory = findViewById(R.id.actvCategory);
        btnSave = findViewById(R.id.btnSave);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        cardRecurring = findViewById(R.id.cardRecurring);
        tvRecurringDetail = findViewById(R.id.tvRecurringDetail);
        voiceInputCard = findViewById(R.id.voiceInputCard);

        // Radius views
        btnRadius100 = findViewById(R.id.btnRadius100);
        btnRadius250 = findViewById(R.id.btnRadius250);
        btnRadius500 = findViewById(R.id.btnRadius500);
        btnRadius1000 = findViewById(R.id.btnRadius1000);
        btnRadius2000 = findViewById(R.id.btnRadius2000);
        btnRadius5000 = findViewById(R.id.btnRadius5000);
        btnApplyCustomRadius = findViewById(R.id.btnApplyCustomRadius);
        etCustomRadius = findViewById(R.id.etCustomRadius);
        tvRadiusHint = findViewById(R.id.tvRadiusHint);
        tvSelectedRadius = findViewById(R.id.tvSelectedRadius);
        customRadiusCard = findViewById(R.id.customRadiusCard);
        selectedRadiusCard = findViewById(R.id.selectedRadiusCard);
        btnChangeRadius = findViewById(R.id.btnChangeRadius);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAfterTransition();
            } else {
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            }
        });
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Grocery", "Medicine", "Bills", "Shopping",
                "Hospital", "Office", "Gym", "Restaurant",
                "Petrol", "School", "Pet Care", "Movie", "Other"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);

        actvCategory.setAdapter(adapter);
        actvCategory.setText("Other", false);
    }

    // ================= PHASE 2 - STEP 1: CUSTOMIZABLE RADIUS =================

    private void setupRadiusButtons() {
        // Preset radius buttons
        btnRadius100.setOnClickListener(v -> selectPresetRadius(100, btnRadius100));
        btnRadius250.setOnClickListener(v -> selectPresetRadius(250, btnRadius250));
        btnRadius500.setOnClickListener(v -> selectPresetRadius(500, btnRadius500));
        btnRadius1000.setOnClickListener(v -> selectPresetRadius(1000, btnRadius1000));
        btnRadius2000.setOnClickListener(v -> selectPresetRadius(2000, btnRadius2000));
        btnRadius5000.setOnClickListener(v -> selectPresetRadius(5000, btnRadius5000));

        // Custom radius input validation
        etCustomRadius.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnApplyCustomRadius.setEnabled(s != null && s.length() > 0);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnApplyCustomRadius.setOnClickListener(v -> applyCustomRadius());
        btnChangeRadius.setOnClickListener(v -> showRadiusSelector());

        // Default selection
        selectPresetRadius(500, btnRadius500);
    }

    private void selectPresetRadius(float radius, MaterialButton selectedButton) {
        // Reset all preset buttons
        btnRadius100.setChecked(false);
        btnRadius250.setChecked(false);
        btnRadius500.setChecked(false);
        btnRadius1000.setChecked(false);
        btnRadius2000.setChecked(false);
        btnRadius5000.setChecked(false);

        selectedButton.setChecked(true);

        // Animate selection
        selectedButton.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        selectedButton.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    }
                })
                .start();

        selectedRadius = radius;
        radiusSource = "preset";
        etCustomRadius.setText("");
        updateRadiusDisplay();
    }

    private void applyCustomRadius() {
        String radiusStr = etCustomRadius.getText().toString().trim();
        if (radiusStr.isEmpty()) {
            etCustomRadius.setError("Enter radius");
            return;
        }

        try {
            float radius = Float.parseFloat(radiusStr);
            if (radius < 100) {
                etCustomRadius.setError("Minimum radius is 100m");
                return;
            }
            if (radius > 5000) {
                etCustomRadius.setError("Maximum radius is 5000m");
                return;
            }

            selectedRadius = radius;
            radiusSource = "custom";

            // Reset preset buttons
            btnRadius100.setChecked(false);
            btnRadius250.setChecked(false);
            btnRadius500.setChecked(false);
            btnRadius1000.setChecked(false);
            btnRadius2000.setChecked(false);
            btnRadius5000.setChecked(false);

            updateRadiusDisplay();
            Toast.makeText(this, "Custom radius set to " + (int)radius + "m", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            etCustomRadius.setError("Invalid number");
        }
    }

    private void updateRadiusDisplay() {
        // Animate card transition
        if (selectedRadiusCard.getVisibility() != View.VISIBLE) {
            selectedRadiusCard.setAlpha(0f);
            selectedRadiusCard.setVisibility(View.VISIBLE);
            selectedRadiusCard.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }

        customRadiusCard.setVisibility(View.GONE);

        String text;
        if (selectedRadius >= 1000) {
            text = String.format(Locale.getDefault(), "%.1f km", selectedRadius / 1000);
        } else {
            text = (int) selectedRadius + " m";
        }

        // Animate text change
        tvSelectedRadius.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        tvSelectedRadius.setText(text);
                        tvSelectedRadius.animate()
                                .alpha(1f)
                                .setDuration(150)
                                .start();
                    }
                })
                .start();
    }

    private void showRadiusSelector() {
        selectedRadiusCard.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        selectedRadiusCard.setVisibility(View.GONE);
                        customRadiusCard.setAlpha(0f);
                        customRadiusCard.setVisibility(View.VISIBLE);
                        customRadiusCard.animate()
                                .alpha(1f)
                                .setDuration(300)
                                .start();
                    }
                })
                .start();
    }

    // ================= PHASE 2 - STEP 2: RECURRING REMINDER =================

    private void showRecurringDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_recurring, null);
        builder.setView(dialogView);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);
        RadioButton radioNever = dialogView.findViewById(R.id.radioNever);
        RadioButton radioDaily = dialogView.findViewById(R.id.radioDaily);
        RadioButton radioWeekly = dialogView.findViewById(R.id.radioWeekly);
        RadioButton radioMonthly = dialogView.findViewById(R.id.radioMonthly);
        RadioButton radioCustom = dialogView.findViewById(R.id.radioCustom);

        LinearLayout customLayout = dialogView.findViewById(R.id.customIntervalLayout);
        TextInputEditText etInterval = dialogView.findViewById(R.id.etCustomInterval);
        Spinner spinnerUnit = dialogView.findViewById(R.id.spinnerIntervalUnit);

        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        // Set current selection
        switch (recurrenceRule) {
            case "daily":
                radioDaily.setChecked(true);
                break;
            case "weekly":
                radioWeekly.setChecked(true);
                break;
            case "monthly":
                radioMonthly.setChecked(true);
                break;
            case "custom":
                radioCustom.setChecked(true);
                customLayout.setVisibility(View.VISIBLE);
                if (customInterval > 0) {
                    etInterval.setText(String.valueOf(customInterval));
                }
                break;
            default:
                radioNever.setChecked(true);
                break;
        }

        // Show/hide custom layout
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCustom) {
                customLayout.setVisibility(View.VISIBLE);
                // Animate appearance
                customLayout.setAlpha(0f);
                customLayout.animate().alpha(1f).setDuration(300).start();
            } else {
                customLayout.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                customLayout.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
        });

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            int id = radioGroup.getCheckedRadioButtonId();

            if (id == R.id.radioDaily) {
                recurrenceRule = "daily";
                tvRecurringDetail.setText("Daily");
            } else if (id == R.id.radioWeekly) {
                recurrenceRule = "weekly";
                tvRecurringDetail.setText("Weekly");
            } else if (id == R.id.radioMonthly) {
                recurrenceRule = "monthly";
                tvRecurringDetail.setText("Monthly");
            } else if (id == R.id.radioCustom) {
                String val = Objects.requireNonNull(etInterval.getText()).toString().trim();
                if (val.isEmpty()) {
                    etInterval.setError("Enter interval");
                    return;
                }

                try {
                    customInterval = Integer.parseInt(val);
                    if (customInterval < 1) {
                        etInterval.setError("Interval must be at least 1");
                        return;
                    }
                    customIntervalUnit = spinnerUnit.getSelectedItem().toString();
                    recurrenceRule = "custom";
                    tvRecurringDetail.setText("Every " + customInterval + " " + customIntervalUnit);
                } catch (NumberFormatException e) {
                    etInterval.setError("Invalid number");
                    return;
                }
            } else {
                recurrenceRule = "never";
                tvRecurringDetail.setText("Never (Once)");
            }

            // Animate recurring card
            cardRecurring.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            cardRecurring.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                        }
                    })
                    .start();

            dialog.dismiss();
        });

        dialog.show();
    }

    // ================= PHASE 2 - STEP 3: VOICE INPUT =================

    private void initVoiceInput() {
        voiceInputHelper = new VoiceInputHelper(this, new VoiceInputHelper.VoiceInputListener() {
            @Override
            public void onPartialResult(String text) {
                runOnUiThread(() -> {
                    if (tvRecognizedText != null) {
                        tvRecognizedText.setText(text);
                    }
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinalResult(String text) {
                runOnUiThread(() -> {
                    recognizedText = text;
                    if (tvRecognizedText != null) {
                        tvRecognizedText.setText(text);
                    }
                    if (tvStatus != null) {
                        tvStatus.setText("Recognition complete");
                    }
                    if (waveAnimation != null) {
                        waveAnimation.setVisibility(View.GONE);
                    }
                    if (tvMicIcon != null) {
                        tvMicIcon.setText("✓");
                        tvMicIcon.clearAnimation();
                    }
                    if (btnUseText != null) {
                        btnUseText.setEnabled(true);
                    }
                    updateButtonStates(false);
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AddReminderActivity.this,
                            "Voice error: " + error, Toast.LENGTH_SHORT).show();
                    if (tvStatus != null) {
                        tvStatus.setText("Error: " + error);
                    }
                    if (waveAnimation != null) {
                        waveAnimation.setVisibility(View.GONE);
                    }
                    if (tvMicIcon != null) {
                        tvMicIcon.setText("🎤");
                        tvMicIcon.clearAnimation();
                    }
                    updateButtonStates(false);
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onListeningStarted() {
                runOnUiThread(() -> {
                    if (tvStatus != null) {
                        tvStatus.setText("Listening... Speak now");
                    }
                    if (waveAnimation != null) {
                        waveAnimation.setVisibility(View.VISIBLE);
                        // Start wave animation
                        ObjectAnimator animator = ObjectAnimator.ofFloat(waveAnimation, "alpha", 0.5f, 1f);
                        animator.setDuration(800);
                        animator.setRepeatCount(ObjectAnimator.INFINITE);
                        animator.setRepeatMode(ObjectAnimator.REVERSE);
                        animator.start();
                    }
                    if (tvMicIcon != null) {
                        tvMicIcon.setText("🎤");
                        Animation pulse = AnimationUtils.loadAnimation(
                                AddReminderActivity.this, R.anim.pulse);
                        tvMicIcon.startAnimation(pulse);
                    }
                    updateButtonStates(true);
                });
            }

            @Override
            public void onListeningStopped() {
                runOnUiThread(() -> {
                    if (waveAnimation != null) {
                        waveAnimation.setVisibility(View.GONE);
                        waveAnimation.clearAnimation();
                    }
                    if (tvMicIcon != null) {
                        tvMicIcon.clearAnimation();
                    }
                    updateButtonStates(false);
                });
            }
        });
    }

    private void showVoiceDialog() {
        if (!voiceInputHelper.checkPermission(this)) {
            requestVoicePermission();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_voice_input, null);
        builder.setView(dialogView);

        tvRecognizedText = dialogView.findViewById(R.id.tvRecognizedText);
        tvStatus = dialogView.findViewById(R.id.tvStatus);
        tvMicIcon = dialogView.findViewById(R.id.tvMicIcon);
        waveAnimation = dialogView.findViewById(R.id.waveAnimation);
        btnStartListening = dialogView.findViewById(R.id.btnStartListening);
        btnStopListening = dialogView.findViewById(R.id.btnStopListening);
        btnUseText = dialogView.findViewById(R.id.btnUseText);
        btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnStartListening.setOnClickListener(v -> voiceInputHelper.startListening());
        btnStopListening.setOnClickListener(v -> voiceInputHelper.stopListening());

        btnUseText.setOnClickListener(v -> {
            if (!recognizedText.isEmpty()) {
                // Sanitize voice input text
                String sanitizedText = securityManager.sanitizeInput(recognizedText);
                etTitle.setText(sanitizedText);
                detectCategoryFromText(sanitizedText);

                // Animate title field
                etTitle.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                etTitle.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(100)
                                        .start();
                            }
                        })
                        .start();
            }
            voiceDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            voiceInputHelper.stopListening();
            voiceDialog.dismiss();
        });

        voiceDialog = builder.create();
        voiceDialog.show();

        // Auto-start listening with delay for animation
        voiceDialog.getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                voiceInputHelper.startListening();
            }
        }, 300);
    }

    private void requestVoicePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                    VOICE_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateButtonStates(boolean isListening) {
        if (btnStartListening != null && btnStopListening != null) {
            btnStartListening.setEnabled(!isListening);
            btnStopListening.setEnabled(isListening);
        }
    }

    // ================= AUTO CATEGORY DETECTION =================

    private void setupAutoCategoryDetection() {
        etTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                autoDetectHandler.removeCallbacks(autoDetectRunnable);
                autoDetectHandler.postDelayed(autoDetectRunnable, 800);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void autoDetectCategory() {
        String title = etTitle.getText().toString().trim();
        if (!title.isEmpty()) {
            String detectedCategory = CategoryDetector.detectCategory(title);
            actvCategory.setText(detectedCategory, false);

            if (!detectedCategory.equals("Other")) {
                Toast.makeText(this,
                        "Category auto-set to: " + detectedCategory,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void detectCategoryFromText(String text) {
        String detectedCategory = CategoryDetector.detectCategory(text);
        actvCategory.setText(detectedCategory, false);
    }

    // ================= MAP =================

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mMap.setOnMapClickListener(latLng -> {
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location"));
            selectedLatLng = latLng;
            updateLocationInfo(latLng);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

            // Animate marker
            if (selectedMarker != null) {
                selectedMarker.showInfoWindow();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        selectedLatLng = new LatLng(
                                location.getLatitude(),
                                location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory
                                .newLatLngZoom(selectedLatLng, 15f));

                        if (selectedMarker != null) {
                            selectedMarker.remove();
                        }
                        selectedMarker = mMap.addMarker(new MarkerOptions()
                                .position(selectedLatLng)
                                .title("Current Location"));
                        updateLocationInfo(selectedLatLng);

                        // Animate my location button
                        btnMyLocation.animate()
                                .rotation(360f)
                                .setDuration(500)
                                .start();
                    }
                });
    }

    private void updateLocationInfo(LatLng latLng) {
        tvCoordinates.setText(String.format(Locale.getDefault(),
                "Lat: %.6f, Lng: %.6f", latLng.latitude, latLng.longitude));

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                tvSelectedLocation.setText(addresses.get(0).getAddressLine(0));
            } else {
                tvSelectedLocation.setText("Selected Location");
            }
        } catch (IOException e) {
            tvSelectedLocation.setText("Selected Location");
        }
    }

    // ================= CLICK LISTENERS =================

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> {
            // Animate save button
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                            saveReminder();
                        }
                    })
                    .start();
        });

        btnMyLocation.setOnClickListener(v -> {
            // Animate my location button
            v.animate()
                    .rotation(360f)
                    .setDuration(500)
                    .start();
            getCurrentLocation();
        });

        cardRecurring.setOnClickListener(v -> {
            // Animate card
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                            showRecurringDialog();
                        }
                    })
                    .start();
        });

        voiceInputCard.setOnClickListener(v -> {
            // Animate voice card
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                            showVoiceDialog();
                        }
                    })
                    .start();
        });
    }

    // ================= SAVE REMINDER WITH SECURITY VALIDATION AND DEBUG LOGS =================

    private void saveReminder() {
        Log.d("DEBUG", "🔵 saveReminder() called");

        String title = etTitle.getText().toString().trim();
        String location = tvSelectedLocation.getText().toString();

        Log.d("DEBUG", "📝 Title: '" + title + "', Location: '" + location + "'");

        // Validate inputs using InputValidator
        inputValidator.clear();
        inputValidator.validateTitle(title)
                .validateLocation(location)
                .validateCoordinates(
                        selectedLatLng != null ? selectedLatLng.latitude : 0,
                        selectedLatLng != null ? selectedLatLng.longitude : 0
                )
                .validateRadius(selectedRadius);

        if (!inputValidator.isValid()) {
            String error = inputValidator.getFirstError();
            Log.d("DEBUG", "❌ Validation failed: " + error);
            if (error.contains("Title")) {
                etTitle.setError(error);
                etTitle.requestFocus();
            } else {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Sanitize inputs using SecurityManager
        title = securityManager.sanitizeInput(title);
        location = securityManager.sanitizeInput(location);

        String selectedCategory = actvCategory.getText().toString();
        if (selectedCategory.isEmpty()) selectedCategory = "Other";
        selectedCategory = securityManager.sanitizeInput(selectedCategory);

        Reminder reminder = new Reminder();
        reminder.setTitle(title);
        reminder.setLocationName(location);
        reminder.setLatitude(selectedLatLng.latitude);
        reminder.setLongitude(selectedLatLng.longitude);
        reminder.setRadius(selectedRadius);
        reminder.setCategory(selectedCategory);
        reminder.setCompleted(false);
        reminder.setCreatedAt(System.currentTimeMillis());

        // Set recurring data with validation
        reminder.setRecurring(!recurrenceRule.equals("never"));
        reminder.setRecurrenceRule(recurrenceRule);
        if (recurrenceRule.equals("custom") && customInterval > 0) {
            reminder.setCustomInterval(customInterval);
            reminder.setCustomIntervalUnit(customIntervalUnit);
        }

        Log.d("DEBUG", "📝 Reminder object created: " + title + " at " + location);

        repository.insert(reminder, new ReminderRepository.OnReminderInsertedListener() {
            @Override
            public void onInserted(Reminder insertedReminder) {
                Log.d("DEBUG", "✅ REMINDER SAVED! ID: " + insertedReminder.getId() + " - " + insertedReminder.getTitle());

                runOnUiThread(() -> {
                    geofenceHelper.addGeofence(insertedReminder);
                    Toast.makeText(AddReminderActivity.this,
                            "Reminder saved successfully!",
                            Toast.LENGTH_SHORT).show();

                    // Start location tracking if needed
                    new LocationTrackingManager(AddReminderActivity.this).updateTrackingState();

                    // After saving reminder, refresh geofence monitoring
                    new GeofenceMonitorManager(AddReminderActivity.this).refreshGeofences();

                    Log.d("DEBUG", "🏁 Finishing AddReminderActivity");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        finishAfterTransition();
                    } else {
                        finish();
                        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
                    }
                });
            }
        });
    }

    // ================= PERMISSION RESULTS =================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == VOICE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showVoiceDialog();
            } else {
                Toast.makeText(this, "Voice input requires microphone permission",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                }
            } else {
                Toast.makeText(this, "Location permission required",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ================= LIFECYCLE =================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoDetectHandler.removeCallbacks(autoDetectRunnable);
        if (voiceInputHelper != null) {
            voiceInputHelper.destroy();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
        }
        return true;
    }
}