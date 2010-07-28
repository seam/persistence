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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jboss.weld.extensions.el.Expressions;

public class QueryParser
{
   private final List<Object> parameterValues = new ArrayList<Object>();
   private final StringBuilder ejbqlBuilder;

   public static String getParameterName(int loc)
   {
      return "el" + (loc + 1);
   }

   public String getEjbql()
   {
      return ejbqlBuilder.toString();
   }

   public List<Object> getParameterValues()
   {
      return parameterValues;
   }

   public QueryParser(Expressions expressions, String ejbql)
   {
      this(expressions, ejbql, 0);
   }

   public QueryParser(Expressions expressions, String ejbql, int startingParameterNumber)
   {
      StringTokenizer tokens = new StringTokenizer(ejbql, "#}", true);
      ejbqlBuilder = new StringBuilder(ejbql.length());
      while (tokens.hasMoreTokens())
      {
         String token = tokens.nextToken();
         if ("#".equals(token) && tokens.hasMoreTokens())
         {
            String expressionToken = tokens.nextToken();

            if (!expressionToken.startsWith("{") || !tokens.hasMoreTokens())
            {
               ejbqlBuilder.append(token).append(expressionToken);
            }
            else
            {
               String expression = token + expressionToken + tokens.nextToken();
               ejbqlBuilder.append(':').append(getParameterName(startingParameterNumber + parameterValues.size()));
               parameterValues.add(expressions.evaluateValueExpression(expression));
            }
         }
         else
         {
            ejbqlBuilder.append(token);
         }
      }
   }

}
