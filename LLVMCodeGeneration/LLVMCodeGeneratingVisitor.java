package LLVMCodeGeneration;

import java.util.Map;
import MiniJavaType.*;
import SemanticAnalysis.SemanticChecks;
import SymbolTable.*;
import Util.ExtendedVisitorParameterInfo;
import Util.ExtendedVisitorReturnInfo;
import Util.MyPair;
import Util.VisitorParameterInfo;
import visitor.GJDepthFirst;
import syntaxtree.*;


public class LLVMCodeGeneratingVisitor extends GJDepthFirst<ExtendedVisitorReturnInfo, VisitorParameterInfo> {

    private static final boolean EMIT_COMMENTS = false;
    private FileWritter out;
    private final SymbolTable ST;
    private LLVMNameGenerator nameGenerator;


    public LLVMCodeGeneratingVisitor(SymbolTable _ST, String outputFilename){
        ST = _ST;
        out = new FileWritter(outputFilename);
        nameGenerator = new LLVMNameGenerator();
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public ExtendedVisitorReturnInfo visit(Goal n, VisitorParameterInfo argu) {

        // generate VTable for main and all other classes
        out.emit( "@." + ST.getMainClassName() + "_vtable = global [0 x i8*] []\n");
        for (MyPair<String, ClassInfo> c : ST.getOrderedClasses()){
            out.emit(LLVMCodeGenerating.generateVTableForClass(c.getFirst(), c.getSecond()) + "\n");
        }

        // emit global utility code
        out.emit("\ndeclare i8* @calloc(i32, i32)\n" +
                "declare i32 @printf(i8*, ...)\n" +
                "declare void @exit(i32)\n" +
                "\n" +
                "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                "@_cNAL = constant [23 x i8] c\"Negative array length\\0a\\00\"\n\n" +
                "define void @print_int(i32 %i) {\n" +
                "    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "    ret void\n" +
                "}\n\n" +
                "define void @throw_oob() {\n" +
                "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str)\n" +
                "    call void @exit(i32 1)\n" +
                "    ret void\n" +
                "}\n\n" +
                "define void @throw_nal() {\n" +
                "    %_str = bitcast [23 x i8]* @_cNAL to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str)\n" +
                "    call void @exit(i32 1)\n" +
                "    ret void\n" +
                "}\n\n");

        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);

        out.close();    // close file writter

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public ExtendedVisitorReturnInfo visit(MainClass n, VisitorParameterInfo argu) {

        out.emit("define i32 @main() {\n");

        // allocate space for local main variables (order does not matter)
        for ( Map.Entry<String, VariableInfo> v : ST.getMainClassInfo().getMethodInfo("main").getVariablesMap().entrySet() ){
            out.emit("    %" + v.getKey() + " = alloca " + v.getValue().getType().getLLVMType() + "\n");
        }

        n.f14.accept(this, new VisitorParameterInfo("main", ST.getMainClassName(), "main"));
        n.f15.accept(this, new VisitorParameterInfo("main", ST.getMainClassName(), "main"));

        out.emit("    ret i32 0\n}\n\n");

        return null;
    }

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public ExtendedVisitorReturnInfo visit(TypeDeclaration n, VisitorParameterInfo argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public ExtendedVisitorReturnInfo visit(ClassDeclaration n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r1 = n.f1.accept(this, argu);
        if (r1 == null) return null;
        n.f3.accept(this, new VisitorParameterInfo(r1.getName(), "method"));
        n.f4.accept(this, new VisitorParameterInfo(r1.getName(), "method"));
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public ExtendedVisitorReturnInfo visit(ClassExtendsDeclaration n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r1 = n.f1.accept(this, argu);
        if (r1 == null) return null;
        n.f5.accept(this, new VisitorParameterInfo(r1.getName(), "method"));
        n.f6.accept(this, new VisitorParameterInfo(r1.getName(), "method"));
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public ExtendedVisitorReturnInfo visit(MethodDeclaration n, VisitorParameterInfo argu) {
        // set start of local variable counter and local label counter to 0
        nameGenerator.resetLocalCounter();

        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);
        if (r2 == null) return null;

        MethodInfo methodInfo = ST.lookupMethod(argu.getName(), r2.getName());
        if (methodInfo == null) return null;

        out.emit("define " + methodInfo.getReturnType().getLLVMType() + " @" + argu.getName() + "." + r2.getName() + "(i8* %this");

        n.f4.accept(this, new VisitorParameterInfo(r2.getName(), argu.getName(), "method"));    // emits parameter code

        out.emit(") {\n");

        // allocate space for arguments and local variables alike (order does not matter)
        for ( Map.Entry<String, VariableInfo> v : methodInfo.getVariablesMap().entrySet() ){
        	out.emit("    %" + v.getKey() + " = alloca " + v.getValue().getType().getLLVMType() + "\n");
        }

        // argument values must be stored from call args (order does not matter
        for ( MyPair<String, VariableInfo> v : methodInfo.getArgList() ){
        	out.emit("    store " + v.getSecond().getType().getLLVMType() + " %." + v.getFirst() + ", " + v.getSecond().getType().getLLVMType() + "* %" + v.getFirst() + "\n");
        }

        n.f8.accept(this, new VisitorParameterInfo(r2.getName(), argu.getName(), "method"));    // emits method code

        ExtendedVisitorReturnInfo r10 = n.f10.accept(this, new VisitorParameterInfo(r2.getName(), argu.getName(), "method"));
        if (r10 == null) return null;

        // Note: what if return type is an object? -> we have to return a reference
        out.emit("    ret " + methodInfo.getReturnType().getLLVMType() + " " + r10.getResultVarNameOrConstant() + "\n");
        out.emit("}\n\n");

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public ExtendedVisitorReturnInfo visit(FormalParameter n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        ExtendedVisitorReturnInfo r1 = n.f1.accept(this, argu);
        if (r0 == null || r1 == null) return null;

        out.emit(", " + r0.getType().getLLVMType() + " %." + r1.getName());

        return null;
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public ExtendedVisitorReturnInfo visit(Type n, VisitorParameterInfo argu) {
        return n.f0.accept(this, new VisitorParameterInfo(null, null, null, "getType"));  // getType is used in Identifier()'s visit() for custom types
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public ExtendedVisitorReturnInfo visit(ArrayType n, VisitorParameterInfo argu) {
        return new ExtendedVisitorReturnInfo(MiniJavaType.INTARRAY, null);
    }

    /**
     * f0 -> "boolean"
     */
    public ExtendedVisitorReturnInfo visit(BooleanType n, VisitorParameterInfo argu) {
        return new ExtendedVisitorReturnInfo(MiniJavaType.BOOLEAN, null);
    }

    /**
     * f0 -> "int"
     */
    public ExtendedVisitorReturnInfo visit(IntegerType n, VisitorParameterInfo argu) {
        return new ExtendedVisitorReturnInfo(MiniJavaType.INTEGER, null);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public ExtendedVisitorReturnInfo visit(AssignmentStatement n, VisitorParameterInfo argu) {
        if (EMIT_COMMENTS) out.emit("    ; assignment\n");

        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);
        if (r0 == null || r2 == null) return null;

        VariableInfo varInfo;
        if (argu.getType().equals("main")) {
            varInfo = ST.lookupMainVariable(r0.getName());
        } else {
            varInfo = ST.lookupVariable(argu.getSupername(), argu.getName(), r0.getName());
        }
        if (varInfo != null){
            // identifier is a local variable
            String llvmType = varInfo.getType().getLLVMType();
            out.emit("    store " + llvmType + " " + r2.getResultVarNameOrConstant() + ", " + llvmType + "* " + r0.getResultVarNameOrConstant() + "\n");
        } else if (!argu.getType().equals("main")) {
            varInfo = SemanticChecks.checkFieldExists(ST, argu.getSupername(), argu.getName(), r0.getName());
            if (varInfo != null){
                // identifier is a field of "this" object
                String llvmType = varInfo.getType().getLLVMType();
                int byteoffset = 8 + varInfo.getOffset();   // + 8 to bypass the vtable pointer
                String fieldptr = nameGenerator.generateLocalVarName();
                String castedfieldptr = nameGenerator.generateLocalVarName();
                out.emit("    " + fieldptr + " = getelementptr i8, i8* %this, i32 " + byteoffset + "\n");
                out.emit("    " + castedfieldptr + " = bitcast i8* " + fieldptr + " to " + llvmType + "*\n");
                out.emit("    store " + llvmType + " " + r2.getResultVarNameOrConstant() + ", " + llvmType + "* " + castedfieldptr + "\n");
            } else System.err.println("Unknown identifier in assignment?!");  // should not happen cause of semantic checks
        } else System.err.println("Unknown identifier in assignment?!");      // ^^

        if (EMIT_COMMENTS) out.emit("    ; end of assignment\n");

        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public ExtendedVisitorReturnInfo visit(ArrayAssignmentStatement n, VisitorParameterInfo argu) {
        if (EMIT_COMMENTS) out.emit("    ; array assignment\n");

        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);
        ExtendedVisitorReturnInfo r5 = n.f5.accept(this, argu);
        if (r0 == null || r2 == null || r5 == null) return null;

        String array = nameGenerator.generateLocalVarName();
        VariableInfo varInfo;
        if (argu.getType().equals("main")) {
            varInfo = ST.lookupMainVariable(r0.getName());
        } else {
            varInfo = ST.lookupVariable(argu.getSupername(), argu.getName(), r0.getName());
        }
        if (varInfo != null){
            // identifier is a local variable
            out.emit("    " + array + " = load i32*, i32** " + r0.getResultVarNameOrConstant() + "\n");
        } else if (!argu.getType().equals("main")) {
            varInfo = SemanticChecks.checkFieldExists(ST, argu.getSupername(), argu.getName(), r0.getName());
            if (varInfo != null){
                // identifier is a field of "this" object
                int byteoffset = 8 + varInfo.getOffset();   // + 8 to bypass the vtable pointer
                String fieldptr = nameGenerator.generateLocalVarName();
                String castedfieldptr = nameGenerator.generateLocalVarName();
                out.emit("    " + fieldptr + " = getelementptr i8, i8* %this, i32 " + byteoffset + "\n");
                out.emit("    " + castedfieldptr + " = bitcast i8* " + fieldptr + " to i32**\n");
                out.emit("    " + array + " = load i32*, i32** " + castedfieldptr + "\n");
            } else System.err.println("Unknown identifier in array assignment?!");  // should not happen cause of semantic checks
        } else System.err.println("Unknown identifier in array assignment?!");      // ^^

        // check index bounds
        String exceptionlabel = nameGenerator.generateLabelName("out_of_bounds");
        String oklabel = nameGenerator.generateLabelName("in_bounds");
        String exitlabel = nameGenerator.generateLabelName("exit_oob_check");

        String arrlen = nameGenerator.generateLocalVarName();
        String comp = nameGenerator.generateLocalVarName();
        String offsetplusone = nameGenerator.generateLocalVarName();
        String elemptr = nameGenerator.generateLocalVarName();

        out.emit("    " + arrlen + " = load i32, i32* " + array + "\n");
        out.emit("    " + comp + " = icmp ult i32 " + r2.getResultVarNameOrConstant() + ", " + arrlen + "\n");
        out.emit("    br i1 " + comp + ", label %" + oklabel + ", label %" + exceptionlabel + "\n");
        out.emit(exceptionlabel + ":\n");
        out.emit("    call void @throw_oob()\n");
        out.emit("    br label %" + exitlabel + "\n");
        out.emit(oklabel + ":\n");

        out.emit("    " + offsetplusone + " = add i32 " + r2.getResultVarNameOrConstant() + ", 1\n");   // negate length in 0 pos
        out.emit("    " + elemptr + " = getelementptr i32, i32* " + array + ", i32 " + offsetplusone + "\n");
        out.emit("    store i32 " + r5.getResultVarNameOrConstant() + ", i32* " + elemptr + "\n");

        out.emit("    br label %" + exitlabel + "\n");
        out.emit(exitlabel + ":\n");

        if (EMIT_COMMENTS) out.emit("    ; end of array assignment\n");

        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public ExtendedVisitorReturnInfo visit(IfStatement n, VisitorParameterInfo argu) {
        if (EMIT_COMMENTS) out.emit("    ; if-else block\n");

        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);    // emits code to calculate condition
        if (r2 == null) return null;

        String trueblock = nameGenerator.generateLabelName("if_true_case");
        String falseblock = nameGenerator.generateLabelName("if_false_case");
        String exit = nameGenerator.generateLabelName("if_exit");

        out.emit("    br i1 " + r2.getResultVarNameOrConstant() + ", label %" + trueblock + ", label %" + falseblock + "\n");
        out.emit(trueblock + ":\n");

        n.f4.accept(this, argu);    // emits code for true-if

        out.emit("    br label %" + exit + "\n");
        out.emit(falseblock + ":\n");

        n.f6.accept(this, argu);    // emits code for false-if

        out.emit("    br label %" + exit + "\n");
        out.emit(exit + ":\n");

        if (EMIT_COMMENTS) out.emit("    ; end of if-else block\n");

        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public ExtendedVisitorReturnInfo visit(WhileStatement n, VisitorParameterInfo argu) {
        String loopstart = nameGenerator.generateLabelName("loop_cond");
        String loopstmts = nameGenerator.generateLabelName("loop_begin");
        String exit = nameGenerator.generateLabelName("loop_end");

        if (EMIT_COMMENTS) out.emit("    ; while loop\n");

        out.emit("    br label %" + loopstart + "\n");
        out.emit(loopstart + ":\n");

        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);   // emits code to calculate expression

        out.emit("    br i1 " + r2.getResultVarNameOrConstant() + ", label %" + loopstmts + ", label %" + exit + "\n");
        out.emit(loopstmts + ":\n");

        n.f4.accept(this, argu);   // emits code of loop

        out.emit("    br label %" + loopstart + "\n");
        out.emit(exit + ":\n");

        if (EMIT_COMMENTS) out.emit("    ; end of while loop\n");

        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public ExtendedVisitorReturnInfo visit(PrintStatement n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);
        if (r2 == null) return null;

        if (r2.getType().getTypeEnum() == TypeEnum.INTEGER) {
            out.emit("    call void (i32) @print_int(i32 " + r2.getResultVarNameOrConstant() + ")\n");
        } else if (r2.getType().getTypeEnum() == TypeEnum.BOOLEAN){
            String casted = nameGenerator.generateLocalVarName();
            out.emit("    " + casted + " = zext i1 " + r2.getResultVarNameOrConstant() + " to i32\n");
            out.emit("    call void (i32) @print_int(i32 " + casted + ")\n");
        }

        return null;
    }

    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | Clause()
     */
    public ExtendedVisitorReturnInfo visit(Expression n, VisitorParameterInfo argu) {
        // Note: I set purpose to "getVariable" as that is the most common one and change it to null where that is not the case later
        return n.f0.accept(this,  new VisitorParameterInfo(argu.getName(), argu.getSupername(), argu.getType(), "getVariable"));
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public ExtendedVisitorReturnInfo visit(AndExpression n, VisitorParameterInfo argu) {
        String falselabel = nameGenerator.generateLabelName("first_is_false");
        String truelabel = nameGenerator.generateLabelName("first_is_true");
        String block_that_jumps_to_phi = nameGenerator.generateLabelName("block_that_jumps_to_phi");
        String exitlabel = nameGenerator.generateLabelName("exit_and_op");
        String res = nameGenerator.generateLocalVarName();

        if (EMIT_COMMENTS) out.emit("    ; short-circuiting \"&&\"\n");

        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);    // emits code to calculate left clause

        out.emit("    br i1 " + r0.getResultVarNameOrConstant() + ", label %" + truelabel + ", label %" + falselabel + "\n");

        out.emit(falselabel + ":\n");
        out.emit("    br label %" + exitlabel + "\n");

        out.emit(truelabel + ":\n");

        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);    // emits code to calculate clause

        out.emit("    br label %" + block_that_jumps_to_phi + "\n");
        out.emit(block_that_jumps_to_phi + ":\n");
        out.emit("    br label %" + exitlabel + "\n");

        out.emit(exitlabel + ":\n");
        out.emit("    " + res + " = phi i1 [0, %" + falselabel + "], [" + r2.getResultVarNameOrConstant() + ", %" + block_that_jumps_to_phi + "]\n");

        if (EMIT_COMMENTS) out.emit("    ; end of short-circuiting \"&&\"\n");

        return new ExtendedVisitorReturnInfo(MiniJavaType.BOOLEAN, res);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public ExtendedVisitorReturnInfo visit(CompareExpression n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);
        if (r0 == null || r2 == null) return null;

        String res = nameGenerator.generateLocalVarName();
        out.emit("    " + res + " = icmp slt i32 " + r0.getResultVarNameOrConstant() + ", " + r2.getResultVarNameOrConstant() + "\n");

        return new ExtendedVisitorReturnInfo(MiniJavaType.BOOLEAN, res);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public ExtendedVisitorReturnInfo visit(PlusExpression n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        ExtendedVisitorReturnInfo r1 = n.f2.accept(this, argu);
        if (r0 == null || r1 == null) return null;

        String res = nameGenerator.generateLocalVarName();
        out.emit("    " + res + " = add i32 " + r0.getResultVarNameOrConstant() + ", " + r1.getResultVarNameOrConstant() + "\n");

        return new ExtendedVisitorReturnInfo(MiniJavaType.INTEGER, res);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public ExtendedVisitorReturnInfo visit(MinusExpression n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        ExtendedVisitorReturnInfo r1 = n.f2.accept(this, argu);
        if (r0 == null || r1 == null) return null;

        String res = nameGenerator.generateLocalVarName();
        out.emit("    " + res + " = sub i32 " + r0.getResultVarNameOrConstant() + ", " + r1.getResultVarNameOrConstant() + "\n");

        return new ExtendedVisitorReturnInfo(MiniJavaType.INTEGER, res);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public ExtendedVisitorReturnInfo visit(TimesExpression n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        ExtendedVisitorReturnInfo r1 = n.f2.accept(this, argu);
        if (r0 == null || r1 == null) return null;

        String res = nameGenerator.generateLocalVarName();
        out.emit("    " + res + " = mul i32 " + r0.getResultVarNameOrConstant() + ", " + r1.getResultVarNameOrConstant() + "\n");

        return new ExtendedVisitorReturnInfo(MiniJavaType.INTEGER, res);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public ExtendedVisitorReturnInfo visit(ArrayLookup n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, argu);
        if (r0 == null || r2 == null) return null;

        String exceptionlabel = nameGenerator.generateLabelName("out_of_bounds");
        String oklabel = nameGenerator.generateLabelName("in_bounds");
        String exitlabel = nameGenerator.generateLabelName("exit_oob_check");
        String arrlen = nameGenerator.generateLocalVarName();
        String comp = nameGenerator.generateLocalVarName();

        // check that (unsigned) len < array.length or throw out of bounds exception
        if (EMIT_COMMENTS) out.emit("    ; array lookup\n");

        out.emit("    " + arrlen + " = load i32, i32* " + r0.getResultVarNameOrConstant() + "\n");
        out.emit("    " + comp + " = icmp ult i32 " + r2.getResultVarNameOrConstant() + ", " + arrlen + "\n");
        out.emit("    br i1 " + comp + ", label %" + oklabel + ", label %" + exceptionlabel + "\n");
        out.emit(exceptionlabel + ":\n");
        out.emit("    call void @throw_oob()\n");
        out.emit("    br label %" + exitlabel + "\n");
        out.emit(oklabel + ":\n");

        String offsetplusone = nameGenerator.generateLocalVarName();
        String elemptr = nameGenerator.generateLocalVarName();
        String element = nameGenerator.generateLocalVarName();

        out.emit("    " + offsetplusone + " = add i32 " + r2.getResultVarNameOrConstant() + ", 1\n");   // negate length in 0 pos
        out.emit("    " + elemptr + " = getelementptr i32, i32* " + r0.getResultVarNameOrConstant() + ", i32 " + offsetplusone + "\n");
        out.emit("    " + element + " = load i32, i32* " + elemptr + "\n");

        out.emit("    br label %" + exitlabel + "\n");
        out.emit(exitlabel + ":\n");

        if (EMIT_COMMENTS) out.emit("    ; end of array lookup\n");

        return new ExtendedVisitorReturnInfo(MiniJavaType.INTEGER, element);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public ExtendedVisitorReturnInfo visit(ArrayLength n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);
        if (r0 == null) return null;

        String len = nameGenerator.generateLocalVarName();
        out.emit("    " + len + " = load i32, i32* " + r0.getResultVarNameOrConstant() + "\n");

        return new ExtendedVisitorReturnInfo(MiniJavaType.INTEGER, len);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public ExtendedVisitorReturnInfo visit(MessageSend n, VisitorParameterInfo argu) {
        if (EMIT_COMMENTS) out.emit("    ; method call\n");

        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);     // emits code to calculate expression
        ExtendedVisitorReturnInfo r2 = n.f2.accept(this, new VisitorParameterInfo(argu.getName(), argu.getSupername(), argu.getType(), null));  // null purpose -> only get name

        if (r0 == null || r2 == null) return null;
        if (r0.getType().getTypeEnum() != TypeEnum.CUSTOM) { System.err.println("Missed something in semantic checks"); return null; }  // should not happen

        MethodInfo methodInfo = SemanticChecks.checkMethodExistsForCustomType(ST, r0.getType().getCustomTypeName(), r2.getName());
        if (methodInfo == null) { System.err.println("Missed something in semantic checks"); return null; }  // should not happen

        if (EMIT_COMMENTS) out.emit("    ; info: called method " + r2.getName() + " of Class " + r0.getType().getCustomTypeName() + "\n");

        String vtableptrptr = nameGenerator.generateLocalVarName();
        String vtableptr = nameGenerator.generateLocalVarName();
        String func_ptr = nameGenerator.generateLocalVarName();
        String func_addr = nameGenerator.generateLocalVarName();
        String casted_func = nameGenerator.generateLocalVarName();
        String ret = nameGenerator.generateLocalVarName();
        String obj = r0.getResultVarNameOrConstant();

        int methodIndex = methodInfo.getOffset() / 8;

        ExtendedVisitorParameterInfo exprListArgs = new ExtendedVisitorParameterInfo(argu.getSupername(), argu.getName(), r0.getType().getCustomTypeName(), r2.getName(), argu.getType());
        n.f4.accept(this, exprListArgs);   // this will emit code to calculate the parameters and store all result var names in exprListArgs

        out.emit("    " + vtableptrptr + " = bitcast i8* " + obj + " to i8***\n");
        out.emit("    " + vtableptr + " = load i8**, i8*** " + vtableptrptr + "\n");
        out.emit("    " + func_ptr + " = getelementptr i8*, i8** " + vtableptr + ", i32 " + methodIndex + "\n");
        out.emit("    " + func_addr + " = load i8*, i8** " + func_ptr + "\n");
        out.emit("    " + casted_func + " = bitcast i8* " + func_addr + " to " + LLVMCodeGenerating.getMethodType(null, null, methodInfo) + "\n");
        out.emit("    " + ret + " = call " + methodInfo.getReturnType().getLLVMType() + " " + casted_func + "(i8* " + obj);
        if (exprListArgs.getListOfResultVars() != null) {
            for (ExtendedVisitorReturnInfo r : exprListArgs.getListOfResultVars()) {
                out.emit(", " + r.getType().getLLVMType() + " " + r.getResultVarNameOrConstant());
            }
        }
        out.emit(")\n");

        if (EMIT_COMMENTS) out.emit("    ; end of method call\n");

        return new ExtendedVisitorReturnInfo(methodInfo.getReturnType(), ret);
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public ExtendedVisitorReturnInfo visit(ExpressionList n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r0 = n.f0.accept(this, argu);      // emits code to calculate expression
        if (r0 == null) return null;
        argu.addToListOfResultVars(r0);         // (!) must be before n.f1.accept() is called to assert correct order
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public ExtendedVisitorReturnInfo visit(ExpressionTail n, VisitorParameterInfo argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public ExtendedVisitorReturnInfo visit(ExpressionTerm n, VisitorParameterInfo argu) {
        // calculate expressions (emit such code)
        ExtendedVisitorReturnInfo r1 = n.f1.accept(this, argu);   // emits code to calculate expression
        if (r1 == null) return null;
        argu.addToListOfResultVars(r1);
        return null;
    }

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public ExtendedVisitorReturnInfo visit(Clause n, VisitorParameterInfo argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */
    public ExtendedVisitorReturnInfo visit(PrimaryExpression n, VisitorParameterInfo argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public ExtendedVisitorReturnInfo visit(IntegerLiteral n, VisitorParameterInfo argu) {
        return new ExtendedVisitorReturnInfo(n.f0.toString(), MiniJavaType.INTEGER, n.f0.toString());
    }

    /**
     * f0 -> "true"
     */
    public ExtendedVisitorReturnInfo visit(TrueLiteral n, VisitorParameterInfo argu) {
        return new ExtendedVisitorReturnInfo("true", MiniJavaType.BOOLEAN, "1");
    }

    /**
     * f0 -> "false"
     */
    public ExtendedVisitorReturnInfo visit(FalseLiteral n, VisitorParameterInfo argu) {
        return new ExtendedVisitorReturnInfo("false", MiniJavaType.BOOLEAN, "0");
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public ExtendedVisitorReturnInfo visit(Identifier n, VisitorParameterInfo argu) {
        if (argu != null && argu.getPurpose() != null && argu.getPurpose().equals("getType"))
            return new ExtendedVisitorReturnInfo(n.f0.toString(), new MiniJavaType(TypeEnum.CUSTOM, n.f0.toString()), null);
        else if (argu != null && argu.getPurpose() != null && argu.getPurpose().equals("getVariable")){

            String value = nameGenerator.generateLocalVarName();
            VariableInfo varInfo;
            if (argu.getType().equals("main")) {
                varInfo = ST.lookupMainVariable(n.f0.toString());
            } else {
                varInfo = ST.lookupVariable(argu.getSupername(), argu.getName(), n.f0.toString());
            }
            if (varInfo != null){
                // identifier is a local variable
                String llvmType = varInfo.getType().getLLVMType();
                out.emit("    " + value + " = load " + llvmType + ", " + llvmType + "* %" + n.f0.toString() + "\n");
            } else if (!argu.getType().equals("main")) {
                varInfo = SemanticChecks.checkFieldExists(ST, argu.getSupername(), argu.getName(), n.f0.toString());
                if (varInfo != null){
                    // identifier is a field of "this" object
                    String llvmType = varInfo.getType().getLLVMType();
                    int byteoffset = 8 + varInfo.getOffset();   // + 8 to bypass the vtable pointer
                    String fieldptr = nameGenerator.generateLocalVarName();
                    String castedfieldptr = nameGenerator.generateLocalVarName();
                    if (EMIT_COMMENTS) out.emit("    ; object field lookup\n");
                    out.emit("    " + fieldptr + " = getelementptr i8, i8* %this, i32 " + byteoffset + "\n");
                    out.emit("    " + castedfieldptr + " = bitcast i8* " + fieldptr + " to " + llvmType + "*\n");
                    out.emit("    " + value + " = load " + llvmType + ", " + llvmType + "* " + castedfieldptr + "\n");
                    if (EMIT_COMMENTS) out.emit("    ; end of object field lookup\n");
                } else System.err.println("Unknown identifier in expression?!");  // should not happen cause of semantic checks
            } else System.err.println("Unknown identifier in expression?!");      // ^^

            return new ExtendedVisitorReturnInfo(n.f0.toString(), (varInfo != null) ? varInfo.getType() : null, value);
        }
        else   // default:
            return new ExtendedVisitorReturnInfo(n.f0.toString(), null, "%" + n.f0.toString());
    }

    /**
     * f0 -> "this"
     */
    public ExtendedVisitorReturnInfo visit(ThisExpression n, VisitorParameterInfo argu) {
        return new ExtendedVisitorReturnInfo("this", new MiniJavaType(TypeEnum.CUSTOM, argu.getSupername()), "%this");
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public ExtendedVisitorReturnInfo visit(ArrayAllocationExpression n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r3 = n.f3.accept(this, argu);
        if (r3 == null) return null;

        if (EMIT_COMMENTS) out.emit("    ; array allocation\n");

        // check that len >= 0 or throw exception
        String exceptionlabel = nameGenerator.generateLabelName("negative_array_length");
        String oklabel = nameGenerator.generateLabelName("ok_array_length");
        String exitlabel = nameGenerator.generateLabelName("exit_nal_check");
        String comp = nameGenerator.generateLocalVarName();

        out.emit("    " + comp + " = icmp sge i32 " + r3.getResultVarNameOrConstant() + ", 0\n");
        out.emit("    br i1 " + comp + ", label %" + oklabel + ", label %" + exceptionlabel + "\n");
        out.emit(exceptionlabel + ":\n");
        out.emit("    call void @throw_nal()\n");
        out.emit("    br label %" + exitlabel + "\n");
        out.emit(oklabel + ":\n");

        String lenplusone = nameGenerator.generateLocalVarName();
        String arr = nameGenerator.generateLocalVarName();
        String castedarr = nameGenerator.generateLocalVarName();

        out.emit("    " + lenplusone + " = add i32 " + r3.getResultVarNameOrConstant() + ", 1\n");
        out.emit("    " + arr + " = call i8* @calloc(i32 4, i32 " + lenplusone + ")\n");
        out.emit("    " + castedarr + " = bitcast i8* " + arr + " to i32*\n");
        // (!) Store length of array at its first element - real elements start from 1...
        out.emit("    store i32 " + r3.getResultVarNameOrConstant() + ", i32* " + castedarr + "\n");

        out.emit("    br label %" + exitlabel + "\n");
        out.emit(exitlabel + ":\n");

        if (EMIT_COMMENTS) out.emit("    ; end of array allocation\n");

        ExtendedVisitorReturnInfo res = new ExtendedVisitorReturnInfo(MiniJavaType.INTARRAY, castedarr);
        res.setAlloced(true);    // probably unnecessary here
        return res;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public ExtendedVisitorReturnInfo visit(AllocationExpression n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r1 = n.f1.accept(this, new VisitorParameterInfo(argu.getName(), argu.getSupername(), argu.getType(), null));  // do not ask for a variable we only want the class name
        if (r1 == null) return null;

        ClassInfo classInfo = ST.lookupClass(r1.getName());
        if (classInfo == null) { System.err.println("Unknown class allocation"); return null; }   // should not happen

        String newobj = nameGenerator.generateLocalVarName();
        String vtableptr = nameGenerator.generateLocalVarName();
        String vtablefirstelem = nameGenerator.generateLocalVarName();
        int numOfMethods = classInfo.getTotalNumberOfMethods();

        if (EMIT_COMMENTS) out.emit("    ; object allocation of \"" + r1.getName() + "\"\n");

        out.emit("    " + newobj + " = call i8* @calloc(i32 " + (classInfo.getNextFieldOffset() + 8) + ", i32 1)\n");
        out.emit("    " + vtableptr + " = bitcast i8* " + newobj + " to i8***\n");
        out.emit("    " + vtablefirstelem + " = getelementptr [" + numOfMethods + " x i8*], [" + numOfMethods + " x i8*]* @." + r1.getName() + "_vtable, i32 0, i32 0\n");
        out.emit("    store i8** " + vtablefirstelem + ", i8*** " + vtableptr + "\n");

        if (EMIT_COMMENTS) out.emit("    ; end of object allocation\n");

        ExtendedVisitorReturnInfo res = new ExtendedVisitorReturnInfo(r1.getName(), new MiniJavaType(TypeEnum.CUSTOM, r1.getName()), newobj);
        res.setAlloced(true);    // probably unnecessary here
        return res;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public ExtendedVisitorReturnInfo visit(NotExpression n, VisitorParameterInfo argu) {
        ExtendedVisitorReturnInfo r1 = n.f1.accept(this, argu);
        if (r1 == null) return null;

        String res = nameGenerator.generateLocalVarName();
        out.emit("    " + res + " = xor i1 " + r1.getResultVarNameOrConstant() + ", 1\n");   // xor with 1 is "not"

        return new ExtendedVisitorReturnInfo(MiniJavaType.BOOLEAN, res);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public ExtendedVisitorReturnInfo visit(BracketExpression n, VisitorParameterInfo argu) {
        return n.f1.accept(this, argu);
    }

}
