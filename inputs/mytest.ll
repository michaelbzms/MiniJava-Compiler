@.mytest_vtable = global [0 x i8*] []
@.A_vtable = global [1 x i8*] [i8* bitcast (i32* (i8*, i32*, i32*)* @A.afunct to i8*)]
@.B_vtable = global [2 x i8*] [i8* bitcast (i32* (i8*, i32*, i32*)* @A.afunct to i8*), i8* bitcast (i32 (i8*, i32*, i32)* @B.bfunct to i8*)]
@.C_vtable = global [2 x i8*] [i8* bitcast (i32* (i8*, i32*, i32*)* @C.afunct to i8*), i8* bitcast (i32 (i8*, i32*, i32)* @B.bfunct to i8*)]
@.D_vtable = global [3 x i8*] [i8* bitcast (i32* (i8*, i32*, i32*)* @A.afunct to i8*), i8* bitcast (i32 (i8*, i32*, i32)* @B.bfunct to i8*), i8* bitcast (i1 (i8*)* @D.checkInheritanceAndArgs to i8*)]

declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)

@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"

define void @print_int(i32 %i) {
    %_str = bitcast [4 x i8]* @_cint to i8*
    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
    ret void
}

define void @throw_oob() {
    %_str = bitcast [15 x i8]* @_cOOB to i8*
    call i32 (i8*, ...) @printf(i8* %_str)
    call void @exit(i32 1)
    ret void
}

define i32 @main() {
    %arr = alloca i32*
    %b = alloca i8*
    %d = alloca i8*
    %bull = alloca i1
     ; array allocation
    %_0 = icmp slt i32 -1, 102
    br i1 %_0, label %label1, label %label0
label0:
    call void @throw_oob()
    br label %label2
label1:
    %_1 = add i32 102, 1
    %_2 = call i8* @calloc(i32 4, i32 %_1)
    %_3 = bitcast i8* %_2 to i32*
    store i32 102, i32* %_3
    br label %label2
label2:
    ; assignment
    store i32* %_3, i32** %arr
    ; This is an object allocation of "C"
    %_4 = call i8* @calloc(i32 32, i32 1)
    %_5 = bitcast i8* %_4 to i8***
    %_6 = getelementptr [2 x i8*], [2 x i8*]* @.C_vtable, i32 0, i32 0
    store i8** %_6, i8*** %_5
    ; assignment
    store i8* %_4, i8** %b
    ; This is an object allocation of "D"
    %_7 = call i8* @calloc(i32 32, i32 1)
    %_8 = bitcast i8* %_7 to i8***
    %_9 = getelementptr [3 x i8*], [3 x i8*]* @.D_vtable, i32 0, i32 0
    store i8** %_9, i8*** %_8
    ; assignment
    store i8* %_7, i8** %d
    ; Method call
    %_10 = load i8*, i8** %b
    %_17 = load i32*, i32** %arr
    %_18 = load i32*, i32** %arr
    %_11 = bitcast i8* %_10 to i8***
    %_12 = load i8**, i8*** %_11
    %_13 = getelementptr i8*, i8** %_12, i32 0
    %_14 = load i8*, i8** %_13
    %_15 = bitcast i8* %_14 to i32* (i8*, i32*, i32*)*
    %_16 = call i32* %_15(i8* %_10, i32* %_18, i32* %_17)
    ; assignment
    store i32* %_16, i32** %arr
    ; Method call
    %_19 = load i8*, i8** %d
    %_26 = load i32*, i32** %arr
    %_27 = load i32*, i32** %arr
    %_20 = bitcast i8* %_19 to i8***
    %_21 = load i8**, i8*** %_20
    %_22 = getelementptr i8*, i8** %_21, i32 0
    %_23 = load i8*, i8** %_22
    %_24 = bitcast i8* %_23 to i32* (i8*, i32*, i32*)*
    %_25 = call i32* %_24(i8* %_19, i32* %_27, i32* %_26)
    ; assignment
    store i32* %_25, i32** %arr
    ; This is an object allocation of "B"
    %_28 = call i8* @calloc(i32 24, i32 1)
    %_29 = bitcast i8* %_28 to i8***
    %_30 = getelementptr [2 x i8*], [2 x i8*]* @.B_vtable, i32 0, i32 0
    store i8** %_30, i8*** %_29
    ; assignment
    store i8* %_28, i8** %b
    ; Method call
    %_31 = load i8*, i8** %b
    %_38 = load i32*, i32** %arr
    %_39 = load i32*, i32** %arr
    %_32 = bitcast i8* %_31 to i8***
    %_33 = load i8**, i8*** %_32
    %_34 = getelementptr i8*, i8** %_33, i32 0
    %_35 = load i8*, i8** %_34
    %_36 = bitcast i8* %_35 to i32* (i8*, i32*, i32*)*
    %_37 = call i32* %_36(i8* %_31, i32* %_39, i32* %_38)
    ; assignment
    store i32* %_37, i32** %arr
    ; Method call
    %_40 = load i8*, i8** %d
    %_41 = bitcast i8* %_40 to i8***
    %_42 = load i8**, i8*** %_41
    %_43 = getelementptr i8*, i8** %_42, i32 2
    %_44 = load i8*, i8** %_43
    %_45 = bitcast i8* %_44 to i1 (i8*)*
    %_46 = call i1 %_45(i8* %_40)
    ; assignment
    store i1 %_46, i1* %bull
    ret i32 0
}

define i32* @A.afunct(i8* %this, i32* %.a1, i32* %.a2) {
    %a1 = alloca i32*
    %a2 = alloca i32*
    store i32* %.a1, i32** %a1
    store i32* %.a2, i32** %a2
    %_0 = load i32*, i32** %a1
     ; array primary expression
    %_1 = load i32, i32* %_0
    %_2 = icmp ult i32 1, %_1
    br i1 %_2, label %label1, label %label0
label0:
    call void @throw_oob()
    br label %label2
label1:
    %_3 = add i32 1, 1
    %_4 = getelementptr i32, i32* %_0, i32 %_3
    %_5 = load i32, i32* %_4
    br label %label2
label2:
    %_6 = load i32*, i32** %a2
    ; array assignment
    %_7 = load i32, i32* %_6
    %_8 = icmp ult i32 0, %_7
    br i1 %_8, label %label4, label %label3
label3:
    call void @throw_oob()
    br label %label5
label4:
    ; Array assignment
    %_9 = add i32 0, 1
    %_10 = getelementptr i32, i32* %_6, i32 %_9
    store i32 %_5, i32* %_10
    br label %label5
label5:
    %_11 = load i32*, i32** %a2
    ; assignment
    store i32* %_11, i32** %a1
    call void (i32) @print_int(i32 42)
     ; array allocation
    %_12 = icmp slt i32 -1, 2
    br i1 %_12, label %label7, label %label6
label6:
    call void @throw_oob()
    br label %label8
label7:
    %_13 = add i32 2, 1
    %_14 = call i8* @calloc(i32 4, i32 %_13)
    %_15 = bitcast i8* %_14 to i32*
    store i32 2, i32* %_15
    br label %label8
label8:
    ret i32* %_15
}

define i32 @B.bfunct(i8* %this, i32* %.aarr, i32 %.i) {
    %i = alloca i32
    %aarr = alloca i32*
    store i32* %.aarr, i32** %aarr
    store i32 %.i, i32* %i
    %_0 = load i32*, i32** %aarr
    %_1 = load i32, i32* %i
    %_2 = add i32 %_1, 1
     ; array primary expression
    %_3 = load i32, i32* %_0
    %_4 = icmp ult i32 %_2, %_3
    br i1 %_4, label %label1, label %label0
label0:
    call void @throw_oob()
    br label %label2
label1:
    %_5 = add i32 %_2, 1
    %_6 = getelementptr i32, i32* %_0, i32 %_5
    %_7 = load i32, i32* %_6
    br label %label2
label2:
    ret i32 %_7
}

define i32* @C.afunct(i8* %this, i32* %.c1, i32* %.c2) {
    %res = alloca i32*
    %a = alloca i8*
    %c1 = alloca i32*
    %c2 = alloca i32*
    store i32* %.c1, i32** %c1
    store i32* %.c2, i32** %c2
    call void (i32) @print_int(i32 102)
    ; This is an object allocation of "mytest"
    %_0 = call i8* @calloc(i32 8, i32 1)
    %_1 = bitcast i8* %_0 to i8***
    %_2 = getelementptr [0 x i8*], [0 x i8*]* @.mytest_vtable, i32 0, i32 0
    store i8** %_2, i8*** %_1
    ; assignment
    store i8* %_0, i8** %a
    %_3 = load i32*, i32** %c1
     ; array primary expression
    %_4 = load i32, i32* %_3
    %_5 = icmp ult i32 0, %_4
    br i1 %_5, label %label1, label %label0
label0:
    call void @throw_oob()
    br label %label2
label1:
    %_6 = add i32 0, 1
    %_7 = getelementptr i32, i32* %_3, i32 %_6
    %_8 = load i32, i32* %_7
    br label %label2
label2:
    %_9 = load i32*, i32** %c2
     ; array primary expression
    %_10 = load i32, i32* %_9
    %_11 = icmp ult i32 1, %_10
    br i1 %_11, label %label4, label %label3
label3:
    call void @throw_oob()
    br label %label5
label4:
    %_12 = add i32 1, 1
    %_13 = getelementptr i32, i32* %_9, i32 %_12
    %_14 = load i32, i32* %_13
    br label %label5
label5:
    %_15 = icmp slt i32 %_8, %_14
    br i1 %_15, label %label6, label %label7
label6:
    %_16 = load i32*, i32** %c1
    ; assignment
    store i32* %_16, i32** %res
    br label %label8
label7:
    %_17 = load i32*, i32** %c2
    ; assignment
    store i32* %_17, i32** %res
    br label %label8
label8:
    %_18 = load i32*, i32** %res
    ret i32* %_18
}

define i1 @D.checkInheritanceAndArgs(i8* %this) {
    %array = alloca i32*
    %i = alloca i32
    ; assignment
    store i32 1, i32* %i
    %_0 = load i32*, i32** %array
    ; array assignment
    %_1 = load i32, i32* %_0
    %_2 = icmp ult i32 0, %_1
    br i1 %_2, label %label1, label %label0
label0:
    call void @throw_oob()
    br label %label2
label1:
    ; Array assignment
    %_3 = add i32 0, 1
    %_4 = getelementptr i32, i32* %_0, i32 %_3
    store i32 4, i32* %_4
    br label %label2
label2:
    %_5 = load i32*, i32** %array
     ; array primary expression
    %_6 = load i32, i32* %_5
    %_7 = icmp ult i32 0, %_6
    br i1 %_7, label %label4, label %label3
label3:
    call void @throw_oob()
    br label %label5
label4:
    %_8 = add i32 0, 1
    %_9 = getelementptr i32, i32* %_5, i32 %_8
    %_10 = load i32, i32* %_9
    br label %label5
label5:
    call void (i32) @print_int(i32 %_10)
    %_11 = load i32*, i32** %array
     ; array primary expression
    %_12 = load i32, i32* %_11
    %_13 = icmp ult i32 1, %_12
    br i1 %_13, label %label7, label %label6
label6:
    call void @throw_oob()
    br label %label8
label7:
    %_14 = add i32 1, 1
    %_15 = getelementptr i32, i32* %_11, i32 %_14
    %_16 = load i32, i32* %_15
    br label %label8
label8:
    call void (i32) @print_int(i32 %_16)
    %_17 = load i32*, i32** %array
    %_18 = load i32, i32* %_17
    call void (i32) @print_int(i32 %_18)
    ret i1 1
}

