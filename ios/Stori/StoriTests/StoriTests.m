//
//  StoriTests.m
//  StoriTests
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <XCTest/XCTest.h>
#import "SlideShareJSON.h"

@interface StoriTests : XCTestCase
@end

@implementation StoriTests

NSString *_testSlideUuid;
SlideShareJSON *_ssj;

- (void)setUp
{
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown
{
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testCreateSlideShareJSONFromString {
    // Note: jsonString without keys having quotes will fail to create the ssj.jsonDictionary.

    NSString *jsonString = @"{\"title\":\"My Stori\", \"description\":\"My description\"}";
    HFLogDebug(@"Calling SlideShareJSON.initWithString: jsonString=%@", jsonString);
    _ssj = [[SlideShareJSON alloc] initWithString:jsonString];
    HFLogDebug(@"ssj.getJsonDictionary: %@", [_ssj toString]);
    
    //XCTFail(@"No implementation for \"%s\"", __PRETTY_FUNCTION__);
}

- (void)testCreateMultipleSlides {
    HFLogDebug(@"Calling SlideShareJSON.init");
    _ssj = [[SlideShareJSON alloc] init];
    HFLogDebug(@"ssj.getJsonDictionary: %@", [_ssj toString]);
    HFLogDebug(@"Create 5 slides");
    for (int i = 0; i < 5; i++) {
        NSString *slideUuid = [[NSUUID UUID] UUIDString];
        if (i == 1) {
            _testSlideUuid = slideUuid;
        }
        NSString *imageUrl = [NSString stringWithFormat:@"http://stori-app.com/foobar/image%d.jpg", i];
        NSString *audioUrl = [NSString stringWithFormat:@"http://stori-app.com/foobar/audio%d.m4p", i];
        [_ssj upsertSlideWithSlideId:slideUuid atIndex:i withImageUrl:imageUrl withAudioUrl:audioUrl withSlideText:nil forceNulls:TRUE];
    }
    HFLogDebug(@"ssj.getJsonDictionary: %@", [_ssj toString]);
}

- (void)testGetSlideBySlideId {
    HFLogDebug(@"Calling ssj.getSlideBySlideId for second slide with id: %@", _testSlideUuid);
    
    [self testCreateMultipleSlides];
    
    SlideJSON *sj = [_ssj getSlideBySlideId:_testSlideUuid];
    NSString *imageUrl = [sj getImageUrlString];
    NSString *audioUrl = [sj getAudioUrlString];
    NSString *imageFilename = [sj getImageFilename];
    NSString *audioFilename = [sj getAudioFilename];
    NSString *slideText = [sj getText];
    HFLogDebug(@"Values: imageUrl=%@, audioUrl=%@, imageFilename=%@, audioFilename=%@, slideText=%@", imageUrl, audioUrl, imageFilename, audioFilename, slideText);
}

- (void)testGetOrderIndexForSlide {
    HFLogDebug(@"Calling sj.getOrderIndexForSlide on second slide");
    
    [self testCreateMultipleSlides];
    
    int index = [_ssj getOrderIndexForSlide:_testSlideUuid];
    HFLogDebug(@"Order index: %d", index);
}

- (void)testGetSlideAtIndex {
    
    [self testCreateMultipleSlides];

    HFLogDebug(@"Calling ssj.getSlideAtIndex for second slide");
    SlideJSON *sj = [_ssj getSlideAtIndex:1];
    NSString *imageUrl = [sj getImageUrlString];
    NSString *audioUrl = [sj getAudioUrlString];
    NSString *imageFilename = [sj getImageFilename];
    NSString *audioFilename = [sj getAudioFilename];
    NSString *slideText = [sj getText];
    HFLogDebug(@"Values: imageUrl=%@, audioUrl=%@, imageFilename=%@, audioFilename=%@, slideText=%@", imageUrl, audioUrl, imageFilename, audioFilename, slideText);
}

@end
