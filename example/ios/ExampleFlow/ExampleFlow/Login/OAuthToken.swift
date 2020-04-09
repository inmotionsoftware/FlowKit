//
//  OAuthToken.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import Foundation

public struct OAuthToken {
    public let token: String
    public let type: String
    public let expiration: Date
}
