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
package org.jboss.seam.persistence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManagerFactory;

/**
 * Signifies that a resource producer field or producer method that produces an
 * {@link EntityManagerFactory} should also produce a Seam managed persistence
 * context. For example:
 * 
 * <pre>
 * &#064;SeamManaged
 * &#064;Produces
 * &#064;PersistenceUnit
 * &#064;ConversationScoped
 * &#064;SomeQualifier
 * EntityManagerFactory emf;
 * </pre>
 * 
 * will create a conversation scoped seam managed persistence context that is
 * conversation scoped with the qualifier @SomeQualifier.
 * 
 * This field still produces the EntityManagerFactory with qualifier
 * 
 * @SomeQualifier, however the scope for the producer field is changed to
 *                 {@link Dependent}, as the specification does not allow
 *                 resource producer fields to have a scope other than Depedent
 * 
 * @author Stuart Douglas
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface SeamManaged
{

}
