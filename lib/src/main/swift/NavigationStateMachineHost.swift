//
//  FlowNavigationController.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import PromiseKit
import UIKit
import os


/*
    Simple class for managing the navigation stack for a FlowStateMachine
**/
open class NavigationStateMachineHost<SM: NavStateMachine>: Flow  {
    public typealias Input = SM.Input
    public typealias Output = SM.Output
    public typealias State = SM.State

    private let stack: [UIViewController]
    public var animated: Bool = true
    public let nav: UINavigationController

    private var host: StateMachineHost<SM>

    public init<D: StateMachineDelegate>(stateMachine: SM, nav: UINavigationController, delegate: D) where D.State == State {
        self.nav = nav
        self.stack = nav.viewControllers
        var cp = stateMachine
        cp.nav = nav
        self.host = StateMachineHost(stateMachine: cp, delegate: delegate)
    }

    public init(stateMachine: SM, nav: UINavigationController) {
        self.nav = nav
        self.stack = nav.viewControllers
        var cp = stateMachine
        cp.nav = nav
        self.host = StateMachineHost(stateMachine: cp)
    }

    public func startFlow(context: Input) -> Promise<Output> {
        return self.host
            .startFlow(context: context)
            .ensure {
                if let trans = self.nav.currentTransaction {
                    trans.setStack(self.stack)
                    trans.commit()
                }
                else {
                    self.nav.setViewControllers(self.stack, animated: self.animated)
                }
            }
    }
}

public extension NavigationStateMachineHost {
    @discardableResult
    func showAlert(title: String, message: String) -> Promise<Void> {
        return self.showAlert(title: title, message: message, actions: ["OK"]).then { _ -> Promise<Void> in return Promise() }
    }

    @discardableResult
    func showAlert(title: String? = nil, message: String, actions: [String], preferredActionIndex: Int? = nil) -> Promise<Int> {
        return Promise { seal in
            let vc = UIAlertController(title: title, message: message, preferredStyle: .alert)
            actions.enumerated().forEach { idx, title in
                let action = UIAlertAction(title: title, style: .default) { _ in seal.fulfill(idx) }
                vc.addAction(action)

                if (preferredActionIndex != nil && preferredActionIndex! == idx)
                    || idx == 0 {
                    vc.preferredAction = action
                }
            }

            self.nav.present(vc, animated: true, completion: nil)
        }
    }
}
