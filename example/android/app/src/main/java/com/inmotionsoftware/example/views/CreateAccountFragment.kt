// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a 
// copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the 
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
//  CreateAccountFragment.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.example.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inmotionsoftware.example.R
import com.inmotionsoftware.example.databinding.FragmentCreateAccountBinding
import com.inmotionsoftware.example.flows.User
import com.inmotionsoftware.flowkit.android.FlowFragment

/**
 * A simple [Fragment] subclass.
 * Use the [CreateAccount.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateAccountFragment : FlowFragment<String?, User>() {
    private lateinit var binding: FragmentCreateAccountBinding

    private var errorString: String? = null
    override fun onInputAttached(input: String?) {
        errorString = input
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.error.setText(errorString ?: "")

        binding.create.setOnClickListener {
            val firstName = binding.firstName.text.toString()
            val lastName = binding.lastName.text.toString()
            val email = binding.email.text.toString()
            val pass = binding.password.text.toString()
            val user = User(firstName=firstName, lastName = lastName, email = email, password = pass)
            this.resolve(user)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        binding = FragmentCreateAccountBinding.inflate( inflater, container, false)
        val view = binding.root
        return view
    }
}
