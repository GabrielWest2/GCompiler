section .data
    str0 db "%d", 0 ; Declare string
    str1 db "params: %d  %d  %d  %d", 10, 0 ; Declare string
    d dd 0 ; Declare int
    str2 db "false", 0 ; Declare string
    str3 db "true", 0 ; Declare string
    str4 db "d was: %s", 10, 0 ; Declare string
section .bss
section .text
    global _main
    extern _ExitProcess@4
    extern _printf

_main:
    

    ; Function
    

    push 4                   ; Push function arg 3 onto stack
    push 3                   ; Push function arg 2 onto stack
    push 2                   ; Push function arg 1 onto stack
    push 1                   ; Push function arg 0 onto stack
    call coolFunc            ; Result will be in eax
    mov  ebx, eax            ; Move return value to ebx
    add esp, 16              ; Clean up the stack
    mov eax, ebx
    mov byte [d], al         ; Move value into variable
    

    ; Expression
    mov  ebx, [d]            ; Store variable in register
    cmp ebx, 1               ; Check condition for ternary
    je CMP_IS_TRUE0          ; Jump if condition is true
    mov ebx, str2            ; Store memory address of string in ebx
    jmp CMP_END0             ; Jump to end of ternary
CMP_IS_TRUE0:
    mov edi, str3            ; Store memory address of string in edi
    mov  ebx, edi            ;  
CMP_END0:
    push ebx                 ; Push function arg 1 onto stack
    mov ebx, str4            ; Store memory address of string in ebx
    push ebx                 ; Push function arg 0 onto stack
    call _printf             ; Result will be in eax
    

    

    ; Return
    push dword 0             ; Push exit code
    call _ExitProcess@4      ; Exit program
    

coolFunc:
    ; Prologue
    push ebp                 ; Push base pointer
    mov ebp, esp             ; Move stack ptr to base ptr
                             ; [ IDENTIFIER a ]  at  [ebp+8]
                             ; [ IDENTIFIER b ]  at  [ebp+12]
                             ; [ IDENTIFIER c ]  at  [ebp+16]
                             ; [ IDENTIFIER d ]  at  [ebp+20]
    

    ; Expression
    mov  ebx, [ebp+20]       ; Store variable in register
    push ebx                 ; Push function arg 4 onto stack
    mov  ebx, [ebp+16]       ; Store variable in register
    push ebx                 ; Push function arg 3 onto stack
    mov  ebx, [ebp+12]       ; Store variable in register
    push ebx                 ; Push function arg 2 onto stack
    mov  ebx, [ebp+8]        ; Store variable in register
    push ebx                 ; Push function arg 1 onto stack
    mov ebx, str1            ; Store memory address of string in ebx
    push ebx                 ; Push function arg 0 onto stack
    call _printf             ; Result will be in eax
    

    sub esp, 4               ; Allocate space for local var 'myChar'
    mov dword [ebp-4], 1     ; Store value in local var
    

    ; Return
    mov  ebx, [ebp-4]        ; Store variable in register
    mov  eax, ebx            ; Put return value in eax
    ; Epilogue
    mov esp, ebp             ; Return stack pointer
    pop ebp                  ; Recover base pointer
    ret                      ; Return from function
    

