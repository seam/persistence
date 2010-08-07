package org.jboss.seam.persistence.transaction;


public interface PersistenceContexts
{

   public abstract FlushModeType getFlushMode();

   public abstract void changeFlushMode(FlushModeType flushMode);

   public abstract void changeFlushMode(FlushModeType flushMode, boolean temporary);

   /**
    * Restore the previous flush mode if the current flush mode is marked as
    * temporary.
    */
   public abstract void restoreFlushMode();

   public abstract void beforeRender();

   public abstract void afterRender();

}