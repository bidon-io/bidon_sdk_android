package org.bidon.demoapp;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.bidon.sdk.BidOnSdk;
import org.bidon.sdk.config.InitializationCallback;
import org.bidon.sdk.logs.logging.Logger;

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
public class JavaMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
