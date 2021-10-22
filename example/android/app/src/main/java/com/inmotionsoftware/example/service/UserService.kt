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
//  UserService.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.example.service

import com.inmotionsoftware.example.flows.Credentials
import com.inmotionsoftware.example.flows.OAuthToken
import com.inmotionsoftware.example.flows.User
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import java.lang.RuntimeException
import java.time.Instant
import java.util.*

private var users = listOf<User>(
    User(firstName="test", lastName="test", email="test@test.com", password="abc123")
)

class UserService {
    sealed class Error(cause: String): RuntimeException(cause) {
        class invalidCreds(): Error("Invalid Credentials")
        class duplicateUser: Error("User already exists")
        class invalidEmail: Error("Invalid Email")
        class invalidPass: Error("Invalid Password")
    }

    fun resetPassword(email: String): Promise<Unit> {
        val user = users.find { it.email.equals(email, ignoreCase=true) }
        return if (user == null) {
            Promise(error=Error.invalidEmail())
        } else {
            Promise.value(Unit)
        }
    }

    fun autenticate(credentials: Credentials): Promise<OAuthToken> =
        Promise.value(Unit).map {
//            val user = users.find { it.email.equals(credentials.username, ignoreCase = true) && it.password.equals(credentials.password, ignoreCase = true) }
//            if (user == null) {
//                throw Error.invalidCreds()
//            }
            val token = "ABC123DEF456"
            OAuthToken(token=token, type="Bearer", expiration=Date())
        }

    fun createAccount(user: User): Promise<Unit> {
        if (user.email.isBlank()) return Promise(error=Error.invalidEmail())
        if (user.password.trim().length < 8) return Promise(error=Error.invalidPass())
        val existing = users.find { it.email.equals(user.email, ignoreCase = true) }
        if (existing != null) return Promise(error=Error.duplicateUser())
        return Promise.value(Unit)
    }
}