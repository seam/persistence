package org.jboss.seam.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.proxy.HibernateProxy;
import org.jboss.seam.persistence.transaction.FlushModeType;
import org.jboss.weld.extensions.core.Veto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for non-standardized features of Hibernate, when used as the JPA
 * persistence provider.
 * 
 * @author Gavin King
 * @author Pete Muir
 * @author Stuart Douglas
 * 
 */
@Veto
public class HibernatePersistenceProvider extends SeamPersistenceProvider
{

   @Inject
   Instance<PersistenceContextsImpl> persistenceContexts;

   private static Logger log = LoggerFactory.getLogger(HibernatePersistenceProvider.class);
   private static Class<?> FULL_TEXT_SESSION_PROXY_CLASS;
   private static Method FULL_TEXT_SESSION_CONSTRUCTOR;
   private static Class<?> FULL_TEXT_ENTITYMANAGER_PROXY_CLASS;
   private static Method FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR;
   static
   {
      try
      {
         String version = null;
         try
         {
            Class<?> searchVersionClass = Class.forName("org.hibernate.search.Version");
            Field versionField = searchVersionClass.getDeclaredField("VERSION");
            version = (String) versionField.get(null);
         }
         catch (Exception e)
         {
            log.debug("no Hibernate Search, sorry :-(", e);
         }
         if (version != null)
         {
            Class<?> searchClass = Class.forName("org.hibernate.search.Search");
            try
            {
               FULL_TEXT_SESSION_CONSTRUCTOR = searchClass.getDeclaredMethod("getFullTextSession", Session.class);
            }
            catch (NoSuchMethodException noSuchMethod)
            {
               log.debug("org.hibernate.search.Search.getFullTextSession(Session) not found, trying deprecated method name createFullTextSession");
               FULL_TEXT_SESSION_CONSTRUCTOR = searchClass.getDeclaredMethod("createFullTextSession", Session.class);
            }
            FULL_TEXT_SESSION_PROXY_CLASS = Class.forName("org.jboss.seam.persistence.FullTextHibernateSessionProxy");
            Class<?> jpaSearchClass = Class.forName("org.hibernate.search.jpa.Search");
            try
            {
               FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR = jpaSearchClass.getDeclaredMethod("getFullTextEntityManager", EntityManager.class);
            }
            catch (NoSuchMethodException noSuchMethod)
            {
               log.debug("org.hibernate.search.jpa.getFullTextSession(EntityManager) not found, trying deprecated method name createFullTextEntityManager");
               FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR = jpaSearchClass.getDeclaredMethod("createFullTextEntityManager", EntityManager.class);
            }
            FULL_TEXT_ENTITYMANAGER_PROXY_CLASS = Class.forName("org.jboss.seam.persistence.FullTextEntityManagerProxy");
            log.debug("Hibernate Search is available :-)");
         }
      }
      catch (Exception e)
      {
         log.debug("no Hibernate Search, sorry :-(", e);
      }
   }

   @Inject
   public void init()
   {
      featureSet.add(Feature.WILDCARD_AS_COUNT_QUERY_SUBJECT);
   }

   @Override
   public void setFlushModeManual(EntityManager entityManager)
   {
      try
      {
         getSession(entityManager).setFlushMode(FlushMode.MANUAL);
      }
      catch (NotHibernateException nhe)
      {
         super.setFlushModeManual(entityManager);
      }
   }

   @Override
   public void setRenderFlushMode()
   {
      persistenceContexts.get().changeFlushMode(FlushModeType.MANUAL, true);
   }

   @Override
   public boolean isDirty(EntityManager entityManager)
   {
      try
      {
         return getSession(entityManager).isDirty();
      }
      catch (NotHibernateException nhe)
      {
         return super.isDirty(entityManager);
      }
   }

   @Override
   public Object getId(Object bean, EntityManager entityManager)
   {
      try
      {
         return getSession(entityManager).getIdentifier(bean);
      }
      catch (NotHibernateException nhe)
      {
         return super.getId(bean, entityManager);
      }
      catch (TransientObjectException e)
      {
         if (bean instanceof HibernateProxy)
         {
            return super.getId(((HibernateProxy) bean).getHibernateLazyInitializer().getImplementation(), entityManager);
         }
         else
         {
            return super.getId(bean, entityManager);
         }
      }
   }

   @Override
   public boolean registerSynchronization(Synchronization sync, EntityManager entityManager)
   {
      try
      {
         // TODO: just make sure that a Hibernate JPA EntityTransaction
         // delegates to the Hibernate Session transaction
         getSession(entityManager).getTransaction().registerSynchronization(sync);
         return true;
      }
      catch (NotHibernateException nhe)
      {
         return super.registerSynchronization(sync, entityManager);
      }

   }

   @Override
   public String getName(Object bean, EntityManager entityManager) throws IllegalArgumentException
   {
      try
      {
         return getSession(entityManager).getEntityName(bean);
      }
      catch (NotHibernateException nhe)
      {
         return super.getName(bean, entityManager);
      }
      catch (TransientObjectException e)
      {
         return super.getName(bean, entityManager);
      }
   }

   private Session getSession(EntityManager entityManager)
   {
      Object delegate = entityManager.getDelegate();
      if (delegate instanceof Session)
      {
         return (Session) delegate;
      }
      else
      {
         throw new NotHibernateException();
      }
   }

   /**
    * Wrap the Hibernate Session in a proxy that implements FullTextSession if
    * Hibernate Search is available in the classpath.
    */
   static Session proxySession(Session session)
   {
      if (FULL_TEXT_SESSION_PROXY_CLASS == null)
      {
         return session;
      }
      else
      {
         try
         {
            return (Session) FULL_TEXT_SESSION_CONSTRUCTOR.invoke(null, session);
         }
         catch (Exception e)
         {
            log.warn("Unable to wrap into a FullTextSessionProxy, regular SessionProxy returned", e);
            return session;
         }
      }
   }

   /**
    * Wrap the delegate Hibernate Session in a proxy that implements
    * FullTextSession if Hibernate Search is available in the classpath.
    */
   @Override
   public Object proxyDelegate(Object delegate)
   {
      try
      {
         return proxySession((Session) delegate);
      }
      catch (NotHibernateException nhe)
      {
         return super.proxyDelegate(delegate);
      }
      catch (Exception e)
      {
         throw new RuntimeException("could not proxy delegate", e);
      }
   }

   @Override
   public EntityManager proxyEntityManager(EntityManager entityManager)
   {
      if (FULL_TEXT_ENTITYMANAGER_PROXY_CLASS == null)
      {
         return super.proxyEntityManager(entityManager);
      }
      else
      {
         try
         {
            return (EntityManager) FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR.invoke(null, super.proxyEntityManager(entityManager));
         }
         catch (Exception e)
         {
            // throw new
            // RuntimeException("could not proxy FullTextEntityManager", e);
            return super.proxyEntityManager(entityManager);
         }
      }
   }

   /**
    * Occurs when Hibernate is in the classpath, but this particular
    * EntityManager is not from Hibernate
    * 
    * @author Gavin King
    * 
    */
   static class NotHibernateException extends IllegalArgumentException
   {
   }

}
