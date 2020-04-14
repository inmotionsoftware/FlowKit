//
//  HomeView.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/8/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import SwiftUI
import FlowKit
import PromiseKit

enum HomeResult {
    case login
}

struct HomeView: FlowableView {
    typealias Output = HomeResult
    typealias Input = Void

    public let resolver: Resolver<Output>

    init(context: Void, resolver: Resolver<HomeResult>) {
        self.resolver = resolver
    }

    func login() {
        self.resolve(.login)
    }

    var body: some View {
        return VStack {
            Text("Welcome")
            Button("Login", action: login)
        }
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        let pending = Promise<HomeResult>.pending()
        return HomeView(resolver: pending.resolver)
    }
}
