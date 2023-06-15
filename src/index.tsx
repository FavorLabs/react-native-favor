import {
  NativeModules,
  Platform,
  NativeEventEmitter,
  EmitterSubscription,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-favor' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const Favor = NativeModules.Favor
  ? NativeModules.Favor
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const localEventEmitter = new NativeEventEmitter(Favor);
let stateListener: null | EmitterSubscription = null;

export interface VPNState {
  up: string;
  down: string;
  total: string;
  running: boolean;
}

export const addVpnStateListener = (callback: (e: VPNState) => void) => {
  stateListener = localEventEmitter.addListener('stateChanged', (e) =>
    callback(e)
  );
};

export const removeVpnStateListener = () => {
  if (!stateListener) {
    return;
  }
  stateListener.remove();
  stateListener = null;
};

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

export function version(): Promise<string> {
  return Favor.version();
}

export function start(o: Options): Promise<Error> {
  const param = { ...defaultOptions, ...o };
  console.log(param);
  return Favor.start(param);
}

export function stop(): Promise<Error> {
  return Favor.stop();
}

export function startVPN(cfg: VpnConfig): Promise<Error> {
  return Favor.startVPN(cfg);
}

export function stopVPN(): Promise<Error> {
  return Favor.stopVPN();
}

const defaultOptions = {
  'api-port': 2633,
  'debug-api-port': 2635,
  'ws-port': 2637,
  'debug-api-enable': false,
  'proxy-enable': false,
  'proxy-group': '',
  'proxy-port': 2639,
  'vpn-enable': false,
  'vpn-port': 2638,
  'groups': [],
  'network-id': 0,
  'p2p-port': 2634,
  'welcome-message': 'react-native node(' + Platform.OS + ')',
  'bin-max-peers': 50,
  'light-max-peers': 100,
  'cache-capacity': 4096,
  'boot-nodes': '',
  'dev-mode': false,
  'full-node': false,
  'chain-endpoint': '',
  'oracle-contract-addr': '',
  'traffic': false,
  'traffic-contract-addr': '',
  'verbosity': 'silent',
  'enable-tls': false,
  'password': '123456',
};
