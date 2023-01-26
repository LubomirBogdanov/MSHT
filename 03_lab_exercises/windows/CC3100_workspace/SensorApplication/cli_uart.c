/*
 * cli_uart.c - msp430fr5969 launchpad application uart interface implementation
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


//*****************************************************************************
//
//! \addtogroup CLI_UART
//! @{
//
//*****************************************************************************

#include <string.h>
#include <msp430.h>

#include "cli_uart.h"


#define UCA1_OS   1    /* 1 = oversampling mode, 0 = low-freq mode */
#define UCA1_BR0  17   /* Value of UCA1BR0 register */
#define UCA1_BR1  0    /* Value of UCA1BR1 register */
#define UCA1_BRS  0    /* Value of UCBRS field in UCA1MCTL register */
#define UCA1_BRF  6    /* Value of UCBRF field in UCA1MCTL register */

#define ASCII_ENTER     0x0D

#ifdef _USE_CLI_
unsigned char *g_ucUARTBuffer;
int cli_have_cmd = 0;
#endif /* _USE_CLI_ */

/*!
    \brief          Application Uart interrupt handler

    \param[in]      none

    \return         none

    \note

    \warning
*/
#if defined(__TI_COMPILER_VERSION__) || defined(__IAR_SYSTEMS_ICC__)
#pragma vector=USCI_A1_VECTOR
__interrupt
#elif defined(__GNUC__)
__attribute__((interrupt(USCI_A0_VECTOR)))
#endif
void UART_ISR(void)
{
    switch(__even_in_range(UCA1IV,0x08))
    {
        case 0:break;                             /* Vector 0 - no interrupt */
        case 2:                                   /* Vector 2 - RXIFG */
#ifdef _USE_CLI_
            *g_ucUARTBuffer = UCA1RXBUF;
            if (*g_ucUARTBuffer == ASCII_ENTER)
            {
                cli_have_cmd = 1;
                *g_ucUARTBuffer = 0x00;
                __bic_SR_register_on_exit(LPM0_bits);
            }
            g_ucUARTBuffer++;
#endif /* _USE_CLI_ */
            __no_operation();
            break;
        case 4:break;                             /* Vector 4 - TXIFG */
        default: break;
    }
}

int
CLI_Read(unsigned char *pBuff)
{
    if(pBuff == NULL)
        return -1;

#ifdef _USE_CLI_
    cli_have_cmd = 0;
    g_ucUARTBuffer = pBuff;
    UCA1IE |= UCRXIE;

    __bis_SR_register(LPM0_bits + GIE);

    while(cli_have_cmd == 0)
        ;

    UCA1IE &= ~UCRXIE;
    return strlen((const char *)pBuff);
#else /* _USE_CLI_ */
    return 0;
#endif /* _USE_CLI_ */
}

int
CLI_Write(unsigned char *inBuff)
{
    if(inBuff == NULL)
        return -1;

#ifdef _USE_CLI_
    unsigned short ret,usLength = strlen((const char *)inBuff);
    ret = usLength;
    while (usLength)
    {
        while (!(UCA1IFG & UCTXIFG)) ;
        UCA1TXBUF = *inBuff;
        usLength--;
        inBuff++;
    }
    return (int)ret;
#else /* _USE_CLI_ */
    return 0;
#endif /* _USE_CLI_ */
}

void
CLI_Configure(void)
{
#ifdef _USE_CLI_
    PM5CTL0 &= ~LOCKLPM5;           /* Disable the GPIO power-on default high-impedance mode
                                       to activate previously configured port settings */

    //USCI_A0 TXD P3.4
    P3SEL0 |= BIT4;
    P3SEL1 &= ~BIT4;

    //USCI_A0 RXD P3.5
    P3SEL0 |= BIT5;
    P3SEL1 &= ~BIT5;


    UCA1CTLW0 |= UCSWRST;           /* Put state machine in reset */
    UCA1CTL0 = 0x00;

    UCA1CTLW0 = UCSSEL__SMCLK + UCSWRST;    /* Use SMCLK, keep RESET */
                                            /* 8MHz/115200 =0x45 (see User's Guide) */
    UCA1BRW = 0x341;                        /* 8MHz/9600= 833 =0x341 (see User's Guide) */

    UCA1MCTLW = 0;

    UCA1CTLW0 &= ~UCSWRST;                  /* Initialize USCI state machine */

    /* Disable RX Interrupt on UART */
    UCA1IFG &= ~ (UCRXIFG | UCRXIFG);
    UCA1IE &= ~UCRXIE;
#endif /* _USE_CLI_ */
}

//*****************************************************************************
//
// Close the Doxygen group.
//! @}
//
//*****************************************************************************
