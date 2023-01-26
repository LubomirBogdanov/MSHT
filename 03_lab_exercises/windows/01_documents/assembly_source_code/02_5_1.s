#include        "msp430.h"      ; #define controlled include file
		
        NAME    main    ; module name
        PUBLIC  main    ; make the main label vissible
                        ; outside this module
        ORG     0FFFEh	
        DC16    init    ; set reset vector to 'init' label
	
        RSEG    CSTACK  ; pre-declaration of segment
        RSEG    CODE    ; place program in 'CODE' segment

init:   MOV     #SFE(CSTACK), SP        ; set up stack
        MOV.W   #0x00, R4               ; Нулирай работен регистър R5
        MOV.W   #0x00, R5               ; Нулирай работен регистър R6
        MOV.W   #0x0020, R7             ; Зареди числото 32 в R7     
        MOV.W   #0x1C00, R8             ; Зареди началния адрес на SRAM в R8
        MOV.W   #0x1C00, R9             ; Зареди началния адрес на SRAM в R9
        MOV.W   #0x0000, &0x1C00        ; Инициализирай първите 2 байта от SRAM
;Нулирай първите 64 байта от SRAM-----------------------			
zero:   MOV.W   0(R8), 0(R9)	
        ADD.W   #2, R9	
        DEC.W   R7	
        JNZ     zero	
;-------------------------------------------------------  			
        CLR     R2                      ; Нулирай STATUS регистъра
			
main:   NOP                             ; main program
        MOV.W   #WDTPW+WDTHOLD,&WDTCTL  ; Stop watchdog timer
		
        MOV.W   #0x5555, R4      	; Поставете точка на прекъсване на този ред
	MOV.W   R4, R5           	
		
        MOV.W   #0x1BFF, R4             ; Демонстрация на Непосредствена адресация
        MOV.W   #0x1C00, R5
        MOV.W   #0x3412, &0x1C00	
        MOV.W   #0x7856, &0x1C04	
		
        MOV.W   1(R4), 4(R5)            ; Демонстрация на Индексна адресация

        MOV     #0x05, R4	
L1:     DEC     R4	
        JNZ     L1                      ; Демонстрация на Символна адресация
	
        NOP		
		
        JMP     L2                      ; Демонстрация на Символна адресация
        NOP                             ; Обърнете внимание на инструкцията в двоичен вид
        NOP                             ; и броя на NOP инструкциите
        NOP                    		
        NOP                    		
        NOP                    		
			
L2:     JMP     L3                      ; Демонстрация на Символна адресация
        NOP                             ; Обърнете внимание на инструкцията в двоичен вид
        NOP                             ; и броя на NOP инструкциите
        NOP		
			
L3:     MOV.W   #0xcdab, &0x1C00        	
        MOV.W	&0x1C00, &0x1C04        ; Демонстрация на Абсолютна адресация
			
        CLR.W   &0x1C04	
        MOV.W   #0x1C00, R4	
        MOV.W   @R4, &0x1C04            ; Демонстрация на Индиректна адресация

        MOV.W   R4, &0x1C04	

        MOV.W   @R4+, R5                ;Демонстрация на Индиректна автоинкрементираща 
        MOV.W   @R4+, R5                ; адресация
        MOV.W   @R4+, R5	
        MOV.W   @R4+, R5	
        MOV.W   #0x1C00, R4	
			
        MOV.B   @R4+, R5                ; Демонстрация на Индиректна автоинкрементираща
        MOV.B   @R4+, R5                ; адресация с байтов достъп
        MOV.B   @R4+, R5
        MOV.B   @R4+, R5	
			
        MOV.W   #0x1234, R4	
        MOV.W   #0xabcd, R5	
			
        PUSH.W  R4                      ; Демонстрация на PUSH и POP инструкции
        PUSH.W  R5                      ; Обърнете внимание на реда на извикването им
			
        MOV.W   #0x5555, R4	
        MOV.W   #0x3333, R5	
			
        POP.W   R5	
        POP.W   R4	
			
        JMP     $	
        END		

