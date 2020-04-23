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

public class NavigationTransaction {
    public enum Animation {
        case back
        case forward
        case none
        case `default`
    }

    private(set) public weak var navigationController : UINavigationController?
    public let animation : Animation
    private var stack : [UIViewController]

    internal init(navigationController: UINavigationController, animation: Animation) {
        if let curr = navigationController.currentTransaction {
            curr.commit() // commit any pending transactions
        }

        self.navigationController = navigationController
        self.animation = animation
        self.stack = navigationController.viewControllers

        navigationController.currentTransaction = self
    }

    deinit {
        guard let nav = self.navigationController else { return }
        guard let cur = nav.currentTransaction else { return }

        if cur === self {
            print("WARNING!!! Uncommitted navigation transaction is being deallocated, this is usually an error")
            nav.currentTransaction = nil
        }
    }

    public func push(viewController: UIViewController) {
        self.stack.append(viewController)
    }

    public func setRootViewController(_ viewController: UIViewController) {
        self.stack.removeAll()
        self.stack.append(viewController)
    }

    @discardableResult
    public func popViewController() -> UIViewController? {
        return self.stack.popLast()
    }

    @discardableResult
    public func popToRootViewController() -> UIViewController? {
        guard self.stack.count > 0 else { return nil }
        self.stack.removeLast(self.stack.count-1)
        return self.stack.first
    }

    @discardableResult
    public func popToOrPush<T:UIViewController>(viewController: T) -> T {
        guard !self.stack.isEmpty else { self.push(viewController: viewController); return viewController }

        if let found = self.stack.first(where: { $0 == viewController }) {
            self.popTo(viewController: found)
            return found as! T
        } else {
            self.push(viewController: viewController)
        }
        return viewController
    }

    public func popTo(viewController: UIViewController) {

        // find the matching controller searching in reverse order
        var found : Int? = nil
        for (idx, item) in self.stack.reversed().enumerated() {
            if item == viewController { found = idx; break }
        }
        guard let idx = found else { return }
        self.stack.removeLast(idx)
        return
    }

    public func setStack(_ stack: [UIViewController]) {
        self.stack = stack
    }

    public func commit() {
        guard let nav = self.navigationController else { return }
        guard let curr = nav.currentTransaction else { return }
        guard curr === self else { return }
        defer {
            self.stack.removeAll()
            nav.currentTransaction = nil
            self.navigationController = nil
        }

        switch(self.animation) {
            case .back:
                // force the "pop" animation by putting our top view controller 
                // into the existing stack just before animating
                if let top = self.stack.last {
                    var existing = nav.viewControllers
                    if !existing.contains(top) && existing.count > 0 {
                        // place this just below the top most one
                        existing.insert(top, at: existing.count-1)
                        nav.setViewControllers(existing, animated: false)
                    }
                }
                nav.setViewControllers(stack, animated: true)

            case .forward:
                if let top = self.stack.last {
                    var existing = nav.viewControllers
                    if let idx = existing.firstIndex(of: top), idx != existing.count-1 {
                        existing.remove(at: idx)
                        nav.setViewControllers(existing, animated: false)
                    }
                }
                nav.setViewControllers(self.stack, animated: true)

            case .none:
                nav.setViewControllers(self.stack, animated: false)
            default:
                nav.setViewControllers(self.stack, animated: true)
        }
    }
}

#endif
