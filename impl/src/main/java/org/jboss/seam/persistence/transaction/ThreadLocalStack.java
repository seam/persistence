package org.jboss.seam.persistence.transaction;

import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingDeque;

class ThreadLocalStack<T>
{
   private final ThreadLocal<LinkedBlockingDeque<T>> stack = new ThreadLocal<LinkedBlockingDeque<T>>();

   public void push(T t)
   {
      if (stack.get() == null)
      {
         stack.set(new LinkedBlockingDeque<T>());
      }
      stack.get().push(t);
   }

   public T pop()
   {
      LinkedBlockingDeque<T> queue = stack.get();
      if (queue == null)
      {
         throw new NoSuchElementException();
      }
      T t = queue.pop();
      if (queue.isEmpty())
      {
         stack.remove();
      }
      return t;
   }

   public T peek()
   {
      LinkedBlockingDeque<T> queue = stack.get();
      if (queue == null)
      {
         return null;
      }
      return queue.peek();
   }

   public boolean isEmpty()
   {
      LinkedBlockingDeque<T> queue = stack.get();
      if (queue == null)
      {
         return true;
      }
      return queue.isEmpty();
   }
}
