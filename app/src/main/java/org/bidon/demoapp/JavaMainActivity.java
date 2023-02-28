package org.bidon.demoapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.bidon.sdk.BidOnSdk;
import org.bidon.sdk.ads.Ad;
import org.bidon.sdk.ads.interstitial.InterstitialListener;
import org.bidon.sdk.auction.AuctionResult;
import org.bidon.sdk.config.BidonError;
import org.bidon.sdk.config.InitializationCallback;
import org.bidon.sdk.logs.analytic.AdValue;
import org.bidon.sdk.logs.logging.Logger;

import java.util.List;

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
@SuppressWarnings("AccessStaticViaInstance")
public class JavaMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InterstitialListener listener = new InterstitialListener() {
            @Override
            public void onAdLoaded(@NonNull Ad ad) {

            }

            @Override
            public void onAdLoadFailed(@NonNull BidonError bidonError) {

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
            public void onAuctionStarted() {

            }

            @Override
            public void onAuctionSuccess(@NonNull List<AuctionResult> list) {
            }

            @Override
            public void onAuctionFailed(@NonNull Throwable throwable) {

            }

            @Override
            public void onRoundStarted(@NonNull String s, double v) {

            }

            @Override
            public void onRoundSucceed(@NonNull String s, @NonNull List<AuctionResult> list) {
                list.get(0).getAdSource().getDemandId();
            }

            @Override
            public void onRoundFailed(@NonNull String s, @NonNull Throwable throwable) {

            }

            @Override
            public void onRevenuePaid(@NonNull Ad ad, @NonNull AdValue adValue) {
                double a = ad.getEcpm();
                adValue.getAdRevenue();
                adValue.getCurrency();
                adValue.getPrecision();
            }
        };
        BidOnSdk.setLoggerLevel(Logger.Level.Verbose)
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
