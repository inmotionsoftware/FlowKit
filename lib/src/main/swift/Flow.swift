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
//  Flow.swift
//  FlowKit
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import Foundation
import PromiseKit

public protocol Flow {
    associatedtype Input
    associatedtype Output
    func startFlow(context: Input) -> Promise<Output>
}

public extension Flow where Input == Void {
    func startFlow() -> Promise<Output> {
        return self.startFlow(context: ())
    }
}

public enum FlowError: Error {
    case canceled
    case back
}

public protocol Resolvable {
    associatedtype Output
    var resolver: Resolver<Output> { get }
}

public extension Resolvable {
    func resolve(_ value: Output) {
        self.resolver.fulfill(value)
    }

    func reject(_ error: Error) {
        self.resolver.reject(error)
    }

    func cancel() {
        self.reject(FlowError.canceled)
    }

    func back() {
        self.reject(FlowError.back)
    }
}

public enum Bootstrap {

}
