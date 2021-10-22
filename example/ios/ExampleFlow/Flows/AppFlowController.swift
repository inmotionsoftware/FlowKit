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
//  AppFlowController.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/13/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//
import Foundation
import PromiseKit
import FlowKit
import SwiftUI

final class AppFlowController: ViewCache, NavStateMachine, AppStateMachine {
    var nav: UINavigationController!

    func onBegin(state: AppState, context: Void) -> Promise<AppState.Begin> {
        return Promise.value(.home(()))
    }

    func onHome(state: AppState, context: Void) -> Promise<AppState.Home> {
        return self.subflow(to: HomeView.self, context: context)
        .map { result in
            switch result {
            case .login: return .login(context)
            }
        }
    }

    func onLogin(state: AppState, context: Void) -> Promise<AppState.Login> {
        return self.subflow(to: LoginFlowController(), context: context)
            .map { token -> AppState.Login in .home(()) }
//            .recover { err in Promise.value(AppState.Login.home(())) }
    }
}
