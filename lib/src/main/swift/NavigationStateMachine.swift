//
//  FlowNavigationController.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit
import UIKit

/*
    Simple class for managing the navigation stack for a FlowStateMachine
**/
open class NavigationStateMachine<Output> {
    private let stack: [UIViewController]
    public let nav: UINavigationController
    public var animated: Bool = true

    public init(nav: UINavigationController) {
        self.nav = nav
        self.stack = self.nav.viewControllers
    }

    func attach(_ promise: Promise<Output>) -> Promise<Output> {
        return promise.ensure {
            self.nav.setViewControllers(self.stack, animated: self.animated)
        }
    }
}
