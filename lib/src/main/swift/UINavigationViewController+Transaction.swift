//
//  NavigationTransaction.swift
//  FlowKit
//
//  Created by Brian Howard on 3/29/17.
//  Copyright © 2017 InMotion Software, LLC. All rights reserved.
//

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
