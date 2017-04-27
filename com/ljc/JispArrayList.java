package com.ljc;

import java.util.ArrayList;
import java.util.Collection;

class JispArrayList extends ArrayList {

    JispArrayList() {
    }

    JispArrayList(Object first) {
        add(0, first);
    }

    JispArrayList(Object first, Object second) {
        add(0, first);
        if (second != null)
            if (second instanceof Collection)
                addAll((Collection) second);
            else
                add(second);
    }

    Object car() {
        return get(0);
    }

    Object cdr() {
        return subList(1, size());
    }

}