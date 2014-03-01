//
//  HFLogging.h
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

/*
 HFLogging. Simple wrapper macros/functions around ASL (Apple System
 Log)
 
 We support a compile-time log level through
 HF_COMPILE_TIME_LOG_LEVEL. This will turn the associated log calls
 into NOPs.
 
 The log levels are the constants defined in asl.h:
 
 #define ASL_LEVEL_EMERG   0
 #define ASL_LEVEL_ALERT   1
 #define ASL_LEVEL_CRIT    2
 #define ASL_LEVEL_ERR     3
 #define ASL_LEVEL_WARNING 4
 #define ASL_LEVEL_NOTICE  5
 #define ASL_LEVEL_INFO    6
 #define ASL_LEVEL_DEBUG   7
 
 For a description of when to use each level, see here:
 
 http://developer.apple.com/library/mac/#documentation/MacOSX/Conceptual/BPSystemStartup/Chapters/LoggingErrorsAndWarnings.html#//apple_ref/doc/uid/10000172i-SW8-SW1
 
 Emergency (level 0) - The highest priority, usually reserved for
 catastrophic failures and reboot notices.
 
 Alert (level 1)     - A serious failure in a key system.
 
 Critical (level 2)  - A failure in a key system.
 
 Error (level 3)     - Something has failed.
 
 Warning (level 4)   - Something is amiss and might fail if not
 corrected.
 
 Notice (level 5)    - Things of moderate interest to the user or
 administrator.
 
 Info (level 6)      - The lowest priority that you would normally log, and
 purely informational in nature.
 
 Debug (level 7)     - The lowest priority, and normally not logged except
 for messages from the kernel.
 
 
 Note that by default the iOS syslog/console will only record items up
 to level ASL_LEVEL_NOTICE.
 
 */

/** @todo
 
 We want better multithread support. Default NULL client uses
 locking. Perhaps we can check for [NSThread mainThread] and associate
 an asl client object to that thread. Then we can specify
 ASL_OPT_STDERR and not need an extra call to add stderr.
 
 */

#import <Foundation/Foundation.h>

// By default, in non-debug mode we want to disable any logging
// statements except NOTICE and above.
#ifndef HF_COMPILE_TIME_LOG_LEVEL
#ifdef NDEBUG
#define HF_COMPILE_TIME_LOG_LEVEL ASL_LEVEL_NOTICE
#else
#define HF_COMPILE_TIME_LOG_LEVEL ASL_LEVEL_DEBUG
#endif
#endif

#include <asl.h>

#if HF_COMPILE_TIME_LOG_LEVEL >= ASL_LEVEL_EMERG
void HFLogEmergency(NSString *format, ...);
#else
#define HFLogEmergency(...)
#endif

#if HF_COMPILE_TIME_LOG_LEVEL >= ASL_LEVEL_ALERT
void HFLogAlert(NSString *format, ...);
#else
#define HFLogAlert(...)
#endif

#if HF_COMPILE_TIME_LOG_LEVEL >= ASL_LEVEL_CRIT
void HFLogCritical(NSString *format, ...);
#else
#define HFLogCritical(...)
#endif

#if HF_COMPILE_TIME_LOG_LEVEL >= ASL_LEVEL_ERR
void HFLogError(NSString *format, ...);
#else
#define HFLogError(...)
#endif

#if HF_COMPILE_TIME_LOG_LEVEL >= ASL_LEVEL_WARNING
void HFLogWarning(NSString *format, ...);
#else
#define HFLogWarning(...)
#endif

#if HF_COMPILE_TIME_LOG_LEVEL >= ASL_LEVEL_NOTICE
void HFLogNotice(NSString *format, ...);
#else
#define HFLogNotice(...)
#endif

#if HF_COMPILE_TIME_LOG_LEVEL >= ASL_LEVEL_INFO
void HFLogInfo(NSString *format, ...);
#else
#define HFLogInfo(...)
#endif

#if HF_COMPILE_TIME_LOG_LEVEL >= ASL_LEVEL_DEBUG
void HFLogDebug(NSString *format, ...);
#else
#define HFLogDebug(...)
#endif
