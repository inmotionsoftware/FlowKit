//
//  FlowView.swift
//  FlowKit
//
//  Created by Brian Howard 4/8/2020.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import SwiftUI
import PromiseKit

public protocol FlowView: SwiftUI.View, Flow, Resolvable, Cancelable, Backable, Promisable, Reusable where Result == Output {
    var proxy: ProxyFlow<Output> { get }
}

public extension FlowView {
    var isPending: Bool { return self.proxy.isPending }
    var isResolved: Bool { return self.proxy.isResolved }
    var isRejected: Bool { return self.proxy.isRejected }
    func resolve(_ result: Output) { self.proxy.resolve(result) }
    func reject(_ error: Error) { self.proxy.reject(error) }

    func prepareForReuse() {}
    func attach(context: Input) {}

    func startFlow(context: Input) -> Promise<Output> {
        prepareForReuse()
        return proxy.startFlow {
            attach(context: context)
        }
    }
}

