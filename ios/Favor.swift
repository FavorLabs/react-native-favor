import Foundation
import FavorX
import Starscream
import NetworkExtension

@objc(Favor)
class Favor: RCTEventEmitter {

    var hasListener: Bool = false
    var node: MobileNode!
    var vpnGroup: String = ""
    var vpnNodes: NSArray = []
    var vpnBlacklist: NSArray? = []
    var vpnWhitelist: NSArray? = []
    var vpn: NETunnelProviderManager!

    override static func requiresMainQueueSetup() -> Bool {
      return false
    }

    override func supportedEvents() -> [String]! {
        return ["stateChanged"]
    }

    override func startObserving() {
        hasListener = true
    }

    override func stopObserving() {
        hasListener = false
    }

    func sendState(data: Any!) {
        if hasListener {
            sendEvent(withName: "stateChanged", body: data)
        }
    }

    @objc(version:rejecter:)
    func version(resolve:@escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) -> Void {
        resolve(FavorX.MobileVersion())
    }

    @objc(start:resolver:rejecter:)
    func start(cfg: NSDictionary, resolve:@escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) -> Void {

        let options = MobileOptions()

        // api setting
        options.apiPort = cfg.object(forKey: "api-port") as! Int
        options.debugAPIPort = cfg.object(forKey: "debug-api-port") as! Int
        options.websocketPort = cfg.object(forKey: "ws-port") as! Int
        options.enableDebugAPI = cfg.object(forKey: "debug-api-enable") as! Bool

        // proxy setting
        options.proxyEnable = cfg.object(forKey: "proxy-enable") as! Bool
        options.proxyGroupName = cfg.object(forKey: "proxy-group") as! String
        options.proxyPort = cfg.object(forKey: "proxy-port") as! Int

        // vpn setting
        options.vpnEnable = cfg.object(forKey: "vpn-enable") as! Bool
        options.vpnPort = cfg.object(forKey: "vpn-port") as! Int

        do {
            // group setting
            let groups = cfg.object(forKey: "groups") as Any
            let data: Data = try JSONSerialization.data(withJSONObject: groups)
            options.group = String(data: data, encoding: String.Encoding.utf8)!
        }catch {
            reject("error", error.localizedDescription, nil)
            return
        }

        // p2p setup
        options.networkID = cfg.object(forKey: "network-id") as! Int64
        options.p2PPort = cfg.object(forKey: "p2p-port") as! Int
        options.welcomeMessage = cfg.object(forKey: "welcome-message") as! String

        // kademlia
        options.binMaxPeers = cfg.object(forKey: "bin-max-peers") as! Int
        options.lightMaxPeers = cfg.object(forKey: "light-max-peers") as! Int

        // cache size
        options.cacheCapacity = cfg.object(forKey: "cache-capacity") as! Int64

        // node bootstrap
        options.bootNodes = cfg.object(forKey: "boot-nodes") as! String
        options.enableDevNode = cfg.object(forKey: "dev-mode") as! Bool
        options.enableFullNode = cfg.object(forKey: "full-node") as! Bool

        // chain setting
        options.chainEndpoint = cfg.object(forKey: "chain-endpoint") as! String
        options.oracleContract = cfg.object(forKey: "oracle-contract-addr") as! String

        // traffic stat
        options.enableFlowStat = cfg.object(forKey: "traffic") as! Bool
        options.flowContract = cfg.object(forKey: "traffic-contract-addr") as! String

        // security
        options.password = cfg.object(forKey: "password") as! String
        let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
        options.dataPath = paths[0]

        // misc
        options.verbosity = cfg.object(forKey: "verbosity") as! String
        options.enableTLS = cfg.object(forKey: "enable-tls") as! Bool

        var error: NSError?
        node = FavorX.MobileNewNode(options, &error)
        if error != nil {
            reject("error", error?.localizedDescription, nil)
            return
        }
        resolve(nil)
    }

    @objc(stop:rejecter:)
    func stop(resolve:@escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) -> Void {
        if node != nil {
            do {
                try node!.stop()
                node = nil
            } catch {
                reject("error", error.localizedDescription, nil)
                return
            }
        }
        resolve(nil)
    }

    @objc(startVPN:resolver:rejecter:)
    func startVPN(cfg: NSDictionary, resolve:@escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) -> Void {
        vpnGroup = cfg.object(forKey: "group") as! String
        vpnNodes = cfg.object(forKey: "nodes") as! NSArray
        vpnBlacklist = cfg.object(forKey: "blacklist") as? NSArray
        vpnWhitelist = cfg.object(forKey: "whitelist") as? NSArray

        if addObserveGroup() && health() {
            if vpn != nil {
                reject("500", "VPN Already started", nil)
                return
            }
            createVPN(resolve: resolve, reject: reject)
        }else{
            reject("500", "health check failed", nil)
        }
    }

    @objc(stopVPN:rejecter:)
    func stopVPN(resolve:@escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) -> Void {
        vpn.connection.stopVPNTunnel()
        vpn = nil
        resolve(nil)
    }

    fileprivate func createVPN(resolve:@escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) -> Void {
        // prepare
        NotificationCenter.default.addObserver(forName: NSNotification.Name.NEVPNStatusDidChange, object : nil , queue: nil) {
            notification in let nevpnconn = notification.object as! NEVPNConnection
            self.sendState(data: [ "state" : nevpnconn.status])
        }
        let semaphore = DispatchSemaphore(value: 0)
        NETunnelProviderManager.loadAllFromPreferences { [unowned self] managers, error in
            managers?.forEach({ m in
                if m.localizedDescription == "FavorDAO" {
                    vpn = m
                }
            })
            semaphore.signal()
        }
        _ = semaphore.wait(timeout: DispatchTime.distantFuture)
        if vpn == nil {
            let manager = NETunnelProviderManager()
            let p = NETunnelProviderProtocol()
            p.disconnectOnSleep = false
//            p.includeAllNetworks = true
            p.serverAddress = "hshh"
            p.providerBundleIdentifier = "org.reactjs.native.example.FavorExample.Vpn"
            manager.protocolConfiguration = p
            manager.localizedDescription = "FavorDAO"
            manager.isEnabled = true

            var rules = [NEOnDemandRule]()
            let rule = NEOnDemandRuleConnect()
            rule.interfaceTypeMatch = .any
            rules.append(rule)
            manager.onDemandRules = rules

            manager.saveToPreferences { [weak self] err in
                if err != nil {
                    reject("500", err?.localizedDescription, nil)
                }else {
                    self!.vpn = manager
                }
                semaphore.signal()
            }
            _ = semaphore.wait(timeout: DispatchTime.distantFuture)
        }

        if vpn != nil {
            do {
                try vpn.connection.startVPNTunnel()
            }catch{
                reject("500", "failed", nil)
                return
            }
            resolve(nil)
        }
    }
}

extension Favor {
    func addObserveGroup() ->Bool {
        let nodes = vpnNodes.componentsJoined(by: ",")
        let res = httpGet(uri: "/observe/add/group?nodes="+nodes)
        if res == "OK" {
            return true
        }
        return false
    }

    func health() ->Bool {
        let res = httpGet(uri: "/test")
        if res == "OK" {
            return true
        }
        return false
    }

    func httpGet(uri: String) ->String {
        let url = URL(string: "http://127.0.0.1:2638"+uri)!
        var request = URLRequest(url: url)
        request.timeoutInterval = 10
        request.httpMethod = "GET"
        request.addValue(vpnGroup, forHTTPHeaderField:"group")

        var response = ""

        let session = URLSession(configuration: .ephemeral)

        let semaphore = DispatchSemaphore(value: 0)
        let task = session.dataTask(with: request, completionHandler: {(data, resp, err)->Void in
            if err == nil {
                response = String(data: data!, encoding: String.Encoding.utf8)!
            }
            semaphore.signal()
        })
        task.resume()
        _ = semaphore.wait(timeout: DispatchTime.distantFuture)
        return response
    }
}

class Vpn: NEPacketTunnelProvider {
    var favor: Favor!
    var socket: WebSocket!
    var running: Bool = false

    public init(fx: Favor) {
        favor = fx
    }

    override func startTunnel(options: [String: NSObject]?, completionHandler: @escaping (Error?) -> Void) {
        wsConnect()
    }

    override func stopTunnel(with reason: NEProviderStopReason, completionHandler: @escaping () -> Void) {
        socket.disconnect()
        completionHandler()
    }

    func wsConnect() ->Void {
        let url = URL(string: "ws://127.0.0.1:2638/ws")!
        var request = URLRequest(url: url)
        request.timeoutInterval = 10
        request.httpMethod = "GET"
        request.addValue(self.favor.vpnGroup, forHTTPHeaderField:"group")

        socket = WebSocket(request: request)
        socket.delegate = self
        socket.connect()
    }

    func onStop() {
        running = false
        socket = nil
        cancelTunnelWithError(nil)
        self.favor.sendState(data: "stop ok \(running)")
    }

    func onStart() {
        packetFlow.readPackets { packets, length in
            packets.forEach { data in
                self.favor.sendState(data: "read from tun len:\(length)")
                self.socket.write(data: data)
            }
        }
        running = true
        self.favor.sendState(data: "tun start ok")
    }

    func createTun() {
        let settings = NEPacketTunnelNetworkSettings()
        let ipv4 = NEIPv4Settings(addresses: ["172.16.0.100"], subnetMasks: ["255.255.255.0"])
        ipv4.includedRoutes = [NEIPv4Route.default()]
        settings.ipv4Settings = ipv4
        let dnsSettings = NEDNSSettings(servers: ["8.8.8.8"])
        // overrides system DNS settings
        dnsSettings.matchDomains = [""]
        settings.dnsSettings = dnsSettings
        settings.mtu = 1500

        setTunnelNetworkSettings(settings) { [self] error in
            if error != nil {
                self.favor.sendState(data: error)
            }else{
                self.favor.sendState(data: "tun settings ok")
                onStart()
            }
        }
    }
}

extension Vpn: WebSocketDelegate {
    func didReceive(event: WebSocketEvent, client: WebSocket) {
        switch event {
        case .connected(let headers):
            createTun()
            self.favor.sendState(data: "connected: \(headers) \(running)")
        case .disconnected(_, _):
            onStop()
        case .text(_):
            break
        case .binary(let data):
            self.favor.sendState(data: "read from ws len:\(data.count)")
            packetFlow.writePackets([data], withProtocols: [AF_INET as NSNumber])
        case .pong(_):
            break
        case .ping(_):
            break
        case .error(_):
            onStop()
        case .viabilityChanged(_):
            break
        case .reconnectSuggested(_):
            break
        case .cancelled:
            onStop()
        }
    }
}
