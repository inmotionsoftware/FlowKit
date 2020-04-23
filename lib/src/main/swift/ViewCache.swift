//
//  ViewCache.swift
//  FlowKit
//
//  Created by Brian Howard on 4/14/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

#if os(iOS)

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

public extension ViewCacher {
    func getView<T: UIViewController>(of view: T.Type, storyboard: String) -> T {
        return getCache(type: T.self) {
            let storyboard: UIStoryboard = UIStoryboard(name: storyboard, bundle: nil)
            let id = String(describing: T.self)
            return storyboard.instantiateViewController(withIdentifier: id) as! T
        }
    }

    func getView<T: UIViewController>(of view: T.Type, nib: String) -> T {
        return getCache(type: T.self) { T(nibName: nib, bundle: Bundle.main) }
    }
}

#endif
