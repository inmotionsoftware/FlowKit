package com.inmotionsoftware.example.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inmotionsoftware.example.R
import com.inmotionsoftware.example.databinding.FragmentCreateAccountBinding
import com.inmotionsoftware.example.models.User
import com.inmotionsoftware.flowkit.android.FlowFragment
import kotlinx.android.synthetic.main.fragment_create_account.*

/**
 * A simple [Fragment] subclass.
 * Use the [CreateAccount.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateAccountFragment : FlowFragment<String?, User>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        this.error.setText(this.input ?: "")
        this.create.setOnClickListener {
            val firstName = this.firstName.text.toString()
            val lastName = this.lastName.text.toString()
            val email = this.email.text.toString()
            val pass = this.password.text.toString()
            val user = User(firstName=firstName, lastName = lastName, email = email, password = pass)
            this.resolve(user)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_account, container, false)
    }
}
