package com.appodeal.mads

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appodeal.mads.databinding.ActivityMainBinding
import com.appodealstack.adapter.admob.AdmobDemand
import com.appodealstack.adapter.admob.FirebaseRemoteConfigConfiguration
import com.appodealstack.adapter.admob.StaticJsonConfiguration
import com.appodealstack.mads.Mads
import com.appodealstack.mads.initializing.InitializationResult

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Mads.withContext(context = this)
            .withConfigurations(
                StaticJsonConfiguration(),
                FirebaseRemoteConfigConfiguration()
            )
            .registerDemands(
                AdmobDemand::class.java
            )
            .build { result: InitializationResult ->
            }
    }

}