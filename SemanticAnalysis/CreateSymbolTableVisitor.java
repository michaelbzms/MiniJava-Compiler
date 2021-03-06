package SemanticAnalysis;

import MiniJavaType.*;
import SymbolTable.*;
import Util.VisitorParameterInfo;
import Util.VisitorReturnInfo;
import syntaxtree.*;
import visitor.GJDepthFirst;


public class CreateSymbolTableVisitor extends GJDepthFirst<VisitorReturnInfo, VisitorParameterInfo> {

    // Fields used to detect error and stop DFS
    public boolean detectedSemanticError = false;
    public String errorMsg = "";
    public SymbolTable ST;

    public CreateSymbolTableVisitor(SymbolTable _ST){
        super();
        this.ST = _ST;
        if (_ST == null) {
            this.detectedSemanticError = true;
            this.errorMsg = "Null Symbol Table parameter given";
        }
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
    public VisitorReturnInfo visit(MainClass n, VisitorParameterInfo argu)  {
        n.f0.accept(this, null);
        VisitorReturnInfo r1 = n.f1.accept(this, null);      // r1 -> main class name
        if (r1 == null) return null;
        n.f2.accept(this, null);
        n.f3.accept(this, null);
        n.f4.accept(this, null);
        n.f5.accept(this, null);
        n.f6.accept(this, null);
        n.f7.accept(this, null);
        n.f8.accept(this, null);
        n.f9.accept(this, null);
        n.f10.accept(this, null);
        VisitorReturnInfo r11 = n.f11.accept(this, null);
        if (r11 == null) return null;
        ST.setMainClassNameAndArg(r1.getName(), r11.getName());
        n.f12.accept(this, null);
        n.f13.accept(this, null);
        n.f14.accept(this, new VisitorParameterInfo(null, "mainclass"));
        n.f15.accept(this, null);
        n.f16.accept(this, null);
        n.f17.accept(this, null);
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public VisitorReturnInfo visit(ClassDeclaration n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        n.f0.accept(this, null);
        VisitorReturnInfo r1 = n.f1.accept(this, null);
        if (r1 == null) return null;
        if (!ST.putClass(r1.getName(), new ClassInfo())){
            this.detectedSemanticError = true;
            this.errorMsg = SemanticErrors.duplicateClass(r1.getName(), r1.getBeginLine());
            return null;
        }
        n.f2.accept(this, null);
        n.f3.accept(this, new VisitorParameterInfo(r1.getName(), "class"));
        n.f4.accept(this, new VisitorParameterInfo(r1.getName(), "class"));
        n.f5.accept(this, null);
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
    public VisitorReturnInfo visit(ClassExtendsDeclaration n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        n.f0.accept(this, null);
        VisitorReturnInfo r1 = n.f1.accept(this, null);
        if (r1 == null) return null;
        n.f2.accept(this, null);
        VisitorReturnInfo r3 = n.f3.accept(this, null);
        if (r3 == null) return null;

        ClassInfo motherClass = ST.lookupClass(r3.getName());
        if (motherClass == null){  // in "class B extends A", if A is not defined previously then error
            this.detectedSemanticError = true;
            this.errorMsg = SemanticErrors.extendingNonDefinedYet(r1.getName(), r3.getName(), r3.getBeginLine());
            return null;
        }
        if (!ST.putClass(r1.getName(), new ClassInfo(r3.getName(), motherClass))){
            this.detectedSemanticError = true;
            this.errorMsg = SemanticErrors.duplicateClass(r1.getName(), r1.getBeginLine());
            return null;
        }
        n.f4.accept(this, null);
        n.f5.accept(this, new VisitorParameterInfo(r1.getName(), r3.getName(), "class"));
        n.f6.accept(this, new VisitorParameterInfo(r1.getName(), r3.getName(), "class"));
        n.f7.accept(this, null);
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public VisitorReturnInfo visit(VarDeclaration n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        VisitorReturnInfo r0 = n.f0.accept(this, null);
        VisitorReturnInfo r1 = n.f1.accept(this, null);
        if (r0 == null || r1 == null) return null;
        boolean feedback;
        switch(argu.getType()){
            case "mainclass":
                feedback = ST.putMainVariable(r1.getName(),  new VariableInfo(r0.getType()));
                if (!feedback){
                    this.detectedSemanticError = true;
                    this.errorMsg = SemanticErrors.duplicateVarDeclaration(r1.getName(), r1.getBeginLine());
                    return null;
                }
                break;
            case "class":
                feedback = ST.putField(argu.getName(), r1.getName(), new VariableInfo(r0.getType()));
                if (!feedback){
                    this.detectedSemanticError = true;
                    this.errorMsg = SemanticErrors.duplicateFieldDeclaration(argu.getName(), r1.getName(), r1.getBeginLine());
                    return null;
                }
                break;
            case "method":
                feedback = ST.putVariable(argu.getSupername(), argu.getName(), r1.getName(), new VariableInfo(r0.getType()));
                if (!feedback){
                    this.detectedSemanticError = true;
                    this.errorMsg = SemanticErrors.duplicateVarDeclaration(argu.getSupername(), argu.getName(), r1.getName(), r1.getBeginLine());
                    return null;
                }
                break;
            default:
                System.err.println("Error: invalid type parameter in visit(VarDeclaration)! Please debug...");
                return null;
        }
        n.f2.accept(this, null);
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
    public VisitorReturnInfo visit(MethodDeclaration n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        n.f0.accept(this, null);
        VisitorReturnInfo r1 = n.f1.accept(this, null);
        VisitorReturnInfo r2 = n.f2.accept(this, null);
        if (r1 == null || r2 == null) return null;

        if (!ST.putMethod(argu.getName(), r2.getName(), new MethodInfo(r1.getType()))){
            this.detectedSemanticError = true;
            this.errorMsg = SemanticErrors.duplicateMethodDeclaration(argu.getName(), r2.getName(), r2.getBeginLine());
            return null;
        }

        n.f3.accept(this, null);
        n.f4.accept(this, new VisitorParameterInfo(r2.getName(), argu.getName(),"method"));
        n.f5.accept(this, null);
        n.f6.accept(this, null);
        n.f7.accept(this, new VisitorParameterInfo(r2.getName(), argu.getName(),"method"));
        //n.f8.accept(this, null);   // no need to check statements
        n.f9.accept(this, null);
        n.f10.accept(this, null);
        n.f11.accept(this, null);
        n.f12.accept(this, null);

        // (!) Have to check after method declaration is added to SymbolTable so that it has all the arguments, etc
        if (!SemanticChecks.checkThatIfOverrideThenCorrect(ST, argu.getName(), r2.getName())){
            this.detectedSemanticError = true;
            this.errorMsg = SemanticErrors.invalidOverride(argu.getName(), r2.getName(), r2.getBeginLine());
            return null;
        }

        return null;
    }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    public VisitorReturnInfo visit(FormalParameterList n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public VisitorReturnInfo visit(FormalParameter n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        VisitorReturnInfo r0 = n.f0.accept(this, null);
        VisitorReturnInfo r1 = n.f1.accept(this, null);
        if (r0 == null || r1 == null) return null;
        boolean feedback = ST.putArgument(argu.getSupername(), argu.getName(), r1.getName(), new VariableInfo(r0.getType()));
        if (!feedback){
            this.detectedSemanticError = true;
            this.errorMsg = SemanticErrors.duplicateFormalParameter(argu.getSupername(), argu.getName(), r1.getName(), r1.getBeginLine());
            return null;
        }
        return null;
    }

    /**
    * f0 -> ( FormalParameterTerm() )*
    */
    public VisitorReturnInfo visit(FormalParameterTail n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        n.f0.accept(this, argu);
        return null;
    }

    /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
    public VisitorReturnInfo visit(FormalParameterTerm n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        n.f0.accept(this, null);
        n.f1.accept(this, argu);
        return null;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public VisitorReturnInfo visit(Type n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        return n.f0.accept(this, new VisitorParameterInfo(null, null, null, "getTypeEnum"));  // getTypeEnum is used in Identifier()'s visit() for custom types
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public VisitorReturnInfo visit(ArrayType n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        n.f2.accept(this, null);
        return new VisitorReturnInfo(MiniJavaType.INTARRAY);
    }

    /**
    * f0 -> "boolean"
    */
    public VisitorReturnInfo visit(BooleanType n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        return new VisitorReturnInfo(MiniJavaType.BOOLEAN);
    }

    /**
    * f0 -> "int"
    */
    public VisitorReturnInfo visit(IntegerType n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        return new VisitorReturnInfo(MiniJavaType.INTEGER);
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public VisitorReturnInfo visit(Identifier n, VisitorParameterInfo argu) {
        if (detectedSemanticError) return null;
        if (argu != null && argu.getPurpose().equals("getTypeEnum")) return new VisitorReturnInfo(n.f0.toString(), new MiniJavaType(TypeEnum.CUSTOM, n.f0.toString()), n.f0.beginLine);
        else return new VisitorReturnInfo(n.f0.toString(), null, n.f0.beginLine);
    }

}
