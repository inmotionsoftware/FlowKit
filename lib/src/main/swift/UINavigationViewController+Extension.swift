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
//  UINavigationViewController+Extension.swift
//  FlowKit
//
//  Created by Khuong Huynh on 1/5/17.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
#if os(iOS)

import UIKit
import SwiftUI

extension UINavigationController {
    var rootViewController : UIViewController? {
        let ctrls = self.viewControllers
        guard ctrls.count > 0 else { return nil }
        return ctrls[0]
    }

    func popToRootViewController() {
        self.popToRootViewController(animated: true)
    }

    func setRootViewController(_ viewController: UIViewController, animated: Bool = true) {
        self.setViewControllers([viewController], animated: animated)
    }
    
    @discardableResult
    func popToOrPush<T:UIViewController>(viewController: T, animated: Bool = true) -> T {
        let top = self.topViewController
        guard top != viewController else { return top as! T }

        if let found = self.viewControllers.first(where: { $0 == viewController }) {
            self.popToViewController(found, animated: animated)
            return found as! T
        } else {
            self.pushViewController(viewController, animated: animated)
        }
        return viewController
    }
}

#endif
