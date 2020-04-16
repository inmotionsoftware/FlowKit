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
            val user = users.find { it.email.equals(credentials.username, ignoreCase = true) && it.password.equals(credentials.password, ignoreCase = true) }
            if (user == null) {
                throw Error.invalidCreds()
            }
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