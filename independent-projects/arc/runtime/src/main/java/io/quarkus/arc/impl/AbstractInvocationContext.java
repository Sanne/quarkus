package io.quarkus.arc.impl;

import io.quarkus.arc.ArcInvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

abstract class AbstractInvocationContext implements ArcInvocationContext {

    private static final Object[] EMPTY_PARAMS = new Object[0];

    protected final Method method;
    protected final Constructor<?> constructor;
    protected final Set<Annotation> interceptorBindings;
    protected final List<InterceptorInvocation> chain;
    protected Object target;
    protected Object[] parameters;
    protected Map<String, Object> contextData;

    protected AbstractInvocationContext(Object target, Method method,
            Constructor<?> constructor,
            Object[] parameters, Map<String, Object> contextData,
            Set<Annotation> interceptorBindings, List<InterceptorInvocation> chain) {
        this.target = target;
        this.method = method;
        this.constructor = constructor;
        this.parameters = parameters != null ? parameters : EMPTY_PARAMS;
        this.contextData = contextData != null ? contextData : new ContextData();
        this.interceptorBindings = interceptorBindings;
        this.chain = chain;
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData.get();
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return interceptorBindings;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> T findIterceptorBinding(Class<T> annotationType) {
        for (Annotation annotation : interceptorBindings) {
            if (annotation.annotationType().equals(annotationType)) {
                return (T) annotation;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> List<T> findIterceptorBindings(Class<T> annotationType) {
        List<T> found = new ArrayList<>();
        for (Annotation annotation : (Set<Annotation>) interceptorBindings) {
            if (annotation.annotationType().equals(annotationType)) {
                found.add((T) annotation);
            }
        }
        return found;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Object[] params) {
        validateParameters(params);
        this.parameters = params;
    }

    protected void validateParameters(Object[] params) {
        int newParametersCount = Objects.requireNonNull(params).length;
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != newParametersCount) {
            throw new IllegalArgumentException(
                    "Wrong number of parameters - method has " + Arrays.toString(parameterTypes) + ", attempting to set "
                            + Arrays.toString(params));
        }
        for (int i = 0; i < params.length; i++) {
            if (parameterTypes[i].isPrimitive() && params[i] == null) {
                throw new IllegalArgumentException("Trying to set a null value to a primitive parameter [position: " + i
                        + ", type: " + parameterTypes[i] + "]");
            }
            if (params[i] != null) {
                if (!Types.boxedClass(parameterTypes[i]).isAssignableFrom(Types.boxedClass(params[i].getClass()))) {
                    throw new IllegalArgumentException("The parameter type [" + params[i].getClass()
                            + "] can not be assigned to the type for the target method [" + parameterTypes[i] + "]");
                }
            }
        }
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object getTimer() {
        return null;
    }

    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }

    private class ContextData implements Map<String, Object> {

		@Override
		public void clear() {
			throw new UnsupportedOperationException("Not allowed to clear the context data map");
		}

		@Override
		public boolean containsKey(Object key) {
			if (ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS.equals(key)) {
				return true;
			}
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Set entrySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object get(Object key) {
			if (ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS.equals(key)) {
				return AbstractInvocationContext.this.interceptorBindings;
			}
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public Set keySet() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void putAll(Map m) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Object remove(Object key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Collection values() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object put(String arg0, Object arg1) {
			// TODO Auto-generated method stub
			return null;
		}
    	
    }

}
