//
//  User.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import Foundation

extension String {
    func isValidEmail() -> Bool{
        let pattern = "[A-Za-z-0-9.-_]+@[A-Za-z0-9]+\\.[A-Za-z]{2,3}"
        let regex = try! NSRegularExpression(pattern: pattern, options: .caseInsensitive)
        return regex.numberOfMatches(in: self, options: .anchored, range: NSRange(location: 0, length: self.count)) > 0
    }
}

public struct User {
    public let userId: UUID = UUID()
    public let firstName: String
    public let lastName: String
    public let email: String
    public let password: String
}
