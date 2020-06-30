package com.inmotionsoftware.example.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inmotionsoftware.example.R
import com.inmotionsoftware.example.databinding.FragmentForgotPasswordBinding
import com.inmotionsoftware.flowkit.android.FlowFragment
import kotlinx.android.synthetic.main.fragment_forgot_password.*

/**
 * A simple [Fragment] subclass.
 * Use the [ForgotPassword.newInstance] factory method to
 * create an instance of this fragment.
 */
class ForgotPasswordFragment : FlowFragment<String?, String>() {

    private var emailString: String? = null
    override fun onInputAttached(input: String?) {
        emailString = input
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.email.setText(emailString)
        this.submit.setOnClickListener {
            val email = email.text.toString()
            this.resolve(email)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }
}
