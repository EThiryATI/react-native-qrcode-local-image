//
//  RCTQRCodeLocalImage.m
//  RCTQRCodeLocalImage
//
//  Created by fangyunjiang on 15/11/4.
//  Copyright (c) 2015年 remobile. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <React/RCTLog.h>
#import <React/RCTUtils.h>
#import "RCTQRCodeLocalImage.h"

@implementation RCTQRCodeLocalImage
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(decode:(NSString *)path callback:(RCTResponseSenderBlock)callback)
{
    UIImage *srcImage;
    if ([path hasPrefix:@"http://"] || [path hasPrefix:@"https://"]) {
        srcImage = [UIImage imageWithData: [NSData dataWithContentsOfURL:[NSURL URLWithString: path]]];
    } else if ([path hasPrefix:@"data:image/jpeg;base64,"]) {
        NSRange replaceRange = [path rangeOfString:@"data:image/jpeg;base64,"];
        if (replaceRange.location != NSNotFound){
            path = [path stringByReplacingCharactersInRange:replaceRange withString:@""];
        }
        NSData *data = [[NSData alloc]initWithBase64EncodedString:path options:NSDataBase64DecodingIgnoreUnknownCharacters];
        srcImage = [UIImage imageWithData:data];
    } else {
        srcImage = [[UIImage alloc] initWithContentsOfFile:path];
    }
    if (nil==srcImage){
        NSLog(@"PROBLEM! IMAGE NOT LOADED\n");
        callback(@[RCTMakeError(@"IMAGE NOT LOADED!", nil, nil)]);
        return;
    }
    NSLog(@"OK - IMAGE LOADED\n");
    NSDictionary *detectorOptions = @{@"CIDetectorAccuracy": @"CIDetectorAccuracyHigh"};
    CIDetector *detector = [CIDetector detectorOfType:CIDetectorTypeQRCode context:nil options:detectorOptions];
    CIImage *image = [CIImage imageWithCGImage:srcImage.CGImage];
    NSArray *features = [detector featuresInImage:image];
    if (0==features.count) {
        NSLog(@"PROBLEM! Feature size is zero!\n");
        callback(@[RCTMakeError(@"Feature size is zero!", nil, nil)]);
        return;
    }

    CIQRCodeFeature *feature = [features firstObject];
    NSLog(@"feature: %@", feature);

    NSMutableDictionary *result = [NSMutableDictionary dictionaryWithDictionary:@{
                                                                                  @"type" : feature.type,
                                                                                  @"data" : feature.messageString,
                                                                                  @"bottomLeft" : NSStringFromCGPoint(feature.bottomLeft),
                                                                                  @"topLeft" : NSStringFromCGPoint(feature.topLeft),
                                                                                  @"topRight" : NSStringFromCGPoint(feature.topRight),
                                                                                  @"bottomRight" : NSStringFromCGPoint(feature.bottomRight),
                                                                                  }];

    if (result) {
        callback(@[[NSNull null], result]);
    } else {
        callback(@[RCTMakeError(@"QR Parse failed!", nil, nil)]);
        return;
    }
}
@end
