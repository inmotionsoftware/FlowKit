//
//  ViewCache.swift
//  FlowKit
//
//  Created by Brian Howard on 4/14/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import UIKit

public protocol ViewCacher {
    func getCache<T: UIViewController>(type: T.Type, initializer: () -> T) -> T
}

open class ViewCache: ViewCacher {
    private var cache = [String : UIViewController]()

    public init() {}

    public func getCache<T>(type: T.Type, initializer: () -> T) -> T where T : UIViewController {
        let desc: String = type.description()
        guard let res = cache[desc] else {
            let view = initializer()
            cache[desc] = view
            return view
        }
        return (res as! T)
    }
}
