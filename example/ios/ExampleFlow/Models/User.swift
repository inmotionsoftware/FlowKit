//
//  User.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import Foundation

public struct User {
    public let userId: UUID = UUID()
    public let firstName: String
    public let lastName: String
    public let email: String
    public let password: String
}
