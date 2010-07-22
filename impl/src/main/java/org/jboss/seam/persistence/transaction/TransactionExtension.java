/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.seam.persistence.transaction;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.jboss.seam.persistence.transaction.literal.DefaultTransactionLiteral;
import org.jboss.weld.extensions.annotated.AnnotatedTypeBuilder;
import org.jboss.weld.extensions.bean.BeanBuilder;
import org.jboss.weld.extensions.bean.BeanImpl;
import org.jboss.weld.extensions.bean.BeanLifecycle;
import org.jboss.weld.extensions.defaultbean.DefaultBeanExtension;
import org.jboss.weld.extensions.util.BeanResolutionException;
import org.jboss.weld.extensions.util.BeanResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension than provides a {@link SeamTransaction} if no other UserTransaction
 * has been registered.
 * 
 * This allows the user to register a transaction via seam XML and have it
 * automatically replace the default UserTransaction implementation
 * 
 * This is not done with alternatives, because that would require specifying the
 * transaction manager on a per module basis, and some of the UserTransaction
 * implementations need to be configured via seam xml anyway, so they would have
 * to be configured twice
 * 
 * @author Stuart Douglas
 * 
 */
public class TransactionExtension implements Extension
{

   private static final Logger log = LoggerFactory.getLogger(TransactionExtension.class);

   public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager)
   {
      AnnotatedTypeBuilder<SeamTransaction> utbuilder = AnnotatedTypeBuilder.newInstance(SeamTransaction.class);
      BeanBuilder<SeamTransaction> builder = new BeanBuilder<SeamTransaction>(utbuilder.create(), manager);
      builder.defineBeanFromAnnotatedType();
      builder.setBeanLifecycle(new TransactionLifecycle(manager));
      builder.getQualifiers().clear();
      builder.getQualifiers().add(DefaultTransactionLiteral.INSTANCE);
      DefaultBeanExtension.addDefaultBean(SeamTransaction.class, builder.create());
   }

   private static class TransactionLifecycle implements BeanLifecycle<SeamTransaction>
   {

      private final BeanManager manager;

      private Bean<?> transactionBean;

      public TransactionLifecycle(BeanManager manager)
      {
         this.manager = manager;
      }

      public SeamTransaction create(BeanImpl<SeamTransaction> bean, CreationalContext<SeamTransaction> ctx)
      {
         if (transactionBean == null)
         {
            // this does not need to be thread safe, it does not matter if this
            // is initialised twice
            setupBeanDefinition();
         }
         return (SeamTransaction) manager.getReference(transactionBean, SeamTransaction.class, ctx);
      }

      public void destroy(BeanImpl<SeamTransaction> bean, SeamTransaction arg0, CreationalContext<SeamTransaction> arg1)
      {
         arg1.release();
      }

      /**
       * we need to init the bean definition lazily
       */
      private void setupBeanDefinition()
      {
         try
         {
            transactionBean = BeanResolver.resolveBean(SeamTransaction.class, manager, new TransactionQualifier.TransactionQualifierLiteral());
         }
         catch (BeanResolutionException e)
         {
            throw new RuntimeException(e);
         }
      }

   }

}
