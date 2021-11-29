/*
 * sensors.c
 *
 *  Created on: 6.10.2017 �.
 *      Author: lbogdanov
 */
#include <stdio.h>
#include <stdlib.h>
#include "sensors.h"

void init_sensors(void)
{
    //������ ��������� �� ����� P9.2 (A10) � P9.3 (A11) ���� ���� �� ���
    P9SEL0 |= 0x0C;
    P9SEL1 |= 0x0C;
    ADC12CTL0 &= ~ADC12ENC;     //������� ���������������, ENC = 0
    ADC12CTL0 |= ADC12ON;       //������ ���
    ADC12CTL0 |= ADC12SHT1_15;  //������ Sample-and-hold ����� = 512 ����� �� ���-�� ��
                                //������ 8 - 23
    ADC12CTL0 |= ADC12MSC;      //����������� ���������� �� SH ������� ���� �����
                                //�������������
    ADC12CTL1 |= ADC12SSEL_2;   //������ MCLK �� ������ ������
    ADC12CTL1 |= ADC12SHP;      //��������� sampling �������
    ADC12CTL1 |= ADC12CONSEQ_1; //������ ����� "����� ������, �� ���� ��������� �� �����"
    ADC12CTL1 |= ADC12PDIV_0;   //������� �������� ������ �� 1
    ADC12CTL1 |= ADC12DIV_1;    //������� �������� ������ �� 2 (�� ������ �������)
    ADC12CTL1 &= ~ADC12ISSH;    //�� ���������� sample-and-hold �������
    ADC12CTL1 |= ADC12SHS_0;    //��������� ADC12SC ���� ���� ������ �� ������ �� sample-
                                //and-hold
    ADC12CTL2 &= ~ADC12DF;      //������ �� ��������� - ���� ���
    ADC12CTL2 &= ~ADC12RES_3;   //������ �������� �� ������������
    ADC12CTL2 |= ADC12RES_2;    //������ 12-������ �������������, Userguide ���. 880
//-------------------------MCTL ������������----------------------------------------------------------------------
    ADC12CTL3 |= ADC12CSTARTADD_10; //������ �������� ADC12MEM10 �� � �������� ��
                                    //��������� �� ������� �������������
    //������������ ����������� �� ��������������
    ADC12MCTL10 &= ~ADC12DIF; //������ single-ended �������������
    ADC12MCTL10 |= ADC12VRSEL_0; //��������� Vdd �� ����� ����, Vss - �� �����.
    ADC12MCTL10 |= ADC12INCH_10; //������ A10 �� ������ �����
    ADC12MCTL10 &= ~ADC12EOS; //����� �10 �� � �������� ����� �� �������������
    //������������ ����������� �� NTC ��������� (����������)
    while(REFCTL0 & REFGENBUSY); //������� ���� REFGENBUSY � 1, ��� �� - �������
    REFCTL0 |=(REFVSEL_2  | REFON); //����� 2.5 V | ������ ��������� ������
    ADC12MCTL11 &= ~ADC12DIF; //������ single-ended �������������
    ADC12MCTL11 |= ADC12VRSEL_1; //��������� Vref(���������) �� ����� ����, Vss - �� �����.
    ADC12MCTL11 |= ADC12INCH_11; //������ A30 �� ������ �����
    ADC12MCTL11 &= ~ADC12EOS; //����� �30 �� � �������� ����� �� �������������
    //������������ ����������� �� ��������� ���������� ������� (�31)
    ADC12CTL3 |= ADC12BATMAP; //������ ��������� �������� �� ����� A31
    ADC12MCTL31 &= ~ADC12DIF; //������ single-ended �������������
    ADC12MCTL31 |= ADC12VRSEL_1; //��������� Vref(���������) �� ����� ����, Vss - �� �����.
    ADC12MCTL31 |= ADC12INCH_31; //������ A31 �� ������ �����
    ADC12MCTL31 |= ADC12EOS; //����� �31 � �������� ����� �� �������������
//----------------------------------------------------------------------------------------------------------------------------
    ADC12CTL0 |= ADC12ENC; //�������� ENC � 1
}

//data_packet[] = { '\r', 0xBE, X, Y, Z, t, Vdd, 0xEF };
void sensors_get_data(uint8_t *data_packet_buff)
{
    uint16_t pot, tempr;
    char vdd_str[3];
    float vdd;
    uint8_t vdd_dec;
    ADC12CTL0 |= ADC12SC; // ������� ��������e, Userguide ?���. 877
    while(ADC12CTL1 & ADC12BUSY){ }

    pot =  ADC12MEM10;
    tempr =  ADC12MEM11;
    vdd =  ADC12MEM31;

    pot /= 32;

    data_packet_buff[2] = 128 - pot;
    data_packet_buff[3] = pot;
   //data_packet_buff[4] = 128 + pot;

    tempr -= 2200;
    tempr = 600 - tempr;
    tempr /= 5;

    data_packet_buff[5] = tempr;

    vdd *= 0.00061;
    vdd *= 2;
    sprintf(vdd_str, "%03.1f", vdd);
    vdd_str[1] = vdd_str[2];
    vdd_str[2] = '\0';
    vdd_dec = atoi(vdd_str);
    data_packet_buff[6] = vdd_dec;

}
