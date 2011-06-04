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
package org.jboss.seam.transaction;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.Synchronization;

import org.jboss.logging.Logger;
import org.jboss.seam.solder.bean.defaultbean.DefaultBean;

/**
 * Receives JTA transaction completion notifications from the EJB container, and passes them on to the registered
 * Synchronizations. This implementation is fully aware of container managed transactions and is able to register
 * Synchronizations for the container transaction.
 * 
 * @author Gavin King
 * @author Stuart Douglass
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
@ApplicationScoped
@DefaultBean(Synchronizations.class)
public class EjbSynchronizations implements Synchronizations {
    private static final Logger log = Logger.getLogger(EjbSynchronizations.class);

    @Inject
    private Instance<LocalEjbSynchronizations> instance;

    private final ThreadLocal<LocalEjbSynchronizations> delegate = new ThreadLocal<LocalEjbSynchronizations>();

    private LocalEjbSynchronizations getThreadDelegate() {
        if (delegate.get() == null) {
            log.debug("Instantiating new EjbSynchronizationsDelegate");
            delegate.set(instance.get());
        }
        return delegate.get();
    }

    private void removeThreadDelegate() {
        log.debug("Removing EjbSynchronizationsDelegate");
        delegate.remove();
    }

    @Override
    public boolean isAwareOfContainerTransactions() {
        return true;
    }

    @Override
    public void afterTransactionBegin() {
        // noop, let JTA notify us
    }

    @Override
    public void afterTransactionCompletion(boolean success) {
        // noop, let JTA notify us
    }

    @Override
    public void beforeTransactionCommit() {
        // noop, let JTA notify us
    }

    @Override
    public void registerSynchronization(Synchronization sync) {
        log.debug("Registering synchronization");
        LocalEjbSynchronizations threadDelegate = getThreadDelegate();
        threadDelegate.setController(this);
        threadDelegate.registerSynchronization(sync);
    }

    public void cleanup() {
        removeThreadDelegate();
    }

}
