package org.jboss.seam.persistence;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jboss.seam.persistence.transaction.FlushModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the set of persistence contexts that have been touched in a
 * conversation. Also controls the flush mode used by the persistence contexts
 * during the render phase.
 * 
 * @author Gavin King
 */
@ConversationScoped
public class PersistenceContextsImpl implements Serializable, PersistenceContexts
{
   private static final long serialVersionUID = -4897350516435283182L;
   private static final Logger log = LoggerFactory.getLogger(PersistenceContextsImpl.class);
   /**
    * persistences contexts are referenced by their qualifiers
    */
   private Set<PersistenceContextDefintition> set = new HashSet<PersistenceContextDefintition>();

   private FlushModeType flushMode;

   // the real flush mode is a backup of the flush mode when doing a temporary
   // switch (such as during render)
   private FlushModeType realFlushMode;

   @Inject
   BeanManager beanManager;

   @Inject
   @Any
   Instance<ManagedPersistenceContext> persistenceContexts;

   @Inject
   Instance<SeamPersistenceProvider> persistenceProvider;

   @Inject
   public void create(FlushModeManager manager)
   {
      FlushModeType defaultFlushMode = manager.getFlushModeType();
      if (defaultFlushMode != null)
      {
         flushMode = defaultFlushMode;
      }
      else
      {
         flushMode = FlushModeType.AUTO;
      }
   }

   public FlushModeType getFlushMode()
   {
      return flushMode;
   }

   public Set<PersistenceContextDefintition> getTouchedContexts()
   {
      return Collections.unmodifiableSet(set);
   }

   public void touch(ManagedPersistenceContext context)
   {
      set.add(new PersistenceContextDefintition(context.getQualifiers(), context.getBeanType()));
   }

   public void untouch(ManagedPersistenceContext context)
   {
      set.remove(new PersistenceContextDefintition(context.getQualifiers(), context.getBeanType()));
   }

   public void changeFlushMode(FlushModeType flushMode)
   {
      changeFlushMode(flushMode, false);
   }

   public void changeFlushMode(FlushModeType flushMode, boolean temporary)
   {
      if (temporary)
      {
         realFlushMode = this.flushMode;
      }
      this.flushMode = flushMode;
      changeFlushModes();
   }

   public void restoreFlushMode()
   {
      if (realFlushMode != null && realFlushMode != flushMode)
      {
         flushMode = realFlushMode;
         realFlushMode = null;
         changeFlushModes();
      }
   }

   private void changeFlushModes()
   {

      for (ManagedPersistenceContext context : persistenceContexts)
      {
         if (set.contains(new PersistenceContextDefintition(context.getQualifiers(), context.getBeanType())))
            try
            {
               context.changeFlushMode(flushMode);
            }
            catch (UnsupportedOperationException uoe)
            {
               // we won't be nasty and throw and exception, but we'll log a
               // warning to the developer
               log.warn(uoe.getMessage());
            }
      }
   }

   public void beforeRender()
   {
      // some JPA providers may not support MANUAL flushing
      // defer the decision to the provider manager component
      persistenceProvider.get().setRenderFlushMode();
   }

   public void afterRender()
   {
      restoreFlushMode();
   }

   public static class PersistenceContextDefintition
   {
      private final Set<Annotation> qualifiers;
      private final Class<?> type;

      public PersistenceContextDefintition(Set<Annotation> qualifiers, Class<?> type)
      {
         this.qualifiers = new HashSet<Annotation>(qualifiers);
         this.type = type;
      }

      public Set<Annotation> getQualifiers()
      {
         return qualifiers;
      }

      public Class<?> getType()
      {
         return type;
      }

      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((qualifiers == null) ? 0 : qualifiers.hashCode());
         result = prime * result + ((type == null) ? 0 : type.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         PersistenceContextDefintition other = (PersistenceContextDefintition) obj;
         if (qualifiers == null)
         {
            if (other.qualifiers != null)
               return false;
         }
         else if (!qualifiers.equals(other.qualifiers))
            return false;
         if (type == null)
         {
            if (other.type != null)
               return false;
         }
         else if (!type.equals(other.type))
            return false;
         return true;
      }

   }

}
