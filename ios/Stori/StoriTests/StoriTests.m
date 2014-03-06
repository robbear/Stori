//
//  StoriTests.m
//  StoriTests
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <XCTest/XCTest.h>
#import "SlideShareJSON.h"
#import "STOPreferences.h"
#import "STOUtilities.h"

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
    
    NSString *title = [_ssj getTitle];
    if (![title isEqualToString:@"My Stori"]) {
        XCTFail(@"Failed to get the correct title from the SlideShareJSON object");
    }
    
    NSString *description = [_ssj getDescription];
    if (![description isEqualToString:@"My description"]) {
        XCTFail(@"Failed to get the correct description from the SlideShareJSON object");
    }
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
    
    int count = [_ssj getSlideCount];
    if (count != 5) {
        XCTFail(@"Slide count should be 5. Instead, it is %d", count);
    }
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
    
    if (!imageUrl || !audioUrl || !imageFilename || !audioFilename) {
        XCTFail("Failed to retrieve slide image and/or audio strings");
    }
}

- (void)testGetOrderIndexForSlide {
    HFLogDebug(@"Calling sj.getOrderIndexForSlide on second slide");
    
    [self testCreateMultipleSlides];
    
    int index = [_ssj getOrderIndexForSlide:_testSlideUuid];
    HFLogDebug(@"Order index: %d", index);
    
    if (index != 1) {
        XCTFail("Failed to retrieve index as 1. Instead, we got index of %d", index);
    }
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

    if (!imageUrl || !audioUrl || !imageFilename || !audioFilename) {
        XCTFail("Failed to retrieve slide image and/or audio strings");
    }
}

- (void)testUpserSlide {
    [self testCreateMultipleSlides];
    
    NSString *urlString = @"http://hyperfine.com/foo.jpg";
    [_ssj upsertSlideWithSlideId:_testSlideUuid atIndex:1 withImageUrl:urlString withAudioUrl:nil withSlideText:nil forceNulls:FALSE];
    SlideJSON *sj = [_ssj getSlideBySlideId:_testSlideUuid];
    
    NSString *imageUrl = [sj getImageUrlString];
    if (![imageUrl isEqualToString:urlString]) {
        XCTFail("Failed to retrieve modified slide");
    }
    
    NSString *audioUrl = [sj getAudioUrlString];
    if (!audioUrl) {
        XCTFail("Failed to properly upsert slide. The audio url was overwritten to null");
    }
    
    NSString *slideText = [sj getText];
    if (!(slideText == (id)[NSNull null] || slideText.length == 0)) {
        XCTFail("Failed to retrieve nil slideText");
    }

    [_ssj upsertSlideWithSlideId:_testSlideUuid atIndex:1 withImageUrl:nil withAudioUrl:audioUrl withSlideText:nil forceNulls:TRUE];
    sj = [_ssj getSlideBySlideId:_testSlideUuid];
    imageUrl = [sj getImageUrlString];
    audioUrl = [sj getAudioUrlString];
    slideText = [sj getText];
    
    if (!(imageUrl == (id)[NSNull null] || imageUrl.length == 0)) {
        XCTFail("Failed to properly overwrite imageUrl with nil in upsert with forceNulls true");
    }
    if (audioUrl == (id)[NSNull null] || audioUrl.length == 0) {
        XCTFail("Failed to properly upsert the slide, with audio url being overwritten");
    }
    if (!(slideText == (id)[NSNull null] || slideText.length == 0)) {
        XCTFail("Failed to retrieve nil slideText");
    }
}

- (void)testReorder {
    [self testCreateMultipleSlides];
    
    [_ssj reorderWithCurrentPosition:1 atNewPosition:0];
    NSString *slideUuid = [_ssj getSlideUuidByOrderIndex:0];
    if (![slideUuid isEqualToString:_testSlideUuid]) {
        XCTFail("Failed to move slide index 1 to index 0");
    }
    
    [_ssj reorderWithCurrentPosition:0 atNewPosition:2];
    slideUuid = [_ssj getSlideUuidByOrderIndex:2];
    if (![slideUuid isEqualToString:_testSlideUuid]) {
        XCTFail("Failed to move slide index 0 to index 2");
    }
    
    [_ssj reorderWithCurrentPosition:2 atNewPosition:4];
    slideUuid = [_ssj getSlideUuidByOrderIndex:4];
    if (![slideUuid isEqualToString:_testSlideUuid]) {
        XCTFail("Failed to move slide index 0 to index 2");
    }
}

- (void)testRemoveSlide {
    [self testCreateMultipleSlides];
    
    [_ssj removeSlideBySlideId:_testSlideUuid];
    int count = [_ssj getSlideCount];
    if (count != 4) {
        XCTFail("Failed to remove slide");
    }
}

- (void)testSTOPreferencesInitialization {
    [STOPreferences initializeDefaults];
    
    NSString *editPlayName = [STOPreferences getEditPlayName];
    HFLogDebug(@"testSTOPreferencesInitialization: editPlayName=%@", editPlayName);
    if (editPlayName) {
        XCTFail("EditPlayName preference should be null");
    }
    
    BOOL autoPlay = [STOPreferences getPlaySlidesAutoAudio];
    HFLogDebug(@"testSTOPreferencesInitialization: playSlidesAutoAudio=%d", autoPlay);
    if (autoPlay) {
        XCTFail("PlaySlidesAutoAudio should default to FALSE");
    }
    
    [STOPreferences savePlaySlidesAutoAudio:YES];
    autoPlay = [STOPreferences getPlaySlidesAutoAudio];
    HFLogDebug(@"testSTOPreferencesInitialization: playSlidesAutoAudio=%d", autoPlay);
    if (!autoPlay) {
        XCTFail("PlaySlidesAutoAudio should be set to TRUE");
    }
}

- (void)testRootFilesDir {
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    HFLogDebug(@"testRootFilesDir: rootDir=%@", [rootDir path]);
    if (!rootDir) {
        XCTFail("Root directory returns nil");
    }
}

- (void)testAbsolutePath {
    NSURL *absPath = [STOUtilities getAbsoluteFilePathWithFolder:@"MyDirectory" withFileName:@"MyFileName"];
    HFLogDebug(@"testAbsolutePath: absPath=%@", [absPath path]);
}

- (void)testCreateOrGetSlideShareDirectory {
    NSURL *slideShareDirectory = [STOUtilities createOrGetSlideShareDirectory:@"myslidesharename"];

    NSFileManager *fm = [NSFileManager defaultManager];
    BOOL isDirectory;
    if (!([fm fileExistsAtPath:[slideShareDirectory path] isDirectory:&isDirectory] && isDirectory)) {
        XCTFail(@"Failed to create SlideShareDirectory");
    }
}

@end
