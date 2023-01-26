/*
 * sensors.h
 *
 *  Created on: 6.10.2017 �.
 *      Author: lbogdanov
 */

#ifndef SENSORS_H_
#define SENSORS_H_

#include <stdint.h>
#include <msp430.h>

void init_sensors(void);
void sensors_get_data(uint8_t *data_packet_buff);

#endif /* SENSORS_H_ */
