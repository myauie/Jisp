package com.ljc;

import com.ljc.JispCompiler.CompileFlags;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.DoublePredicate;
import java.util.stream.IntStream;

import static java.lang.Math.*;
import static java.util.stream.Collectors.toCollection;

class JispBuiltinFunction {

    static Object jisp_add(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be added");
            return null;
        }

        if (vals.stream().anyMatch(i -> i instanceof Double))
            return vals.stream()
                    .mapToDouble(f -> ((Number) f).doubleValue()).sum();
        else
            return vals.stream()
                    .mapToInt(v -> (Integer) v).sum();
    }

    static Object jisp_divide(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be divided");
            return null;
        }

        Object first = vals.remove(0);
        if (vals.isEmpty()) {
            return (first instanceof Double) ?
                    (Double) first :
                    (Integer) first;
        }

        if ((first instanceof Double) || vals.stream().anyMatch(i -> i instanceof Double))
            return vals.stream()
                    .mapToDouble(f -> ((Number) f).doubleValue())
                    .reduce(((Number) first).doubleValue(), (a, b) -> a / b);
        else
            return vals.stream()
                    .mapToInt(v -> (Integer) v)
                    .reduce((Integer) first, (a, b) -> a / b);
    }

    static Object jisp_equals(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be compared");
            return null;
        }

        Double match = ((Number) vals.remove(0)).doubleValue();
        DoublePredicate equ = match::equals;
        return vals.stream()
                .mapToDouble(f -> ((Number) f).doubleValue())
                .allMatch(equ) ?
                "T" :
                null;
    }

    static Object jisp_lessthan(ArrayList items, JispInterp context) {
        /* Check if all provided numbers are in sorted ascending order */

        items.remove(0); // 0th parm == function name
        if (items.size() < 1) {
            System.err.println("ERROR: operation requires at least one number");
            return null;
        }

        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be compared");
            return null;
        }

        double[] prev = new double[1];
        prev[0] = ((Number) vals.remove(0)).doubleValue();

        boolean res = vals.stream()
                .mapToDouble(f -> ((Number) f).doubleValue()) // compare all as doubles
                .allMatch(f -> {
                    boolean b = f > prev[0];
                    prev[0] = f;
                    return b;
                });

        return res ?
                "T" :
                null;
    }

    static Object jisp_lessthanorequal(ArrayList items, JispInterp context) {
        /* <= function compares numbers according to "less than or equal" predicate.
        Each (overlapping) pair of the numbers is compared by it.
        The result is true if all compared pairs satisfy comparison. */

        items.remove(0); // 0th parm == function name
        if (items.size() < 1) {
            System.err.println("ERROR: operation requires at least one number");
            return null;
        }

        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be compared");
            return null;
        }

        double[] prev = new double[1];
        prev[0] = ((Number) vals.remove(0)).doubleValue();

        boolean res = vals.stream()
                .mapToDouble(f -> ((Number) f).doubleValue()) // compare all as doubles
                .allMatch(f -> {
                    boolean b = f >= prev[0];
                    prev[0] = f;
                    return b;
                });

        return res ?
                "T" :
                null;
    }

    static Object jisp_mult(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be multiplied");
            return null;
        }

        if (vals.stream().anyMatch(i -> i instanceof Double))
            return vals.stream()
                    .mapToDouble(f -> ((Number) f).doubleValue())
                    .reduce(1.0, (a, b) -> a * b);
        else
            return vals.stream()
                    .mapToInt(v -> (Integer) v)
                    .reduce(1, (a, b) -> a * b);
    }

    static Object jisp_morethan(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name

        if (items.size() < 1) {
            System.err.println("ERROR: operation requires at least one number");
            return null;
        }

        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be compared");
            return null;
        }

        double[] prev = new double[1];
        prev[0] = ((Number) vals.remove(0)).doubleValue();

        boolean res = vals.stream()
                .mapToDouble(f -> ((Number) f).doubleValue()) // compare all as doubles
                .allMatch(f -> {
                    boolean b = f < prev[0];
                    prev[0] = f;
                    return b;
                });

        return res ?
                "T" :
                null;
    }

    static Object jisp_morethanorequal(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name

        if (items.size() < 1) {
            System.err.println("ERROR: operation requires at least one number");
            return null;
        }

        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be compared");
            return null;
        }

        double[] prev = new double[1];
        prev[0] = ((Number) vals.remove(0)).doubleValue();

        boolean res = vals.stream()
                .mapToDouble(f -> ((Number) f).doubleValue()) // compare all as doubles
                .allMatch(f -> {
                    boolean b = f <= prev[0];
                    prev[0] = f;
                    return b;
                });

        return res ?
                "T" :
                null;
    }

    static Object jisp_negateequals(ArrayList items, JispInterp context) {
        /* /= function compares numbers according to "equal" predicate.
        Result is true if no two numbers are equal to each other,
        otherwise result is false. Note that only two argument version
        result is negation of = function, that is (/= a b) is same as (not (= a b)). */

        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be compared");
            return null;
        }

        int v = vals.size();
        for (int i = 0; i < v; i++) {
            double cur = ((Number) vals.remove(0)).doubleValue();
            DoublePredicate equ = t -> t == cur;

            // needs to think equivalent ints and doubles are the same, eg 2 and 2.0
            long t = vals.stream()
                    .mapToDouble(f -> ((Number) f).doubleValue())
                    .filter(equ)
                    .count();
            if (t > 0)
                return null;
        }
        return "T";
    }

    static Object jisp_oneplus(ArrayList items, JispInterp context) {
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 2 got " + Integer.toString(items.size()));
            return null;
        }
        items.add(1);
        return jisp_add(items, context);
    }

    static Object jisp_oneminus(ArrayList items, JispInterp context) {
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 2 got " + Integer.toString(items.size()));
            return null;
        }
        items.add(1);
        return jisp_subtract(items, context);

    }

    static Object jisp_subtract(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be subtracted");
            return null;
        }

        Object first = vals.remove(0);
        if (vals.isEmpty()) {
            return (first instanceof Double) ?
                    -(Double) first :
                    -(Integer) first;
        }

        if ((first instanceof Double) || vals.stream().anyMatch(i -> i instanceof Double))
            return vals.stream()
                    .mapToDouble(f -> ((Number) f).doubleValue())
                    .reduce(((Number) first).doubleValue(), (a, b) -> a - b);
        else
            return vals.stream()
                    .mapToInt(v -> (Integer) v)
                    .reduce((Integer) first, (a, b) -> a - b);
    }

    static Object AND(ArrayList items, JispInterp context) {
        // Logical AND, Value from the first form that decides result is returned
        items.remove(0); // 0th parm == function name
        if (items.stream()
                .map(context::eval)
                .anyMatch(Objects::isNull))
            return null;
        else {
            int itemsSize = items.size() - 1;
            return context.eval(items.get(itemsSize));
        }
    }

    static Object ATOM(ArrayList items, JispInterp context) {
        // returns true if the argument is not a cons cell, otherwise it returns false
        items.remove(0); // 0th parm == function name
        if (items.size() > 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " + Integer.toString(items.size()));
            return null;
        }
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if ((vals.get(0) instanceof ArrayList)) {
            return ((ArrayList) vals.get(0)).isEmpty() ?
                    "T" :
                    null;
        }
        return "T";
    }

    static Object OR(ArrayList items, JispInterp context) {
        // evaluates forms, returns first item which does not eval to nil
        items.remove(0); // 0th parm == function name

        for (int i = 0; i < items.size(); i++) {
            items.set(i, context.eval(items.get(i)));
            if (items.get(i) != null)
                return items.get(i);
        }
        return null;
    }

    static Object NOT(ArrayList items, JispInterp context) {
        // if parameter is null, return True; else return null
        items.remove(0); // 0th parm == function name
        return (context.eval(items.get(0)) == null) ? true : null;
    }

    static Object CAR(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() > 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " + Integer.toString(items.size()));
            return null;
        }
        Object cons = context.eval(items.get(0));
        if (cons instanceof JispArrayList)
            return context.eval(((JispArrayList) cons).car());
        if (cons instanceof ArrayList) {
            if (((ArrayList) cons).isEmpty())
                return null;
            return new JispArrayList(((ArrayList) cons).get(0));
        }
        return null;
    }

    static Object FIRST(ArrayList items, JispInterp context) {
        return CAR(items, context);
    }

    static Object CDR(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() > 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " + Integer.toString(items.size()));
            return null;
        }
        Object cons = context.eval(items.get(0));
        if (cons == null)
            return null;
        if ((cons instanceof ArrayList) && (((ArrayList) cons).isEmpty()))
            return null;
        if (!(cons instanceof ArrayList)) {
            System.err.println("ERROR: CDR requires cons parameter");
            return null;
        }
        if (cons instanceof JispArrayList)
            return ((JispArrayList) cons).cdr();
        ((ArrayList) cons).remove(0);
        return new JispArrayList(cons);
    }

    static Object REST(ArrayList items, JispInterp context) {
        return CDR(items, context);
    }

    static Object COMPILE(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name

        JispCompiler com = new JispCompiler();
        Object func;
        String name;

        if (items.get(0) instanceof String) {
            name = items.get(0).toString().toUpperCase();
            func = context.getSymbols().get(name);
        } else {
            Object ev = context.eval(items.get(0));
            if (ev instanceof JispFunction) {
                name = null;
                func = (JispFunction) ev;
            } else {
                System.err.println("ERROR: must provide a function name or definition");
                return null;
            }
        }

        EnumSet<CompileFlags> options = EnumSet.noneOf(CompileFlags.class);
        while (!items.isEmpty()) {
            Object x = items.remove(0);
            if (x instanceof String) {
                switch ((String) x) {
                    case "debug":
                        options.add(CompileFlags.DEBUG);
                        break;

                    case "name":
                        options.add(CompileFlags.PATH);
                        name = items.remove(0).toString();
                        break;

                    default:
                        break;
                }
            }
        }
        if (func instanceof JispFunction)
            return com.compile(name, (JispFunction) func, options);
        else {
            System.err.println("ERROR: " + items.get(0) + " not an interpreted function\n");
            return null;
        }
    }

    static Object COND(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        System.out.println(items);

        int i = items.size() - 1;
        System.out.println("bindings size: " + i);

        while (i >= 0) {
            System.out.println(items);
            ArrayList variant = (ArrayList) items.remove(0);
            int variantSize = variant.size();

            if ((context.eval(variant.get(0)) != null)) {
                return context.eval(variant.get((variantSize > 1) ? 1 : 0));
            }
            i--;
        }
        return null;
    }

    static Object CONS(ArrayList items, JispInterp context) {
        /* A list is a series of cons cells, linked together so that the CDR
        slot of each cons cell holds either the next cons cell or the empty list.
        The empty list == the symbol nil. */

        items.remove(0); // 0th parm == function name
        if (items.size() < 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 2 got " +
                    Integer.toString(items.size()));
            return null;
        }

        JispArrayList ret = new JispArrayList();
        Object cons0 = context.eval(items.get(0));
        Object cons1 = context.eval(items.get(1));

        if (cons1 == null) {
            ret.add(cons0);
            return ret;
        }
        if ((cons1 instanceof ArrayList)) {
            // b.add(0, A), return B
            ((ArrayList) cons1).add(0, cons0);
            return cons1;
        }
        ret.add(cons0);
        //if (!(cons1 == null))
        ret.add(cons1);
        return ret;
    }

    static Object COS(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }

        return StrictMath.cos((items.get(0) instanceof Double) ?
                (Double) items.get(0) :
                (Integer) items.get(0));
    }

    static Object copy(Object f) {
        if (f instanceof ArrayList) {
            ArrayList list = (ArrayList) f;
            return list.stream()
                    .map(JispBuiltinFunction::copy)
                    .collect(toCollection(ArrayList::new));
        }
        return f;
    }

    static Object DEFUN(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 3) {
            System.err.println("ERROR: incorrect number of parameters, expected 3 got " +
                    Integer.toString(items.size()));
            return null;
        }
        String symbol;
        if (items.get(0) instanceof String)
            symbol = ((String) items.get(0)).toUpperCase();
        else {
            System.err.println("ERROR: " + items.get(0).toString() + "not a string");
            return null;
        }
        try {
            Class direct = Class.forName(symbol, true, JispCompiler.loader);
            System.err.println("ERROR: cannot redefine a function after compile");
            return null;
        } catch (ClassNotFoundException ignored) {
        }
        ArrayList<Object> all = (ArrayList<Object>) items.get(1);
        ArrayList<String> parm = new ArrayList<>();
        ArrayList<String> optional = new ArrayList<>();

        boolean opt = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof ArrayList) {
                String name = (String) ((ArrayList) all.get(i)).get(0);
                Object val = ((ArrayList) all.get(i)).get(1);
                context.getSymbols().put(name.toUpperCase(), val);
                all.set(i, name);
            }

/*          if (all.get(i).equals("&rest")) {

            } else if (all.get(i).equals("&key")) {
*/
            if (all.get(i).equals("&optional")) {
                opt = true;
            } else if (!opt) {
                parm.add((String) all.get(i));
            } else {
                optional.add((String) all.get(i));
            }
        }

        ArrayList func = (ArrayList) items.get(2);
        JispFunction newfunc = new JispFunction(parm, optional, func);
        context.getSymbols().put(symbol, newfunc);
        return symbol;
    }

    static Object DOTIMES(ArrayList items, JispInterp context) {
        /* loop i times:  dolist (v i) (b)
            v items(0, 0) store name in locals, bind with i
            i items(0, 1) integer iterator
            b items(1) body to execute */

        // b.add(0, A), return B


        items.remove(0); // 0th parm == function name
        if (items.size() < 2) {
            System.err.println("ERROR: incorrect number of parameters, expected at least 2 got " +
                    Integer.toString(items.size()));
            return null;
        }
        if (!(items.get(0) instanceof ArrayList)) {
            System.err.println("ERROR: first parameter must be list");
            return null;
        }
        ArrayList p1 = (ArrayList) items.remove(0);
        if (p1.size() != 2) {
            System.err.println("ERROR: incorrect number of variable parameters, expected 2 got " +
                    Integer.toString(items.size()));
            return null;
        }
        Object s1 = p1.remove(0);
        Object s2 = context.eval(p1.remove(0));
        if ((s1 instanceof String)) {
            if ((s2 instanceof Integer)) {
                String v = (String) s1;
                Integer i = (Integer) s2;
                IntStream.range(0, i).forEach(x -> {
                    context.setLocal(v, x);
                    items.forEach(context::eval);
                });
                return null;
            } else {
                System.err.println("ERROR: variable parameter 2 is not an integer");
                return null;
            }
        } else {
            System.err.println("ERROR: variable parameter 1 is not a string");
            return null;
        }
    }

    static Object DOLIST(ArrayList items, JispInterp context) {
        /*  loop through l: dolist (v l) (b)
            v items(0, 0) store name in locals, bind with l
            l items(0, 1) list to iterate through
            b items(1) body to execute */

        items.remove(0); // 0th parm == function name
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 2 got " +
                    Integer.toString(items.size()));
            return null;
        }

        if (!(items.get(0) instanceof ArrayList)) {
            System.err.println("ERROR: first parameter must be list");
            return null;
        }

        ArrayList p1 = (ArrayList) items.get(0);
        if (p1.size() != 2) {
            System.err.println("ERROR: incorrect number of variable parameters, expected 2 got " +
                    Integer.toString(items.size()));
            return null;
        }

        Object s1 = p1.get(0);
        Object s2 = context.eval(p1.get(1));
        if ((s1 instanceof String)) {
            if ((s2 instanceof ArrayList)) {
                String v = (String) s1;
                ArrayList l = (ArrayList) s2;
                l.forEach(x -> {
                    context.setLocal(v, x);
                    context.eval(items.get(1));
                });
                return null;
            } else {
                System.err.println("ERROR: variable parameter 2 is not a list");
                return null;
            }
        } else {
            System.err.println("ERROR: variable parameter 1 is not a string");
            return null;
        }
    }

    static Object DO(ArrayList items, JispInterp context) {
        /* loop to process multiple lists: do ((v s i)...) (p) [b]...[b])
           v items(0) list of lists, containing variables to update.
           Each sublist is:
                v store name in locals
                s starting value
                i increment body, re-evaluate i each iteration for new value
           p items(1) predicate, loop until null
           b items(2...n) bodies to execute */

        items.remove(0); // 0th parm == function name
        if (items.size() < 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 2 got " +
                    Integer.toString(items.size()));
            return null;
        }

        if (!items.stream().allMatch(x -> x instanceof ArrayList)) {
            System.err.println("ERROR: all parameters must be lists");
            return null;
        }

        ArrayList v = (ArrayList) items.remove(0);
        if (!v.stream().allMatch(x -> x instanceof ArrayList)) {
            System.err.println("ERROR: all variable declarations must be lists");
            return null;
        }

        if (!v.stream().allMatch(x -> ((ArrayList) x).get(0) instanceof String)) {
            System.err.println("ERROR: all variable names must be Strings");
            return null;
        }

        // first part
        v.forEach(x -> {
            ArrayList vn = (ArrayList) x;
            String s = (String) vn.get(0);
            context.setLocal(s, context.eval(vn.get(1)));
        });

        // second part
        Object p = items.remove(0);
        while (context.eval(p) != null) {
            items.forEach(context::eval);
            v.forEach(x -> {
                ArrayList vn = (ArrayList) x;
                String s = (String) vn.get(0);
                context.setLocal(s, context.eval(vn.get(2)));
            });
        }
        return null;
    }

    static Object EQ(ArrayList items, JispInterp context) {
        /* TODO: fix this
            input one, output negation
            object identity. It works for symbols and identical objects.
            If same object, return T  */

        items.remove(0); // 0th parm == function name
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        return (Objects.equals(items.get(0), items.get(1))) ?
                "T" :
                null;
    }

    static Object EQL(ArrayList items, JispInterp context) {
        /* compares object identity, numbers and characters.
           Numbers are considered as equal only when they have the both same value and type.
           Result is true if they are same, otherwise false. */

        items.remove(0); // 0th parm == function name
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));

        if ((vals.get(0)).equals(vals.get(1)))
            return ((vals.get(0) instanceof ArrayList) || ((vals.get(1) instanceof ArrayList))) ?
                    null :
                    "T";
        return null;
    }

    static Object EQUAL(ArrayList items, JispInterp context) {
        /* compares same things as eql,
           additionally result is true under some other situations: conses are compared
           recursively (in both car and cdr part), string and bit-vectors are compared element-wise.
           Result is true if they are same, otherwise false. */

        items.remove(0); // 0th parm == function name
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }

        return (items.get(0)).equals(items.get(1)) ?
                "T" :
                null;
    }

    static Object FORMAT(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        String dest = ((String) items.remove(0)).toUpperCase();
        String fmt = (String) items.remove(0);
        StringBuilder res = new StringBuilder();
        for (int ch = 0; ch < fmt.length(); ch++) {
            if (fmt.charAt(ch) == '~') {
                char i = Character.toUpperCase(fmt.charAt(ch + 1));
                if ((i == 'A') || (i == 'D')) {
                    Object ob = items.remove(0);
                    if (ob != null)
                        res.append(context.eval(ob).toString());
                } else if (i == '%') {
                    res.append("\n");
                }
                ch++;
            } else {
                res.append(fmt.charAt(ch));
            }
        }
        if (dest.equals("E") || dest.equals("T")) {
            System.err.print(res);
            return null;
        }
        return "\"" + res + "\"";
    }

    static Object IF(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 3) {
            System.err.println("ERROR: incorrect number of parameters, expected 3 got " +
                    Integer.toString(items.size()));
            return null;
        }
        return context.eval(items.get((context.eval(items.get(0)) != null) ?
                1 :
                2));
    }

    static Object LAMBDA(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 3 got " +
                    Integer.toString(items.size()));
            return null;
        }

        ArrayList<Object> all = (ArrayList<Object>) items.get(0);
        ArrayList<String> parm = new ArrayList<>();
        ArrayList<String> optional = new ArrayList<>();

        boolean opt = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) instanceof ArrayList) {
                String name = (String) ((ArrayList) all.get(i)).get(0);
                Object val = ((ArrayList) all.get(i)).get(1);
                context.getSymbols().put(name.toUpperCase(), val);
                all.set(i, name);
            }

/*            if (all.get(i).equals("&rest")) {

            } else if (all.get(i).equals("&key")) {
*/

            if (all.get(i).equals("&optional")) {
                opt = true;
            } else if (!opt) {
                parm.add((String) all.get(i));
            } else {
                optional.add((String) all.get(i));
            }
        }

        ArrayList func = (ArrayList) items.get(1);
        return new JispFunction(parm, optional, func);
    }

    static Object LET(ArrayList items, JispInterp context) {
        /* Bindings are described in two element lists where the first element specifies name and the second is code
           to compute its value, or single variable without default initialization.
           final element is body: program code in which definitions above are effective, implicit progn */

        items.remove(0); // 0th parm == function name
        ArrayList bindings = (ArrayList) items.remove(0);

        System.out.println(bindings.size());
        for (Object binding : bindings) {
            String key;
            Object value = null;

            if (binding instanceof ArrayList) {
                key = (String) ((ArrayList) binding).get(0);
                value = context.eval(((ArrayList) binding).get(1));
            } else {
                key = (String) binding;
                value = "nil";
            }
            context.setGlobal(key, value);
        }

        // check if there is body
        return items.isEmpty() ?
                null :
                context.eval(items.get(0));
    }

    static Object LIST(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval).collect(toCollection(ArrayList::new));

        if (vals.isEmpty())
            return null;
        if (vals.size() == 1)
            return new JispArrayList(vals.get(0));
        Object n1 = vals.remove(0);
        return new JispArrayList(n1, vals);
    }

    static Object LOAD(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }

        Object target = context.eval(items.get(0));
        if (!(target instanceof String)) {
            System.err.println("ERROR: parameter 1 not a string");
            return null;
        }

        String fn = (String) target;
        JispParser parser = new JispParser();

        try {
            String line = new String(Files.readAllBytes(Paths.get(fn)));
            ArrayList res = (ArrayList) parser.interpret(line);
            for (Object i : res) {
                Object result = context.eval(i);
            }
        } catch (IOException x) {
            System.out.println(x.getMessage());
            return null;
        }
        return true;
    }

    static Object MAX(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be compared");
            return null;
        }
        return vals.stream().anyMatch(i -> (i instanceof Double)) ?
                vals.stream()
                        .mapToDouble(f -> ((Number) f).doubleValue())
                        .max().getAsDouble() :
                vals.stream()
                        .mapToInt(f -> ((Number) f).intValue())
                        .max().getAsInt();
    }

    static Object MIN(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream()
                .map(context::eval)
                .collect(toCollection(ArrayList::new));
        if (vals.stream()
                .anyMatch(i -> !((i instanceof Integer) || (i instanceof Double)))) {
            System.err.println("ERROR: only numbers can be compared");
            return null;
        }
        return vals.stream().anyMatch(i -> (i instanceof Double)) ?
                vals.stream()
                        .mapToDouble(f -> ((Number) f).doubleValue())
                        .min().getAsDouble() :
                vals.stream()
                        .mapToInt(f -> ((Number) f).intValue())
                        .min().getAsInt();
    }

    static Object MOD(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        ArrayList vals = (ArrayList) items.stream().map(context::eval).collect(toCollection(ArrayList::new));
        if (vals.stream().anyMatch(i -> !(i instanceof Integer))) {
            System.err.println("ERROR: modulus only works for integer values");
            return null;
        }

        return (Integer) vals.get(0) % (Integer) vals.get(1);
    }

    static Object QUIT(ArrayList items, JispInterp context) {
        Main.running = false;
        return null;
    }

    static Object QUOTE(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        return items.get(0);
    }

    static Object RANDOM(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        Random random = new Random();
        if (items.stream().anyMatch(i -> i instanceof Double))
            return (random.nextDouble() * (Double) context.eval(items.get(0)));
        else
            return random.nextInt((Integer) context.eval(items.get(0)));
    }

    static Object ROUND(ArrayList items, JispInterp context) {
        /* TODO: fix this
           first: quotient (integer), truncating toward nearest even integer
           second: remainder (integer or double)
           remainder: number - (quotient * divisor) */

        items.remove(0); // 0th parm == function name
        if (items.size() > 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 or 2, got " + Integer.toString(items.size()));
            return null;
        }
        int quotient;
        Double remainder;
        if (items.stream().anyMatch(i -> i instanceof Double)) {
            if (items.size() == 1)
                items.add(1d);
            quotient = toIntExact(round((Double) items.get(0) / (Double) items.get(1)));
            remainder = (Double) items.get(0) - (round((Double) items.get(0) / (Double) items.get(1)) * (Double) items.get(1));
        } else {
            if (items.size() == 1)
                items.add(1);
            quotient = round((int) items.get(0) / (int) items.get(1));
            remainder = (Double) items.get(0) - (round((int) items.get(0) / (int) items.get(1)) * (Double) items.get(1));
        }

        items.set(0, quotient);
        // remainder: number - (quotient * divisor)
        items.set(1, ((remainder % 1) == 0) ?
                ((int) items.get(0) - (quotient * (int) items.get(1))) :
                ((Double) items.get(0) - (quotient * (Double) items.get(1))));
        return items;
    }

    static Object SET(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 2 got " +
                    Integer.toString(items.size()));
            return null;
        }
        String key = (String) items.get(0);
        Object value = context.eval(items.get(1));
        context.setGlobal(key, value);
        return value;
    }

    static Object SIN(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        return StrictMath.sin((items.get(0) instanceof Double) ?
                (Double) items.get(0) :
                (Integer) items.get(0));
    }

    static Object SQRT(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        double num = ((Number) items.remove(0)).doubleValue();
        return sqrt(num);
    }

    static Object TYPEOF(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        Object res = context.eval(items.get(0));
        return (res == null) ?
                null :
                res.getClass().toString();
    }

    static Object ZEROP(ArrayList items, JispInterp context) {
        // returns true if argument is zero
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }

        return items.stream()
                .mapToDouble(f -> ((Number) f).doubleValue())
                .allMatch(t -> t == 0) ?
                "T" :
                null;
    }

    static Object PROGN(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        Object result = null;
        for (Object x : items)
            result = context.eval(x);
        return result;
    }

    static Object PUSH(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 2 got " +
                    Integer.toString(items.size()));
            return null;
        }

        Object item = context.eval(items.get(0));
        Object stack = context.getVariable((String) items.get(1));
        Object res = null;
        if (stack instanceof ArrayList) {
            res = copy(stack);
            ((ArrayList) res).add(0, item);
        } else {
            ArrayList newstack = new ArrayList();
            if (stack != null) newstack.add(stack);
            newstack.add(item);
            res = newstack;
        }
        context.setGlobal((String) items.get(1), res);
        return res;
    }

    static Object POP(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }

        Object stack = context.getVariable((String) items.get(0));
        Object res = null;
        if (stack instanceof ArrayList) {
            res = ((ArrayList) stack).isEmpty() ?
                    null :
                    ((ArrayList) stack).remove(0);
        } else {
            res = stack;
            stack = null;
        }
        context.setGlobal((String) items.get(0), stack);
        return res;
    }

    static Object STRING(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        return context.eval(items.get(0)).toString();
    }

    static Object STRING_UPCASE(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        return context.eval(items.get(0)).toString().toUpperCase();
    }

    static Object STRING_DOWNCASE(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        return context.eval(items.get(0)).toString().toLowerCase();
    }

    static Object ELT(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 2) {
            System.err.println("ERROR: incorrect number of parameters, expected 2 got " +
                    Integer.toString(items.size()));
            return null;
        }

        Object idx = context.eval(items.get(1));
        if (!(idx instanceof Integer)) {
            System.err.println("ERROR: index parameter must be Integer");
            return null;
        }
        Object seq = context.eval(items.get(0));
        if (seq instanceof String) {
            return ((String) seq).charAt((Integer) idx);
        } else if (seq instanceof ArrayList) {
            return ((ArrayList) seq).get((Integer) idx);
        } else
            return null;
    }

    static Object NULL(ArrayList items, JispInterp context) {
        items.remove(0); // 0th parm == function name
        if (items.size() != 1) {
            System.err.println("ERROR: incorrect number of parameters, expected 1 got " +
                    Integer.toString(items.size()));
            return null;
        }
        Object res = context.eval(items.get(0));
        if (res instanceof Collection)
            return ((Collection) res).isEmpty() ?
                    true :
                    null;
        return res == null;
    }
}