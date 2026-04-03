package com.example.geonex;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.Fade;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    // Add this as a class variable at the top
    private static final String TAG = "OnboardingActivity";

    private ViewPager2 viewPager;
    private MaterialButton btnNext, btnSkip, btnGetStarted, btnPrevious, btnNextIcon;
    private TabLayout tabLayout;
    private OnboardingPagerAdapter pagerAdapter;
    private List<Object> slidePages = new ArrayList<>();
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    private WeakReference<ViewPager2> viewPagerRef;
    private WeakReference<TabLayout> tabLayoutRef;

    // Permission state variables
    private boolean locationGranted = false;
    private boolean notificationGranted = false;
    private boolean backgroundGranted = false;
    private boolean batteryOptimizationIgnored = false;

    // Permission constants
    private static final String PREFS_NAME = "geonex_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final int LOCATION_PERMISSION_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final int BACKGROUND_LOCATION_CODE = 102;

    // Premium colors for variety in slides
    private final int[] slideColors = {
            R.color.primary_container,
            R.color.secondary_container,
            R.color.tertiary_container,
            R.color.primary_container,
            R.color.secondary_container
    };

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable window transitions for premium feel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Fade());
            getWindow().setExitTransition(new Fade());
        }

        setContentView(R.layout.activity_onboarding);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // DEBUG: Log current state (remove in production)
        boolean onboardingDone = sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false);
        Log.d(TAG, "onCreate - onboarding_done = " + onboardingDone);

        initViews();
        setupFeatureSlides();
        setupViewPager();
        setupTabLayout();
        setupButtonListeners();

        checkAllPermissions();

        // Premium entry animation
        startEntryAnimation();

        // Set up weak references
        viewPagerRef = new WeakReference<>(viewPager);
        tabLayoutRef = new WeakReference<>(tabLayout);
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNextIcon = findViewById(R.id.btnNextIcon);
    }

    /**
     * Create premium feature slides
     */
    private void setupFeatureSlides() {
        // Slide 1: Geo Reminders
        slidePages.add(new FeatureData(
                R.drawable.ic_location_onboarding,
                R.string.feature_geo_title,
                R.string.feature_geo_desc,
                getResources().getIntArray(R.array.feature_geo_bullets),
                ContextCompat.getColor(this, slideColors[0])
        ));

        // Slide 2: Voice Input
        slidePages.add(new FeatureData(
                R.drawable.ic_voice_onboarding,
                R.string.feature_voice_title,
                R.string.feature_voice_desc,
                getResources().getIntArray(R.array.feature_voice_bullets),
                ContextCompat.getColor(this, slideColors[1])
        ));

        // Slide 3: Recurring Reminders
        slidePages.add(new FeatureData(
                R.drawable.ic_recurring_onboarding,
                R.string.feature_recurring_title,
                R.string.feature_recurring_desc,
                getResources().getIntArray(R.array.feature_recurring_bullets),
                ContextCompat.getColor(this, slideColors[2])
        ));

        // Slide 4: Battery Optimization
        slidePages.add(new FeatureData(
                R.drawable.ic_battery_onboarding,
                R.string.feature_battery_title,
                R.string.feature_battery_desc,
                getResources().getIntArray(R.array.feature_battery_bullets),
                ContextCompat.getColor(this, slideColors[3])
        ));

        // Slide 5: Auto Category
        slidePages.add(new FeatureData(
                R.drawable.ic_category_onboarding,
                R.string.feature_category_title,
                R.string.feature_category_desc,
                getResources().getIntArray(R.array.feature_category_bullets),
                ContextCompat.getColor(this, slideColors[4])
        ));

        // Add existing fragments as the last pages
        slidePages.add(new WelcomeFragment());           // Page 6
        slidePages.add(new PermissionFragment());        // Page 7
        slidePages.add(new CategoriesFragment());        // Page 8
    }

    /**
     * Setup ViewPager with adapter
     */
    private void setupViewPager() {
        pagerAdapter = new OnboardingPagerAdapter(this, slidePages);
        viewPager.setAdapter(pagerAdapter);

        // Premium page transformer for animations - UPDATED
        viewPager.setPageTransformer(new PremiumPageTransformer());

        // Premium page change callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateButtonVisibility(position);

                // Provide haptic feedback
                provideHapticFeedback();

                // Celebrate on last page
                if (position == slidePages.size() - 1) {
                    celebrateLastPage();
                }
            }
        });
    }

    /**
     * Setup TabLayout with ViewPager
     */
    private void setupTabLayout() {
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    // No text needed - just dots
                }).attach();
    }

    /**
     * Update button visibility based on current page
     */
    private void updateButtonVisibility(int position) {
        boolean isLastPage = position == slidePages.size() - 1;
        boolean isFirstPage = position == 0;

        // Show/hide main buttons
        btnNext.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
        btnGetStarted.setVisibility(isLastPage ? View.VISIBLE : View.GONE);
        btnSkip.setVisibility(isLastPage ? View.GONE : View.VISIBLE);

        // Show/hide navigation buttons
        btnPrevious.setVisibility(!isFirstPage ? View.VISIBLE : View.GONE);
        btnNextIcon.setVisibility((!isLastPage && !isFirstPage) ? View.VISIBLE : View.GONE);

        // Animate button transitions
        if (isLastPage) {
            animateGetStartedEntry();
        }
    }

    /**
     * Setup button click listeners with premium animations
     */
    private void setupButtonListeners() {
        btnSkip.setOnClickListener(v -> {
            animateButtonClick(v);
            // Jump to last page
            viewPager.setCurrentItem(slidePages.size() - 1, true);
        });

        btnNext.setOnClickListener(v -> {
            animateButtonClick(v);
            int nextItem = viewPager.getCurrentItem() + 1;
            if (nextItem < slidePages.size()) {
                viewPager.setCurrentItem(nextItem, true);
            }
        });

        btnGetStarted.setOnClickListener(v -> {
            animateButtonClick(v);

            // Save that onboarding is done - THIS IS THE ONLY PLACE WHERE FLAG IS SAVED
            Log.d(TAG, "Get Started clicked - saving onboarding_done = true");
            sharedPreferences.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();

            // Navigate to Home
            proceedToHome();
        });

        btnPrevious.setOnClickListener(v -> {
            animateButtonClick(v);
            int prevItem = viewPager.getCurrentItem() - 1;
            if (prevItem >= 0) {
                viewPager.setCurrentItem(prevItem, true);
            }
        });

        btnNextIcon.setOnClickListener(v -> {
            animateButtonClick(v);
            int nextItem = viewPager.getCurrentItem() + 1;
            if (nextItem < slidePages.size()) {
                viewPager.setCurrentItem(nextItem, true);
            }
        });
    }

    // ==================== PREMIUM ANIMATION METHODS ====================

    /**
     * Premium entry animation for activity
     */
    private void startEntryAnimation() {
        viewPager.setAlpha(0f);
        viewPager.setTranslationY(30f);

        viewPager.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /**
     * Premium button click animation
     */
    private void animateButtonClick(View button) {
        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                button,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0.9f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0.9f)
        );
        scaleDown.setDuration(100);
        scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                button,
                PropertyValuesHolder.ofFloat("scaleX", 0.9f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 0.9f, 1f)
        );
        scaleUp.setDuration(100);
        scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleDown.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                scaleUp.start();
            }
        });

        scaleDown.start();
    }

    /**
     * Premium entry animation for Get Started button
     */
    private void animateGetStartedEntry() {
        btnGetStarted.setAlpha(0f);
        btnGetStarted.setScaleX(0.8f);
        btnGetStarted.setScaleY(0.8f);

        btnGetStarted.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /**
     * Premium particle effect for final slide
     * Creates a magical sparkle effect when reaching last page
     */
    private void celebrateLastPage() {
        ViewGroup root = findViewById(android.R.id.content);
        for (int i = 0; i < 8; i++) {
            View sparkle = new View(this);
            sparkle.setBackgroundResource(R.drawable.sparkle);

            // Random position
            int size = 40;
            int x = (int) (Math.random() * (root.getWidth() - size));
            int y = (int) (Math.random() * (root.getHeight() - size));

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.leftMargin = x;
            params.topMargin = y;
            sparkle.setLayoutParams(params);
            sparkle.setAlpha(0f);

            root.addView(sparkle);

            // Animate sparkle
            sparkle.animate()
                    .alpha(1f)
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        sparkle.animate()
                                .alpha(0f)
                                .scaleX(0f)
                                .scaleY(0f)
                                .setDuration(200)
                                .withEndAction(() -> {
                                    root.removeView(sparkle);
                                })
                                .start();
                    })
                    .start();
        }
    }

    /**
     * Premium haptic feedback (vibration) for button clicks
     */
    private void provideHapticFeedback() {
        viewPager.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    // ==================== PREMIUM PAGE TRANSFORMER ====================

    /**
     * Premium ViewPager page transformer for smooth transitions
     * UPDATED with enhanced animation effects
     */
    private class PremiumPageTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            float absPosition = Math.abs(position);

            if (position < -1) {
                // Page off-screen to the left
                page.setAlpha(0f);
                page.setScaleX(0.9f);
                page.setScaleY(0.9f);

            } else if (position <= 0) {
                // Page entering from left (becoming active)
                page.setAlpha(1 - absPosition * 0.3f);
                page.setScaleX(1 - absPosition * 0.1f);
                page.setScaleY(1 - absPosition * 0.1f);
                page.setTranslationX(position * page.getWidth() * -0.3f);

            } else if (position <= 1) {
                // Page exiting to right
                page.setAlpha(1 - absPosition * 0.3f);
                page.setScaleX(1 - absPosition * 0.1f);
                page.setScaleY(1 - absPosition * 0.1f);
                page.setTranslationX(position * page.getWidth() * -0.3f);

            } else {
                // Page off-screen to the right
                page.setAlpha(0f);
                page.setScaleX(0.9f);
                page.setScaleY(0.9f);
            }
        }
    }

    // ==================== PERMISSION METHODS ====================

    // Check all permission states
    private void checkAllPermissions() {
        PermissionHelper helper = new PermissionHelper(this);

        locationGranted = helper.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationGranted = helper.hasPermission(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            notificationGranted = true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundGranted = helper.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        } else {
            backgroundGranted = true;
        }

        batteryOptimizationIgnored = helper.isBatteryOptimizationDisabled();
    }

    // Permission request methods
    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_CODE);
    }

    public void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
        } else {
            notificationGranted = true;
            updatePermissionUI();
        }
    }

    public void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    BACKGROUND_LOCATION_CODE);
        } else {
            backgroundGranted = true;
            updatePermissionUI();
        }
    }

    public void ignoreBatteryOptimization() {
        PermissionHelper helper = new PermissionHelper(this);
        helper.requestDisableBatteryOptimization();
        batteryOptimizationIgnored = true;
        updatePermissionUI();
        Toast.makeText(this, "Please allow battery optimization in settings", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermissionHelper helper = new PermissionHelper(this);

        for (int i = 0; i < permissions.length; i++) {

            String permission = permissions[i];
            boolean granted = grantResults.length > i &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED;

            switch (permission) {

                case Manifest.permission.ACCESS_FINE_LOCATION:
                case Manifest.permission.ACCESS_COARSE_LOCATION:
                    locationGranted = granted;
                    if (!granted && i < permissions.length - 1) {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            helper.showPermissionRationale(permission,
                                    () -> ActivityCompat.requestPermissions(
                                            this,
                                            new String[]{permission},
                                            LOCATION_PERMISSION_CODE));
                        } else {
                            helper.showSettingsDialog(permission);
                        }
                    }
                    break;

                case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                    backgroundGranted = granted;
                    if (!granted) {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            helper.showPermissionRationale(permission,
                                    () -> ActivityCompat.requestPermissions(
                                            this,
                                            new String[]{permission},
                                            BACKGROUND_LOCATION_CODE));
                        } else {
                            helper.showSettingsDialog(permission);
                        }
                    }
                    break;

                case Manifest.permission.POST_NOTIFICATIONS:
                    notificationGranted = granted;
                    if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            helper.showPermissionRationale(permission,
                                    () -> ActivityCompat.requestPermissions(
                                            this,
                                            new String[]{permission},
                                            NOTIFICATION_PERMISSION_CODE));
                        } else {
                            helper.showSettingsDialog(permission);
                        }
                    }
                    break;
            }
        }

        updatePermissionUI();

        if (locationGranted && notificationGranted && backgroundGranted) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
        }
    }

    // GPS check
    private boolean isGPSEnabled() {
        android.location.LocationManager locationManager =
                (android.location.LocationManager)
                        getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(
                android.location.LocationManager.GPS_PROVIDER);
    }

    // Proceed to home
    private void proceedToHome() {

        PermissionHelper helper = new PermissionHelper(this);

        if (!helper.hasAllPermissions()) {

            new AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("Some permissions are still missing. The app may not function properly.")
                    .setPositiveButton("Continue Anyway", (d, w) -> {
                        startActivity(new Intent(OnboardingActivity.this, HomeActivity.class));
                        finish();
                    })
                    .setNegativeButton("Grant Permissions", (d, w) -> {
                        viewPager.setCurrentItem(slidePages.size() - 2); // Go to permission page
                    })
                    .show();

        } else if (!isGPSEnabled()) {

            helper.showGPSSettingsDialog();
            startActivity(new Intent(OnboardingActivity.this, HomeActivity.class));
            finish();

        } else {

            startActivity(new Intent(OnboardingActivity.this, HomeActivity.class));
            finish();
        }
    }

    private void updatePermissionUI() {
        if (pagerAdapter != null && pagerAdapter.permissionFragment != null) {
            pagerAdapter.permissionFragment.updateUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clear references to prevent memory leaks
        if (viewPager != null) {
            viewPager.setAdapter(null);
        }

        // Cancel all pending animations
        if (viewPagerRef != null) {
            viewPagerRef.clear();
        }
        if (tabLayoutRef != null) {
            tabLayoutRef.clear();
        }

        // Clean up handlers
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
    }

    // ==================== ADAPTER ====================
    private class OnboardingPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {

        private List<Object> pages;
        private PermissionFragment permissionFragment;

        public OnboardingPagerAdapter(@NonNull AppCompatActivity activity, List<Object> pages) {
            super(activity);
            this.pages = pages;
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            Object page = pages.get(position);

            if (page instanceof FeatureData) {
                return FeatureSlideFragment.newInstance((FeatureData) page);
            } else if (page instanceof WelcomeFragment) {
                return new WelcomeFragment();
            } else if (page instanceof PermissionFragment) {
                permissionFragment = new PermissionFragment();
                return permissionFragment;
            } else if (page instanceof CategoriesFragment) {
                return new CategoriesFragment();
            }

            return new WelcomeFragment();
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }

    // ==================== EXISTING FRAGMENTS ====================

    public static class WelcomeFragment extends androidx.fragment.app.Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_welcome, container, false);
        }
    }

    public static class PermissionFragment extends androidx.fragment.app.Fragment {

        private OnboardingActivity activity;
        private View view;
        private MaterialButton btnLocation, btnNotification, btnBackground, btnBattery;
        private MaterialCardView cardLocation, cardNotification, cardBackground, cardBattery;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            if (context instanceof OnboardingActivity) {
                activity = (OnboardingActivity) context;
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            view = inflater.inflate(R.layout.fragment_onboarding_permission, container, false);

            if (activity != null) {
                initializeViews();
                setupClickListeners();
                updateUI();
            }

            return view;
        }

        private void initializeViews() {
            cardLocation = view.findViewById(R.id.cardLocation);
            cardNotification = view.findViewById(R.id.cardNotification);
            cardBackground = view.findViewById(R.id.cardBackground);
            cardBattery = view.findViewById(R.id.cardBattery);

            btnLocation = view.findViewById(R.id.btnLocation);
            btnNotification = view.findViewById(R.id.btnNotification);
            btnBackground = view.findViewById(R.id.btnBackground);
            btnBattery = view.findViewById(R.id.btnBattery);
        }

        private void setupClickListeners() {
            btnLocation.setOnClickListener(v -> {
                if (activity != null) {
                    activity.requestLocationPermission();
                }
            });

            btnNotification.setOnClickListener(v -> {
                if (activity != null) {
                    activity.requestNotificationPermission();
                }
            });

            btnBackground.setOnClickListener(v -> {
                if (activity != null) {
                    activity.requestBackgroundLocationPermission();
                }
            });

            btnBattery.setOnClickListener(v -> {
                if (activity != null) {
                    activity.ignoreBatteryOptimization();
                }
            });
        }

        public void updateUI() {
            if (activity == null || getContext() == null) return;

            if (activity.locationGranted) {
                btnLocation.setText(R.string.granted);
                btnLocation.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success));
                btnLocation.setEnabled(false);
                cardLocation.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.success));
                cardLocation.setStrokeWidth(2);
            } else {
                btnLocation.setText(R.string.grant);
                btnLocation.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
                btnLocation.setEnabled(true);
                cardLocation.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.border));
                cardLocation.setStrokeWidth(1);
            }

            if (activity.notificationGranted) {
                btnNotification.setText(R.string.granted);
                btnNotification.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success));
                btnNotification.setEnabled(false);
                cardNotification.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.success));
                cardNotification.setStrokeWidth(2);
            } else {
                btnNotification.setText(R.string.grant);
                btnNotification.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
                btnNotification.setEnabled(true);
                cardNotification.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.border));
                cardNotification.setStrokeWidth(1);
            }

            if (activity.backgroundGranted) {
                btnBackground.setText(R.string.granted);
                btnBackground.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success));
                btnBackground.setEnabled(false);
                cardBackground.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.success));
                cardBackground.setStrokeWidth(2);
            } else {
                btnBackground.setText(R.string.grant);
                btnBackground.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
                btnBackground.setEnabled(true);
                cardBackground.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.border));
                cardBackground.setStrokeWidth(1);
            }

            if (activity.batteryOptimizationIgnored) {
                btnBattery.setText(R.string.granted);
                btnBattery.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success));
                btnBattery.setEnabled(false);
                cardBattery.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.success));
                cardBattery.setStrokeWidth(2);
            } else {
                btnBattery.setText(R.string.grant);
                btnBattery.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
                btnBattery.setEnabled(true);
                cardBattery.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.border));
                cardBattery.setStrokeWidth(1);
            }
        }
    }

    public static class CategoriesFragment extends androidx.fragment.app.Fragment {

        private final String[] categories = {
                "Grocery", "Medicine", "Bills", "Shopping",
                "Hospital", "Office", "Gym", "Restaurant",
                "Petrol", "School", "Pet Care", "Movie"
        };

        private final String[] icons = {
                "🛒", "💊", "📄", "🛍️",
                "🏥", "🏢", "💪", "🍽️",
                "⛽", "🏫", "🐕", "🎬"
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_onboarding_categories, container, false);

            GridLayout gridLayout = view.findViewById(R.id.categoriesGrid);

            for (int i = 0; i < categories.length; i++) {
                View itemView = createCategoryItem(categories[i], icons[i]);
                gridLayout.addView(itemView);
            }

            return view;
        }

        private View createCategoryItem(String name, String icon) {
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            layout.setLayoutParams(params);

            layout.setBackgroundResource(R.drawable.category_item_bg);
            layout.setPadding(16, 16, 16, 16);

            TextView iconView = new TextView(getContext());
            iconView.setText(icon);
            iconView.setTextSize(24);

            TextView nameView = new TextView(getContext());
            nameView.setText(name);
            nameView.setTextSize(12);
            nameView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            nameView.setPadding(0, 8, 0, 0);

            layout.addView(iconView);
            layout.addView(nameView);

            layout.setOnClickListener(v -> {
                boolean selected = layout.isSelected();
                layout.setSelected(!selected);
            });

            return layout;
        }
    }
}