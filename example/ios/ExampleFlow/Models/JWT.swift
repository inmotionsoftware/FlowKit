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
