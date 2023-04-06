import { NativeModules, Platform } from 'react-native';
const LINKING_ERROR = `The package 'react-native-favor' doesn't seem to be linked. Make sure: \n\n` + Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';
const Favor = NativeModules.Favor ? NativeModules.Favor : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
export function version() {
  return Favor.version();
}
export function start(o) {
  const param = {
    ...defaultOptions,
    ...o
  };
  console.log(param);
  return Favor.start(param);
}
export function stop() {
  return Favor.stop();
}
const defaultOptions = {
  'api-port': 2633,
  'debug-api-port': 2635,
  'ws-port': 2637,
  'debug-api-enable': false,
  'proxy-enable': false,
  'proxy-group': '',
  'proxy-port': 2639,
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
  'password': '123456'
};
//# sourceMappingURL=index.js.map