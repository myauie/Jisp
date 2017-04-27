package com.ljc;

import java.util.ArrayList;

class JispFunction {

    private ArrayList<String> parm;
    private ArrayList<String> opt;
    private Object func;

    JispFunction(ArrayList<String> parameters, ArrayList<String> optionalParameters, Object function) {
        parm = parameters;
        opt = optionalParameters;
        func = function;
    }

    JispFunction(ArrayList<String> parameters, Object function) {
        parm = parameters;
        func = function;
    }

    ArrayList<String> getParameters() {
        return parm;
    }

    ArrayList<String> getOptionalParameters() {
        return opt;
    }

    ArrayList<String> getAllParameters() {
        ArrayList<String> combined = new ArrayList<>();
        combined.addAll(parm);
        combined.addAll(opt);
        return combined;
    }

    Object getFunction() {
        return func;
    }
}