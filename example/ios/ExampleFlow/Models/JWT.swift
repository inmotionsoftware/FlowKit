//
//  JWT.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/10/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import Foundation
import CryptoKit

extension String {
    func isValidEmail() -> Bool{
        let pattern = "[A-Za-z-0-9.-_]+@[A-Za-z0-9]+\\.[A-Za-z]{2,3}"
        let regex = try! NSRegularExpression(pattern: pattern, options: .caseInsensitive)
        return regex.numberOfMatches(in: self, options: .anchored, range: NSRange(location: 0, length: self.count)) > 0
    }
}

struct JWT {
    struct Header: Encodable, Decodable {
        let alg: String
        let typ: String
    }

    struct Payload: Encodable, Decodable {
        let sub: String
    }

    let header: Header
    let payload: Payload
    let signature: Data

    init(payload: Payload) {
        self.header = Header(alg: "HS256", typ: "JWT")
        self.payload = payload
        self.signature = Data() // TODO
    }

    func token() throws -> String {
        let encoder = JSONEncoder()
        let header = try encoder.encode(self.header)
        let payload = try encoder.encode(self.payload)

        let keyStr = "Ki5/auXfpPzwCDYwUM9jfJpanBLLm02hBIk+4yXKhI8="
        let keyData = Data(base64Encoded: keyStr)

        let body = header.base64EncodedString() + "." + payload.base64EncodedString()
        let bodyData = body.data(using: .utf8)
        let key = SymmetricKey(data: keyData!)
        let sig = HMAC<SHA256>.authenticationCode(for: bodyData!, using: key)
        let sigData = Data(sig)
        let sigStr = sigData.base64EncodedString()
        return body + "." + sigStr
    }
}
