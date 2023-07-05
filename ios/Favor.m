#import "Favor-Bridging-Header.h"

@interface RCT_EXTERN_MODULE(Favor, RCTEventEmitter);

RCT_EXTERN_METHOD(version:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject);

RCT_EXTERN_METHOD(start:(NSDictionary*)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject);

RCT_EXTERN_METHOD(stop:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject);

RCT_EXTERN_METHOD(startVPN:(NSDictionary*)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject);

RCT_EXTERN_METHOD(stopVPN:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject);


@end
