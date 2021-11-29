/*
 * uart.h
 *
 *  Created on: 13.08.2014
 *      Author: lbogdanov
 */

#ifndef UART_H_
#define UART_H_

void initClockSystem();
void initUART();
void UARTCharPut(char TXChar);
char UARTCharGet( );

#endif /* UART_H_ */
