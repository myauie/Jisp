package com.ljc;

class JispParser {

    JispParser() {
    }

    Object interpret(String text) {
        JispArrayList tokens = new JispArrayList();
        StringBuilder cur = new StringBuilder();
        boolean notQuoting = true;
        boolean notCommenting = true;

        for (int ch = 0; ch < text.length(); ch++) {
            if ((text.charAt(ch) == ')') || (text.charAt(ch) == '(') || (text.charAt(ch) == '\'')) {
                if (notQuoting) {
                    if (cur.length() > 0)
                        tokens.add(cur.toString());
                    cur = new StringBuilder(String.valueOf(text.charAt(ch)));
                    tokens.add(cur.toString());
                    cur = new StringBuilder();
                } else {
                    cur.append(text.charAt(ch));
                }

            } else if (text.charAt(ch) == ';') {
                if (notQuoting && notCommenting) {
                    if (cur.length() > 0)
                        tokens.add(cur.toString());
                    cur = new StringBuilder();
                    notCommenting = false;
                } else {
                    // if a quote or a comment
                    cur.append(";");
                }
                // whitespace
            } else if ((text.charAt(ch) == '\r') || (text.charAt(ch) == '\n')) {
                if (notQuoting && notCommenting) {
                    if (cur.length() > 0)
                        tokens.add(cur.toString());
                    cur = new StringBuilder();
                }
                notCommenting = true;
            } else if (text.charAt(ch) == '\t') {
                if (notQuoting && notCommenting) {
                    if (cur.length() > 0)
                        tokens.add(cur.toString());
                    cur = new StringBuilder();
                } else if (notCommenting)
                    cur.append("\t");
            } else if (text.charAt(ch) == ' ') {
                if (notQuoting && notCommenting) {
                    if (cur.length() > 0)
                        tokens.add(cur.toString());
                    cur = new StringBuilder();
                } else {
                    if (notCommenting)
                        cur.append(" ");
                }
            } else if (text.charAt(ch) == '"') {
                notQuoting = !notQuoting;
            } else {
                if (notCommenting)
                    cur.append(text.charAt(ch));
            }
        }
        if (cur.length() > 0)
            tokens.add(cur.toString());

        return tokenise(tokens);
    }

    private Object tokenise(JispArrayList tokens) {
        JispArrayList list = new JispArrayList();
        JispArrayList quote = null;

        while (!tokens.isEmpty()) {
            if (tokens.isEmpty()) {
                System.out.println("Syntax error");
                return null;
            }

            Object cur = tokens.remove(0);

            if (cur instanceof String) {
                String s = (String) cur;
                switch (s) {
                    case ")":
                        return list;
                    case "(":
                        if (quote == null) {
                            list.add(tokenise(tokens));
                        } else {
                            quote.add(tokenise(tokens));
                            list.add(quote);
                            quote = null;
                        }
                        break;
                    case "'":
                        quote = new JispArrayList();
                        quote.add("quote");
                        break;
                    default:
                        try { //converting it to an int constant
                            Integer i = Integer.parseInt(s);
                            if (quote == null) {
                                list.add(i);
                            } else {
                                quote.add(i);
                                list.add(quote);
                                quote = null;
                            }
                        } catch (NumberFormatException x) {
                            try { //maybe a Double?
                                Double f = Double.parseDouble(s);
                                if (quote == null) {
                                    list.add(f);
                                } else {
                                    quote.add(f);
                                    list.add(quote);
                                    quote = null;
                                }
                            } catch (NumberFormatException ix) { //just a string
                                if (quote == null) {
                                    list.add(s);
                                } else {
                                    quote.add(s);
                                    list.add(quote);
                                    quote = null;
                                }
                            }
                        }
                        break;
                }
            }
        }
        return list;
    }
}
