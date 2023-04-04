
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNFavorSpec.h"

@interface Favor : NSObject <NativeFavorSpec>
#else
#import <React/RCTBridgeModule.h>

@interface Favor : NSObject <RCTBridgeModule>
#endif

@end
