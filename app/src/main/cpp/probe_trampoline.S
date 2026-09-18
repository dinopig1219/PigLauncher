.text
.p2align 2

.global pig_dart_return_true
.type pig_dart_return_true, %function
pig_dart_return_true:
    bti c
    add x0, x22, #0x20
    ret
.size pig_dart_return_true, .-pig_dart_return_true

.global pig_dart_return_7
.type pig_dart_return_7, %function
pig_dart_return_7:
    bti c
    mov x0, #7
    ret
.size pig_dart_return_7, .-pig_dart_return_7

.global pig_dart_return_12
.type pig_dart_return_12, %function
pig_dart_return_12:
    bti c
    mov x0, #12
    ret
.size pig_dart_return_12, .-pig_dart_return_12

.global pig_dart_return_8
.type pig_dart_return_8, %function
pig_dart_return_8:
    bti c
    mov x0, #8
    ret
.size pig_dart_return_8, .-pig_dart_return_8

.macro PIG_SAVE_VOLATILE
    sub sp, sp, #0x120
    stp x0, x1, [sp, #0x00]
    stp x2, x3, [sp, #0x10]
    stp x4, x5, [sp, #0x20]
    stp x6, x7, [sp, #0x30]
    stp x8, x9, [sp, #0x40]
    stp x10, x11, [sp, #0x50]
    stp x12, x13, [sp, #0x60]
    stp x14, x15, [sp, #0x70]
    stp x16, x17, [sp, #0x80]
    stp x18, x30, [sp, #0x90]
    stp q0, q1, [sp, #0xa0]
    stp q2, q3, [sp, #0xc0]
    stp q4, q5, [sp, #0xe0]
    stp q6, q7, [sp, #0x100]
.endm

.macro PIG_RESTORE_VOLATILE
    ldp q6, q7, [sp, #0x100]
    ldp q4, q5, [sp, #0xe0]
    ldp q2, q3, [sp, #0xc0]
    ldp q0, q1, [sp, #0xa0]
    ldp x18, x30, [sp, #0x90]
    ldp x16, x17, [sp, #0x80]
    ldp x14, x15, [sp, #0x70]
    ldp x12, x13, [sp, #0x60]
    ldp x10, x11, [sp, #0x50]
    ldp x8, x9, [sp, #0x40]
    ldp x6, x7, [sp, #0x30]
    ldp x4, x5, [sp, #0x20]
    ldp x2, x3, [sp, #0x10]
    ldp x0, x1, [sp, #0x00]
    add sp, sp, #0x120
.endm

.extern pig_native_probe_hit
.extern g_pig_back_process_backup
.extern g_pig_maml_animation_backup

.global pig_probe_back_process
.type pig_probe_back_process, %function
pig_probe_back_process:
    bti c
    PIG_SAVE_VOLATILE
    mov w0, #4
    bl pig_native_probe_hit
    PIG_RESTORE_VOLATILE
    adrp x16, g_pig_back_process_backup
    ldr x16, [x16, :lo12:g_pig_back_process_backup]
    br x16
.size pig_probe_back_process, .-pig_probe_back_process

.global pig_probe_maml_animation
.type pig_probe_maml_animation, %function
pig_probe_maml_animation:
    bti c
    PIG_SAVE_VOLATILE
    mov w0, #5
    bl pig_native_probe_hit
    PIG_RESTORE_VOLATILE
    adrp x16, g_pig_maml_animation_backup
    ldr x16, [x16, :lo12:g_pig_maml_animation_backup]
    br x16
.size pig_probe_maml_animation, .-pig_probe_maml_animation
