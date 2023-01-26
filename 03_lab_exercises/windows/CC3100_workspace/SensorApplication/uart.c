/*
 * uart.c
 *
 *  Created on: 13.08.2014
 *      Author: lbogdanov
 */
#include "uart.h"

#include  <msp430.h>


void UARTCharPut(char TXChar)
{
	while (!(UCA1IFG & UCTXIFG)) ;
	UCA1TXBUF = TXChar;
}

char UARTCharGet( )
{
    return 0;
}
