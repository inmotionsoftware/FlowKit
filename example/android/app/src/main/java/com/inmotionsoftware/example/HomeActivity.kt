package com.inmotionsoftware.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import com.inmotionsoftware.example.databinding.ActivityHomeBinding
import com.inmotionsoftware.flowkit.android.FlowActivity
import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.then
import kotlinx.parcelize.Parcelize

sealed class HomeResult: Parcelable {
    @Parcelize class Login(): HomeResult()
    @Parcelize class Profile(): HomeResult()
}

class HomeActivity : FlowActivity<HomeResult>() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        binding = ActivityHomeBinding.inflate( layoutInflater )
        val view = binding.root
        setContentView(view)

        binding.login.setOnClickListener {
            this.resolve(HomeResult.Login())
        }

        binding.profile.setOnClickListener {
            this.resolve(HomeResult.Profile())
        }
    }
}
