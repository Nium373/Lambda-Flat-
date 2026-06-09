package lambdaflat;

import java.util.HashMap;
import java.util.Map;

class Binding {
    Value value;
    boolean isPublic;

    Binding(Value value, boolean isPublic) {
        this.value = value;
        this.isPublic = isPublic;
    }
}

class Environment {
    private Map<String, Binding> values = new HashMap<>();
    private Environment enclosing;

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Value value, boolean isPublic) {
        values.put(name, new Binding(value, isPublic));
    }

    void assign(String name, Value value) {
        if (values.containsKey(name)) {
            values.get(name).value = value;
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        values.put(name, new Binding(value, true));
    }

    Value get(String name) {
        Binding b = values.get(name);
        if (b != null) {
            return b.value;
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError("Undefined variable '" + name + "'.", 0);
    }
}
