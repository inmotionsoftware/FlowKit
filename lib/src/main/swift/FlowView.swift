//
//  Flow.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import SwiftUI
import PromiseKit

public protocol FlowableView: SwiftUI.View, Flow, FlowResolver {
    func attach(context: Input)
}

public extension FlowableView {
    func attach(context: Input) {}
    func startFlow(context: Input) -> Promise<Output> {
        self.proxy.reset()
        attach(context: context)
        return self.proxy.wrappedValue
    }
}

public class FlowHostingController<Content: FlowableView>: UIHostingController<Content>, FlowViewController {

    public typealias Input = Content.Input
    public typealias Output = Content.Output

    public weak var delegate: ViewControllerDelegate? = nil

    public func startFlow(context: Content.Input) -> Promise<Content.Output> {
        return self.rootView.startFlow(context: context)
    }

    public func navigationController(_ navigationController: UINavigationController, shouldPop viewController: UIViewController) -> Bool {
        return self.delegate?.navigationController(navigationController, shouldPop: viewController) ?? true
    }

    override public func willMove(toParent parent: UIViewController?) {
        self.delegate?.willMove(toParent: parent)
    }
}

@propertyWrapper
public struct FlowView<Value: FlowableView> {
    public let projectedValue: FlowHostingController<Value>

    public init(wrappedValue value: Value) {
        self.projectedValue = FlowHostingController(rootView: value)
    }

    public var wrappedValue: Value {
        get { return self.projectedValue.rootView }
        set { self.projectedValue.rootView = newValue }
    }
}

public extension Flow {
    func startFlow<View: FlowableView>(view: View, nav: UINavigationController, context: View.Input) -> Promise<View.Output> {
        let host = FlowHostingController(rootView: view)
        return FlowSubController(viewController: host, nav: nav).startFlow(context: context)
    }

    func startFlow<View: FlowViewController>(view: View, nav: UINavigationController, context: View.Input) -> Promise<View.Output> {
        return FlowSubController(viewController: view, nav: nav).startFlow(context: context)
    }
}
