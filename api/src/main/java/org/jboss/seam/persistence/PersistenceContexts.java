package org.jboss.seam.persistence;


/**
 * PersistenceContexts tracks active persistence contexts within a conversation.
 * 
 * This allows for features such as changing the flush mode of all entity
 * managers to @{link {@link FlushModeType#MANUAL} during the render response
 * phase when using seam managed transactions in JSF
 * 
 */
public interface PersistenceContexts
{

   public abstract FlushModeType getFlushMode();

   /**
    * Changes the flush mode of all persistence contexts in the conversation
    * 
    * @param flushMode the new flush mode
    */
   public abstract void changeFlushMode(FlushModeType flushMode);

   /**
    * Restore the previous flush mode if the current flush mode is marked as
    * temporary.
    */
   public abstract void restoreFlushMode();

   /**
    * Perform
    */
   public abstract void beforeRender();

   public abstract void afterRender();

   public abstract void touch(ManagedPersistenceContext context);

   public abstract void untouch(ManagedPersistenceContext context);

}