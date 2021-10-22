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
