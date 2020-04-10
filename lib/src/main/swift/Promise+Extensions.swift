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
