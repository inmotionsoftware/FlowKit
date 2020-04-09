//
//  FlowView.swift
//  FlowKit
//
//  Created by Brian Howard 4/8/2020.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import SwiftUI
import PromiseKit

public protocol FlowProxy: Resolvable, Cancelable, Backable {
    associatedtype Output
    var proxy: ProxyFlow<Output> { get }
}

public extension FlowProxy {
    var isPending: Bool { return self.proxy.isPending }
    var isResolved: Bool { return self.proxy.isResolved }
    var isRejected: Bool { return self.proxy.isRejected }
    func resolve(_ result: Output) { self.proxy.resolve(result) }
    func reject(_ error: Error) { self.proxy.reject(error) }
}

public protocol FlowView: SwiftUI.View, Flow, FlowProxy, Reusable where Result == Output {
    var proxy: ProxyFlow<Output> { get }
    func attach(context: Input)
}

public extension FlowView {
    func prepareForReuse() {}
    func startFlow(context: Input) -> Promise<Output> {
        prepareForReuse()
        return proxy.startFlow {
            attach(context: context)
        }
    }
}

