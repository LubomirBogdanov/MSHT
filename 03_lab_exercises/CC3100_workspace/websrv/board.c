/*
 * board.c - msp430fr5969 launchpad configuration
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

#include "simplelink.h"
#include "board.h"

/*
#define XT1_XT2_PORT_SEL    P5SEL
#define XT1_ENABLE          (BIT4 + BIT5)
#define XT2_ENABLE          (BIT2 + BIT3)
#define PMM_STATUS_ERROR    1
#define PMM_STATUS_OK       0
#define XT1HFOFFG           0
*/

P_EVENT_HANDLER             pIraEventHandler = 0;
_u8                         IntIsMasked;

/*!
    \brief          Initialize the system clock of MCU

    \param[in]      none

    \return         none

    \note

    \warning
*/
void initClk()
{
    CSCTL0_H = 0xA5;
    CSCTL0_L = 0;
    CSCTL1 = DCORSEL + DCOFSEL0 + DCOFSEL1;     /* Set DCO setting = 8MHz */
    CSCTL2 = SELA_1 + SELS_3 + SELM_3;          /*set ACLK - VLO, the rest = MCLK = DCO*/
    CSCTL3 = DIVA_0 + DIVS_0 + DIVM_0;          /* set all dividers to 0 */

    /* Globally enable interrupts */
    __bis_SR_register(GIE);
}

/*!
    \brief register an interrupt handler for the host IRQ

    \param[in]      InterruptHdl    -    pointer to interrupt handler function

    \param[in]      pValue          -    pointer to a memory strcuture that is
                    passed to the interrupt handler.

    \return         upon successful registration, the function shall return 0.
                    Otherwise, -1 shall be returned

    \sa
    \note           If there is already registered interrupt handler, the
                    function should overwrite the old handler with the new one
    \warning
*/
int registerInterruptHandler(P_EVENT_HANDLER InterruptHdl , void* pValue)
{
    pIraEventHandler = InterruptHdl;
    return 0;
}

/*!
    \brief          Disables the CC3100

    \param[in]      none

    \return         none

    \note

    \warning
*/
void CC3100_disable()
{
    P3OUT &= ~BIT2;
}

/*!
    \brief          Enables the CC3100

    \param[in]      none

    \return         none

    \note

    \warning
*/
void CC3100_enable()
{
    P3OUT |= BIT2;
}

/*!
    \brief          Enables the interrupt from the CC3100

    \param[in]      none

    \return         none

    \note

    \warning
*/
void CC3100_InterruptEnable(void)
{
    P2DIR &= ~BIT1;
    P2IES &= ~BIT1;
    P2IE |= BIT1;
}

/*!
    \brief          Disables the interrupt from the CC3100

    \param[in]      none

    \return         none

    \note

    \warning
*/
void CC3100_InterruptDisable()
{
    P2DIR &= ~BIT1;
    P2IE &= ~BIT1;
}

/*!
    \brief      Masks the Host IRQ

    \param[in]      none

    \return         none

    \warning
*/
void MaskIntHdlr()
{
    IntIsMasked = TRUE;
}

/*!
    \brief     Unmasks the Host IRQ

    \param[in]      none

    \return         none

    \warning
*/
void UnMaskIntHdlr()
{
    IntIsMasked = FALSE;
}

/*!
    \brief          Stops the Watch Dog timer

    \param[in]      none

    \return         none

    \note

    \warning
*/
void stopWDT()
{
    WDTCTL = WDTPW | WDTHOLD;
}

#if defined(__TI_COMPILER_VERSION__) || defined(__IAR_SYSTEMS_ICC__)
#pragma vector=PORT2_VECTOR
__interrupt
#elif defined(__GNUC__)
__attribute__((interrupt(PORT1_VECTOR)))
#endif
void Port2_ISR(void)
{
    switch (__even_in_range(P2IV, P2IV_P2IFG1))
    {
        /* Vector  P2IV_P2IFG1:  P2IV P2IFG.1 */
        case  P2IV_P2IFG1:
            if (pIraEventHandler)
            {
                pIraEventHandler(0);
            }
            break;

        /* Default case */
        default:
            break;
    }
}

/*!
    \brief     Produce delay in ms

    \param[in]         interval - Time in ms

    \return            none

    \note

    \warning
*/
void Delay(unsigned long interval)
{
    while(interval > 0)
    {
        __delay_cycles(25000);
        interval--;
    }
}

/* Catch interrupt vectors that are not initialized. */
#ifdef __CCS__
#pragma vector=USCI_A0_VECTOR, WDT_VECTOR, TIMER2_A0_VECTOR, ADC12_VECTOR,  \
    TIMER1_A0_VECTOR, TIMER1_A1_VECTOR, TIMER0_A1_VECTOR, TIMER0_A0_VECTOR, \
    TIMER2_A1_VECTOR, UNMI_VECTOR,DMA_VECTOR, \
    TIMER0_B0_VECTOR, TIMER0_B1_VECTOR,SYSNMI_VECTOR, USCI_B0_VECTOR, RTC_VECTOR
__interrupt void Trap_ISR(void)
{
    while(1);
}

#endif

