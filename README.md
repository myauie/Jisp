# Jisp

Jisp is an implementation of Common Lisp built in Java. As you would expect from a functional language implementation, it provides a REPL environment in the form of an interactive shell.

Usage of the shell is performed by typing in valid expressions from Common Lisp. A reference of the functions that are supported in this prototype are listed in the reference section.

With one exception, the syntax for all functions implemented should match (or intended to a work in progress implementation matching) the definitions listed in the Common Lisp Hyperspec.

## Compiler Options

The syntax for the compile function deviates slightly from the Common Lisp Hyperspec, as we introduce a number of flags that are intended to support 

- debug: in addition to compiling and loading the new class into Jisp, it will also create a class file representation in the default directory (the directory we are currently running Jisp)
- name: performs the same action as debug, but will save the class file to a directory path specified after this argument

## Library Usage

In addition to running expressions in the language environment, Jisp expressions can also be run while in another Java program. To be able to do this first requires creating a class file of the function in question using the compiler.

In the same way Jisp is able to evaluate a compiled function, we can access a class file by referring to the class file's sole method, evaluate. The parameters it requires are a list representing the parameters to be used by the function and the interpreter.

## Supported Lisp Functions

```
+
/
=
<
<=
*
>
>=
/=
1+
1-
-
AND
ATOM
OR
NOT
CAR
FIRST
CDR
REST
COMPILE
COND
CONS
COS
copy
DEFUN
DOTIMES
DOLIST
DO
EQ
EQL
EQUAL
FORMAT
IF
LAMBDA
LET
LIST
LOAD
MAX
MIN
MOD
QUIT
QUOTE
RANDOM
ROUND
SET
SIN
SQRT
TYPEOF
ZEROP
PROGN
PUSH
POP
STRING
STRING_UPCASE
STRING_DOWNCASE
ELT
NULL
```
