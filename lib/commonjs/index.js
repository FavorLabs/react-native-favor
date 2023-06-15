"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.removeVpnStateListener = exports.addVpnStateListener = void 0;
exports.start = start;
exports.startVPN = startVPN;
exports.stop = stop;
exports.stopVPN = stopVPN;
exports.version = version;
var _reactNative = require("react-native");
const LINKING_ERROR = `The package 'react-native-favor' doesn't seem to be linked. Make sure: \n\n` + _reactNative.Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';
const Favor = _reactNative.NativeModules.Favor ? _reactNative.NativeModules.Favor : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
const localEventEmitter = new _reactNative.NativeEventEmitter(Favor);
let stateListener = null;
const addVpnStateListener = callback => {
  stateListener = localEventEmitter.addListener('stateChanged', e => callback(e));
};
exports.addVpnStateListener = addVpnStateListener;
const removeVpnStateListener = () => {
  if (!stateListener) {
    return;
  }
  stateListener.remove();
  stateListener = null;
};
exports.removeVpnStateListener = removeVpnStateListener;
function version() {
  return Favor.version();
}
function start(o) {
  const param = {
    ...defaultOptions,
    ...o
  };
  console.log(param);
  return Favor.start(param);
}
function stop() {
  return Favor.stop();
}
function startVPN(cfg) {
  return Favor.startVPN(cfg);
}
function stopVPN() {
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
  'welcome-message': 'react-native node(' + _reactNative.Platform.OS + ')',
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