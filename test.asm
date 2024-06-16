section .data
    str0 db "%d", 0 ; Declare string
    a dd 1 ; Declare int
    b dd 1 ; Declare int
    c dd 0 ; Declare int
    myChar db 'h' ; Declare character
    v dd 0 ; Declare int
    str1 db "fib(%d):   %d    %c", 10, 0 ; Declare string
section .bss
section .text
    global _main
    extern _ExitProcess@4
    extern _printf

_main:

    ; While
WHILE_BEGIN0:
    mov dword ebx, [v]       ; Store variable in register
    mov edi, 10              ; Move literal into edi
    cmp ebx, edi             ; Compare value in register ebx with register edi
    jg CMP_IS_TRUE0          ; Jump if ebx > edi
    mov edi, 1               ; Set edi to 1 because ebx <= edi
    jmp CMP_END0             ; Jump to end of compare, edi has been set
CMP_IS_TRUE0:
    mov edi, 0               ; Set edi to 0 because ebx > edi
CMP_END0:
    test edi, edi            ; Check for false (0)
    jz WHILE_END0            ; Jump to end if false

    ; Expression
    mov dword edi, [a]       ; Store variable in register
    mov dword ebx, [b]       ; Store variable in register
    add edi, ebx
    mov dword [c], edi       ; Assign variable


    ; Expression
    mov byte edi, [myChar]   ; Store variable in register
    push edi                 ; Push function arg 3 onto stack
    mov dword edi, [b]       ; Store variable in register
    push edi                 ; Push function arg 2 onto stack
    mov dword edi, [v]       ; Store variable in register
    push edi                 ; Push function arg 1 onto stack
    mov edi, str1            ; Store memory address of string in edi
    push edi                 ; Push function arg 0 onto stack
    call _printf             ; Result will be in eax
    mov dword edi, eax       ; Move return value to edi
    add esp, 16              ; Clean up the stack


    ; Expression
    mov dword edi, [a]       ; Store variable in register
    mov dword [b], edi       ; Assign variable


    ; Expression
    mov dword edi, [c]       ; Store variable in register
    mov dword [a], edi       ; Assign variable


    ; Expression
    mov dword edi, [v]       ; Store variable in register
    mov ebx, 1               ; Move literal into ebx
    add edi, ebx
    mov dword [v], edi       ; Assign variable

    jmp WHILE_BEGIN0         ; Jump to start, try again
WHILE_END0:


    ; Return
    mov edi, 0               ; Move literal into edi
    push dword edi           ; Push exit code
    call _ExitProcess@4      ; Exit program


    ; Function

