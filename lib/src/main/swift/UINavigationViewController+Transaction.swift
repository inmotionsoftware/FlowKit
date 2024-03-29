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
//  NavigationTransaction.swift
//  FlowKit
//
//  Created by Brian Howard on 3/29/17.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

#if os(iOS)
import Foundation
import UIKit

public extension UINavigationController {
    func beginTransaction(animated: Bool) -> NavigationTransaction {
        return self.beginTransaction(animation: animated ? .default : .none)
    }

    func beginTransaction(animation: NavigationTransaction.Animation) -> NavigationTransaction {
        return NavigationTransaction(navigationController: self, animation: animation)
    }

    func beginOrAmmendTransaction(animated: Bool) -> NavigationTransaction {
        return self.beginOrAmmendTransaction(animation: animated ? .default : .none)
    }

    func beginOrAmmendTransaction(animation: NavigationTransaction.Animation) -> NavigationTransaction {
        let curr = self.currentTransaction
        if curr != nil { return curr! }
        return beginTransaction(animation: animation)
    }
}

public extension UINavigationController {
    private static var HANDLE: UInt32 = 0

    internal(set) var currentTransaction: NavigationTransaction? {
        get {
            return objc_getAssociatedObject(self, &UINavigationController.HANDLE) as? NavigationTransaction
        }
        set {
            objc_setAssociatedObject(self, &UINavigationController.HANDLE, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
}

#endif
