#include "html_sensors.h"
#include "simplelink.h"
#include "sl_common.h"
#include "simple_link_func.h"
#include "uartstdio.h"
#include "leds.h"
#include "html.h"
#include "sensors.h"

#define SSID_NAME       "LAB1362"          // Access point name to connect to.
#define SEC_TYPE        SL_SEC_TYPE_WPA_WPA2    // Security type of the Access piont
#define PASSKEY         "arm-cortex"           // Password in case of secure AP
#define PASSKEY_LEN     pal_Strlen(PASSKEY)     // Password length in case of secure AP
#define MAX_MSG_SIZE    20
#define TCP_PORT        80

int main(int argc, char** argv)
{
    uint8_t data_sens_html_dynamic[40];
    SlSecParams_t secParams = {0};
    SlSockAddrIn_t server_sock_addr;
    _u16 server_sock_addr_size = sizeof(SlSockAddrIn_t);
    SlSockAddrIn_t client_sock_addr;
    _u16 client_sock_addr_size = sizeof(SlSockAddrIn_t);
    _i16 server_socket = 0;
    _i16 client_socket = -1;
    char recv_buff[255];
    unsigned long html_size;

    stopWDT();
    initClk();
    CLI_Configure();
    init_leds();
    init_sensors();
    UARTprintf("\n\r************************\n\r");
    UARTprintf("Starting web server application ...\n\r");

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
    server_sock_addr.sin_port = sl_Htons((_u16)TCP_PORT);
    server_sock_addr.sin_addr.s_addr = 0;

    server_socket = sl_Socket(SL_AF_INET,SL_SOCK_STREAM, SL_IPPROTO_TCP);

    sl_Bind(server_socket, (SlSockAddr_t *)&server_sock_addr, server_sock_addr_size);

    sl_Listen(server_socket, 1);

    while(1){

        while(client_socket < 0){
            client_socket = sl_Accept(server_socket, ( struct SlSockAddr_t *)&client_sock_addr, (SlSocklen_t*)&client_sock_addr_size);
            _SlNonOsMainLoopTask();
        }

        //Wait for HTTP GET request
        sl_Recv(client_socket, recv_buff, 255, 0);
        UARTprintf("MSG from client: %s \n\r", recv_buff);

        //Static Hello, WORLD web page---------
        //unsigned char *html_msg = "<HTML><HEAD></HEAD><BODY>Hello, WORLD!</BODY></HTML>";
        //unsigned char *html_msg = "HTTP/1.0 200 OK\r\nContent-Type: text/html\r\n\r\n<HTML>\n<BODY>\n\t<H1>Hello, WORLD!</H1>\n</BODY>\n</HTML>\n";
        //sl_Send(client_socket, html_msg, strlen(html_msg), 0);
        //-------------------------------------

        //Invoke the sensor reading function here
        ???

        html_size = strlen(index_sens_html);
        sl_Send(client_socket, index_sens_html, html_size, 0);

        //Send the sensor data in HTML format here
        ???

        html_size = strlen(close_sens_html);
        sl_Send(client_socket, close_sens_html, html_size, 0);

        sl_Close(client_socket);
        client_socket = -1;

        _SlNonOsMainLoopTask();

        blink_led(LED_GREEN);
    }
}



