package com.ljc;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import static java.util.stream.Collectors.toCollection;

public class JispInterp {

    /* symbols contains functions with names. If the symbol is compiled, the call site
       associated with that function will be found here instead. */
    private static HashMap<String, Object> symbols;

    /* locals are current local values.
       saved_locals is a stack of caller's local values, saved when a function is called. */
    private static Stack<HashMap> saved_locals;
    private static HashMap<String, Object> locals;

    // global variables
    private static HashMap<String, Object> globals;

    /* names maps functions with names that we cannot use as Java method names */
    private static HashMap<String, String> names;

    static {
        symbols = new HashMap<>();
        globals = new HashMap<>();
        locals = new HashMap<>();
        globals.put("nil", null);
        saved_locals = new Stack<>();

        /* names list is for looking up symbol-based internals,
        since we can't easily have java functions with these names. */

        names = new HashMap<>();
        names.put("+", "jisp_add");
        names.put("-", "jisp_subtract");
        names.put("*", "jisp_mult");
        names.put("/", "jisp_divide");
        names.put("=", "jisp_equals");
        names.put("1+", "jisp_oneplus");
        names.put("1-", "jisp_oneminus");
        names.put("/=", "jisp_negateequals");
        names.put("<", "jisp_lessthan");
        names.put(">", "jisp_morethan");
        names.put("<=", "jisp_lessthanorequal");
        names.put(">=", "jisp_morethanorequal");
    }

    public static String getName(String name) {
        return names.containsKey(name) ?
                names.get(name) :
                name.toUpperCase().replace('-', '_');
    }

    public static CallSite bootstrapCompiledCall(Lookup caller, String name, MethodType type)
            throws NoSuchMethodException, IllegalAccessException {
        Lookup lookup = MethodHandles.lookup();
        Object o = symbols.get(name);
        if (o instanceof MutableCallSite) {
            return (MutableCallSite) o;
        }
        try {
            MethodHandle evaluate = lookup.findStatic(JispBuiltinFunction.class,
                    name, MethodType.methodType(Object.class,
                            new Class[]{ArrayList.class, JispInterp.class}));
            return new ConstantCallSite(evaluate);
        } catch (NoSuchMethodException ex) { // interpreted function
            MethodHandle evaluate = lookup.findStatic(JispInterp.class,
                    "evalFromCompiled", MethodType.methodType(Object.class,
                            new Class[]{ArrayList.class, JispInterp.class}));
            return new ConstantCallSite(evaluate);
        }
    }

    public static void setCallSite(String name, MethodHandle target) {
        Object sym = symbols.containsKey(name);
        if ((sym == null) || !(sym instanceof MutableCallSite)) {
            symbols.put(name, new MutableCallSite(target));
        } else {
            MutableCallSite ms = (MutableCallSite) symbols.get(name);
            ms.setTarget(target);
        }
    }

    // need this so a compiled function can call an interpreted one
    public static Object evalFromCompiled(ArrayList val, JispInterp context) {
        return context.eval(val);
    }

    public void saveLocals() {
        HashMap saved = new HashMap(locals);
        saved_locals.push(saved);
    }

    public void restoreLocals() {
        HashMap restored = saved_locals.pop();
        locals.clear();
        locals.putAll(restored);
    }

    public boolean variableIsSet(String variable) {
        String name = variable.toUpperCase();
        return locals.containsKey(name) || globals.containsKey(name);
    }

    public Object getVariable(String variable) {
        String name = variable.toUpperCase();
        if (locals.containsKey(name))
            return locals.get(name);
        return globals.getOrDefault(name, null);
    }

    public void setLocal(String variable, Object value) {
        locals.put(variable.toUpperCase(), value);
    }

    public void setGlobal(String variable, Object value) {
        globals.put(variable.toUpperCase(), value);
    }

    protected HashMap<String, Object> getSymbols() {
        return symbols;
    }

    private Object copy(Object f) {
        if (f instanceof JispArrayList) {
            JispArrayList list = (JispArrayList) f;
            return list.stream()
                    .map(this::copy)
                    .collect(toCollection(JispArrayList::new));
        } else
            return f;
    }

    private Object invokeInterpreted(JispFunction func, ArrayList list) {
        /* list size must be inbetween number required parms and number total parms (reqs and optionals)
           getParameters.size <= list.size <= getAllParameters.size */
        if ((func.getAllParameters().size() < list.size()) || (func.getParameters().size() > list.size())) {
            System.err.println("ERROR: wrong # of parameters, expected " +
                    Integer.toString(func.getParameters().size()) +
                    " got " + Integer.toString(list.size()));
            return null;
        }

        Object f = copy(func.getFunction());
        HashMap<String, Object> parms = new HashMap<>();

        for (String key : func.getParameters()) {
            Object val = eval(list.remove(0));
            parms.put(key.toUpperCase(), val);
        }

        int i = 0;
        while (i < func.getOptionalParameters().size()) {
            String key = func.getOptionalParameters().get(i);
            Object val = null;
            if (!list.isEmpty()) {
                val = eval(list.remove(0));
            }
            parms.put(key.toUpperCase(), val);
            i++;
        }
        saveLocals();
        locals.putAll(parms);
        Object result = eval(f);
        restoreLocals();
        return result;
    }

    private Object evalString(String s) {
        if (variableIsSet(s))
            return getVariable(s);
        if (s.toUpperCase().equals("T"))
            return "T";
        if (s.toUpperCase().equals("NIL"))
            return null;
        return s;
    }

    private Object evalInvokeSymbol(String fn, ArrayList list) {
        Object target = symbols.get(fn);
        if (target instanceof JispFunction) {
            list.remove(0);
            return invokeInterpreted((JispFunction) target, list);
        }
        if (target instanceof MutableCallSite) {
            try {
                saveLocals();
                Object retval = ((MutableCallSite) target).getTarget().invoke(copy(list), this);
                restoreLocals();
                return retval;
            } catch (Throwable error) {
                System.err.println("ERROR: " + error.getMessage());
                return null;
            }
        }
        // possibly a lambda in a variable
        target = getVariable(fn);
        if (target instanceof JispFunction) {
            JispFunction func = (JispFunction) target;
            list.remove(0);
            return invokeInterpreted((JispFunction) target, list);
        } else {
            System.err.println("ERROR:" + list.get(0) + " not a function");
            return null;
        }
    }

    private Object evalArrayList(ArrayList list) {
        if (list.isEmpty())
            return null;
        Object first = eval(list.get(0));

        if (first instanceof String) {
            String fn = getName((String) first);
            try { // check if builtin function
                Method eval = JispBuiltinFunction.class.getDeclaredMethod(fn, ArrayList.class, JispInterp.class);
                saveLocals();
                Object retval = eval.invoke(null, copy(list), this);
                restoreLocals();
                return retval;
            } catch (IllegalAccessException x) {
                System.err.println("ERROR: illegal access exception: " + x.getMessage());
                return null;
            } catch (InvocationTargetException ix) {
                System.err.println("ERROR: Invocation target exception: " + ix.getCause().getMessage());
                return null;
            } catch (NoSuchMethodException np) { // check if interpreted function
                return evalInvokeSymbol(fn, list);
            }
        }
        if (first instanceof JispFunction) {
            list.remove(0);
            return invokeInterpreted((JispFunction) first, list);
        }
        if (first instanceof JispCompiledFunction) {
            return ((JispCompiledFunction) first).evaluate(list, this);
        }
        System.err.println("ERROR: not a function");
        return null;
    }

    public Object eval(Object tk) {
        if (tk == null)
            return null;

        if (tk instanceof String)
            return evalString((String) tk);

        if (tk instanceof ArrayList)
            return evalArrayList((ArrayList) tk);

        return tk;
    }
}