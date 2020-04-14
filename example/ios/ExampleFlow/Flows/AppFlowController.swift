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
