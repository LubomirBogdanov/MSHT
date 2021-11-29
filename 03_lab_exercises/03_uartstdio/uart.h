/*
 * uart.h
 *
 *  Created on: 3.10.2017
 *      Author: lbogdanov
 */

#ifndef UART_H_
#define UART_H_

#define PHYSICAL_RS232 //UCA0
//If define is commented out, UCA1 is used through the MSP430FR6989 debugger 

void initClockSystem();
void initUART();
void UARTCharPut(char TXChar);
char UARTCharGet( );

#endif /* UART_H_ */
