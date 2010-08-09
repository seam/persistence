package org.jboss.seam.persistence;

import java.util.Set;

import org.jboss.seam.persistence.PersistenceContextsImpl.PersistenceContextDefintition;
import org.jboss.seam.persistence.transaction.FlushModeType;

public interface PersistenceContexts
{
   public abstract void create(FlushModeManager manager);

   public abstract FlushModeType getFlushMode();

   public abstract Set<PersistenceContextDefintition> getTouchedContexts();

   public abstract void touch(PersistenceContext context);

   public abstract void untouch(PersistenceContext context);

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