/*
 * leds.c
 *
 *  Created on: 25.06.2017 �.
 *      Author: lbogdanov
 */
#include "leds.h"

void soft_delay(uint32_t value)
{
    volatile long i;
    for(i = 0; i < value; i++){

    }
}

void init_leds(void)
{
    PM5CTL0 &= ~LOCKLPM5;
    P8DIR |= 0xF0;
    P9DIR |= 0x63;
    P8OUT &= ~0xF0;
    P9OUT &= ~0x63;

    P1OUT &= ~BIT0;
    P1DIR |= BIT0;
    P9OUT &= ~BIT7;
    P9DIR |= BIT7;
}

void blink_led(uint8_t led_number)
{
    switch(led_number){
    case LED_RED:
        P1OUT ^= BIT0;
        soft_delay(100000);
        P1OUT ^= BIT0;
        break;
    case LED_GREEN:
        P9OUT ^= BIT7;
        soft_delay(100000);
        P9OUT ^= BIT7;
        break;
    }
}

void set_led(uint8_t led_number, uint8_t led_state)
{
    switch(led_number){
    case LED_RED:
        if(led_state){
            P1OUT |= BIT0;
        }
        else{
            P1OUT &= ~BIT0;
        }
        break;
    case LED_GREEN:
        if(led_state){
            P9OUT |= BIT7;
        }
        else{
            P9OUT &= ~BIT7;
        }
        break;
    }
}

