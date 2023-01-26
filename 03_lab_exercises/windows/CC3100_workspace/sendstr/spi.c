/*
 * spi.c - msp430fr5969 launchpad spi interface implementation
 *
 * Copyright (C) 2014 Texas Instruments Incorporated - http://www.ti.com/
 *
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *    Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 *    Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
*/

#include <msp430.h>
#include <msp430fr6989.h>

#include "simplelink.h"
#include "spi.h"
#include "board.h"

#define ASSERT_CS()          (P1OUT &= ~BIT5)
#define DEASSERT_CS()        (P1OUT |= BIT5)


int spi_Close(Fd_t fd)
{
    /* Disable WLAN Interrupt ... */
    CC3100_InterruptDisable();
    return NONOS_RET_OK;
}

Fd_t spi_Open(char *ifName, unsigned long flags)
{

    PM5CTL0 &= ~LOCKLPM5;       /* Disable the GPIO power-on default high-impedance mode
                                   to activate previously configured port settings */

    /* Select the SPI lines: MOSI/MISO/SCK on P1.6/P1.7/P1.4 */
    P1SEL1 &= ~BIT6;
    P1SEL0 |= BIT6;

    P1SEL1 &= ~BIT7;
    P1SEL0 |= BIT7;
    P1OUT |= BIT7; //Pull up/down -> pull up, ???

    P1SEL1 &= ~BIT4;
    P1SEL0 |= BIT4;
    
    /* Enable pull up on P4.2, CC3100 UART RX */
    P4OUT |= BIT2;
    P4REN |= BIT2;

    /* Put state machine in reset */
    UCB0CTLW0 |= UCSWRST;
    /* 3-pin, 8-bit SPI master */
    UCB0CTLW0 = UCMSB + UCMST + UCSYNC + UCCKPH;
    /* Use SMCLK, keep RESET */
    UCB0CTL1 = UCSWRST + UCSSEL_2;

    /* Set SPI clock */
    UCB0BR0 = 0x00; /* f_UCxCLK = 8MHz */
    UCB0BR1 = 0;
    UCB0CTL1 &= ~UCSWRST;


    /* P3.2 - WLAN enable full DS */
    P3SEL1 &= ~BIT2;
    P3SEL0 &= ~BIT2;

    P3OUT &= ~BIT2;
    P3DIR |=  BIT2;


    /* Configure SPI IRQ line on P2.1 */         /*  00 (Setting as a GPIO)*/
    P2DIR &= ~BIT1;
    P2SEL1 &= ~BIT1;
    P2SEL0 &= ~BIT1;

    P2REN |= BIT1;

    /* Configure the SPI CS to be on P1.5 */
    P1OUT  &= ~BIT5;
    P1SEL1 &= ~BIT5;
    P1SEL0 &= ~BIT5;
    P1DIR |= BIT5;

    /* 50 ms delay */
    Delay(50);

    __enable_interrupt();

    /* Enable WLAN interrupt */
    CC3100_InterruptEnable();

    return NONOS_RET_OK;
}


int spi_Write(Fd_t fd, unsigned char *pBuff, int len)
{
    int len_to_return = len;

    ASSERT_CS();
    while (len)
    {
        while (!(UCB0IFG&UCTXIFG));
        UCB0TXBUF = *pBuff;
        while (!(UCB0IFG&UCRXIFG));
        UCB0RXBUF;
        len --;
        pBuff++;
    }

    /* At lower SPI clock frequencies the clock may not be in idle state
     * soon after exiting the above loop. Therefore, the user should poll for 
     * for the clock pin (P2.2) to go to idle state(low) before de-asserting 
     * the Chip Select.
     * 
     * while(P2IN & BIT2);
     */

    DEASSERT_CS();

    return len_to_return;
}


int spi_Read(Fd_t fd, unsigned char *pBuff, int len)
{
    int i = 0;

    ASSERT_CS();

    for (i = 0; i < len; i ++)
    {
        while (!(UCB0IFG&UCTXIFG));
        UCB0TXBUF = 0xFF;
        while (!(UCB0IFG&UCRXIFG));
        pBuff[i] = UCB0RXBUF;
    }

    /* At lower SPI clock frequencies the clock may not be in idle state
     * soon after exiting the above loop. Therefore, the user should poll for 
     * for the clock pin (P2.2) to go to idle state(low) before de-asserting 
     * the Chip Select.
     * 
     * while(P2IN & BIT2);
     */

    DEASSERT_CS();

    return len;
}

