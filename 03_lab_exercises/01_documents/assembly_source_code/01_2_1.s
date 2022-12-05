#include        "msp430.h"      ;#define controlled include file
        NAME    main            ; module name
        PUBLIC  main            ;make the main label vissible
                                ;outside this module
        ORG     0FFFEh
        DC16    init            ;set reset vector to 'init' label
        RSEG    CSTACK          ;pre-declaration of segment
        RSEG    CODE            ;place program in 'CODE' 
                                ;segment
init:   MOV     #SFE(CSTACK), SP                ;set up stack
main:   NOP                                     ;main program
        MOV.W   #WDTPW+WDTHOLD,&WDTCTL          ;Stop watchdog timer
		
        BIC.W   #LOCKLPM5,&PM5CTL0              ;Изключи високо- 
			                        ;импедансното състояние на 
			                        ;входно-изходните изводи

        BIS.B   #0xF0, &P8DIR                   ;Конфигуриране на изводите 
        BIS.B   #0x63, &P9DIR                   ;като изходи
			
        BIC.B   #0xF0, &P8OUT                   ;Инициализиране на изходи-
        BIC.B   #0x63, &P9OUT                   ;те в логическа 0
		
L1      BIS.B   #0x10, &P8OUT                   ;Включване на D0
        CALL    #Delay                          ;Софтуерно закъснение
        BIC.B   #0x10, &P8OUT                   ;Изключване на D0
        BIS.B   #0x20, &P8OUT                   ;Включване на D1
        CALL    #Delay                          ;Софтуерно закъснение
        BIC.B   #0x20, &P8OUT                   ;Изключване на D1
        BIS.B   #0x40, &P8OUT                   ;Включване на D2
        CALL    #Delay                          ;Софтуерно закъснение
        BIC.B   #0x40, &P8OUT                   ;Изключване на D2
        BIS.B   #0x80, &P8OUT                   ;Включване на D3
        CALL    #Delay                          ;Софтуерно закъснение
        BIC.B   #0x80, &P8OUT                   ;Изключване на D3
        BIS.B   #0x01, &P9OUT                   ;Включване на D4
        CALL    #Delay                          ;Софтуерно закъснение
        BIC.B   #0x01, &P9OUT                   ;Изключване на D4
        BIS.B   #0x02, &P9OUT                   ;Включване на D5
        CALL    #Delay                          ;Софтуерно закъснение
        BIC.B   #0x02, &P9OUT                   ;Изключване на D5
        BIS.B   #0x20, &P9OUT                   ;Включване на D6
        CALL    #Delay                          ;Софтуерно закъснение
        BIC.B   #0x20, &P9OUT                   ;Изключване на D6
        BIS.B   #0x40, &P9OUT                   ;Включване на D7
        CALL    #Delay                          ;Софтуерно закъснение
        BIC.B   #0x40, &P9OUT                   ;Изключване на D7		
	CALL	L1	                        ;Започни отначало
		
Delay:  MOV.W   #65535, R15                     ;Зареди регист. R15 с 65535
L2      DEC.W   R15                             ;Намали R15 с 1
        JNZ     L2                              ; Ако R15 не е 0, иди в L2
        RET                                     ;Aко R15 e 0, излез 
		
        END                                     ;Край на асемблерния файл

