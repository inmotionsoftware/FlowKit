//
//  AppDelegate.swift
//  ExampleFlow
//
//  Created by Brian Howard on 4/7/20.
//  Copyright © 2020 InMotion Software. All rights reserved.
//

import UIKit
import PromiseKit

public enum CanceledType {
    case up
    case back
}

public enum FlowError: Error {
    case canceled(type: CanceledType)
    case failed(error: Error)
}

public typealias FlowResult<T> = Swift.Result<T, FlowError>

public extension FlowResult {
    static func cancel() -> FlowResult<Success> { return FlowResult.failure(FlowError.canceled(type: .up)) }
    static func back() -> FlowResult<Success> { return FlowResult.failure(FlowError.canceled(type: .back)) }
}

public struct Credentials {
    public let username: String
    public let password: String
}

public struct OAuthToken {
    public let token: String
}

public struct User {
    public let credentials: Credentials
}

public protocol Flow {
    associatedtype Input
    associatedtype Output
    func startFlow(context: Input) -> Promise<Output>
}

class Foo: LoginFlowStateMachine {
    func onBegin(state: LoginFlowState, context: String) -> Promise<LoginFlowState.Begin> {
        return Promise.value(.prompt(()))
    }

    func onPrompt(state: LoginFlowState, context: Void) -> Promise<LoginFlowState.Prompt> {
        return Promise.value(.authenticate(Credentials(username: "", password: "")))
    }

    func onAuthenticate(state: LoginFlowState, context: Credentials) -> Promise<LoginFlowState.Authenticate> {
        return Promise.value(.end(OAuthToken(token: "")))
    }

    func onForgotPass(state: LoginFlowState, context: String) -> Promise<LoginFlowState.ForgotPass> {
        return Promise.value(.prompt(()))
    }

    func onEnterAccountInfo(state: LoginFlowState, context: Void) -> Promise<LoginFlowState.EnterAccountInfo> {
        return Promise.value(.createAccount(User(credentials: Credentials(username: "", password: ""))))
    }

    func onCreateAccount(state: LoginFlowState, context: User) -> Promise<LoginFlowState.CreateAccount> {
        return Promise.value(.authenticate(Credentials(username: "", password: "")))
    }
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {



    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        return true
    }

    // MARK: UISceneSession Lifecycle

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        // Called when a new scene session is being created.
        // Use this method to select a configuration to create the new scene with.
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
        // Called when the user discards a scene session.
        // If any sessions were discarded while the application was not running, this will be called shortly after application:didFinishLaunchingWithOptions.
        // Use this method to release any resources that were specific to the discarded scenes, as they will not return.
    }


}

