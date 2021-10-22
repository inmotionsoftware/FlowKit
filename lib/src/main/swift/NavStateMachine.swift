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
//  NavStateMachine.swift
//  FlowKit+SwiftUI
//
//  Created by Brian Howard on 4/20/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

#if os(iOS)

import Foundation
import UIKit
import PromiseKit
import os

public protocol Navigable {
    var nav: UINavigationController! { get set }
}

public protocol NavStateMachine: StateMachine, Navigable {}


public extension NavStateMachine where Self: ViewCacher {
    func subflow<View: FlowViewController>(to view: View.Type, nib: String, context: View.Input, animated: Bool = true, transition: UIViewControllerTransitioningDelegate? = nil) -> Promise<View.Output> {
        let view = self.getView(of: View.self, nib: nib)
        view.transitioningDelegate = transition
        return self.subflow(to: view, context: context, animated: animated)
    }

    func subflow<View: FlowViewController>(to view: View.Type, storyboard: String, context: View.Input, animated: Bool = true, transition: UIViewControllerTransitioningDelegate? = nil) -> Promise<View.Output> {
        let view = self.getView(of: View.self, storyboard: storyboard)
        view.transitioningDelegate = transition
        return self.subflow(to: view, context: context, animated: animated)
    }
}

public extension NavStateMachine {
    func subflow<SM: NavStateMachine>(to stateMachine: SM, context: SM.Input) -> Promise<SM.Output> {
        return NavigationStateMachineHost(stateMachine: stateMachine, nav: self.nav)
            .startFlow(context: context)
    }

    func subflow<View: FlowViewController>(to view: View, context: View.Input, animated: Bool = true) -> Promise<View.Output> {
        return subflow(to: view, nav: self.nav, context: context, animated: animated)
    }

    func subflow<View: FlowViewController>(to view: View, nav: UINavigationController, context: View.Input, animated: Bool = true) -> Promise<View.Output> {
        return SubFlow(viewController: view, nav: nav, animated: animated).startFlow(context: context)
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

#endif
