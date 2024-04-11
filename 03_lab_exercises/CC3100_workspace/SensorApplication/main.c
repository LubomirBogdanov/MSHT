#include "simplelink.h"
#include "sl_common.h"
#include "simple_link_func.h"
#include "uartstdio.h"
#include "leds.h"
#include "sensors.h"

#define SSID_NAME       "LAB1362"              // Access point name to connect to.
#define SEC_TYPE        SL_SEC_TYPE_WPA_WPA2   // Security type of the Access piont
#define PASSKEY         "arm-cortex"           // Password in case of secure AP
#define PASSKEY_LEN     pal_Strlen(PASSKEY)    // Password length in case of secure AP
#define MAX_MSG_SIZE    20
#define BUFF_SIZE_MAX   255
#define SERVER_PORT     1204

uint8_t data_packet[] = { '\r', 0xBE, 128, 128, 128, 70, 36, 0xEF };

int main(int argc, char** argv)
{
    SlSecParams_t secParams = {0};
    SlSockAddrIn_t server_sock_addr;
    _u16 server_sock_addr_size = sizeof(SlSockAddrIn_t);
    SlSockAddrIn_t client_sock_addr;
    _u16 client_sock_addr_size = sizeof(SlSockAddrIn_t);
    _i16 server_socket = 0;
    _i16 client_socket = -1;
    char recv_buff[BUFF_SIZE_MAX];
    _i16 bytes_sent = 0;
    volatile long test_sock = 0;
    int optval, optlen;


    stopWDT();
    initClk();
    CLI_Configure();
    init_leds();
    init_sensors();
    UARTprintf("\n\r************************\n\r");
    UARTprintf("Starting Saturn server application ...\n\r");

    configureSimpleLinkToDefaultState();

    sl_Start(0, 0, 0);

    sl_NetAppStop(SL_NET_APP_HTTP_SERVER_ID); //Stop internal SimpleLink web page on port 80

    secParams.Key = (_i8 *)PASSKEY;
    secParams.KeyLen = pal_Strlen(PASSKEY);
    secParams.Type = SEC_TYPE;
    sl_WlanConnect((_i8 *)SSID_NAME, pal_Strlen(SSID_NAME), 0, &secParams, 0);

    while((!IS_CONNECTED(g_Status)) || (!IS_IP_ACQUIRED(g_Status))) { _SlNonOsMainLoopTask(); }

    set_led(LED_RED, 1);

    server_sock_addr.sin_family = SL_AF_INET;
    server_sock_addr.sin_port = sl_Htons((_u16)SERVER_PORT);
    server_sock_addr.sin_addr.s_addr = 0;

    server_socket = sl_Socket(SL_AF_INET,SL_SOCK_STREAM, SL_IPPROTO_TCP);

    sl_Bind(server_socket, (SlSockAddr_t *)&server_sock_addr, server_sock_addr_size);

    sl_Listen(server_socket, 1);



        UARTprintf("Waiting for a client ... \n\r");
        while(client_socket < 0){
            client_socket = sl_Accept(server_socket, ( struct SlSockAddr_t *)&client_sock_addr, (SlSocklen_t*)&client_sock_addr_size);
            _SlNonOsMainLoopTask();
        }
        UARTprintf("Client connected! \n\r");
        while(1){
            //Wait for "DATA" command from client
            sl_Recv(client_socket, recv_buff, BUFF_SIZE_MAX, 0);

            if(strncmp(recv_buff, "DATA", 4) == 0){
                memset(recv_buff, 0x00, BUFF_SIZE_MAX);
                UARTprintf("Client issued the DATA command!\n\r");
                while(1){
                    sensors_get_data(data_packet);
                    bytes_sent = sl_Send(client_socket, data_packet, 8, 0);

                    if(bytes_sent != 8){
                        UARTprintf("Checking if client has been disconnected...\n\r");
                        test_sock = sl_GetSockOpt(client_socket, SL_SOL_SOCKET, SL_SO_NONBLOCKING, &optval, (SlSocklen_t*)&optlen);
                    }

                    if(test_sock != 0){
                        UARTprintf("Client has been disconnected!\n\r");
                        break;
                    }

                    _SlNonOsMainLoopTask();
                    blink_led(LED_GREEN);
                }

                sl_Close(client_socket);
                client_socket = -1;
                break;
            }
        }

        sl_WlanDisconnect();

        sl_Stop(SL_STOP_TIMEOUT);

        set_led(LED_RED, 0);
        set_led(LED_GREEN, 0);

        while(1){
            __bis_SR_register(LPM0_bits + GIE); //Enter sleep mode
        }
}



