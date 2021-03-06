package MiniJavaType;


public class MiniJavaType {
    private TypeEnum type = null;
    private String customTypeName = null;

    public MiniJavaType(TypeEnum _type){
        type = _type;
    }

    public MiniJavaType(String _customTypeName){
        type = TypeEnum.CUSTOM;
        customTypeName = _customTypeName;
    }

    public MiniJavaType(TypeEnum _type, String _customTypeName){
        if (_type == TypeEnum.CUSTOM && _customTypeName == null) {
            System.err.println("Warning: Custom type but not name given in MiniJavaType.MiniJavaType constructor");
        } else if (_type == TypeEnum.CUSTOM){
            type = TypeEnum.CUSTOM;
            customTypeName = _customTypeName;
        } else {
            type = _type;
        }
    }

    public TypeEnum getTypeEnum() { return type; }

    public String getCustomTypeName() { return customTypeName; }

    public boolean isCustom() { return type == TypeEnum.CUSTOM && customTypeName != null; }

    public boolean equals(MiniJavaType other){
        return (this.type == other.type && (this.type != TypeEnum.CUSTOM || (this.getCustomTypeName() != null && this.customTypeName.equals(other.getCustomTypeName()))));
    }

    public int getOffsetOfType(){
        if (type == TypeEnum.BOOLEAN) return 1;
        else if (type == TypeEnum.INTEGER) return 4;
        else if (type == TypeEnum.INTARRAY) return 8;
        else if (type == TypeEnum.CUSTOM) return 8;
        else return -1;
    }

    // DEBUG
    public String getDebugInfo(){
        if (type == null && customTypeName == null) return "VOID";
        else if (type == null) return "UNKNOWN";
        else return (type == TypeEnum.CUSTOM ? customTypeName : type.toString());
    }

    // CONSTANTS
    public static final MiniJavaType INTEGER = new MiniJavaType(TypeEnum.INTEGER);
    public static final MiniJavaType BOOLEAN = new MiniJavaType(TypeEnum.BOOLEAN);
    public static final MiniJavaType INTARRAY = new MiniJavaType(TypeEnum.INTARRAY);
    public static final MiniJavaType VOID = new MiniJavaType(null, null);

    // LLVM
    public String getLLVMType(){
        if (type == TypeEnum.CUSTOM) return "i8*";
        else if (type == TypeEnum.INTEGER) return "i32";
        else if (type == TypeEnum.INTARRAY) return "i32*";
        else if (type == TypeEnum.BOOLEAN) return "i1";
        else return "?";   // should not happen
    }

}
