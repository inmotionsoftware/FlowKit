package com.inmotionsoftware.example.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inmotionsoftware.example.R
import com.inmotionsoftware.flowkit.android.FlowFragment
import kotlinx.android.synthetic.main.fragment_login.*

sealed class LoginViewResult {
    class Login(val email: String, val password: String): LoginViewResult()
    class Register: LoginViewResult()
    class ForgotPassword(val email: String): LoginViewResult()
}

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : FlowFragment<String?, LoginViewResult>() {
//    lateinit var binding: FragmentLoginBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.error.setText(this.input ?: "")
        this.login.setOnClickListener {
            val email = email.text.toString()
            val pass = password.text.toString()
            this.resolve(LoginViewResult.Login(email,pass))
        }

        this.forgot_pass.setOnClickListener {
            val email = email.text.toString()
            this.resolve(LoginViewResult.ForgotPassword(email = email))
        }

        this.create.setOnClickListener {
            val email = email.text.toString()
            this.resolve(LoginViewResult.Register())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_login, container, false)
        return root
    }
}
