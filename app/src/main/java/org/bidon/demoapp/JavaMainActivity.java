package org.bidon.demoapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.bidon.sdk.BidonSdk;
import org.bidon.sdk.ads.Ad;
import org.bidon.sdk.ads.AuctionInfo;
import org.bidon.sdk.ads.interstitial.InterstitialListener;
import org.bidon.sdk.config.BidonError;
import org.bidon.sdk.config.InitializationCallback;
import org.bidon.sdk.logs.analytic.AdValue;
import org.bidon.sdk.logs.logging.Logger;

/**
 * Created by Bidon Team on 28/02/2023.
 */
@SuppressWarnings("AccessStaticViaInstance")
public class JavaMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InterstitialListener listener = new InterstitialListener() {
            @Override
            public void onAdLoaded(@NonNull Ad ad, @NonNull AuctionInfo auctionInfo) {

            }

            @Override
            public void onAdLoadFailed(@Nullable AuctionInfo auctionInfo, @NonNull BidonError bidonError) {

            }

            @Override
            public void onAdShown(@NonNull Ad ad) {

            }

            @Override
            public void onAdClicked(@NonNull Ad ad) {

            }

            @Override
            public void onAdExpired(@NonNull Ad ad) {

            }

            @Override
            public void onAdShowFailed(@NonNull BidonError bidonError) {

            }

            @Override
            public void onAdClosed(@NonNull Ad ad) {

            }

            @Override
            public void onRevenuePaid(@NonNull Ad ad, @NonNull AdValue adValue) {
                double a = ad.getPrice();
                adValue.getAdRevenue();
                adValue.getCurrency();
                adValue.getPrecision();
            }
        };
        BidonSdk.setLoggerLevel(Logger.Level.Verbose)
                .registerDefaultAdapters()
                .setInitializationCallback(new InitializationCallback() {
                    @Override
                    public void onFinished() {

                    }
                })
                .setBaseUrl("asd")
                .initialize(this, BuildConfig.BIDON_API_KEY);
    }
}
