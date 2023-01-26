/*
 * uart.c
 *
 *  Created on: 13.08.2014
 *      Author: lbogdanov
 */
#include "uart.h"

#include  <msp430.h>
#include <msp430fr6989.h> //For Eclipse's intellisense

void initClockSystem()
{
 #ifdef PHYSICAL_RS232
    // Set DCO to 16MHz
    CSCTL0_H = 0xA5;
    CSCTL1 |= DCORSEL;
    CSCTL1 &= ~(DCOFSEL0 | DCOFSEL1);
    CSCTL1 |= DCOFSEL2;

    //Needed for DCO = 16 MHz, set FRAM controller for FRAMCTL_ACCESS_TIME_CYCLES_1
    FRCTL0 = FWPW;
    FRCTL0_L &= ~NWAITS_7;
    FRCTL0_L |= NWAITS_1;

    CSCTL2 = SELA_1 | SELS_3 | SELM_3; // set ACLK = VLO; SMCLK = DCO; MCLK = DCO
    CSCTL3 = 0x00; // set all dividers to 1
#else
    CSCTL0_H = 0xA5;
    CSCTL0_L = 0;
    CSCTL1 &= ~DCOFSEL_7; //Clear DCOFSEL bits
    CSCTL1 = DCORSEL | DCOFSEL_3;               /* Set DCO setting = 8MHz */
    CSCTL2 = SELA_1 | SELS_3 | SELM_3; // set ACLK = VLO; SMCLK = DCO; MCLK = DCO
    CSCTL3 = 0x00; // set all dividers to 1
#endif
}

void initUART()
{
#ifdef PHYSICAL_RS232
    //N = fBRCLK / baud rate = 16 000 000 / 9600 = 1666.666
    //OS16 = 1, UCBRx = INT(N/16) = 1666.666 / 16 = 104.166 = 104 (0x68, 0x00)
    //UCBRFx = INT([(N/16) – INT(N/16)] × 16) = 0.166 x 16 = 2.656 = 2
    //UCBRSx = N - INT(N) = 0,666 => in Table 30-4 => 0xD6
  
    // Configure UART pins
    P4SEL0 |= (BIT2 | BIT3);
    P4SEL1 &= ~(BIT2 | BIT3);
    // Configure UART 0
    UCA0CTLW0 |= UCSWRST;  //Hold in reset while configuring
    UCA0CTLW0 = UCSSEL_2;  // Set SMCLK = 16 000 000 as UCBRCLK

    UCA0CTLW0 |= UCMODE_0; //Operate as UART
    UCA0BR0 = 0x68;
    UCA0BR1 = 0x0;

    UCA0MCTLW |= UCOS16;
    UCA0MCTLW |= (0xD6UL<<8);
    UCA0MCTLW |= UCBRF_2;

    UCA0CTLW0 &= ~UCSWRST; // release from reset
#else    
    //USCI_A0 TXD P3.4
    P3SEL0 |= BIT4;
    P3SEL1 &= ~BIT4;

    //USCI_A0 RXD P3.5
    P3SEL0 |= BIT5;
    P3SEL1 &= ~BIT5;

    UCA1CTLW0 |= UCSWRST;  /* Put state machine in reset */
    UCA1CTL0 = 0x00;

    UCA1CTLW0 = UCSSEL__SMCLK | UCSWRST;    /* Use SMCLK, keep RESET */
                                            /* 8MHz/115200 =0x45 (see User's Guide) */
    UCA1BRW = 0x341;                        /* 8MHz/9600= 833 =0x341 (see User's Guide) */
    UCA1MCTLW = 0;
    UCA1CTLW0 &= ~UCSWRST;                  /* Initialize USCI state machine */
#endif
}

void UARTCharPut(char TXChar)
{
#ifdef PHYSICAL_RS232
    while(!(UCA0IFG & UCTXIFG)){ }
    UCA0TXBUF = TXChar;
#else
    while (!(UCA1IFG & UCTXIFG)) ;
    UCA1TXBUF = TXChar;
#endif
}

char UARTCharGet( )
{
    char tempCh;
#ifdef PHYSICAL_RS232
    while( !(UCA0IFG&UCRXIFG) ) { }     //Wait for RX buffer to receive a char.
    tempCh = UCA0RXBUF;

    UCA0TXBUF = tempCh;                //Echo the
    while( UCA0STATW & UCBUSY ) { }     //received char.
#else
    while( !(UCA1IFG&UCRXIFG) ) { }     //Wait for RX buffer to receive a char.
    tempCh = UCA1RXBUF;

    UCA1TXBUF = tempCh;                //Echo the
    while( UCA1STATW & UCBUSY ) { }     //received char.
#endif

    return tempCh;
}
