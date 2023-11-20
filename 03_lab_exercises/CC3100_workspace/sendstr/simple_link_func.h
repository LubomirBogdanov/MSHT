#ifndef SIMPLE_LINK_FUNC_H_
#define SIMPLE_LINK_FUNC_H_

#include "simplelink.h"
#include "sl_common.h"

#define SL_STOP_TIMEOUT        0xFF

/* Application specific status/error codes */
typedef enum{
    LAN_CONNECTION_FAILED = -0x7D0,        /* Choosing this number to avoid overlap with host-driver's error codes */
    INTERNET_CONNECTION_FAILED = LAN_CONNECTION_FAILED - 1,
    DEVICE_NOT_IN_STATION_MODE = INTERNET_CONNECTION_FAILED - 1,

    STATUS_CODE_MAX = -0xBB8
}e_AppStatusCodes;

extern _u32 g_Status;

_i32 configureSimpleLinkToDefaultState();

#endif /* SIMPLE_LINK_FUNC_H_ */
