//
//  Promise+Extensions.swift
//  FlowKit
//
//  Created by Brian Howard on 6/20/17.
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

import Foundation
import PromiseKit


public extension Promise where T: FlowResultBase {
    typealias Result = T

    func back(on q: DispatchQueue = .main, execute body: @escaping () throws -> Void) -> Promise<T> {
        return self.then(on: q) { result -> Promise<T> in
            if result.isBack { try body() }
            return Promise.value(result)
        }
    }

    func cancel(on q: DispatchQueue = .main, execute body: @escaping () throws -> Void) -> Promise<T> {
        return self.then(on: q) { result -> Promise<T> in
            if result.isCancel { try body() }
            return Promise.value(result)
        }
    }

    func complete(on q: DispatchQueue = .main, execute body: @escaping (T.Result) throws -> Void) -> Promise<T> {
        return self.then(on: q) { result -> Promise<T> in
            let rt = result.isComplete
            if rt.0 { try body(rt.1!) }
            return Promise.value(result)
        }
    }
}
