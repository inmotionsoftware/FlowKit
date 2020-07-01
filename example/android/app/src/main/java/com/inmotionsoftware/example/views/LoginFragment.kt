package com.inmotionsoftware.example.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inmotionsoftware.example.R
import com.inmotionsoftware.example.flows.Credentials
import com.inmotionsoftware.example.flows.LoginViewResult
import com.inmotionsoftware.flowkit.android.FlowFragment
import kotlinx.android.synthetic.main.fragment_login.*

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : FlowFragment<String?, LoginViewResult>() {

    private var errorString: String? = null
    override fun onInputAttached(input: String?) {
        errorString = input
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.error.setText(errorString ?: "")
        this.login.setOnClickListener {
            val email = email.text.toString()
            val pass = password.text.toString()
            this.resolve(LoginViewResult.Login(Credentials(username = email, password = pass)))
        }

        this.forgot_pass.setOnClickListener {
            val email = email.text.toString()
            this.resolve(LoginViewResult.ForgotPassword(email))
        }

        this.create.setOnClickListener {
            val email = email.text.toString()
            this.resolve(LoginViewResult.Register())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_login, container, false)
}
