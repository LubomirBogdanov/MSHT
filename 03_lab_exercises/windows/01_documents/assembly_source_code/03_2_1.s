#include "msp430.h"     ;#define controlled include file			
					
        NAME	main    ;module name
					
        PUBLIC	main    ;make the main label vissible
                        ; outside this module
        ORG	0FFFEh	
        DC16	init    ;set reset vector to 'init' label
					
        RSEG	CSTACK  ;pre-declaration of segment
        RSEG	CODE    ;place program in 'CODE' segment
					
init:   MOV     #SFE(CSTACK), SP	;set up stack
					
main:   NOP                             ;main program
        MOV.W   #WDTPW+WDTHOLD,&WDTCTL  ;Stop watchdog timer
        BIC.W   #LOCKLPM5,&PM5CTL0      ;Изключи високоимпедансното състояние
                                        ;на входно-изходните изводи
;------------------------------------------------------------------------------------------------------------------------------------------------					
        BIS.B   #???, &P8DIR            ;Инициализация на порт P8, извод 4 – изход
                                        ;MSP430FR6989 Userguide -> стр. 386
        BIC.B   #???, &P8OUT            ;Установи изводa в логическа 0
                                        ;MSP430FR6989 Userguide -> стр. 386
					
L1      XOR.B   #???, &P8OUT            ;Смяна състоянието на извод P8.4
                                        ;MSP430FR6989 Userguide -> стр. 386
        CALL    #Delay                  ;Извикай подпрограма за изчакване
        JMP     L1                      ;Върни се при етикет L1
					
					
Delay:  MOV.W   #65535, R15             ;Зареди регистър R15 с числото 65535
L2      DEC.W   R15                     ;Намали регистър R15 с 1
        JNZ     L2                      ;Ако R15 == 0, продължи. Ако R15 != 0, 
                                        ;върни се при етикет L2
        RET                             ;Върни се в main
;------------------------------------------------------------------------------------------------------------------------------------------------					
        END		

