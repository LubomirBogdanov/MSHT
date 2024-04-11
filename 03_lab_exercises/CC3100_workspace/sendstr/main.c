#include "simplelink.h"
#include "sl_common.h"
#include "simple_link_func.h"
#include "uartstdio.h"
#include "leds.h"

#define SSID_NAME       "LAB1362"          // Access point name to connect to.
#define SEC_TYPE        SL_SEC_TYPE_WPA_WPA2    // Security type of the Access piont
#define PASSKEY         "arm-cortex"           // Password in case of secure AP
#define PASSKEY_LEN     pal_Strlen(PASSKEY)     // Password length in case of secure AP
#define TCP_PORT        3333

int main(int argc, char** argv)
{
    SlSockAddrIn_t sock_addr;
    SlSecParams_t secParams = {0};
    _u32 server_ip_addr[4] = {192, 168, 0, 100};
    _u32 server_ip_converted = 0;
    _u8 *tcp_send_msg = "Hello, World!";
    _u8 tcp_recv_msg[128];
    _i16 tcp_recv_msg_size;
    _i16 client_socket = 0;

    stopWDT();
    initClk();
    CLI_Configure();
    init_leds();
    UARTprintf("\n\r************************\n\r");
    UARTprintf("Starting sendstr client application ...\n\r");

    configureSimpleLinkToDefaultState();

    sl_Start(0, 0, 0);

    secParams.Key = (_i8 *)PASSKEY;
    secParams.KeyLen = pal_Strlen(PASSKEY);
    secParams.Type = SEC_TYPE;
    sl_WlanConnect((_i8 *)SSID_NAME, pal_Strlen(SSID_NAME), 0, &secParams, 0);

    while((!IS_CONNECTED(g_Status)) || (!IS_IP_ACQUIRED(g_Status))) { _SlNonOsMainLoopTask(); }

    set_led(LED_RED, 1);

    UARTprintf("Establishing connection with TCP server... ");

    server_ip_converted = (server_ip_addr[0]<<24) | (server_ip_addr[1]<<16) | (server_ip_addr[2]<<8) | (server_ip_addr[3]);
    sock_addr.sin_family = SL_AF_INET;
    sock_addr.sin_port = sl_Htons((_u16)TCP_PORT);
    sock_addr.sin_addr.s_addr = sl_Htonl(server_ip_converted);

    client_socket = sl_Socket(SL_AF_INET,SL_SOCK_STREAM, 0);

    sl_Connect(client_socket, ( SlSockAddr_t *)&sock_addr, sizeof(SlSockAddrIn_t));

    set_led(LED_GREEN, 1);

    UARTprintf("done!\n\r");

    //Send string in TCP/IP terminal
    ???

    while(IS_CONNECTED(g_Status)){
        tcp_recv_msg_size = sl_Recv(client_socket, tcp_recv_msg, 128, 0);

        if(tcp_recv_msg_size <= 0){
            break;
        }

        tcp_recv_msg[tcp_recv_msg_size] = '\0';

        UARTprintf("MSG from server: %s\n\r", tcp_recv_msg);

        blink_led(LED_GREEN);
    }

    sl_Close(client_socket);

    sl_WlanDisconnect();

    sl_Stop(SL_STOP_TIMEOUT);

    set_led(LED_RED, 0);
    set_led(LED_GREEN, 0);

    while(1){
        __bis_SR_register(LPM0_bits + GIE); //Enter sleep mode
    }
}



