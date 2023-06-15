export interface VPNState {
    up: string;
    down: string;
    total: string;
    running: boolean;
}
export declare const addVpnStateListener: (callback: (e: VPNState) => void) => void;
export declare const removeVpnStateListener: () => void;
export interface Group {
    'name': string;
    'type': number;
    'keep-connected-peers': number;
    'nodes': Array<String>;
}
export interface Options {
    'api-port'?: number;
    'debug-api-port'?: number;
    'ws-port'?: number;
    'debug-api-enable'?: boolean;
    'proxy-enable'?: boolean;
    'proxy-group'?: string;
    'proxy-port'?: number;
    'vpn-enable'?: boolean;
    'vpn-port'?: number;
    'groups'?: Array<Group>;
    'network-id': number;
    'p2p-port'?: number;
    'welcome-message'?: string;
    'bin-max-peers'?: number;
    'light-max-peers'?: number;
    'cache-capacity'?: number;
    'boot-nodes': string;
    'dev-mode'?: boolean;
    'full-node'?: boolean;
    'chain-endpoint': string;
    'oracle-contract-addr': string;
    'traffic'?: boolean;
    'traffic-contract-addr'?: string;
    'verbosity'?: string;
    'enable-tls'?: boolean;
    'password'?: string;
}
export interface VpnConfig {
    group: string;
    nodes: Array<String>;
    whitelist?: Array<String>;
    blacklist?: Array<String>;
}
export declare function version(): Promise<string>;
export declare function start(o: Options): Promise<Error>;
export declare function stop(): Promise<Error>;
export declare function startVPN(cfg: VpnConfig): Promise<Error>;
export declare function stopVPN(): Promise<Error>;
//# sourceMappingURL=index.d.ts.map