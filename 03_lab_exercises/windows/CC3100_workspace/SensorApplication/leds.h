/*
 * leds.h
 *
 *  Created on: 25.06.2017 ã.
 *      Author: lbogdanov
 */

#ifndef LEDS_H_
#define LEDS_H_

#include "simplelink.h"

enum{
    LED_RED,
    LED_GREEN
};

void init_leds(void);
void set_led(uint8_t led_number, uint8_t led_state);
void blink_led(uint8_t led_number);

#endif /* LEDS_H_ */
