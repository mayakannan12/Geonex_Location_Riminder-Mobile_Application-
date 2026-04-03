package com.example.geonex;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FeatureSlideFragment extends Fragment {

    private static final String TAG = "FeatureSlideFragment";
    private static final String ARG_FEATURE_DATA = "feature_data";
    private static final long ANIMATION_DURATION = 600;
    private static final long STAGGER_DELAY = 150;

    private FeatureData featureData;
    private WeakReference<View> rootViewRef;
    private Handler animationHandler = new Handler(Looper.getMainLooper());

    // Animation variables
    private AnimatorSet iconPulseAnim;
    private AnimatorSet ringOuterAnim;
    private ObjectAnimator shineAnim;
    private ObjectAnimator orb1Anim, orb2Anim, orb3Anim;
    private List<View> particles = new ArrayList<>();

    public static FeatureSlideFragment newInstance(FeatureData featureData) {
        FeatureSlideFragment fragment = new FeatureSlideFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FEATURE_DATA, featureData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            featureData = (FeatureData) getArguments().getSerializable(ARG_FEATURE_DATA);
        }

        if (featureData == null) {
            Log.e(TAG, "FeatureData is null - cannot create fragment");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feature_slide, container, false);
        rootViewRef = new WeakReference<>(view);

        if (featureData != null) {
            setupViews(view);
        } else {
            Log.e(TAG, "Cannot setup views: featureData is null");
            TextView tvTitle = view.findViewById(R.id.tvFeatureTitle);
            if (tvTitle != null) {
                tvTitle.setText("Feature unavailable");
            }
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (featureData != null && getView() != null) {
            startAllAnimations(getView());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop animations when fragment is not visible
        cleanupAnimations();

        View rootView = rootViewRef.get();
        if (rootView != null) {
            rootView.animate().cancel();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanupAnimations();

        if (rootViewRef != null) {
            rootViewRef.clear();
        }
        animationHandler.removeCallbacksAndMessages(null);
    }

    private void setupViews(@NonNull View rootView) {
        if (featureData == null || getContext() == null) return;

        try {
            ImageView ivIcon = rootView.findViewById(R.id.ivFeatureIcon);
            TextView tvTitle = rootView.findViewById(R.id.tvFeatureTitle);
            TextView tvDescription = rootView.findViewById(R.id.tvFeatureDescription);
            LinearLayout bulletContainer = rootView.findViewById(R.id.bulletPointsContainer);

            // Set content with safety checks
            if (ivIcon != null && featureData.getIconResId() != 0) {
                ivIcon.setImageResource(featureData.getIconResId());
            }

            if (tvTitle != null && featureData.getTitleResId() != 0) {
                tvTitle.setText(featureData.getTitleResId());
                // Apply gradient to text
                tvTitle.setBackgroundResource(R.drawable.gradient_text_bg);
            }

            if (tvDescription != null && featureData.getDescriptionResId() != 0) {
                tvDescription.setText(featureData.getDescriptionResId());
            }

            // Clear any existing bullet points
            if (bulletContainer != null) {
                bulletContainer.removeAllViews();

                // Create bullet points dynamically
                int[] bulletPoints = featureData.getBulletPoints();
                if (bulletPoints != null) {
                    for (int i = 0; i < bulletPoints.length; i++) {
                        if (bulletPoints[i] != 0) {
                            View bulletItem = createBulletItem(bulletPoints[i], i);
                            if (bulletItem != null) {
                                bulletContainer.addView(bulletItem);
                            }
                        } else {
                            Log.e(TAG, "Bullet point at index " + i + " has invalid resource ID 0");
                        }
                    }
                }
            }

            // Initially hide all animated views
            setViewsAlpha(rootView, 0f);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up views: " + e.getMessage());
        }
    }

    private View createBulletItem(int textResId, int index) {
        if (getContext() == null || textResId == 0) {
            Log.e(TAG, "Cannot create bullet item: invalid resource ID " + textResId);
            return null;
        }

        try {
            LinearLayout bulletLayout = new LinearLayout(requireContext());
            bulletLayout.setOrientation(LinearLayout.HORIZONTAL);
            bulletLayout.setPadding(0, 8, 0, 8);
            bulletLayout.setId(View.generateViewId());

            // Bullet icon (premium star instead of bullet)
            TextView tvBulletIcon = new TextView(requireContext());
            tvBulletIcon.setText("✦");
            tvBulletIcon.setTextSize(20);
            tvBulletIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            tvBulletIcon.setPadding(0, 0, 12, 0);

            // Bullet text
            TextView tvBulletText = new TextView(requireContext());

            // Safely set text with resource ID
            try {
                tvBulletText.setText(textResId);
            } catch (android.content.res.Resources.NotFoundException e) {
                Log.e(TAG, "String resource not found: " + textResId);
                tvBulletText.setText("• Feature");
            }

            tvBulletText.setTextSize(16);
            tvBulletText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvBulletText.setLineSpacing(4f, 1.2f);

            bulletLayout.addView(tvBulletIcon);
            bulletLayout.addView(tvBulletText);

            return bulletLayout;

        } catch (Exception e) {
            Log.e(TAG, "Error creating bullet item: " + e.getMessage());
            return null;
        }
    }

    private void setViewsAlpha(@NonNull View rootView, float alpha) {
        try {
            View iconCard = rootView.findViewById(R.id.iconCard);
            if (iconCard != null) iconCard.setAlpha(alpha);

            TextView tvTitle = rootView.findViewById(R.id.tvFeatureTitle);
            if (tvTitle != null) tvTitle.setAlpha(alpha);

            TextView tvDesc = rootView.findViewById(R.id.tvFeatureDescription);
            if (tvDesc != null) tvDesc.setAlpha(alpha);

            LinearLayout bulletContainer = rootView.findViewById(R.id.bulletPointsContainer);
            if (bulletContainer != null) {
                for (int i = 0; i < bulletContainer.getChildCount(); i++) {
                    bulletContainer.getChildAt(i).setAlpha(alpha);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting alpha: " + e.getMessage());
        }
    }

    // ==================== PREMIUM ANIMATION METHODS ====================

    /**
     * Start all animations when fragment becomes visible
     */
    private void startAllAnimations(View rootView) {
        startPremiumAnimations();
        startIconPulse(rootView);
        startContentReveal(rootView);
        startShineEffect(rootView);
        startBackgroundAnimations();
        createParticles();
    }

    /**
     * Icon Pulse Animation
     */
    private void startIconPulse(View rootView) {
        View iconRing = rootView.findViewById(R.id.iconRing);
        View outerRing = rootView.findViewById(R.id.outerRing);

        if (iconRing != null) {
            iconPulseAnim = (AnimatorSet) AnimatorInflater.loadAnimator(requireContext(), R.animator.pulse_icon);
            iconPulseAnim.setTarget(iconRing);
            iconPulseAnim.start();
        }

        if (outerRing != null) {
            ringOuterAnim = (AnimatorSet) AnimatorInflater.loadAnimator(requireContext(), R.animator.ring_outer_pulse);
            ringOuterAnim.setTarget(outerRing);
            ringOuterAnim.start();
        }
    }

    /**
     * Content Reveal Animation (Staggered)
     */
    private void startContentReveal(View rootView) {
        TextView tvTitle = rootView.findViewById(R.id.tvFeatureTitle);
        TextView tvDesc = rootView.findViewById(R.id.tvFeatureDescription);
        LinearLayout bulletContainer = rootView.findViewById(R.id.bulletPointsContainer);

        // Title animation
        if (tvTitle != null) {
            tvTitle.setAlpha(0f);
            tvTitle.setTranslationY(10f);
            tvTitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(100)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Description animation
        if (tvDesc != null) {
            tvDesc.setAlpha(0f);
            tvDesc.setTranslationY(10f);
            tvDesc.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Bullet points stagger animation
        if (bulletContainer != null) {
            for (int i = 0; i < bulletContainer.getChildCount(); i++) {
                View bullet = bulletContainer.getChildAt(i);
                bullet.setAlpha(0f);
                bullet.setTranslationX(-14f);

                long delay = 300 + (i * 120); // 300ms, 420ms, 540ms
                bullet.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(500)
                        .setStartDelay(delay)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }
    }

    /**
     * Shine Effect Animation
     */
    private void startShineEffect(View rootView) {
        View shineOverlay = rootView.findViewById(R.id.shineOverlay);
        if (shineOverlay != null) {
            shineOverlay.setAlpha(0.08f);

            shineAnim = ObjectAnimator.ofFloat(
                    shineOverlay,
                    "translationX",
                    -1000f, 1000f
            );
            shineAnim.setDuration(3000);
            shineAnim.setRepeatCount(ValueAnimator.INFINITE);
            shineAnim.setInterpolator(new LinearInterpolator());
            shineAnim.setStartDelay(1000);
            shineAnim.start();
        }
    }

    /**
     * Background Orb Animations
     */
    private void startBackgroundAnimations() {
        Activity activity = getActivity();
        if (activity == null) return;

        View orb1 = activity.findViewById(R.id.orb1);
        View orb2 = activity.findViewById(R.id.orb2);
        View orb3 = activity.findViewById(R.id.orb3);

        if (orb1 != null) {
            orb1Anim = (ObjectAnimator) AnimatorInflater.loadAnimator(activity, R.animator.orb_float);
            orb1Anim.setTarget(orb1);
            orb1Anim.start();
        }

        if (orb2 != null) {
            orb2Anim = (ObjectAnimator) AnimatorInflater.loadAnimator(activity, R.animator.orb_float);
            orb2Anim.setTarget(orb2);
            orb2Anim.setStartDelay(3000);
            orb2Anim.start();
        }

        if (orb3 != null) {
            orb3Anim = (ObjectAnimator) AnimatorInflater.loadAnimator(activity, R.animator.orb_float);
            orb3Anim.setTarget(orb3);
            orb3Anim.setStartDelay(5000);
            orb3Anim.start();
        }
    }

    /**
     * Create Twinkling Particles
     */
    private void createParticles() {
        Activity activity = getActivity();
        if (activity == null) return;

        FrameLayout particleContainer = activity.findViewById(R.id.particleContainer);
        if (particleContainer == null) return;

        // Clear existing particles
        particleContainer.removeAllViews();
        particles.clear();

        Random random = new Random();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        for (int i = 0; i < 40; i++) {
            View particle = new View(activity);
            particle.setBackgroundResource(R.drawable.particle);

            // Random size between 2-4dp
            int size = 2 + random.nextInt(3);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    dpToPx(size),
                    dpToPx(size)
            );

            // Random position
            params.leftMargin = random.nextInt(screenWidth);
            params.topMargin = random.nextInt(screenHeight);
            particle.setLayoutParams(params);

            particleContainer.addView(particle);
            particles.add(particle);

            // Random animation
            ObjectAnimator particleAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(
                    activity,
                    R.animator.particle_twinkle
            );
            particleAnim.setTarget(particle);
            particleAnim.setDuration(2000 + random.nextInt(3000));
            particleAnim.setStartDelay(random.nextInt(5000));
            particleAnim.start();
        }
    }

    /**
     * Helper method to convert dp to px
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Clean up all animations
     */
    private void cleanupAnimations() {
        if (iconPulseAnim != null) iconPulseAnim.cancel();
        if (ringOuterAnim != null) ringOuterAnim.cancel();
        if (shineAnim != null) shineAnim.cancel();
        if (orb1Anim != null) orb1Anim.cancel();
        if (orb2Anim != null) orb2Anim.cancel();
        if (orb3Anim != null) orb3Anim.cancel();

        // Remove particles
        Activity activity = getActivity();
        if (activity != null) {
            FrameLayout particleContainer = activity.findViewById(R.id.particleContainer);
            if (particleContainer != null) {
                particleContainer.removeAllViews();
            }
        }
        particles.clear();
    }

    /**
     * Start premium animations (existing method)
     */
    private void startPremiumAnimations() {
        View rootView = rootViewRef.get();
        if (rootView == null || !isAdded()) return;

        try {
            // Icon animation
            View iconCard = rootView.findViewById(R.id.iconCard);
            if (iconCard != null) {
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconCard, "scaleX", 0.6f, 1.0f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconCard, "scaleY", 0.6f, 1.0f);
                ObjectAnimator alphaIcon = ObjectAnimator.ofFloat(iconCard, "alpha", 0f, 1f);

                AnimatorSet iconAnim = new AnimatorSet();
                iconAnim.playTogether(scaleX, scaleY, alphaIcon);
                iconAnim.setDuration(ANIMATION_DURATION);
                iconAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                iconAnim.start();
            }

            // Glow effect animation
            View glowEffect = rootView.findViewById(R.id.glowEffect);
            if (glowEffect != null) {
                glowEffect.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .alpha(0.8f)
                        .setDuration(1000)
                        .withEndAction(() -> {
                            glowEffect.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .alpha(0.6f)
                                    .setDuration(1000)
                                    .start();
                        })
                        .start();
            }

            // Title animation
            TextView tvTitle = rootView.findViewById(R.id.tvFeatureTitle);
            if (tvTitle != null) {
                tvTitle.setTranslationY(20f);
                tvTitle.setAlpha(0f);
                tvTitle.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(ANIMATION_DURATION)
                        .setStartDelay(150)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }

            // Description animation
            TextView tvDesc = rootView.findViewById(R.id.tvFeatureDescription);
            if (tvDesc != null) {
                tvDesc.setAlpha(0f);
                tvDesc.setTranslationY(15f);
                tvDesc.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(ANIMATION_DURATION)
                        .setStartDelay(300)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }

            // Content card animation
            View contentCard = rootView.findViewById(R.id.contentCard);
            if (contentCard != null) {
                contentCard.setScaleX(0.95f);
                contentCard.setScaleY(0.95f);
                contentCard.setAlpha(0f);
                contentCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(ANIMATION_DURATION)
                        .setStartDelay(50)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }

            // Bullet points animation
            LinearLayout bulletContainer = rootView.findViewById(R.id.bulletPointsContainer);
            if (bulletContainer != null) {
                for (int i = 0; i < bulletContainer.getChildCount(); i++) {
                    View bullet = bulletContainer.getChildAt(i);
                    bullet.setAlpha(0f);
                    bullet.setTranslationX(-20f);

                    long delay = 450 + (i * STAGGER_DELAY);
                    bullet.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(ANIMATION_DURATION)
                            .setStartDelay(delay)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting animations: " + e.getMessage());
        }
    }

    /**
     * Start floating bubbles animation (existing method)
     */
    private void animateBubbles() {
        View rootView = rootViewRef.get();
        if (rootView == null || !isAdded()) return;

        View bubble1 = rootView.findViewById(R.id.bubble1);
        View bubble2 = rootView.findViewById(R.id.bubble2);
        View bubble3 = rootView.findViewById(R.id.bubble3);

        if (bubble1 != null) {
            bubble1.animate()
                    .translationY(-30f)
                    .translationX(20f)
                    .setDuration(3000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        bubble1.animate()
                                .translationY(0f)
                                .translationX(0f)
                                .setDuration(3000)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();
                    })
                    .start();
        }

        if (bubble2 != null) {
            bubble2.animate()
                    .translationY(40f)
                    .translationX(-30f)
                    .setDuration(4000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        bubble2.animate()
                                .translationY(0f)
                                .translationX(0f)
                                .setDuration(4000)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();
                    })
                    .start();
        }

        if (bubble3 != null) {
            bubble3.animate()
                    .rotation(360f)
                    .setDuration(8000)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(() -> {
                        bubble3.animate()
                                .rotation(0f)
                                .setDuration(8000)
                                .setInterpolator(new LinearInterpolator())
                                .start();
                    })
                    .start();
        }
    }

    /**
     * Start particle animation (existing method)
     */
    private void animateParticles() {
        View rootView = rootViewRef.get();
        if (rootView == null || !isAdded()) return;

        View particle1 = rootView.findViewById(R.id.particle1);
        View particle2 = rootView.findViewById(R.id.particle2);
        View particle3 = rootView.findViewById(R.id.particle3);

        if (particle1 != null) {
            particle1.animate()
                    .alpha(0.2f)
                    .setDuration(1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        particle1.animate()
                                .alpha(1f)
                                .setDuration(1000)
                                .start();
                    })
                    .start();
        }

        if (particle2 != null) {
            particle2.animate()
                    .translationX(20f)
                    .translationY(-20f)
                    .setDuration(2000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        particle2.animate()
                                .translationX(0f)
                                .translationY(0f)
                                .setDuration(2000)
                                .start();
                    })
                    .start();
        }

        if (particle3 != null) {
            particle3.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(1500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        particle3.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(1500)
                                .start();
                    })
                    .start();
        }
    }

    /**
     * Start shine effect animation (existing method)
     */
    private void animateShine() {
        View rootView = rootViewRef.get();
        if (rootView == null || !isAdded()) return;

        View shine = rootView.findViewById(R.id.shineEffect);
        if (shine != null) {
            shine.setVisibility(View.VISIBLE);

            shine.animate()
                    .alpha(0.8f)
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(1500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        shine.animate()
                                .alpha(0f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(1000)
                                .withEndAction(() -> {
                                    shine.setVisibility(View.GONE);
                                })
                                .start();
                    })
                    .start();
        }
    }
}