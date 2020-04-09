//
//  ProxyFlowController.swift
//  FlowKit
//
//  Created by Brian Howard on 4/9/20.
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//
import UIKit
import PromiseKit
//
//class ProxyFlowController<Input, Output, Proxy: UIViewController>: UIViewController, Flow, BackableView where Proxy: Flow, Proxy.Input == Input, Output == Proxy.Output {
//
//    typealias Result = Output
//
//    public weak var backDelegate: BackButtonDelegate?
//    private var proxy: Proxy! = nil
//
//
//    public override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
//        super.init(nibName: nil, bundle: nil)
//        self.proxy = Proxy.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
//    }
//
//    public required init?(coder: NSCoder) {
//        super.init(nibName: nil, bundle: nil)
//        self.proxy = Proxy.init(coder: coder);
//    }
//
//    public init() {
//        super.init(nibName: nil, bundle: nil)
//        self.proxy = Proxy.init()
//    }
//
//    public init(proxy: Proxy) {
//        super.init(nibName: nil, bundle: nil)
//        self.proxy = proxy
//    }
//
//    func startFlow(context: Input) -> Promise<Output> {
//        return self.proxy.startFlow(context: context)
//    }
//
//    open override var view: UIView! {
//        get { return self.proxy.view }
//        set { self.proxy.view = newValue }
//    }
//
//    open override func loadView() {
//        self.proxy.loadView()
//    }
//
//    @available(iOS 9.0, *)
//    open override func loadViewIfNeeded() {
//        self.proxy.loadViewIfNeeded()
//    }
//
//    @available(iOS 9.0, *)
//    open override var viewIfLoaded: UIView? {
//        return self.proxy.viewIfLoaded
//    }
//
//    open override func viewDidLoad() {
//        return self.proxy.viewDidLoad()
//    }
//
//    @available(iOS 3.0, *)
//    open override var isViewLoaded: Bool { return self.proxy.isViewLoaded }
//
//    open override var nibName: String? { return self.proxy.nibName }
//
//    open override var nibBundle: Bundle? { return self.proxy.nibBundle }
//
//    @available(iOS 5.0, *)
//    open override var storyboard: UIStoryboard? { return self.proxy.storyboard }
//
//    @available(iOS 5.0, *)
//    open override func performSegue(withIdentifier identifier: String, sender: Any?) {
//        self.proxy.performSegue(withIdentifier: identifier, sender: sender)
//    }
//
//    @available(iOS 6.0, *)
//    open override func shouldPerformSegue(withIdentifier identifier: String, sender: Any?) -> Bool {
//        return self.proxy.shouldPerformSegue(withIdentifier: identifier, sender: sender)
//    }
//
//    @available(iOS 5.0, *)
//    open override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
//        self.proxy.prepare(for: segue, sender: sender)
//    }
//
//    @available(iOS 13.0, *)
//    open override func canPerformUnwindSegueAction(_ action: Selector, from fromViewController: UIViewController, sender: Any?) -> Bool {
//        return self.proxy.canPerformUnwindSegueAction(action, from: fromViewController, sender: sender)
//    }
//
//    @available(iOS, introduced: 6.0, deprecated: 13.0)
//    open override func canPerformUnwindSegueAction(_ action: Selector, from fromViewController: UIViewController, withSender sender: Any) -> Bool {
//        return self.proxy.canPerformUnwindSegueAction(action, from: fromViewController, sender: sender)
//    }
//
//    @available(iOS 9.0, *)
//    open override func allowedChildrenForUnwinding(from source: UIStoryboardUnwindSegueSource) -> [UIViewController] {
//        return self.proxy.allowedChildrenForUnwinding(from: source)
//    }
//
//    @available(iOS 9.0, *)
//    open override func childContaining(_ source: UIStoryboardUnwindSegueSource) -> UIViewController? {
//        return self.proxy.childContaining(source)
//    }
//
//    @available(iOS, introduced: 6.0, deprecated: 9.0)
//    open override func forUnwindSegueAction(_ action: Selector, from fromViewController: UIViewController, withSender sender: Any?) -> UIViewController? {
//        self.proxy.forUnwindSegueAction(action, from: fromViewController, withSender: sender)
//    }
//
//    @available(iOS 9.0, *)
//    open override func unwind(for unwindSegue: UIStoryboardSegue, towards subsequentVC: UIViewController) {
//        self.proxy.unwind(for: unwindSegue, towards: subsequentVC)
//    }
//
//    @available(iOS, introduced: 6.0, deprecated: 9.0)
//    open override func segueForUnwinding(to toViewController: UIViewController, from fromViewController: UIViewController, identifier: String?) -> UIStoryboardSegue? {
//        self.proxy.segueForUnwinding(to: toViewController, from: fromViewController, identifier: identifier)
//    }
//
//    open override func viewWillAppear(_ animated: Bool) {
//        self.proxy.viewWillAppear(animated)
//    }
//
//    open override func viewDidAppear(_ animated: Bool) {
//        self.proxy.viewDidAppear(animated)
//    }
//
//    open override func viewWillDisappear(_ animated: Bool) {
//        self.proxy.viewWillDisappear(animated)
//    }
//
//    open override func viewDidDisappear(_ animated: Bool) {
//        self.proxy.viewDidDisappear(animated)
//    }
//
//    @available(iOS 5.0, *)
//    open override func viewWillLayoutSubviews() {
//        self.proxy.viewWillLayoutSubviews()
//    }
//
//    @available(iOS 5.0, *)
//    open override func viewDidLayoutSubviews() {
//        self.proxy.viewDidLayoutSubviews()
//    }
//
//    open override var title: String? {
//        get { return self.proxy.title }
//        set { self.proxy.title = newValue }
//    }
//
//    open override func didReceiveMemoryWarning() {
//        self.proxy.didReceiveMemoryWarning()
//    }
//
//    weak override open var parent: UIViewController? { return self.proxy.parent }
//
//    @available(iOS 5.0, *)
//    open override var presentedViewController: UIViewController? { return self.proxy.presentedViewController }
//
//    @available(iOS 5.0, *)
//    open override var presentingViewController: UIViewController? { return self.proxy.presentingViewController }
//
//    @available(iOS 5.0, *)
//    open override var definesPresentationContext: Bool{
//        get { return self.proxy.definesPresentationContext }
//        set { self.proxy.definesPresentationContext = newValue }
//    }
//
//    @available(iOS 5.0, *)
//    open override var providesPresentationContextTransitionStyle: Bool{
//        get { return self.proxy.providesPresentationContextTransitionStyle }
//        set { self.proxy.providesPresentationContextTransitionStyle = newValue }
//    }
//
//    @available(iOS 10.0, *)
//    open override var restoresFocusAfterTransition: Bool {
//        get { return self.proxy.restoresFocusAfterTransition }
//        set { self.proxy.restoresFocusAfterTransition = newValue }
//    }
//
//    @available(iOS 5.0, *)
//    open override var isBeingPresented: Bool { return self.proxy.isBeingPresented }
//
//    @available(iOS 5.0, *)
//    open override var isBeingDismissed: Bool { return self.proxy.isBeingDismissed }
//
//    @available(iOS 5.0, *)
//    open override var isMovingToParent: Bool { return self.proxy.isMovingToParent }
//
//    @available(iOS 5.0, *)
//    open override var isMovingFromParent: Bool { return self.proxy.isMovingFromParent }
//
//    @available(iOS 5.0, *)
//    open override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool, completion: (() -> Void)? = nil) {
//        self.proxy.present(viewControllerToPresent, animated: flag, completion: completion)
//    }
//
//    @available(iOS 5.0, *)
//    open override func dismiss(animated flag: Bool, completion: (() -> Void)? = nil) {
//        return self.proxy.dismiss(animated: flag, completion: completion)
//    }
//
//    @available(iOS 3.0, *)
//    open override var modalTransitionStyle: UIModalTransitionStyle {
//        get { return self.proxy.modalTransitionStyle }
//        set { self.proxy.modalTransitionStyle = newValue }
//    }
//
//    @available(iOS 3.2, *)
//    open override var modalPresentationStyle: UIModalPresentationStyle {
//        get { return self.proxy.modalPresentationStyle }
//        set { self.proxy.modalPresentationStyle = newValue }
//    }
//
//    @available(iOS 7.0, *)
//    open override var modalPresentationCapturesStatusBarAppearance: Bool {
//        get { return self.proxy.modalPresentationCapturesStatusBarAppearance }
//        set { self.proxy.modalPresentationCapturesStatusBarAppearance = newValue }
//    }
//
//    @available(iOS 4.3, *)
//    open override var disablesAutomaticKeyboardDismissal: Bool { return self.proxy.disablesAutomaticKeyboardDismissal }
//
//    @available(iOS 7.0, *)
//    open override var edgesForExtendedLayout: UIRectEdge {
//        get { return self.proxy.edgesForExtendedLayout }
//        set { self.proxy.edgesForExtendedLayout = newValue }
//    }
//
//    @available(iOS 7.0, *)
//    open override var extendedLayoutIncludesOpaqueBars: Bool {
//        get { return self.proxy.extendedLayoutIncludesOpaqueBars }
//        set { self.proxy.extendedLayoutIncludesOpaqueBars = newValue }
//    }
//
//    @available(iOS, introduced: 7.0, deprecated: 11.0, message: "Use UIScrollView's contentInsetAdjustmentBehavior instead")
//    open override var automaticallyAdjustsScrollViewInsets: Bool {
//        get { return self.proxy.automaticallyAdjustsScrollViewInsets }
//        set { self.proxy.automaticallyAdjustsScrollViewInsets = newValue }
//    }
//
//    @available(iOS 7.0, *)
//    open override var preferredContentSize: CGSize {
//        get { return self.proxy.preferredContentSize }
//        set { self.proxy.preferredContentSize = newValue }
//    }
//
//    @available(iOS 7.0, *)
//    open override var preferredStatusBarStyle: UIStatusBarStyle { return self.proxy.preferredStatusBarStyle }
//
//    @available(iOS 7.0, *)
//    open override var prefersStatusBarHidden: Bool { return self.proxy.prefersStatusBarHidden }
//
//    @available(iOS 7.0, *)
//    open override var preferredStatusBarUpdateAnimation: UIStatusBarAnimation { return self.proxy.preferredStatusBarUpdateAnimation }
//
//    @available(iOS 7.0, *)
//    open override func setNeedsStatusBarAppearanceUpdate() {
//        self.proxy.setNeedsStatusBarAppearanceUpdate()
//    }
//
//    @available(iOS 8.0, *)
//    open override func targetViewController(forAction action: Selector, sender: Any?) -> UIViewController? {
//        return self.proxy.targetViewController(forAction: action, sender: sender)
//    }
//
//    @available(iOS 8.0, *)
//    open override func show(_ vc: UIViewController, sender: Any?) {
//        self.proxy.show(vc, sender: sender)
//    }
//
//    @available(iOS 8.0, *)
//    open override func showDetailViewController(_ vc: UIViewController, sender: Any?) {
//        self.proxy.showDetailViewController(vc, sender: sender)
//    }
//
//    @available(iOS 13.0, *)
//    open override var overrideUserInterfaceStyle: UIUserInterfaceStyle {
//        get { return self.proxy.overrideUserInterfaceStyle }
//        set { self.proxy.overrideUserInterfaceStyle = newValue }
//    }
//}
