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
