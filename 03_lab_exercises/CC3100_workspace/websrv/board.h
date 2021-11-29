/*
 * board.h - msp430f5969 launchpad configuration
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

#ifndef __BOARD_HEADER__
#define __BOARD_HEADER__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include <msp430.h>
#include <stdint.h>

/*  This macro is for use by other macros to form a fully valid C statement. */
#define st(x)      do { x } while (__LINE__ == -1)

/* Select source for MCLK and SMCLK
 *  e.g. SELECT_MCLK_SMCLK(SELM__DCOCLK + SELS__DCOCLK) */
#define SELECT_MCLK_SMCLK(sources) st(UCSCTL4 = (UCSCTL4 & ~(SELM_7 + SELS_7)) \
                                                | (sources);)

typedef void (*P_EVENT_HANDLER)(void* pValue);

typedef enum
{
    ANT1,
    ANT2
} antenna;

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
int registerInterruptHandler(P_EVENT_HANDLER InterruptHdl , void* pValue);

/*!
    \brief          Enables the CC3100

    \param[in]      none

    \return         none

    \note

    \warning
*/
void CC3100_enable();

/*!
    \brief          Disables the CC3100

    \param[in]      none

    \return         none

    \note

    \warning
*/
void CC3100_disable();

/*!
    \brief          Enables the interrupt from the CC3100

    \param[in]      none

    \return         none

    \note

    \warning
*/
void CC3100_InterruptEnable(void);

/*!
    \brief          Disables the interrupt from the CC3100

    \param[in]      none

    \return         none

    \note

    \warning
*/
void CC3100_InterruptDisable();

/*!
    \brief          Stops the Watch Dog timer

    \param[in]      none

    \return         none

    \note

    \warning
*/
void stopWDT();

/*!
    \brief          Initialize the system clock of MCU

    \param[in]      none

    \return         none

    \note

    \warning
*/
void initClk();

/*!
    \brief     Produce delay in ms

    \param[in]         interval - Time in ms

    \return            none

    \note

    \warning
*/
void Delay(unsigned long interval);

/*!
    \brief      Masks the Host IRQ

    \param[in]      none

    \return         none

    \warning
*/
void MaskIntHdlr();

/*!
    \brief     Unmasks the Host IRQ

    \param[in]      none

    \return         none

    \warning
*/
void UnMaskIntHdlr();

/*!
    \brief     Initialize the antenna section GPIO (P2.4/P2.5)

    \param[in]      none

    \return         none

    \warning
*/
void initAntSelGPIO();

/*!
    \brief     Configure the GPIO for selecting antenna.

    \param[in]      antenna : antenna to be selected

    \return         none

    \warning
*/
void SelAntenna(int antenna);

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __BOARD_HEADER__ */
