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
//  UserService.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/10/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import Foundation
import PromiseKit

fileprivate var users = [
    User(firstName: "test", lastName: "test", email: "test@test.com", password: "abc123")
]

public struct UserService {

    public enum Error: Swift.Error {
        case invalidCreds
        case duplicateUser
        case invalidEmail
        case invalidPass
    }

    func resetPassword(email: String) -> Promise<Void> {
        guard (users.contains { $0.email.caseInsensitiveCompare(email) == .orderedSame }) else {
            return Promise(error: Error.invalidEmail)
        }
        return Promise.value(())
    }

    func autenticate(credentials: Credentials) -> Promise<OAuthToken> {
        return Promise().map {
            let found = users.first { $0.email.caseInsensitiveCompare(credentials.username) == .orderedSame && $0.password == credentials.password }
            guard let user = found else { throw Error.invalidCreds }
            let jwt = JWT(payload: JWT.Payload(sub: user.userId.uuidString))
            let token = try jwt.token()
            return OAuthToken(token: token, type: "Bearer", expiration: Date())
        }
    }

    func createAccount(user: User) -> Promise<Void> {
        guard !user.email.isEmpty else { return Promise(error: Error.invalidEmail) }
        guard user.password.count >= 8 else { return Promise(error: Error.invalidPass) }
        guard !(users.contains {$0.email.caseInsensitiveCompare(user.email) == .orderedSame }) else { return Promise(error: Error.duplicateUser) }
        users.append(user)
        return Promise.value(())
    }
}

extension UserService.Error: LocalizedError {
    public var description: String {
        switch self {
            case .duplicateUser: return "User already exists"
            case .invalidCreds: return "Invalid credentials"
            case .invalidEmail: return "Invalid Email"
            case .invalidPass: return "Invalid Password"
        }
    }

    /// A localized message describing what error occurred.
    public var errorDescription: String? { return self.description }

    /// A localized message describing the reason for the failure.
    public var failureReason: String? { return "reason: \(self.description)" }

    /// A localized message describing how one might recover from the failure.
    public var recoverySuggestion: String? { return nil }

    /// A localized message providing "help" text if the user requests help.
    public var helpAnchor: String? { return nil }
}
