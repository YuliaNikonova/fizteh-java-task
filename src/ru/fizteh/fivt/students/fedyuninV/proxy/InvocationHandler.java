package ru.fizteh.fivt.students.fedyuninV.proxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Fedyunin Valeriy
 * MIPT FIVT 195
 */
public class InvocationHandler implements java.lang.reflect.InvocationHandler{
    private Appendable writer;
    private Object target;
    private boolean tooLong;
    private final int ARG_MAX_LENGTH = 60;
    private Map<String, String> screenMap = new HashMap<>();
    private final Object PARSED = new Object();

    public InvocationHandler(Object target, Appendable writer) {
        this.target = target;
        this.writer = writer;
        screenMap.put("\n", "\\\\n");
        screenMap.put("\r", "\\\\r");
        screenMap.put("\b", "\\\\b");
        screenMap.put("\t", "\\\\t");
        screenMap.put("\f", "\\\\f");
        screenMap.put("\"", "\\\"");
        //screenMap.put("\\", "\\\\\\"); It doesn't work, need for backSlashScreen...
    }

    private String screen(String s) {
        s = backslashScreen(s);
        for (Map.Entry<String, String> it: screenMap.entrySet()) {
            s = s.replaceAll(it.getKey(), it.getValue());
        }
        return s;
    }

    private String backslashScreen(String s) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                builder.append("\\\\");
            }
            builder.append(s.charAt(i));
        }
        return builder.toString();
    }

    private boolean isPrimitive(Class classExample) {
        return (classExample.isPrimitive()
                ||  classExample.equals(Integer.class)
                ||  classExample.equals(Boolean.class)
                ||  classExample.equals(Double.class)
                ||  classExample.equals(Float.class)
                ||  classExample.equals(Byte.class)
                ||  classExample.equals(Long.class)
                ||  classExample.equals(Short.class)
                ||  classExample.equals(Character.class));
    }


    private String wrapInQuotes(String s) {
        StringBuilder builder = new StringBuilder();
        builder.append('\"');
        builder.append(screen(s));
        builder.append('\"');
        return builder.toString();
    }

    private String arrayToString(Object toPrint, Map<Object, Object> parsedObjects) {
        StringBuilder builder = new StringBuilder();
        Object[] array = (Object[]) toPrint;
        builder.append(array.length);
        builder.append('{');
        for (int i = 0; i < array.length; i++) {
            builder.append(wrapInQuotes(printObject(array[i], parsedObjects)));
            if (i != array.length - 1) {
                builder.append(", ");
            }
        }
        builder.append('}');
        return builder.toString();
    }

    private String printObject(Object toPrint, Map<Object, Object> parsedObjects) {
        if (toPrint == null) {
            return "null";
        }
        if (parsedObjects.put(toPrint, PARSED) != null) {
            throw new RuntimeException("Object contains link to itself");
        }
        Class clazz = toPrint.getClass();
        if (isPrimitive(clazz)) {
            return toPrint.toString();
        }
        if (clazz.isEnum()) {
            return ((Enum) toPrint).name();
        }
        if (clazz.isArray()) {
            return arrayToString(toPrint, parsedObjects);
        }
        if (clazz.equals(String.class)) {
            return wrapInQuotes(toPrint.toString());
        }
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        builder.append(screen(toPrint.toString()));
        builder.append(']');
        return builder.toString();
    }

    private String parseArgs(Object[] args, Map<Object, Object> parsedObjects) {
        parsedObjects.clear();
        String[] parsedArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            parsedObjects.clear();
            parsedArgs[i] = printObject(args[i], parsedObjects);
            if (parsedArgs[i].length() >= ARG_MAX_LENGTH) {
                tooLong = true;
            }
        }
        StringBuilder builder = new StringBuilder();
        if (tooLong) {
            builder.append('\n');
        }
        for (int i = 0; i < args.length; i++) {
            if (tooLong) {
                builder.append("  ");
            }
            builder.append(parsedArgs[i]);
            if (i < args.length - 1) {
                builder.append(',');
                if (!tooLong) {
                    builder.append(' ');
                }
            }
            if (tooLong) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Map<Object, Object> parsedObjects = new IdentityHashMap<>();
        method.setAccessible(true);
        tooLong = false;
        StringBuilder logger = new StringBuilder();
        logger.append(method.getDeclaringClass().getSimpleName());
        logger.append('.');
        logger.append(method.getName());
        logger.append('(');
        if (args != null) {
            logger.append(parseArgs(args, parsedObjects));
        }
        if (tooLong) {
            logger.append("  ");
        }
        logger.append(')');
        if (tooLong) {
            logger.append("\n ");
        }
        try {
            Object result = method.invoke(target, args);
            logger.append(" returned ");
            parsedObjects.clear();
            logger.append(printObject(result, parsedObjects));
            logger.append('\n');
            return result;
        } catch (Throwable ex) {
            logger.append(" threw ");
            logger.append(ex.getClass().getName());
            logger.append(": ");
            logger.append(ex.getMessage());
            logger.append('\n');
            StackTraceElement[] traceElements = ex.getStackTrace();
            for (StackTraceElement traceElement: traceElements) {
                if (tooLong) {
                    logger.append("  ");
                }
                logger.append("  ");
                logger.append(traceElement.toString());
                logger.append('\n');
            }
            return null;
        } finally {
            writer.append(logger.toString());
        }
    }
}
