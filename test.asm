section .data
    floatreg1 dd 0.0 ; Declare float
    floatreg2 dd 0.0 ; Declare float
    a dd 50.0 ; Declare float
    b dd 3.0 ; Declare float
    c dd 0.0 ; Declare float
    str0 db "%f", 10, 0 ; Declare string
section .bss
section .text
    global _main
    extern _ExitProcess@4
    extern _printf

_main:
    mov  ebx, [a]                      ; Store variable in register
    mov  edi, [b]                      ; Store variable in register
    mov dword [floatreg1], ebx         ; Move constant to register
    fld dword [floatreg1]              ; Load num1
    mov dword [floatreg1], edi         ; Move constant to register
    fld dword [floatreg1]              ; Load num2
    fdivp                              ; Pop 2 numbers, then push quot on stack
    fstp dword [floatreg1]             ; Pop quot from stack and store in out
    mov  ebx, [floatreg1]              ; Move quot to out reg
    mov dword [c], ebx                 ; Move value into variable
    

    ; Expression
    ; JUST BEFORE CALL   vars: []
    mov  ebx, [c]                      ; Store variable in register
    sub esp, 8                         ; Allocate space on stack for float (promoted to double)
    mov dword [floatreg1], ebx         ; Push function arg 1 onto stack
    fld dword [floatreg1]              ; Load float into st0
    fstp qword [esp]                   ; Push function arg 1 onto stack
    mov  ebx, str0                     ; Store memory address of string
    push ebx                           ; Push function arg 0 onto stack
    call _printf                       ; Result will be in eax
    add esp, 8                         ; Clean up the stack
    

    

    ; Return
    add 1000, 1000
    mov eax, dword 1000                ; Move INT into eax
    push eax                           ; Push exit code
    call _ExitProcess@4                ; Exit program
    

