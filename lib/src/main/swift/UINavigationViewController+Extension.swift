//
//  UINavigationViewController+Extension.swift
//  FlowKit
//
//  Created by Khuong Huynh on 1/5/17.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import UIKit

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
