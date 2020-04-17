package com.inmotionsoftware.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.inmotionsoftware.flowkit.android.FlowActivity
import com.inmotionsoftware.promisekt.features.after
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.then
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.view.*

class HomeActivity : FlowActivity<String, String>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        this.submit.setOnClickListener {
            val output = this.text.text.toString()
            this.resolve(output)
        }

        this.cancel.setOnClickListener {
            this.cancel()
        }
    }
}
