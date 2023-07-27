package org.apache.commons.lang3.reflect;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class InokeMethods {
    /**
     * Invokes a named method without parameters.
     *
     * <p>This method delegates the method search to .</p>
     *
     * <p>This is a convenient wrapper for
     * {@link #invokeMethod(Object object, String methodName, Object[] args, Class[] parameterTypes)}.
     * </p>
     *
     * @param object     invoke method on this object
     * @param methodName get method with this name
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the method invoked
     * @throws IllegalAccessException    if the requested method is not accessible via reflection
     * @since 3.4
     */
    public static Object invokeMethod(final Object object, final String methodName) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        return invokeMethod(object, methodName, ArrayUtils.EMPTY_OBJECT_ARRAY, null);
    }

    /**
     * Invokes a named method without parameters.
     *
     * <p>This is a convenient wrapper for
     * {@link #invokeMethod(InvokeMethodParam)}.
     * </p>
     *
     * @param object      invoke method on this object
     * @param forceAccess force access to invoke method even if it's not accessible
     * @param methodName  get method with this name
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the method invoked
     * @throws IllegalAccessException    if the requested method is not accessible via reflection
     * @since 3.5
     */
    public static Object invokeMethod(final Object object, final boolean forceAccess, final String methodName)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return invokeMethod(new InvokeMethodParam(object, new BooleanString(forceAccess, methodName, null), ArrayUtils.EMPTY_OBJECT_ARRAY));
    }

    /**
     * Invokes a named method whose parameter type matches the object type.
     *
     * <p>This method delegates the method search to .</p>
     *
     * <p>This method supports calls to methods taking primitive parameters
     * via passing in wrapping classes. So, for example, a {@link Boolean} object
     * would match a {@code boolean} primitive.</p>
     *
     * <p>This is a convenient wrapper for
     * {@link #invokeMethod(Object object, String methodName, Object[] args, Class[] parameterTypes)}.
     * </p>
     *
     * @param object     invoke method on this object
     * @param methodName get method with this name
     * @param args       use these arguments - treat null as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the method invoked
     * @throws IllegalAccessException    if the requested method is not accessible via reflection
     * @throws NullPointerException      if the object or method name are {@code null}
     */
    public static Object invokeMethod(final Object object, final String methodName,
                                      Object... args) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        args = ArrayUtils.nullToEmpty(args);
        return invokeMethod(object, methodName, args, ClassUtils.toClass(args));
    }

    /**
     * Invokes a named method whose parameter type matches the object type.
     *
     * <p>This method supports calls to methods taking primitive parameters
     * via passing in wrapping classes. So, for example, a {@link Boolean} object
     * would match a {@code boolean} primitive.</p>
     *
     * <p>This is a convenient wrapper for
     * {@link #invokeMethod(InvokeMethodParam)}.
     * </p>
     *
     * @param object      invoke method on this object
     * @param forceAccess force access to invoke method even if it's not accessible
     * @param methodName  get method with this name
     * @param args        use these arguments - treat null as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the method invoked
     * @throws IllegalAccessException    if the requested method is not accessible via reflection
     * @throws NullPointerException      if the object or method name are {@code null}
     * @since 3.5
     */
    public static Object invokeMethod(final Object object, final boolean forceAccess, final String methodName,
                                      Object... args) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        args = ArrayUtils.nullToEmpty(args);
        return invokeMethod(new InvokeMethodParam(object, new BooleanString(forceAccess, methodName, ClassUtils.toClass(args)), args));
    }

    /**
     * Invokes a named method whose parameter type matches the object type.
     *
     * <p>This method supports calls to methods taking primitive parameters
     * via passing in wrapping classes. So, for example, a {@link Boolean} object
     * would match a {@code boolean} primitive.</p>
     *
     * @param invokeMethodParam@return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the method invoked
     * @throws IllegalAccessException    if the requested method is not accessible via reflection
     * @throws NullPointerException      if the object or method name are {@code null}
     * @since 3.5
     */
    public static Object invokeMethod(InvokeMethodParam invokeMethodParam)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Objects.requireNonNull(invokeMethodParam.getObject(), "object");
        invokeMethodParam.setParameterTypes(ArrayUtils.nullToEmpty(invokeMethodParam.getParameterTypes()));
        invokeMethodParam.setArgs(ArrayUtils.nullToEmpty(invokeMethodParam.getArgs()));

        final String messagePrefix;
        final Method method;

        final Class<? extends Object> cls = invokeMethodParam.getObject().getClass();
        if (invokeMethodParam.isForceAccess()) {
            messagePrefix = "No such method: ";
            method = MethodUtilsGetters.getMatchingMethod(cls, invokeMethodParam.getMethodName(), invokeMethodParam.getParameterTypes());
            if (method != null && !method.isAccessible()) {
                method.setAccessible(true);
            }
        } else {
            messagePrefix = "No such accessible method: ";
            method = MethodUtilsGetters.getMatchingAccessibleMethod(cls, invokeMethodParam.getMethodName(), invokeMethodParam.getParameterTypes());
        }

        if (method == null) {
            throw new NoSuchMethodException(messagePrefix + invokeMethodParam.getMethodName() + "() on object: " + cls.getName());
        }
        invokeMethodParam.setArgs(MethodUtilsGetters.toVarArgs(method, invokeMethodParam.getArgs()));

        return method.invoke(invokeMethodParam.getObject(), invokeMethodParam.getArgs());
    }

    /**
     * Invokes a named method whose parameter type matches the object type.
     *
     * <p>This method delegates the method search to .</p>
     *
     * <p>This method supports calls to methods taking primitive parameters
     * via passing in wrapping classes. So, for example, a {@link Boolean} object
     * would match a {@code boolean} primitive.</p>
     *
     * @param object         invoke method on this object
     * @param methodName     get method with this name
     * @param args           use these arguments - treat null as empty array
     * @param parameterTypes match these parameters - treat null as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the method invoked
     * @throws IllegalAccessException    if the requested method is not accessible via reflection
     */
    public static Object invokeMethod(final Object object, final String methodName,
                                      final Object[] args, final Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        return invokeMethod(new InvokeMethodParam(object, new BooleanString(false, methodName, parameterTypes), args));
    }

    /**
     * Invokes a method whose parameter types match exactly the object
     * types.
     *
     * <p>This uses reflection to invoke the method obtained from a call to
     * (Class, String, Class[])}.</p>
     *
     * @param object     invoke method on this object
     * @param methodName get method with this name
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the
     *                                   method invoked
     * @throws IllegalAccessException    if the requested method is not accessible
     *                                   via reflection
     * @since 3.4
     */
    public static Object invokeExactMethod(final Object object, final String methodName) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        return invokeExactMethod(object, methodName, ArrayUtils.EMPTY_OBJECT_ARRAY, null);
    }

    /**
     * Invokes a method with no parameters.
     *
     * <p>This uses reflection to invoke the method obtained from a call to
     * (Class, String, Class[])}.</p>
     *
     * @param object     invoke method on this object
     * @param methodName get method with this name
     * @param args       use these arguments - treat null as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the
     *                                   method invoked
     * @throws IllegalAccessException    if the requested method is not accessible
     *                                   via reflection
     * @throws NullPointerException      if the object or method name are {@code null}
     */
    public static Object invokeExactMethod(final Object object, final String methodName,
                                           Object... args) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        args = ArrayUtils.nullToEmpty(args);
        return invokeExactMethod(object, methodName, args, ClassUtils.toClass(args));
    }

    /**
     * Invokes a method whose parameter types match exactly the parameter
     * types given.
     *
     * <p>This uses reflection to invoke the method obtained from a call to
     * .</p>
     *
     * @param object         invoke method on this object
     * @param methodName     get method with this name
     * @param args           use these arguments - treat null as empty array
     * @param parameterTypes match these parameters - treat {@code null} as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the
     *                                   method invoked
     * @throws IllegalAccessException    if the requested method is not accessible
     *                                   via reflection
     * @throws NullPointerException      if the object or method name are {@code null}
     */
    public static Object invokeExactMethod(final Object object, final String methodName, Object[] args, Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Objects.requireNonNull(object, "object");
        args = ArrayUtils.nullToEmpty(args);
        parameterTypes = ArrayUtils.nullToEmpty(parameterTypes);
        final Class<?> cls = object.getClass();
        final Method method = MethodUtilsGetters.getAccessibleMethod(cls, methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: " + methodName + "() on object: " + cls.getName());
        }
        return method.invoke(object, args);
    }

    /**
     * Invokes a {@code static} method whose parameter types match exactly the parameter
     * types given.
     *
     * <p>This uses reflection to invoke the method obtained from a call to
     * .</p>
     *
     * @param cls            invoke static method on this class
     * @param methodName     get method with this name
     * @param args           use these arguments - treat {@code null} as empty array
     * @param parameterTypes match these parameters - treat {@code null} as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the
     *                                   method invoked
     * @throws IllegalAccessException    if the requested method is not accessible
     *                                   via reflection
     */
    public static Object invokeExactStaticMethod(final Class<?> cls, final String methodName,
                                                 Object[] args, Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        args = ArrayUtils.nullToEmpty(args);
        parameterTypes = ArrayUtils.nullToEmpty(parameterTypes);
        final Method method = MethodUtilsGetters.getAccessibleMethod(cls, methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: "
                    + methodName + "() on class: " + cls.getName());
        }
        return method.invoke(null, args);
    }

    /**
     * Invokes a named {@code static} method whose parameter type matches the object type.
     *
     * <p>This method delegates the method search to .</p>
     *
     * <p>This method supports calls to methods taking primitive parameters
     * via passing in wrapping classes. So, for example, a {@link Boolean} class
     * would match a {@code boolean} primitive.</p>
     *
     * <p>This is a convenient wrapper for
     * {@link #invokeStaticMethod(Class, String, Object[], Class[])}.
     * </p>
     *
     * @param cls        invoke static method on this class
     * @param methodName get method with this name
     * @param args       use these arguments - treat {@code null} as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the
     *                                   method invoked
     * @throws IllegalAccessException    if the requested method is not accessible
     *                                   via reflection
     */
    public static Object invokeStaticMethod(final Class<?> cls, final String methodName,
                                            Object... args) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        args = ArrayUtils.nullToEmpty(args);
        return invokeStaticMethod(cls, methodName, args, ClassUtils.toClass(args));
    }

    /**
     * Invokes a named {@code static} method whose parameter type matches the object type.
     *
     * <p>This method delegates the method search to .</p>
     *
     * <p>This method supports calls to methods taking primitive parameters
     * via passing in wrapping classes. So, for example, a {@link Boolean} class
     * would match a {@code boolean} primitive.</p>
     *
     * @param cls            invoke static method on this class
     * @param methodName     get method with this name
     * @param args           use these arguments - treat {@code null} as empty array
     * @param parameterTypes match these parameters - treat {@code null} as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the
     *                                   method invoked
     * @throws IllegalAccessException    if the requested method is not accessible
     *                                   via reflection
     */
    public static Object invokeStaticMethod(final Class<?> cls, final String methodName,
                                            Object[] args, Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        args = ArrayUtils.nullToEmpty(args);
        parameterTypes = ArrayUtils.nullToEmpty(parameterTypes);
        final Method method = MethodUtilsGetters.getMatchingAccessibleMethod(cls, methodName,
                parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: "
                    + methodName + "() on class: " + cls.getName());
        }
        args = MethodUtilsGetters.toVarArgs(method, args);
        return method.invoke(null, args);
    }

    /**
     * Invokes a {@code static} method whose parameter types match exactly the object
     * types.
     *
     * <p>This uses reflection to invoke the method obtained from a call to
     * .</p>
     *
     * @param cls        invoke static method on this class
     * @param methodName get method with this name
     * @param args       use these arguments - treat {@code null} as empty array
     * @return The value returned by the invoked method
     * @throws NoSuchMethodException     if there is no such accessible method
     * @throws InvocationTargetException wraps an exception thrown by the
     *                                   method invoked
     * @throws IllegalAccessException    if the requested method is not accessible
     *                                   via reflection
     */
    public static Object invokeExactStaticMethod(final Class<?> cls, final String methodName,
                                                 Object... args) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        args = ArrayUtils.nullToEmpty(args);
        return invokeExactStaticMethod(cls, methodName, args, ClassUtils.toClass(args));
    }
}