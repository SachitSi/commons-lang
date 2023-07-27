package org.apache.commons.lang3.reflect;

public class InvokeMethodParam {
    private final Object object;
    private final boolean forceAccess;
    private final String methodName;
    private Object[] args;
    private Class<?>[] parameterTypes;

    /**
     * @param object        invoke method on this object
     * @param booleanString
     * @param args          use these arguments - treat null as empty array
     */
    public InvokeMethodParam(Object object, BooleanString booleanString, Object[] args) {
        this.object = object;
        this.forceAccess = booleanString.isForceAccess();
        this.methodName = booleanString.getMethodName();
        this.args = args;
        this.parameterTypes = booleanString.getParameterTypes();
    }

    public Object getObject() {
        return object;
    }

    public boolean isForceAccess() {
        return forceAccess;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArgs() {
        return args;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }
}
