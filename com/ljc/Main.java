package com.ljc;

import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {

    static boolean running;

    static {
        running = true;
    }

    public static void main(String... args) {
        Object result = null;
        String line;
        JispInterp interp = new JispInterp();
        JispParser parser = new JispParser();
        InputStream instr;

        if (args.length == 1) { // process Lisp file
            try {
                line = new String(Files.readAllBytes(Paths.get(args[0])));
                ArrayList items = (ArrayList) parser.interpret(line);
                for (Object i : items) {
                    result = interp.eval(i);
                }
            } catch (FileNotFoundException f) {
                System.out.println("could not open " + args[0] + ", " + f.getMessage());
            } catch (IOException ioex) {
                System.err.println("Error reading file: " + ioex.getMessage());
            }
        } else { // interactive mode
            try {
                System.out.println("============================================================");
                System.out.println("                          JISP                              ");
                System.out.println("============================================================");
                System.out.println("");

                Terminal tr = TerminalBuilder.terminal();

                ArrayList<String> builtin_names = new ArrayList<>();
                for (Method m : JispBuiltinFunction.class.getDeclaredMethods()) {
                    if (Character.isUpperCase(m.getName().charAt(0)))
                        builtin_names.add(m.getName().toLowerCase().replace('_', '-'));
                }
                StringsCompleter builtin = new StringsCompleter(builtin_names);
                Completer keywords = (lineReader, parsedLine, list) -> {
                    for (Object x : interp.getSymbols().keySet()) list.add(new Candidate(((String) x).toLowerCase()));
                };
                Parser lispparser = new DefaultParser() {
                    @Override
                    public boolean isDelimiterChar(CharSequence buffer, int pos) {
                        return (buffer.charAt(pos) == '(') || (buffer.charAt(pos) == ')') || super.isDelimiterChar(buffer, pos);
                    }
                };
                LineReader cr = LineReaderBuilder.builder()
                        .terminal(tr)
                        .parser(lispparser)
                        .completer(new AggregateCompleter(keywords, builtin))
                        .build();
                History hist = cr.getHistory();
                line = cr.readLine("jisp> ");
                cr.getHistory().add(line);
                ArrayList items = (ArrayList) parser.interpret(line);
                for (Object i : items) {
                    result = interp.eval(i);
                    if (result != null)
                        System.out.println(result);
                    else
                        System.out.println("NIL");
                }
                while (running) {
                    line = cr.readLine("jisp> ");
                    cr.getHistory().add(line);
                    items = (ArrayList) parser.interpret(line);
                    for (Object i : items) {
                        result = interp.eval(i);
                        if (result != null)
                            System.out.println(result);
                        else
                            System.out.println("NIL");
                    }
                }
            } catch (IOException ex) {
                System.err.println("Error opening console: " + ex.getMessage());
            }
        }
    }
}