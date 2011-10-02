/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jboss.solder.el.Expressions;

/**
 * Parses hql queries and replaces el with named parameters
 */
public class QueryParser {
    private final List<Object> parameterValues = new ArrayList<Object>();
    private final StringBuilder ejbqlBuilder;

    public static String getParameterName(int loc) {
        return "el" + (loc + 1);
    }

    public String getEjbql() {
        return ejbqlBuilder.toString();
    }

    public List<Object> getParameterValues() {
        return parameterValues;
    }

    public QueryParser(Expressions expressions, String ejbql) {
        this(expressions, ejbql, 0);
    }

    public QueryParser(Expressions expressions, String ejbql, int startingParameterNumber) {
        StringTokenizer tokens = new StringTokenizer(ejbql, "#}", true);
        ejbqlBuilder = new StringBuilder(ejbql.length());
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if ("#".equals(token) && tokens.hasMoreTokens()) {
                String expressionToken = tokens.nextToken();

                if (!expressionToken.startsWith("{") || !tokens.hasMoreTokens()) {
                    ejbqlBuilder.append(token).append(expressionToken);
                } else {
                    String expression = token + expressionToken + tokens.nextToken();
                    ejbqlBuilder.append(':').append(getParameterName(startingParameterNumber + parameterValues.size()));
                    parameterValues.add(expressions.evaluateValueExpression(expression));
                }
            } else {
                ejbqlBuilder.append(token);
            }
        }
    }

}
