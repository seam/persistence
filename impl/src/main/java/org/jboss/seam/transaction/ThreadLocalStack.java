/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.transaction;

import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingDeque;

class ThreadLocalStack<T> {
    private final ThreadLocal<LinkedBlockingDeque<T>> stack = new ThreadLocal<LinkedBlockingDeque<T>>();

    public void push(T t) {
        if (stack.get() == null) {
            stack.set(new LinkedBlockingDeque<T>());
        }
        stack.get().push(t);
    }

    public T pop() {
        LinkedBlockingDeque<T> queue = stack.get();
        if (queue == null) {
            throw new NoSuchElementException();
        }
        T t = queue.pop();
        if (queue.isEmpty()) {
            stack.remove();
        }
        return t;
    }

    public T peek() {
        LinkedBlockingDeque<T> queue = stack.get();
        if (queue == null) {
            return null;
        }
        return queue.peek();
    }

    public boolean isEmpty() {
        LinkedBlockingDeque<T> queue = stack.get();
        if (queue == null) {
            return true;
        }
        return queue.isEmpty();
    }
}
