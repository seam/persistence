package org.jboss.seam.persistence;

/**
 * A full set of flush modes, including MANUAL,
 * which is a glaring missing feature of the JPA
 * spec.
 * 
 * @author Gavin King
 *
 */
public enum FlushModeType
{
   
   /**
    * Flushing never occurs automatically, all changes are queued 
    * until the application calls flush() explicitly.
    */
   MANUAL,

   /**
    * Flushing occurs automatically at commit time and when necessary
    * before query executions.
    */
   AUTO,
   
   /**
    * Flushing occurs automatically at transaction commit time.
    */
   COMMIT;
   
   /**
    * Does this flush mode keep unflushed changes past a 
    * transaction commit?
    * 
    * @return false for all flush modes except for MANUAL
    */
   public boolean dirtyBetweenTransactions() 
   { 
      return this==MANUAL;
   }
    
}
