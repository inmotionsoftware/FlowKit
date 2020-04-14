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

public protocol Navigable {
    var nav: UINavigationController! { get set }
}

public protocol NavStateMachine: StateMachine, Navigable {}

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
                    while trans.popViewController() != nil {}
                    self.stack.forEach {
                        trans.push(viewController: $0)
                    }
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

public extension NavStateMachine where Self: ViewCacher {
    func subflow<View: FlowableView>(to view: View.Type, context: View.Input) -> Promise<View.Output> {
        let host = getCache(type: FlowHostingController<View>.self) { FlowHostingController<View>(context: context) }
        return self.subflow(to: host, context: context)
    }

    func subflow<View: FlowViewController>(to view: View.Type, nib: String, context: View.Input) -> Promise<View.Output> {
        let view = getCache(type: View.self) { View(nibName: nib, bundle: Bundle.main) }
        return self.subflow(to: view, context: context)
    }
}

public extension NavStateMachine {
    func subflow<SM: NavStateMachine>(to stateMachine: SM, context: SM.Input) -> Promise<SM.Output> {
        return NavigationStateMachineHost(stateMachine: stateMachine, nav: self.nav)
            .startFlow(context: context)
    }

    func subflow<View: FlowableView>(to view: View, context: View.Input) -> Promise<View.Output> {
        let host = FlowHostingController<View>(context: context)
        return SubFlow(viewController: host, nav: self.nav)
            .startFlow(context: context)
    }

    func subflow<View: FlowViewController>(to view: View, context: View.Input) -> Promise<View.Output> {
        return SubFlow(viewController: view, nav: self.nav).startFlow(context: context)
    }
}

public extension Bootstrap {
    static func startFlow<SM: NavStateMachine&StateMachine>(stateMachine: SM, nav: UINavigationController, context: SM.Input) {
        let _ = NavigationStateMachineHost(stateMachine: stateMachine, nav: nav)
            .startFlow(context: context)
            .ensure {
                os_log("Root flow is being restarted", type: .error)
                startFlow(stateMachine: stateMachine, nav: nav, context: context)
            }
    }

    static func startFlow<SM: NavStateMachine&StateMachine>(stateMachine: SM, nav: UINavigationController) where SM.Input == Void {
        startFlow(stateMachine: stateMachine, nav: nav, context: ())
    }
}
