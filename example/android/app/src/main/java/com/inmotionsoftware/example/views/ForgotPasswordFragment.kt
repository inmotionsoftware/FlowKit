package com.inmotionsoftware.example.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inmotionsoftware.example.FlowFragment
import com.inmotionsoftware.example.R
import com.inmotionsoftware.example.databinding.FragmentForgotPasswordBinding
import kotlinx.android.synthetic.main.fragment_forgot_password.*

/**
 * A simple [Fragment] subclass.
 * Use the [ForgotPassword.newInstance] factory method to
 * create an instance of this fragment.
 */
class ForgotPasswordFragment : FlowFragment<String?, String>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        email.setText(this.input ?: "")
        submit.setOnClickListener {
            val email = email.text.toString()
            this.resolve(email)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }
}
