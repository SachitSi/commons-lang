package org.apache.commons.lang3.reflect;

public class BooleanString {
    private final boolean forceAccess;
    private final String methodName;
    private final Class<?>[] parameterTypes;

    /**
     * @param forceAccess force access to invoke method even if it's not accessible
     * @param methodName get method with this name
     * @param parameterTypes match these parameters - treat null as empty array
     */
    public BooleanString(boolean forceAccess, String methodName, Class<?>[] parameterTypes) {
        this.forceAccess = forceAccess;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    public boolean isForceAccess() {
        return forceAccess;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }
}
