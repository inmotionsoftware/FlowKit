//
//  NavStateMachine.swift
//  FlowKit+SwiftUI
//
//  Created by Brian Howard on 4/20/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import Foundation
import UIKit
import PromiseKit
import os

public protocol Navigable {
    var nav: UINavigationController! { get set }
}

public protocol NavStateMachine: StateMachine, Navigable {}


public extension NavStateMachine where Self: ViewCacher {
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

    func subflow<View: FlowViewController>(to view: View, context: View.Input) -> Promise<View.Output> {
        return subflow(to: view, nav: self.nav, context: context)
    }

    func subflow<View: FlowViewController>(to view: View, nav: UINavigationController, context: View.Input) -> Promise<View.Output> {
        return SubFlow(viewController: view, nav: nav).startFlow(context: context)
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
