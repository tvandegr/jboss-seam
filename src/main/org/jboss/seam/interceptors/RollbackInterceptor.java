//$Id$
package org.jboss.seam.interceptors;

import static org.jboss.seam.ComponentType.JAVA_BEAN;
import static org.jboss.seam.util.EJB.APPLICATION_EXCEPTION;
import static org.jboss.seam.util.EJB.rollback;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.jboss.seam.annotations.AroundInvoke;
import org.jboss.seam.annotations.Interceptor;
import org.jboss.seam.annotations.Outcome;
import org.jboss.seam.annotations.Rollback;
import org.jboss.seam.core.Transaction;
import org.jboss.seam.intercept.InvocationContext;

/**
 * Automatically sets transactions to rollback only.
 * 
 * @author Gavin King
 */
@Interceptor(stateless=true)
public class RollbackInterceptor extends AbstractInterceptor 
{
   private static final long serialVersionUID = 5551801508325093417L;
   
   @AroundInvoke
   public Object aroundInvoke(InvocationContext invocation) throws Exception 
   {
      try
      {
         final Object result = invocation.proceed();
         if ( isRollbackRequired( invocation.getMethod(), result ) )
         {
            Transaction.instance().setRollbackOnly();
         }
         return result;
      }
      catch (Exception e)
      {
         //Reproduce the EJB3 rollback rules for JavaBean components
         if ( getComponent().getType()==JAVA_BEAN )
         {
            if ( isRollbackRequired(e) )
            {
               try
               {
                  Transaction.instance().setRollbackOnly();
               }
               catch (Exception te) {} //swallow
            }
         }
         throw e;
      }
   }
   
   private boolean isRollbackRequired(Method method, final Object result)
   {
      if ( !method.isAnnotationPresent(Rollback.class) ) return false;
      String[] outcomes = method.getAnnotation(Rollback.class).ifOutcome();
      List<String> outcomeList = Arrays.asList(outcomes);
      return outcomes.length==0 || 
            ( result==null && outcomeList.contains(Outcome.REDISPLAY) ) || 
            outcomeList.contains(result);
   }
   
   private boolean isRollbackRequired(Exception e)
   {
      Class<? extends Exception> clazz = e.getClass();
      return ( (e instanceof RuntimeException) && !clazz.isAnnotationPresent(APPLICATION_EXCEPTION) ) || 
            ( clazz.isAnnotationPresent(APPLICATION_EXCEPTION) && rollback( clazz.getAnnotation(APPLICATION_EXCEPTION) ) );
   }
   
}
