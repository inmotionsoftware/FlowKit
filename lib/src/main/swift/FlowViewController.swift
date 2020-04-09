//
//  FlowViewController.swift
//  FlowKit
//
//  Created by Khuong Huynh on 3/21/17.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import UIKit
import PromiseKit

public protocol BaseFlowViewController: Flow, Resolvable, Cancelable, Backable, Reusable where Result == Output {}


public protocol FlowViewController: Flow, Resolvable, Cancelable, Backable, Reusable, BackableView, Promisable, BackButtonDelegate where Self: UIViewController, Result == Output {

}

extension FlowViewController {
    private func shouldPop(_ navigationController: UINavigationController) -> Bool {
        guard self.isPending else { return false }
        self.back()
        navigationController
            .beginOrAmmendTransaction(animation: .back)
            .popViewController()
        return true
    }

    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        // if we handled the pop, tell the delegate not to
        if self.shouldPop(navigationController) { return false }

        // check the view controller delegate
        guard let delegate = self.backDelegate else { return true }
        return delegate.navigationController(navigationController, shouldPop: viewController)
    }
}

extension FlowViewController {
    func willMove(toParent parent: UIViewController?) {
        // Cancel our promise if the view controller is being popped from the
        // view stack
        if parent == nil && self.isPending {
            self.cancel()
        }
        self.willMove(toParent: parent)
    }
}
