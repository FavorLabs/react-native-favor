#import "Favor.h"
#import <Foundation/Foundation.h>
#import <React/RCTConvert.h>
#import <React/RCTLog.h>
#import "FavorX.h"

static MobileNode *node = nil;

@implementation Favor
RCT_EXPORT_MODULE()

RCT_REMAP_METHOD(version, resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  resolve(MobileVersion());
};

RCT_REMAP_METHOD(start, args:(NSDictionary *)params resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  MobileOptions *option = [MobileOptions new];

  // api setting
  [option setApiPort:(long)[RCTConvert int:params[@"api-port"]]];
  [option setDebugAPIPort:(long)[RCTConvert int:params[@"debug-api-port"]]];
  [option setWebsocketPort:(long)[RCTConvert int:params[@"ws-port"]]];
  [option setEnableDebugAPI:(BOOL)[RCTConvert BOOL:params[@"debug-api-enable"]]];

  // proxy setting
  [option setProxyEnable:(BOOL)[RCTConvert BOOL:params[@"proxy-enable"]]];
  [option setProxyGroupName:(NSString * _Nonnull)[RCTConvert NSString:params[@"proxy-group"]]];
  [option setProxyPort:(long)[RCTConvert int:params[@"proxy-port"]]];

  // group setting
  NSArray *g = [params objectForKey:@"groups"];
  NSString *groups =  RCTJSONStringify(g, NULL);
  [option setGroup:(NSString * _Nonnull)groups];

  // p2p setup
  [option setNetworkID:(int64_t)[RCTConvert int64_t:params[@"network-id"]]];
  [option setP2PPort:(long)[RCTConvert int:params[@"p2p-port"]]];
  [option setWelcomeMessage:(NSString * _Nonnull)[RCTConvert NSString:params[@"welcome-message"]]];

  // kademlia
  [option setBinMaxPeers:(long)[RCTConvert int64_t:params[@"bin-max-peers"]]];
  [option setLightMaxPeers:(long)[RCTConvert int64_t:params[@"light-max-peers"]]];

  // cache size
  [option setCacheCapacity:(int64_t)[RCTConvert int64_t:params[@"cache-capacity"]]];

  // node bootstrap
  [option setBootNodes:(NSString * _Nonnull)[RCTConvert NSString:params[@"boot-nodes"]]];
  [option setEnableDevNode:(BOOL)[RCTConvert BOOL:params[@"dev-mode"]]];
  [option setEnableFullNode:(BOOL)[RCTConvert BOOL:params[@"full-node"]]];

  // chain setting
  [option setChainEndpoint:(NSString * _Nonnull)[RCTConvert NSString:params[@"chain-endpoint"]]];
  [option setOracleContract:(NSString * _Nonnull)[RCTConvert NSString:params[@"oracle-contract-addr"]]];

  // traffic stat
  [option setEnableFlowStat:(BOOL)[RCTConvert BOOL:params[@"traffic"]]];
  [option setFlowContract:(NSString * _Nonnull)[RCTConvert NSString:params[@"traffic-contract-addr"]]];

  // security
  [option setPassword:(NSString * _Nonnull)[RCTConvert NSString:params[@"password"]]];
  NSString *documentPath = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES).firstObject;
  NSString *dataPath = [documentPath stringByAppendingString:documentPath];
  [option setDataPath:(NSString * _Nonnull)dataPath];

  // misc
  [option setVerbosity:(NSString * _Nonnull)[RCTConvert NSString:params[@"verbosity"]]];
  [option setEnableTLS:(BOOL)[RCTConvert BOOL:params[@"enable-tls"]]];

  NSError* error = nil;
  node = MobileNewNode(option, &error);
  if (error) {
    reject(nil, error.description, nil);
  }else{
    resolve(NULL);
  }
};

RCT_REMAP_METHOD(stop, success:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  if (node != nil) {
    NSError *error = nil;
    [node stop:(&error)];
    if (error) {
      reject(nil, error.description, nil);
      return;
    }
    node = nil;
  }
  resolve(NULL);
};

// Don't compile this code when we build for the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeFavorSpecJSI>(params);
}
#endif

@end
