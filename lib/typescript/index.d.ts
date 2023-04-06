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
    'traffic'?: false;
    'traffic-contract-addr'?: string;
    'verbosity'?: string;
    'enable-tls'?: boolean;
    'password'?: string;
}
export declare function version(): Promise<string>;
export declare function start(o: Options): Promise<Error>;
export declare function stop(): Promise<Error>;
//# sourceMappingURL=index.d.ts.map