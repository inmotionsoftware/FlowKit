package com.inmotionsoftware.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import com.inmotionsoftware.flowkit.android.FlowActivity
import com.inmotionsoftware.flowkit.android.UnitFlowActivity
import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.then
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.view.*

sealed class HomeResult: Parcelable {
    @Parcelize class Login(): HomeResult()
}

class HomeActivity : UnitFlowActivity<HomeResult>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        this.login.setOnClickListener {
            this.resolve(HomeResult.Login())
        }
    }
}
