//
//  FlowHostingController.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

import SwiftUI
import PromiseKit

public class FlowHostingController<Content: FlowView>: UIHostingController<Content>, BackableView, FlowViewController {

    public typealias Input = Content.Input
    public typealias Output = Content.Output
    public typealias Result = Content.Output
    public weak var backDelegate: BackButtonDelegate? = nil

    public var proxy: ProxyFlow<Output> { return self.rootView.proxy }

    public func startFlow(context: Input) -> Promise<Output> {
        return self.rootView.startFlow(context: context)
    }

    public func attach(context: Input) {
        self.rootView.attach(context: context)
    }

    public func prepareForReuse() {
        self.rootView.prepareForReuse()
    }
}
