package lambdaflat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum ValueType {
    NUMBER, STRING, BOOL, NIL, ARRAY, FUNCTION, NATIVE_FN, LOG, OBJECT, CLASS
}

class ArrayObj {
    List<Value> elements = new ArrayList<>();
}

class LogObj {
    LogObj() {
    }
}

class ObjObj {
    Map<String, Value> fields = new HashMap<>();
    ClassObj klass;

    ObjObj() {
    }
}

class ClassObj {
    String name;
    ClassObj superclass;
    Map<String, FuncObj> methods = new HashMap<>();

    ClassObj(String name, ClassObj superclass) {
        this.name = name;
        this.superclass = superclass;
    }
}

class FuncObj {
    String name;
    List<String> params;
    FuncDecl declaration;
    Environment closureEnv;
}

@FunctionalInterface
interface NativeFn {
    Value apply(List<Value> args);
}

class Value {
    ValueType type;
    Object data;

    Value() {
        type = ValueType.NIL;
        data = null;
    }

    Value(double n) {
        type = ValueType.NUMBER;
        data = n;
    }

    Value(String s) {
        type = ValueType.STRING;
        data = s;
    }

    Value(boolean b) {
        type = ValueType.BOOL;
        data = b;
    }

    Value(ArrayObj arr) {
        type = ValueType.ARRAY;
        data = arr;
    }

    Value(FuncObj fn) {
        type = ValueType.FUNCTION;
        data = fn;
    }

    Value(NativeFn fn) {
        type = ValueType.NATIVE_FN;
        data = fn;
    }

    Value(LogObj _log) {
        type = ValueType.LOG;
        data = _log;
    }

    Value(ObjObj obj) {
        type = ValueType.OBJECT;
        data = obj;
    }

    Value(ClassObj cls) {
        type = ValueType.CLASS;
        data = cls;
    }

    public String toString() {
        switch (type) {
            case NUMBER: {
                double d = (Double) data;
                if (d == Math.floor(d) && Double.isFinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            }
            case STRING:
                return (String) data;
            case BOOL:
                return (Boolean) data ? "true" : "false";
            case NIL:
                return "nil";
            case ARRAY: {
                ArrayObj arr = (ArrayObj) data;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < arr.elements.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(arr.elements.get(i).toString());
                }
                sb.append("]");
                return sb.toString();
            }
            case FUNCTION: {
                FuncObj func = (FuncObj) data;
                return "<fn " + func.name + ">";
            }
            case NATIVE_FN:
                return "<native fn>";
            case LOG:
                return "<log>";
            case OBJECT: {
                ObjObj obj = (ObjObj) data;
                return "<instance of " + (obj.klass != null ? obj.klass.name : "Map") + ">";
            }
            case CLASS: {
                ClassObj cls = (ClassObj) data;
                return "<class " + cls.name + ">";
            }
        }
        return "<?>";
    }

    boolean isTruthy() {
        switch (type) {
            case NIL:
                return false;
            case BOOL:
                return (Boolean) data;
            default:
                return true;
        }
    }

    boolean equals(Value other) {
        if (type != other.type) return false;

        switch (type) {
            case NUMBER:
                return ((Double) data).equals((Double) other.data);
            case STRING:
                return ((String) data).equals(other.data);
            case BOOL:
                return ((Boolean) data).equals(other.data);
            case NIL:
                return true;
            case ARRAY: {
                ArrayObj a = (ArrayObj) data;
                ArrayObj b = (ArrayObj) other.data;
                if (a.elements.size() != b.elements.size()) return false;
                for (int i = 0; i < a.elements.size(); i++) {
                    if (!a.elements.get(i).equals(b.elements.get(i))) return false;
                }
                return true;
            }
            case FUNCTION:
                return data == other.data;
            case NATIVE_FN:
                return false;
            case LOG:
                return true;
            case OBJECT:
                return data == other.data;
            case CLASS:
                return data == other.data;
        }
        return false;
    }
}
