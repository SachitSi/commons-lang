package org.apache.commons.lang3.reflect;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodUtilsGetters {
    public static Object[] toVarArgs(final Method method, Object[] args) {
        if (method.isVarArgs()) {
            final Class<?>[] methodParameterTypes = method.getParameterTypes();
            args = MethodUtils.getVarArgs(args, methodParameterTypes);
        }
        return args;
    }

    /**
     * Returns an accessible method (that is, one that can be invoked via
     * reflection) with given name and parameters. If no such method
     * can be found, return {@code null}.
     * This is just a convenience wrapper for
     * {@link #getAccessibleMethod(Method)}.
     *
     * @param cls            get method from this class
     * @param methodName     get method with this name
     * @param parameterTypes with these parameters types
     * @return The accessible method
     */
    public static Method getAccessibleMethod(final Class<?> cls, final String methodName,
                                             final Class<?>... parameterTypes) {
        try {
            return getAccessibleMethod(cls.getMethod(methodName, parameterTypes));
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns an accessible method (that is, one that can be invoked via
     * reflection) that implements the specified Method. If no such method
     * can be found, return {@code null}.
     *
     * @param method The method that we wish to call
     * @return The accessible method
     */
    public static Method getAccessibleMethod(Method method) {
        if (!MemberUtils.isAccessible(method)) {
            return null;
        }
        // If the declaring class is public, we are done
        final Class<?> cls = method.getDeclaringClass();
        if (ClassUtils.isPublic(cls)) {
            return method;
        }
        final String methodName = method.getName();
        final Class<?>[] parameterTypes = method.getParameterTypes();

        // Check the implemented interfaces and subinterfaces
        method = MethodUtils.getAccessibleMethodFromInterfaceNest(cls, methodName,
                parameterTypes);

        // Check the superclass chain
        if (method == null) {
            method = MethodUtils.getAccessibleMethodFromSuperclass(cls, methodName,
                    parameterTypes);
        }
        return method;
    }

    /**
     * Finds an accessible method that matches the given name and has compatible parameters.
     * Compatible parameters mean that every method parameter is assignable from
     * the given parameters.
     * In other words, it finds a method with the given name
     * that will take the parameters given.
     *
     * <p>This method is used by
     * .
     * </p>
     *
     * <p>This method can match primitive parameter by passing in wrapper classes.
     * For example, a {@link Boolean} will match a primitive {@code boolean}
     * parameter.
     * </p>
     *
     * @param cls            find method in this class
     * @param methodName     find method with this name
     * @param parameterTypes find method with most compatible parameters
     * @return The accessible method
     */
    public static Method getMatchingAccessibleMethod(final Class<?> cls,
                                                     final String methodName, final Class<?>... parameterTypes) {
        try {
            return MemberUtils.setAccessibleWorkaround(cls.getMethod(methodName, parameterTypes));
        } catch (final NoSuchMethodException ignored) {
            // Swallow the exception
        }
        // search through all methods
        final Method[] methods = cls.getMethods();
        final List<Method> matchingMethods = Stream.of(methods)
                .filter(method -> method.getName().equals(methodName) && MemberUtils.isMatchingMethod(method, parameterTypes)).collect(Collectors.toList());

        // Sort methods by signature to force deterministic result
        matchingMethods.sort(MethodUtils.METHOD_BY_SIGNATURE);

        Method bestMatch = null;
        for (final Method method : matchingMethods) {
            // get accessible version of method
            final Method accessibleMethod = getAccessibleMethod(method);
            if (accessibleMethod != null && (bestMatch == null || MemberUtils.compareMethodFit(accessibleMethod, bestMatch, parameterTypes) < 0)) {
                bestMatch = accessibleMethod;
            }
        }
        if (bestMatch != null) {
            MemberUtils.setAccessibleWorkaround(bestMatch);
        }

        if (bestMatch != null && bestMatch.isVarArgs() && bestMatch.getParameterTypes().length > 0 && parameterTypes.length > 0) {
            final Class<?>[] methodParameterTypes = bestMatch.getParameterTypes();
            final Class<?> methodParameterComponentType = methodParameterTypes[methodParameterTypes.length - 1].getComponentType();
            final String methodParameterComponentTypeName = ClassUtils.primitiveToWrapper(methodParameterComponentType).getName();

            final Class<?> lastParameterType = parameterTypes[parameterTypes.length - 1];
            final String parameterTypeName = lastParameterType == null ? null : lastParameterType.getName();
            final String parameterTypeSuperClassName = lastParameterType == null ? null : lastParameterType.getSuperclass().getName();

            if (parameterTypeName != null && parameterTypeSuperClassName != null && !methodParameterComponentTypeName.equals(parameterTypeName)
                    && !methodParameterComponentTypeName.equals(parameterTypeSuperClassName)) {
                return null;
            }
        }

        return bestMatch;
    }

    /**
     * Retrieves a method whether or not it's accessible. If no such method
     * can be found, return {@code null}.
     *
     * @param cls            The class that will be subjected to the method search
     * @param methodName     The method that we wish to call
     * @param parameterTypes Argument class types
     * @return The method
     * @throws IllegalStateException if there is no unique result
     * @throws NullPointerException  if the class is {@code null}
     * @since 3.5
     */
    public static Method getMatchingMethod(final Class<?> cls, final String methodName,
                                           final Class<?>... parameterTypes) {
        Objects.requireNonNull(cls, "cls");
        Validate.notEmpty(methodName, "methodName");

        final List<Method> methods = Stream.of(cls.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .collect(Collectors.toList());

        ClassUtils.getAllSuperclasses(cls).stream()
                .map(Class::getDeclaredMethods)
                .flatMap(Stream::of)
                .filter(method -> method.getName().equals(methodName))
                .forEach(methods::add);

        for (final Method method : methods) {
            if (Arrays.deepEquals(method.getParameterTypes(), parameterTypes)) {
                return method;
            }
        }

        final TreeMap<Integer, List<Method>> candidates = new TreeMap<Integer, List<Method>>();

        methods.stream()
                .filter(method -> ClassUtils.isAssignable(parameterTypes, method.getParameterTypes(), true))
                .forEach(method -> {
                    final int distance = MethodUtils.distance(parameterTypes, method.getParameterTypes());
                    final List<Method> candidatesAtDistance = candidates.computeIfAbsent(distance, k -> new ArrayList<Method>());
                    candidatesAtDistance.add(method);
                });

        if (candidates.isEmpty()) {
            return null;
        }

        final List<Method> bestCandidates = candidates.values().iterator().next();
        if (bestCandidates.size() == 1 || !Objects.equals(bestCandidates.get(0).getDeclaringClass(),
                bestCandidates.get(1).getDeclaringClass())) {
            return bestCandidates.get(0);
        }

        throw new IllegalStateException(
                String.format("Found multiple candidates for method %s on class %s : %s",
                        methodName + Stream.of(parameterTypes).map(String::valueOf).collect(Collectors.joining(",", "(", ")")),
                        cls.getName(),
                        bestCandidates.stream().map(Method::toString).collect(Collectors.joining(",", "[", "]")))
        );
    }

    /**
     * Gets the hierarchy of overridden methods down to {@code result} respecting generics.
     *
     * @param method             lowest to consider
     * @param interfacesBehavior whether to search interfaces, {@code null} {@code implies} false
     * @return Set&lt;Method&gt; in ascending order from sub- to superclass
     * @throws NullPointerException if the specified method is {@code null}
     * @since 3.2
     */
    public static Set<Method> getOverrideHierarchy(final Method method, final ClassUtils.Interfaces interfacesBehavior) {
        Objects.requireNonNull(method, "method");
        final Set<Method> result = new LinkedHashSet<Method>();
        result.add(method);

        final Class<?>[] parameterTypes = method.getParameterTypes();

        final Class<?> declaringClass = method.getDeclaringClass();

        final Iterator<Class<?>> hierarchy = ClassUtils.hierarchy(declaringClass, interfacesBehavior).iterator();
        //skip the declaring class :P
        hierarchy.next();
        hierarchyTraversal:
        while (hierarchy.hasNext()) {
            final Class<?> c = hierarchy.next();
            final Method m = getMatchingAccessibleMethod(c, method.getName(), parameterTypes);
            if (m == null) {
                continue;
            }
            if (Arrays.equals(m.getParameterTypes(), parameterTypes)) {
                // matches without generics
                result.add(m);
                continue;
            }
            // necessary to get arguments every time in the case that we are including interfaces
            final Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(declaringClass, m.getDeclaringClass());
            for (int i = 0; i < parameterTypes.length; i++) {
                final Type childType = TypeUtils.unrollVariables(typeArguments, method.getGenericParameterTypes()[i]);
                final Type parentType = TypeUtils.unrollVariables(typeArguments, m.getGenericParameterTypes()[i]);
                if (!TypeUtils.equals(childType, parentType)) {
                    continue hierarchyTraversal;
                }
            }
            result.add(m);
        }
        return result;
    }

    /**
     * Gets all class level public methods of the given class that are annotated with the given annotation.
     *
     * @param cls           the {@link Class} to query
     * @param annotationCls the {@link Annotation} that must be present on a method to be matched
     * @return an array of Methods (possibly empty).
     * @throws NullPointerException if the class or annotation are {@code null}
     * @since 3.4
     */
    public static Method[] getMethodsWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        return getMethodsWithAnnotation(cls, annotationCls, false, false);
    }

    /**
     * Gets all class level public methods of the given class that are annotated with the given annotation.
     *
     * @param cls           the {@link Class} to query
     * @param annotationCls the {@link Annotation} that must be present on a method to be matched
     * @return a list of Methods (possibly empty).
     * @throws NullPointerException if the class or annotation are {@code null}
     * @since 3.4
     */
    public static List<Method> getMethodsListWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        return getMethodsListWithAnnotation(cls, annotationCls, false, false);
    }

    /**
     * Gets all methods of the given class that are annotated with the given annotation.
     *
     * @param cls           the {@link Class} to query
     * @param annotationCls the {@link Annotation} that must be present on a method to be matched
     * @param searchSupers  determines if a lookup in the entire inheritance hierarchy of the given class should be performed
     * @param ignoreAccess  determines if non-public methods should be considered
     * @return an array of Methods (possibly empty).
     * @throws NullPointerException if the class or annotation are {@code null}
     * @since 3.6
     */
    public static Method[] getMethodsWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls,
                                                    final boolean searchSupers, final boolean ignoreAccess) {
        return getMethodsListWithAnnotation(cls, annotationCls, searchSupers, ignoreAccess).toArray(ArrayUtils.EMPTY_METHOD_ARRAY);
    }

    /**
     * Gets all methods of the given class that are annotated with the given annotation.
     *
     * @param cls           the {@link Class} to query
     * @param annotationCls the {@link Annotation} that must be present on a method to be matched
     * @param searchSupers  determines if a lookup in the entire inheritance hierarchy of the given class should be performed
     * @param ignoreAccess  determines if non-public methods should be considered
     * @return a list of Methods (possibly empty).
     * @throws NullPointerException if either the class or annotation class is {@code null}
     * @since 3.6
     */
    public static List<Method> getMethodsListWithAnnotation(final Class<?> cls,
                                                            final Class<? extends Annotation> annotationCls,
                                                            final boolean searchSupers, final boolean ignoreAccess) {

        Objects.requireNonNull(cls, "cls");
        Objects.requireNonNull(annotationCls, "annotationCls");
        final List<Class<?>> classes = searchSupers ? MethodUtils.getAllSuperclassesAndInterfaces(cls) : new ArrayList<Class<?>>();
        classes.add(0, cls);
        final List<Method> annotatedMethods = new ArrayList<Method>();
        classes.forEach(acls -> {
            final Method[] methods = ignoreAccess ? acls.getDeclaredMethods() : acls.getMethods();
            Stream.of(methods).filter(method -> method.isAnnotationPresent(annotationCls)).forEachOrdered(annotatedMethods::add);
        });
        return annotatedMethods;
    }

    /**
     * Gets the annotation object with the given annotation type that is present on the given method
     * or optionally on any equivalent method in super classes and interfaces. Returns null if the annotation
     * type was not present.
     *
     * <p>Stops searching for an annotation once the first annotation of the specified type has been
     * found. Additional annotations of the specified type will be silently ignored.</p>
     *
     * @param <A>           the annotation type
     * @param method        the {@link Method} to query
     * @param annotationCls the {@link Annotation} to check if is present on the method
     * @param searchSupers  determines if a lookup in the entire inheritance hierarchy of the given class is performed
     *                      if the annotation was not directly present
     * @param ignoreAccess  determines if underlying method has to be accessible
     * @return the first matching annotation, or {@code null} if not found
     * @throws NullPointerException if either the method or annotation class is {@code null}
     * @since 3.6
     */
    public static <A extends Annotation> A getAnnotation(final Method method, final Class<A> annotationCls,
                                                         final boolean searchSupers, final boolean ignoreAccess) {

        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(annotationCls, "annotationCls");
        if (!ignoreAccess && !MemberUtils.isAccessible(method)) {
            return null;
        }

        A annotation = method.getAnnotation(annotationCls);

        if (annotation == null && searchSupers) {
            final Class<?> mcls = method.getDeclaringClass();
            final List<Class<?>> classes = MethodUtils.getAllSuperclassesAndInterfaces(mcls);
            for (final Class<?> acls : classes) {
                final Method equivalentMethod = ignoreAccess ? getMatchingMethod(acls, method.getName(), method.getParameterTypes())
                        : getMatchingAccessibleMethod(acls, method.getName(), method.getParameterTypes());
                if (equivalentMethod != null) {
                    annotation = equivalentMethod.getAnnotation(annotationCls);
                    if (annotation != null) {
                        break;
                    }
                }
            }
        }

        return annotation;
    }
}