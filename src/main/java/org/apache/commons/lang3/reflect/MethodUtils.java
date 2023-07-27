/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang3.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

/**
 * Utility reflection methods focused on {@link Method}s, originally from Commons BeanUtils.
 * Differences from the BeanUtils version may be noted, especially where similar functionality
 * already existed within Lang.
 *
 * <h2>Known Limitations</h2>
 * <h3>Accessing Public Methods In A Default Access Superclass</h3>
 * <p>There is an issue when invoking {@code public} methods contained in a default access superclass on JREs prior to 1.4.
 * Reflection locates these methods fine and correctly assigns them as {@code public}.
 * However, an {@link IllegalAccessException} is thrown if the method is invoked.</p>
 *
 * <p>{@link MethodUtils} contains a workaround for this situation.
 * It will attempt to call {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)} on this method.
 * If this call succeeds, then the method can be invoked as normal.
 * This call will only succeed when the application has sufficient security privileges.
 * If this call fails then the method may fail.</p>
 *
 * @since 2.5
 */
public class MethodUtils {

    static final Comparator<Method> METHOD_BY_SIGNATURE = Comparator.comparing(Method::toString);

    /**
     * {@link MethodUtils} instances should NOT be constructed in standard programming.
     * Instead, the class should be used as
     * {@code MethodUtils.getAccessibleMethod(method)}.
     *
     * <p>This constructor is {@code public} to permit tools that require a JavaBean
     * instance to operate.</p>
     */
    public MethodUtils() {
    }

    /**
     * Given an arguments array passed to a varargs method, return an array of arguments in the canonical form,
     * i.e. an array with the declared number of parameters, and whose last parameter is an array of the varargs type.
     *
     * @param args the array of arguments passed to the varags method
     * @param methodParameterTypes the declared array of method parameter types
     * @return an array of the variadic arguments passed to the method
     * @since 3.5
     */
    static Object[] getVarArgs(final Object[] args, final Class<?>[] methodParameterTypes) {
        if (args.length == methodParameterTypes.length && (args[args.length - 1] == null ||
                args[args.length - 1].getClass().equals(methodParameterTypes[methodParameterTypes.length - 1]))) {
            // The args array is already in the canonical form for the method.
            return args;
        }

        // Construct a new array matching the method's declared parameter types.
        final Object[] newArgs = new Object[methodParameterTypes.length];

        // Copy the normal (non-varargs) parameters
        System.arraycopy(args, 0, newArgs, 0, methodParameterTypes.length - 1);

        // Construct a new array for the variadic parameters
        final Class<?> varArgComponentType = methodParameterTypes[methodParameterTypes.length - 1].getComponentType();
        final int varArgLength = args.length - methodParameterTypes.length + 1;

        Object varArgsArray = Array.newInstance(ClassUtils.primitiveToWrapper(varArgComponentType), varArgLength);
        // Copy the variadic arguments into the varargs array.
        System.arraycopy(args, methodParameterTypes.length - 1, varArgsArray, 0, varArgLength);

        if (varArgComponentType.isPrimitive()) {
            // unbox from wrapper type to primitive type
            varArgsArray = ArrayUtils.toPrimitive(varArgsArray);
        }

        // Store the varargs array in the last position of the array to return
        newArgs[methodParameterTypes.length - 1] = varArgsArray;

        // Return the canonical varargs array.
        return newArgs;
    }

    /**
     * Returns an accessible method (that is, one that can be invoked via
     * reflection) by scanning through the superclasses. If no such method
     * can be found, return {@code null}.
     *
     * @param cls Class to be checked
     * @param methodName Method name of the method we wish to call
     * @param parameterTypes The parameter type signatures
     * @return the accessible method or {@code null} if not found
     */
    static Method getAccessibleMethodFromSuperclass(final Class<?> cls,
                                                    final String methodName, final Class<?>... parameterTypes) {
        Class<?> parentClass = cls.getSuperclass();
        while (parentClass != null) {
            if (ClassUtils.isPublic(parentClass)) {
                try {
                    return parentClass.getMethod(methodName, parameterTypes);
                } catch (final NoSuchMethodException e) {
                    return null;
                }
            }
            parentClass = parentClass.getSuperclass();
        }
        return null;
    }

    /**
     * Returns an accessible method (that is, one that can be invoked via
     * reflection) that implements the specified method, by scanning through
     * all implemented interfaces and subinterfaces. If no such method
     * can be found, return {@code null}.
     *
     * <p>There isn't any good reason why this method must be {@code private}.
     * It is because there doesn't seem any reason why other classes should
     * call this rather than the higher level methods.</p>
     *
     * @param cls Parent class for the interfaces to be checked
     * @param methodName Method name of the method we wish to call
     * @param parameterTypes The parameter type signatures
     * @return the accessible method or {@code null} if not found
     */
    static Method getAccessibleMethodFromInterfaceNest(Class<?> cls,
                                                       final String methodName, final Class<?>... parameterTypes) {
        // Search up the superclass chain
        for (; cls != null; cls = cls.getSuperclass()) {

            // Check the implemented interfaces of the parent class
            final Class<?>[] interfaces = cls.getInterfaces();
            for (final Class<?> anInterface : interfaces) {
                // Is this interface public?
                if (!ClassUtils.isPublic(anInterface)) {
                    continue;
                }
                // Does the method exist on this interface?
                try {
                    return anInterface.getDeclaredMethod(methodName,
                            parameterTypes);
                } catch (final NoSuchMethodException ignored) {
                    /*
                     * Swallow, if no method is found after the loop then this
                     * method returns null.
                     */
                }
                // Recursively check our parent interfaces
                final Method method = getAccessibleMethodFromInterfaceNest(anInterface,
                        methodName, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Returns the aggregate number of inheritance hops between assignable argument class types.  Returns -1
     * if the arguments aren't assignable.  Fills a specific purpose for getMatchingMethod and is not generalized.
     * @param fromClassArray the Class array to calculate the distance from.
     * @param toClassArray the Class array to calculate the distance to.
     * @return the aggregate number of inheritance hops between assignable argument class types.
     */
    static int distance(final Class<?>[] fromClassArray, final Class<?>[] toClassArray) {
        int answer = 0;

        if (!ClassUtils.isAssignable(fromClassArray, toClassArray, true)) {
            return -1;
        }
        for (int offset = 0; offset < fromClassArray.length; offset++) {
            // Note InheritanceUtils.distance() uses different scoring system.
            final Class<?> aClass = fromClassArray[offset];
            final Class<?> toClass = toClassArray[offset];
            if (aClass == null || aClass.equals(toClass)) {
                continue;
            }
            if (ClassUtils.isAssignable(aClass, toClass, true)
                    && !ClassUtils.isAssignable(aClass, toClass, false)) {
                answer++;
            } else {
                answer = answer + 2;
            }
        }

        return answer;
    }

    /**
     * Gets a combination of {@link ClassUtils#getAllSuperclasses(Class)} and
     * {@link ClassUtils#getAllInterfaces(Class)}, one from superclasses, one
     * from interfaces, and so on in a breadth first way.
     *
     * @param cls  the class to look up, may be {@code null}
     * @return the combined {@link List} of superclasses and interfaces in order
     * going up from this one
     *  {@code null} if null input
     */
    static List<Class<?>> getAllSuperclassesAndInterfaces(final Class<?> cls) {
        if (cls == null) {
            return null;
        }

        final List<Class<?>> allSuperClassesAndInterfaces = new ArrayList<>();
        final List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(cls);
        int superClassIndex = 0;
        final List<Class<?>> allInterfaces = ClassUtils.getAllInterfaces(cls);
        int interfaceIndex = 0;
        while (interfaceIndex < allInterfaces.size() ||
                superClassIndex < allSuperclasses.size()) {
            final Class<?> acls;
            if (interfaceIndex >= allInterfaces.size()) {
                acls = allSuperclasses.get(superClassIndex++);
            } else if (superClassIndex >= allSuperclasses.size() || !(superClassIndex < interfaceIndex)) {
                acls = allInterfaces.get(interfaceIndex++);
            } else {
                acls = allSuperclasses.get(superClassIndex++);
            }
            allSuperClassesAndInterfaces.add(acls);
        }
        return allSuperClassesAndInterfaces;
    }
}
