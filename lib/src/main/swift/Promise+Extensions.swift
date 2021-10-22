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
//  Promise+Extensions.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit

public extension Promise {
    func back( _ closure: @escaping () throws -> T ) -> Promise<T> {
        return canceled { type in
            switch (type) {
                case .back: return try closure()
                case .canceled: throw type
            }
        }
    }

    func cancel( _ closure: @escaping () throws -> T ) -> Promise<T> {
        return canceled { type in
            switch (type) {
                case .back: throw type
                case .canceled: return try closure()
            }
        }
    }

    func canceled( _ closure: @escaping (FlowError) throws -> T ) -> Promise<T> {
        return self.recover { err -> Promise<T> in
            guard let error = err as? FlowError else { throw err }
            return Promise.value(try closure(error))
        }
    }
}
