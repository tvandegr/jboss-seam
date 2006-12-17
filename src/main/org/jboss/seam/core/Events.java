package org.jboss.seam.core;

import static org.jboss.seam.InterceptionType.NEVER;
import static org.jboss.seam.annotations.Install.BUILT_IN;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.Seam;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Intercept;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Expressions.MethodBinding;
import org.jboss.seam.core.Init.ObserverMethod;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.jboss.seam.util.Resources;
import org.jboss.seam.util.XML;

@Scope(ScopeType.APPLICATION)
@Intercept(NEVER)
@Name("org.jboss.seam.core.events")
@Install(precedence=BUILT_IN)
public class Events 
{
   
   private static final LogProvider log = Logging.getLogProvider(Events.class);
   
   private final Map<String, List<MethodBinding>> listeners = new HashMap<String, List<MethodBinding>>();
   
   @Create
   public void initialize() throws DocumentException
   {
      InputStream stream = Resources.getResourceAsStream("/WEB-INF/events.xml");      
      if (stream==null)
      {
         log.info("no events.xml file found");
      }
      else
      {
         log.info("reading events.xml");
         List<Element> elements = XML.getRootElement(stream).elements("event");
         for (Element event: elements)
         {
            String type = event.attributeValue("type");
            if (type==null)
            {
               throw new IllegalArgumentException("must specify type for <event/> declaration");
            }
            List<MethodBinding> methodBindings = new ArrayList<MethodBinding>();
            listeners.put(type, methodBindings);
            
            List<Element> actions = event.elements("action");
            for (Element action: actions)
            {
               String actionExpression = action.attributeValue("expression");
               if (actionExpression==null)
               {
                  throw new IllegalArgumentException("must specify expression for <action/> declaration");
               }
               MethodBinding methodBinding = Expressions.instance().createMethodBinding(actionExpression);
               methodBindings.add(methodBinding);
            }
         }
      }
   }
   
   public void addListener(String type, String methodBindingExpression)
   {
      MethodBinding methodBinding = Expressions.instance().createMethodBinding(methodBindingExpression);
      List<MethodBinding> list = listeners.get(type);
      if (list==null)
      {
         list = new ArrayList<MethodBinding>();
         listeners.put(type, list);
      }
      list.add(methodBinding);
   }
   
   public void raiseEvent(String type, Object... parameters)
   {
      log.debug("Processing event:" + type);
      List<MethodBinding> list = listeners.get(type);
      if (list!=null)
      {
         for (MethodBinding listener: list )
         {
            listener.invoke(parameters);
         }
      }
      List<Init.ObserverMethod> observers = Init.instance().getObservers(type);
      if (observers!=null)
      {
         for (ObserverMethod observer: observers)
         {
            Object listener = Component.getInstance( observer.component.getName(), observer.create );
            if (listener!=null)
            {
               observer.component.callComponentMethod(listener, observer.method, parameters);
            }
         }
      }
   }
   
   public void raiseAsynchronousEvent(String type, Object... parameters)
   {
      Dispatcher.instance().scheduleEvent(type, 0l, null, null, parameters);
   }
   
   public void raiseTransactionSuccessEvent(String type, Object... parameters)
   {
      TransactionListener.instance().scheduleEvent(type, parameters);
   }
   
   public void raiseTimedEvent(String type, long duration, Object... parameters)
   {
      Dispatcher.instance().scheduleEvent(type, duration, null, null, parameters);
   }
   
   public void raiseTimedEvent(String type, Date expiration, Object... parameters)
   {
      Dispatcher.instance().scheduleEvent(type, null, expiration, null, parameters);
   }
   
   public void raiseTimedEvent(String type, Date expiration, long intervalDuration, Object... parameters)
   {
      Dispatcher.instance().scheduleEvent(type, null, expiration, intervalDuration, parameters);
   }
   
   public void raiseTimedEvent(String type, long duration, long intervalDuration)
   {
      Dispatcher.instance().scheduleEvent(type, duration, null, intervalDuration);
   }
   
   public static boolean exists()
   {
      return Contexts.isApplicationContextActive() &&
            Contexts.getApplicationContext().isSet( Seam.getComponentName(Events.class) );
   }

   public static Events instance()
   {
      if ( !Contexts.isApplicationContextActive() )
      {
         throw new IllegalStateException("No active application context");
      }
      return (Events) Component.getInstance(Events.class, ScopeType.APPLICATION);
   }
   
}
