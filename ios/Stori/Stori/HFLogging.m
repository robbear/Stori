//
//  HFLogging.m
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

// We need all the log functions visible so we set this to DEBUG
#ifdef HF_COMPILE_TIME_LOG_LEVEL
#undef HF_COMPILE_TIME_LOG_LEVEL
#define HF_COMPILE_TIME_LOG_LEVEL ASL_LEVEL_DEBUG
#endif

#define HF_COMPILE_TIME_LOG_LEVEL ASL_LEVEL_DEBUG

#import "HFLogging.h"

static void AddStderrOnce()
{
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		asl_add_log_file(NULL, STDERR_FILENO);
	});
}

#define __HF_MAKE_LOG_FUNCTION(LEVEL, NAME) \
void NAME (NSString *format, ...) \
{ \
AddStderrOnce(); \
va_list args; \
va_start(args, format); \
NSString *message = [[NSString alloc] initWithFormat:format arguments:args]; \
asl_log(NULL, NULL, (LEVEL), "%s", [message UTF8String]); \
va_end(args); \
}

__HF_MAKE_LOG_FUNCTION(ASL_LEVEL_EMERG, HFLogEmergency)
__HF_MAKE_LOG_FUNCTION(ASL_LEVEL_ALERT, HFLogAlert)
__HF_MAKE_LOG_FUNCTION(ASL_LEVEL_CRIT, HFLogCritical)
__HF_MAKE_LOG_FUNCTION(ASL_LEVEL_ERR, HFLogError)
__HF_MAKE_LOG_FUNCTION(ASL_LEVEL_WARNING, HFLogWarning)
__HF_MAKE_LOG_FUNCTION(ASL_LEVEL_NOTICE, HFLogNotice)
__HF_MAKE_LOG_FUNCTION(ASL_LEVEL_INFO, HFLogInfo)
__HF_MAKE_LOG_FUNCTION(ASL_LEVEL_DEBUG, HFLogDebug)

#undef __HF_MAKE_LOG_FUNCTION
