package com.ljc;

import jas.*;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Stack;
import java.util.Vector;
import java.util.stream.Collectors;

import static jas.RuntimeConstants.*;

class JispCompiler {
    /* evaluate(ArrayList list, JispInterp context)
       opc_aload_0 local variable 0 list
       opc_aload_1 local variable 1 context */

    static JispClassLoader loader = new JispClassLoader(ClassLoader.getSystemClassLoader());
    private static int lambda_number = 0;
    private ArrayList<String> parms; // which array index each variable is at
    private int labelno;
    private int loopdepth;
    private Stack<String> states; // remembers states for stackmap

    JispCompiler() {
    }

    private void prepareClass(ClassEnv cf, String name) {
        cf.requireJava7();
        cf.setClass(new ClassCP(name));
        cf.setClassAccess((short) ACC_PUBLIC);
        cf.setSuperClass(new ClassCP("java/lang/Object"));

        cf.addBootstrapMethod(new MethodHandleCP(MethodHandleCP.STATIC_METHOD_KIND,
                        "com/ljc/JispInterp", "bootstrapCompiledCall",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"),
                new CP[0]);
    }

    Object compile(String functionName, JispFunction fx, EnumSet<CompileFlags> options) {
        ClassEnv cf = new ClassEnv();
        states = new Stack<>();
        labelno = 0;
        loopdepth = 2;
        int bsmno = 0;
        parms = fx.getParameters().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(ArrayList::new));
        String name = "JispLambda" + Integer.toString(lambda_number++);

        if (options.contains(CompileFlags.DEBUG)) {
            name = functionName.toUpperCase();
        }

        String dir = ".";
        if (options.contains(CompileFlags.PATH)) {
            File f = new File(functionName.replace('.', '/'));
            name = f.getName().toUpperCase();
            dir = f.getParent();
        }

        prepareClass(cf, name);

        CodeAttr func = new CodeAttr();
        StackMap sm = new StackMap(cf);
        func.setStackMap(sm);

        try {
            compileSubtree(fx.getFunction(), func, sm);
            func.addInsn(new Insn(opc_areturn));
            func.setStackSize((short) 120);
            func.setVarSize((short) 20);
        } catch (jas.jasError jasError) {
            System.err.println(jasError.getMessage());
            System.err.println("COMPILE FAILED.");
            return null;
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.getMessage());
            System.err.println("COMPILE FAILED.");
            return null;
        }

        Method evaluate = new Method((short) (ACC_PUBLIC | ACC_STATIC), new AsciiCP("evaluate"),
                new AsciiCP("(Ljava/util/ArrayList;Lcom/ljc/JispInterp;)Ljava/lang/Object;"),
                func, new ExceptAttr());
        cf.addMethod(evaluate);

        try {
            ByteArrayOutputStream classdata = new ByteArrayOutputStream();
            cf.write(new DataOutputStream(classdata));

            if (options.contains(CompileFlags.DEBUG) || options.contains(CompileFlags.PATH)) {
                File d = new File(dir);
                d.mkdirs();
                FileOutputStream debugOut = new FileOutputStream(dir + "/" + name + ".class");
                debugOut.write(classdata.toByteArray());
                debugOut.close();
            }

            Class jc = loader.loadJispClass(name, classdata.toByteArray());
            if (functionName == null) { // lambda returns class
                return (JispCompiledFunction) (items, context) -> {
                    try {
                        return jc.getDeclaredMethod("evaluate", new Class[]{ArrayList.class, JispInterp.class})
                                .invoke(null, items, context);
                    } catch (Throwable x) {
                        System.err.println("ERROR: error invoking lambda: " + x.getMessage());
                    }
                    return null;
                };
            } else {
                try {
                    MethodHandle target = MethodHandles.lookup().findStatic(jc, "evaluate",
                            MethodType.methodType(Object.class,
                                    new Class[]{ArrayList.class, JispInterp.class}));
                    JispInterp.setCallSite(functionName, target);
                    return true;
                } catch (Throwable ex) {
                    System.err.println("ERROR: compiler couldn't find it's own method. This should not happen.");
                    return null;
                }
            }
        } catch (IOException | jasError ex) {
            System.err.println("ERROR:" + ex.getMessage());
        }
        System.err.println("COMPILE FAILED.");
        return null;
    }

    private void compileSubtree(Object tree, CodeAttr fn, StackMap sm) throws Exception {
        try {
            if (tree instanceof Integer) {
                pushConstInt((Integer) tree, fn);
            } else if (tree instanceof Float) {
                pushConstFloat((float) tree, fn);
            } else if (tree instanceof String) {
                String s = (String) tree;
                if (parms.contains(s.toUpperCase())) {
                    pushParameter(parms.indexOf(s.toUpperCase()) + 1, fn);
                } else {
                    pushConstString(s, fn);
                }
            } else if (tree instanceof ArrayList) {
                String fname = ((String) ((ArrayList) tree).get(0)).toUpperCase();
                switch (fname) {
                    case "IF":
                        handleIfStatement((ArrayList) tree, fn, sm);
                        break;

                    case "QUOTE":
                        handleQuoteStatement((ArrayList) tree, fn, sm);
                        break;

                    case "DOTIMES":
                        handleDoTimesStatement((ArrayList) tree, fn, sm);
                        break;

                    case "DOLIST":
                        handleDoListStatement((ArrayList) tree, fn, sm);
                        break;

                    case "DO":
                        handleDoStatement((ArrayList) tree, fn, sm);
                        break;

                    case "PROGN":
                        handlePrognStatement((ArrayList) tree, fn, sm);
                        break;

                    default:
                        callSymbolTableFunction(fname, (ArrayList) tree, fn, sm);
                        break;
                }
            }
        } catch (jasError err) {
            err.printStackTrace();
        }
    }

    private void pushConstInt(Integer x, CodeAttr fn) throws jasError {
        fn.addInsn(new Insn(opc_new, new ClassCP("java/lang/Integer")));
        fn.addInsn(new Insn(opc_dup));
        fn.addInsn(new Insn(opc_ldc, new IntegerCP(x)));
        fn.addInsn(new Insn(opc_invokenonvirtual, new MethodCP("java/lang/Integer",
                "<init>", "(I)V")));
    }

    private void pushConstFloat(Float x, CodeAttr fn) throws jasError {
        fn.addInsn(new Insn(opc_new, new ClassCP("java/lang/Float")));
        fn.addInsn(new Insn(opc_ldc, new FloatCP(x)));
        fn.addInsn(new Insn(opc_invokenonvirtual, new MethodCP("java/lang/Float",
                "<init>", "(F)V")));
    }

    private void pushParameter(int parno, CodeAttr fn) throws jasError {
        fn.addInsn(new Insn(opc_aload_1));
        fn.addInsn(new Insn(opc_aload_0));
        fn.addInsn(new Insn(opc_ldc, new IntegerCP(parno)));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("java/util/ArrayList",
                "get", "(I)Ljava/lang/Object;")));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("com/ljc/JispInterp",
                "eval", "(Ljava/lang/Object;)Ljava/lang/Object;")));
    }

    private void pushConstString(String s, CodeAttr fn) throws jasError {
        fn.addInsn(new Insn(opc_ldc, new StringCP(s)));
    }

    private void quoteSubtree(Object tree, CodeAttr fn, StackMap sm) throws Exception {
        try {
            if (tree instanceof Integer) {
                pushConstInt((Integer) tree, fn);
            } else if (tree instanceof Float) {
                pushConstFloat((float) tree, fn);
            } else if (tree instanceof String) {
                String s = (String) tree;
                if (parms.contains(s)) {
                    pushParameter(parms.indexOf(s) + 1, fn);
                } else {
                    pushConstString(s, fn);
                }
            } else if (tree instanceof ArrayList) {
                fn.addInsn(new Insn(opc_new, new ClassCP("java/util/ArrayList")));
                fn.addInsn(new Insn(opc_dup));
                fn.addInsn(new Insn(opc_invokenonvirtual, new MethodCP("java/util/ArrayList", "<init>", "()V")));
                for (Object item : (ArrayList) tree) {
                    fn.addInsn(new Insn(opc_dup));
                    quoteSubtree(item, fn, sm);
                    fn.addInsn(new InvokeinterfaceInsn(new InterfaceCP("java/util/List", "add", "(Ljava/lang/Object;)Z"), 2));
                    fn.addInsn(new Insn(opc_pop)); //throw away the bool
                }
            }
        } catch (jasError err) {
            err.printStackTrace();
        }
    }

    private void handleQuoteStatement(ArrayList func, CodeAttr fn, StackMap sm) throws Exception {
        fn.addInsn(new Insn(opc_new, new ClassCP("java/util/ArrayList")));
        fn.addInsn(new Insn(opc_dup));
        fn.addInsn(new Insn(opc_invokenonvirtual, new MethodCP("java/util/ArrayList", "<init>", "()V")));
        for (Object aFunc : func) {
            fn.addInsn(new Insn(opc_dup));
            quoteSubtree(aFunc, fn, sm);
            fn.addInsn(new InvokeinterfaceInsn(new InterfaceCP("java/util/List", "add", "(Ljava/lang/Object;)Z"), 2));
            fn.addInsn(new Insn(opc_pop)); //throw away the bool
        }
        fn.addInsn(new Insn(opc_aload_1));
        fn.addInsn(new Insn(opc_invokedynamic,
                new InvokeDynamicCP("com/ljc/JispInterp", "bootstrapCompiledCall",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                        "QUOTE", "(Ljava/util/ArrayList;Lcom/ljc/JispInterp;)Ljava/lang/Object;", 0)));
    }

    private void callSymbolTableFunction(String fname, ArrayList func, CodeAttr fn, StackMap sm) throws Exception {
        String name = JispInterp.getName(fname);
        fn.addInsn(new Insn(opc_new, new ClassCP("java/util/ArrayList")));
        fn.addInsn(new Insn(opc_dup));
        fn.addInsn(new Insn(opc_invokenonvirtual, new MethodCP("java/util/ArrayList", "<init>", "()V")));
        for (Object aFunc : func) {
            fn.addInsn(new Insn(opc_dup));
            compileSubtree(aFunc, fn, sm);
            fn.addInsn(new InvokeinterfaceInsn(new InterfaceCP("java/util/List", "add", "(Ljava/lang/Object;)Z"), 2));
            fn.addInsn(new Insn(opc_pop)); //throw away the bool
        }
        fn.addInsn(new Insn(opc_aload_1));
        fn.addInsn(new Insn(opc_invokedynamic,
                new InvokeDynamicCP("com/ljc/JispInterp", "bootstrapCompiledCall",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                        name, "(Ljava/util/ArrayList;Lcom/ljc/JispInterp;)Ljava/lang/Object;", 0)));
    }

    private void handleIfStatement(ArrayList func, CodeAttr fn, StackMap sm) throws Exception {
        Label ifl = new Label("if" + Integer.toString(labelno));
        Label fil = new Label("fi" + Integer.toString(labelno));
        labelno++;
        compileSubtree(func.get(1), fn, sm);
        fn.addInsn(new Insn(opc_ifnull, ifl));
        compileSubtree(func.get(2), fn, sm); // true branch
        fn.addInsn(new Insn(opc_goto, fil));
        fn.addInsn(ifl);
        compileSubtree(func.get(3), fn, sm); // false branch
        fn.addInsn(fil);

        VerifyFrame vf = new VerifyFrame(new Vector());
        vf.setOffset(ifl);
        sm.addFrame(vf);

        VerifyFrame vf2 = new VerifyFrame(new Vector());
        vf2.setOffset(fil);
        vf2.addStackItem("Object", "java/lang/Object");
        sm.addFrame(vf2);
    }

    private void pushSaveLocals(CodeAttr fn) throws jasError {
        fn.addInsn(new Insn(opc_aload_1));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("com/ljc/JispInterp", "saveLocals", "()V")));
    }

    private void pushRestoreLocals(CodeAttr fn) throws jasError {
        fn.addInsn(new Insn(opc_aload_1));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("com/ljc/JispInterp", "restoreLocals", "()V")));
    }

    private void handleDoTimesStatement(ArrayList func, CodeAttr fn, StackMap sm) throws Exception {
        func.remove(0);
        ArrayList parms = (ArrayList) func.remove(0);
        Label loop = new Label("loop" + Integer.toString(labelno++));

        int iterator = loopdepth++;
        int stop = loopdepth++;
        states.push("Integer");
        states.push("Integer");

        pushSaveLocals(fn);
        fn.addInsn(new Insn(opc_iconst_0));
        fn.addInsn(new Insn(opc_istore, iterator));
        compileSubtree(parms.get(1), fn, sm);
        fn.addInsn(new Insn(opc_checkcast, new ClassCP("java/lang/Number")));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("java/lang/Number",
                "intValue", "()I")));
        fn.addInsn(new Insn(opc_istore, stop));

        // start of loop
        fn.addInsn(loop);
        fn.addInsn(new Insn(opc_aload_1));
        pushConstString((String) parms.get(0), fn);
        fn.addInsn(new Insn(opc_new, new ClassCP("java/lang/Integer")));
        fn.addInsn(new Insn(opc_dup));
        fn.addInsn(new Insn(opc_iload, iterator));
        fn.addInsn(new Insn(opc_invokenonvirtual, new MethodCP("java/lang/Integer",
                "<init>", "(I)V")));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("com/ljc/JispInterp",
                "setLocal", "(Ljava/lang/String;Ljava/lang/Object;)V")));
        for (Object x : func) {
            compileSubtree(x, fn, sm);
            fn.addInsn(new Insn(opc_pop)); //throw out the result
        }
        fn.addInsn(new IincInsn(iterator, 1));
        fn.addInsn(new Insn(opc_iload, iterator));
        fn.addInsn(new Insn(opc_iload, stop));
        fn.addInsn(new Insn(opc_if_icmplt, loop)); // if v1 < v2
        pushRestoreLocals(fn);
        fn.addInsn(new Insn(opc_aconst_null)); //returns null

        VerifyFrame vf = new VerifyFrame(new Vector());
        vf.setOffset(loop);
        vf.addLocalsItem("Object", "java/util/ArrayList");
        vf.addLocalsItem("Object", "com/ljc/JispInterp");
        for (String s : states) {
            if (Character.isUpperCase(s.charAt(0)))
                vf.addLocalsItem(s, null);
            else
                vf.addLocalsItem("Object", s);
        }
        sm.addFrame(vf);
        loopdepth -= 2;
    }

    private void handleDoListStatement(ArrayList func, CodeAttr fn, StackMap sm) throws Exception {
        func.remove(0);
        ArrayList parms = (ArrayList) func.remove(0);
        Label loop = new Label("loop" + Integer.toString(labelno++));

        int iterator = loopdepth++;
        states.push("java/util/ArrayList");

        pushSaveLocals(fn);
        compileSubtree(parms.get(1), fn, sm);
        fn.addInsn(new Insn(opc_checkcast, new ClassCP("java/util/ArrayList")));
        fn.addInsn(new Insn(opc_astore, iterator));

        fn.addInsn(loop);
        fn.addInsn(new Insn(opc_aload_1));
        pushConstString((String) parms.get(0), fn);
        fn.addInsn(new Insn(opc_aload, iterator));
        fn.addInsn(new Insn(opc_iconst_0));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("java/util/ArrayList",
                "remove", "(I)Ljava/lang/Object;")));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("com/ljc/JispInterp",
                "setLocal", "(Ljava/lang/String;Ljava/lang/Object;)V")));
        for (Object x : func) {
            compileSubtree(x, fn, sm);
            fn.addInsn(new Insn(opc_pop)); //throw out result
        }
        fn.addInsn(new Insn(opc_aload, iterator));
        fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("java/util/ArrayList",
                "isEmpty", "()Z")));
        fn.addInsn(new Insn(opc_ifeq, loop));
        pushRestoreLocals(fn);
        fn.addInsn(new Insn(opc_aconst_null)); //returns null

        VerifyFrame vf = new VerifyFrame(new Vector());
        vf.setOffset(loop);
        for (String s : states) {
            if (Character.isUpperCase(s.charAt(0)))
                vf.addLocalsItem(s, null);
            else
                vf.addLocalsItem("Object", s);
        }
        sm.addFrame(vf);
        loopdepth -= 2;
    }

    private void handleDoStatement(ArrayList func, CodeAttr fn, StackMap sm) throws Exception {
        func.remove(0);
        ArrayList parms = (ArrayList) func.remove(0);
        ArrayList pred = (ArrayList) func.remove(0);
        Label loop = new Label("loop" + Integer.toString(labelno++));

        pushSaveLocals(fn);
        // initial values
        for (Object x : parms) {
            ArrayList curvar = (ArrayList) x;
            fn.addInsn(new Insn(opc_aload_1));
            pushConstString((String) curvar.get(0), fn);
            compileSubtree(curvar.get(1), fn, sm); // calculate starting value
            fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("com/ljc/JispInterp",
                    "setLocal", "(Ljava/lang/String;Ljava/lang/Object;)V")));
        }

        // start of loop
        fn.addInsn(loop);
        for (Object x : func) {
            compileSubtree(x, fn, sm); // remaining forms left
            fn.addInsn(new Insn(opc_pop)); //throw out result
        }
        // update values
        for (Object x : parms) {
            ArrayList curvar = (ArrayList) x;
            fn.addInsn(new Insn(opc_aload_1));
            pushConstString((String) curvar.get(0), fn);
            compileSubtree(curvar.get(2), fn, sm); // calculate updated value
            fn.addInsn(new Insn(opc_invokevirtual, new MethodCP("com/ljc/JispInterp",
                    "setLocal", "(Ljava/lang/String;Ljava/lang/Object;)V")));
        }
        compileSubtree(pred, fn, sm); //continue until this returns null
        fn.addInsn(new Insn(opc_ifnonnull, loop));
        pushRestoreLocals(fn);
        fn.addInsn(new Insn(opc_aconst_null)); //returns null

        VerifyFrame vf = new VerifyFrame(new Vector());
        vf.setOffset(loop);
        for (String s : states) {
            if (Character.isUpperCase(s.charAt(0)))
                vf.addLocalsItem(s, null);
            else
                vf.addLocalsItem("Object", s);
        }
        sm.addFrame(vf);
    }

    private void handlePrognStatement(ArrayList func, CodeAttr fn, StackMap sm) throws Exception {
        func.remove(0);
        while (func.size() > 1) {
            compileSubtree(func.remove(0), fn, sm);
            fn.addInsn(new Insn(opc_pop)); //throw out the result
        }
        compileSubtree(func.remove(0), fn, sm);
    }

    public enum CompileFlags {
        DEBUG, PATH
    }
}
