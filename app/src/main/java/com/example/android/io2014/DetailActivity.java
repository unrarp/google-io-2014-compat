/**
 * Copyright 2014 Rahul Parsani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.io2014;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.PaletteItem;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.io2014.ui.AnimatedPathView;
import com.example.android.io2014.ui.AnimatorListener;
import com.example.android.io2014.ui.SpotlightView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class DetailActivity extends ActionBarActivity {

    private static final Interpolator sDecelerator = new DecelerateInterpolator();
    private static final Interpolator sAccelerator = new AccelerateInterpolator();

    // Used for the circular reveal of the map
    private SpotlightView spotlight;

    // Used for the circular reveal of the map. On entry, animatedHero is scaled and
    // translated to the correct position and then crossfades to hero which has no tint.
    // The exit animation runs all of this in reverse.
    private ImageView hero, animatedHero;

    private View mapContainer, infoContainer, container;
    private float maskScale = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Hide the back button until the entry animation is complete
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        spotlight = (SpotlightView) findViewById(R.id.spotlight);
        mapContainer = findViewById(R.id.map_container);
        infoContainer = findViewById(R.id.info_container);
        container = findViewById(R.id.container);
        hero = (ImageView) findViewById(R.id.photo);
        animatedHero = (ImageView) findViewById(R.id.animated_photo);

        // Setup the alpha values here for Gingerbread support
        ViewHelper.setAlpha(infoContainer, 0);
        ViewHelper.setAlpha(container, 0);

        Bitmap heroImage = setupPhoto(getIntent().getIntExtra("photo", R.drawable.photo1));
        colorize(heroImage);

        setupMap();
        setupText();
    }

    @Override
    public void onBackPressed() {
        ViewPropertyAnimator.animate(animatedHero).alpha(1);
        ViewPropertyAnimator.animate(infoContainer).alpha(0).setListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                runExitAnimation();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupText() {
        TextView titleView = (TextView) findViewById(R.id.title);
        titleView.setText(getIntent().getStringExtra("title"));

        TextView descriptionView = (TextView) findViewById(R.id.description);
        descriptionView.setText(getIntent().getStringExtra("description"));
    }

    private void setupMap() {
        final GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

        double lat = getIntent().getDoubleExtra("lat", 37.6329946);
        double lng = getIntent().getDoubleExtra("lng", -122.4938344);
        float zoom = getIntent().getFloatExtra("zoom", 15);

        LatLng position = new LatLng(lat, lng);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
        map.addMarker(new MarkerOptions().position(position));

        // We need the snapshot of the map to prepare the shader for the circular reveal.
        // So the map is visible on activity start and then once the snapshot is taken, quickly hidden.
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                map.snapshot(new GoogleMap.SnapshotReadyCallback() {
                    @Override
                    public void onSnapshotReady(Bitmap bitmap) {
                        mapContainer.setVisibility(View.INVISIBLE);
                        spotlight.createShaderB(bitmap);
                        maskScale = spotlight.computeMaskScale(Math.max(spotlight.getHeight(), spotlight.getWidth()) * 4.0f);
                    }
                });
            }
        });

        // This is basically a OnGlobalLayoutListener callback.
        // The views have their dimensions at this point.
        // At this point we can run the entry animation.
        spotlight.setAnimationSetupCallback(new SpotlightView.AnimationSetupCallback() {
            @Override
            public void onSetupAnimation(SpotlightView spotlight) {
                View button = findViewById(R.id.info);
                spotlight.setMaskX(button.getRight() - (button.getWidth() / 2));
                spotlight.setMaskY(button.getBottom() - (button.getHeight() / 2));

                runEnterAnimation();
            }
        });
    }

    private void colorize(Bitmap photo) {
        Palette palette = Palette.generate(photo);
        applyPalette(palette);
    }

    private void applyPalette(Palette palette) {
        container.setBackgroundColor(getColor(palette.getDarkMutedColor(), R.color.default_dark_muted));

        TextView titleView = (TextView) findViewById(R.id.title);
        titleView.setTextColor(getColor(palette.getVibrantColor(), R.color.default_vibrant));

        TextView descriptionView = (TextView) findViewById(R.id.description);
        descriptionView.setTextColor(getColor(palette.getLightVibrantColor(), R.color.default_light_vibrant));

        colorButton(R.id.info_button, getColor(palette.getDarkMutedColor(), R.color.default_dark_muted),
                getColor(palette.getDarkVibrantColor(), R.color.default_dark_vibrant));
        colorButton(R.id.star_button, getColor(palette.getMutedColor(), R.color.default_muted),
                getColor(palette.getVibrantColor(), R.color.default_vibrant));

        mapContainer.setBackgroundColor(getColor(palette.getLightMutedColor(), R.color.default_light_muted));
        spotlight.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        AnimatedPathView star = (AnimatedPathView) findViewById(R.id.star_container);
        star.setFillColor(getColor(palette.getVibrantColor(), R.color.default_vibrant));
        star.setStrokeColor(getColor(palette.getLightVibrantColor(), R.color.default_light_vibrant));
    }

    private void colorButton(int id, int bgColor, int tintColor) {
        ImageButton buttonView = (ImageButton) findViewById(id);

        // TODO Create a StateListDrawable and use the tintColor
        GradientDrawable bgShape = (GradientDrawable) buttonView.getBackground();
        bgShape.setColor(bgColor);
    }

    /**
     * Helper function to help protect against palette items that are null
     *
     * @param item
     * @param resId
     * @return
     */
    private int getColor(PaletteItem item, int resId) {
        return item == null ? getResources().getColor(resId) : item.getRgb();
    }

    private Bitmap setupPhoto(int resource) {
        Bitmap bitmap = MainActivity.sPhotoCache.get(resource);
        hero.setImageBitmap(bitmap);
        animatedHero.setImageBitmap(bitmap);
        return bitmap;
    }

    public void showStar(View view) {
        toggleStarView();
    }

    private void toggleStarView() {
        final AnimatedPathView starContainer = (AnimatedPathView) findViewById(R.id.star_container);

        if (starContainer.getVisibility() == View.INVISIBLE) {
            ViewPropertyAnimator.animate(hero).alpha(0.2f);
            ViewPropertyAnimator.animate(starContainer).alpha(1);
            starContainer.setVisibility(View.VISIBLE);
            starContainer.reveal();
        } else {
            ViewPropertyAnimator.animate(hero).alpha(1);
            ViewPropertyAnimator.animate(starContainer).alpha(0).setListener(new AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    starContainer.setVisibility(View.INVISIBLE);
                    ViewPropertyAnimator.animate(starContainer).setListener(null);
                }
            });
        }
    }

    public void showInformation(View view) {
        toggleInformationView(view);
    }

    private void toggleInformationView(View view) {
        if (mapContainer.getVisibility() == View.INVISIBLE) {
            createScaleAnimation(spotlight);
        } else {
            createShrinkAnimation(spotlight);
        }
    }

    private void createScaleAnimation(final SpotlightView spotlight) {
        spotlight.setVisibility(View.VISIBLE);
        ObjectAnimator superScale = ObjectAnimator.ofFloat(spotlight, "maskScale", maskScale);

        AnimatorSet set = new AnimatorSet();
        set.play(superScale);
        set.start();

        set.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                hero.setVisibility(View.INVISIBLE);
                mapContainer.setVisibility(View.VISIBLE);
                spotlight.setVisibility(View.GONE);
            }
        });
    }

    private void createShrinkAnimation(final SpotlightView spotlight) {
        mapContainer.setVisibility(View.INVISIBLE);
        spotlight.setVisibility(View.VISIBLE);
        hero.setVisibility(View.VISIBLE);
        spotlight.setMaskScale(maskScale);

        ObjectAnimator superShrink = ObjectAnimator.ofFloat(spotlight, "maskScale", maskScale, 0.5f);

        AnimatorSet set = new AnimatorSet();
        set.play(superShrink);
        set.start();

        set.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                spotlight.setVisibility(View.GONE);
            }
        });
    }

    /**
     * The enter animation scales the hero in from its previous thumbnail
     * size/location. In parallel, the container of the activity is fading in.
     * When the picture is in place, we crossfade in the actual hero image.
     */
    public void runEnterAnimation() {
        // Retrieve the data we need for the picture to display and
        // the thumbnail to animate it from
        Bundle bundle = getIntent().getExtras();
        final int thumbnailTop = bundle.getInt("top");
        final int thumbnailLeft = bundle.getInt("left");
        final int thumbnailWidth = bundle.getInt("width");
        final int thumbnailHeight = bundle.getInt("height");

        // Scale factors to make the large version the same size as the thumbnail
        float mWidthScale = (float) thumbnailWidth / animatedHero.getWidth();
        float mHeightScale = (float) thumbnailHeight / animatedHero.getHeight();

        // Set starting values for properties we're going to animate. These
        // values scale and position the full size version down to the thumbnail
        // size/location, from which we'll animate it back up
        ViewHelper.setPivotX(animatedHero, 0);
        ViewHelper.setPivotY(animatedHero, 0);
        ViewHelper.setScaleX(animatedHero, mWidthScale);
        ViewHelper.setScaleY(animatedHero, mHeightScale);
        ViewHelper.setTranslationX(animatedHero, thumbnailLeft);
        ViewHelper.setTranslationY(animatedHero, thumbnailTop);

        // Animate scale and translation to go from thumbnail to full size
        ViewPropertyAnimator.animate(animatedHero).
                scaleX(1).scaleY(1).
                translationX(0).translationY(0).
                setInterpolator(sDecelerator).
                setListener(new AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewPropertyAnimator.animate(animatedHero).alpha(0);
                        ViewPropertyAnimator.animate(infoContainer).alpha(1);

                        // The back button can be shown
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    }
                });
        // Animate in the container with the background and text
        ViewPropertyAnimator.animate(container).alpha(1);
    }

    /**
     * The exit animation is basically a reverse of the enter animation
     */
    public void runExitAnimation() {
        Bundle bundle = getIntent().getExtras();
        final int thumbnailTop = bundle.getInt("top");
        final int thumbnailLeft = bundle.getInt("left");
        final int thumbnailWidth = bundle.getInt("width");
        final int thumbnailHeight = bundle.getInt("height");

        float mWidthScale = (float) thumbnailWidth / animatedHero.getWidth();
        float mHeightScale = (float) thumbnailHeight / animatedHero.getHeight();

        ViewHelper.setPivotX(animatedHero, 0);
        ViewHelper.setPivotY(animatedHero, 0);
        ViewHelper.setScaleX(animatedHero, 1);
        ViewHelper.setScaleY(animatedHero, 1);
        ViewHelper.setTranslationX(animatedHero, 0);
        ViewHelper.setTranslationY(animatedHero, 0);

        ViewPropertyAnimator.animate(animatedHero).
                scaleX(mWidthScale).scaleY(mHeightScale).
                translationX(thumbnailLeft).translationY(thumbnailTop).
                setInterpolator(sAccelerator).
                setListener(new AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        finish();
                    }
                });
        ViewPropertyAnimator.animate(container).alpha(0);

        // Hide the back button during the exit animation
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void finish() {
        super.finish();

        // Override transitions: we don't want the normal window animation in addition to our
        // custom one
        overridePendingTransition(0, 0);
    }

}
