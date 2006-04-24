//$Id$
package org.jboss.seam.example.booking;

import static javax.persistence.PersistenceContextType.EXTENDED;
import static org.jboss.seam.annotations.Outcome.REDISPLAY;

import java.util.Calendar;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.validator.Valid;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Conversational;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.IfInvalid;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.core.Events;
import org.jboss.seam.core.FacesMessages;

@Stateful
@Name("hotelBooking")
@Conversational(ifNotBegunOutcome="main")
@LoggedIn
public class HotelBookingAction implements HotelBooking
{
   
   @PersistenceContext(type=EXTENDED)
   private EntityManager em;
   
   @In(required=false) @Out
   private Hotel hotel;
   
   @In(required=false) 
   @Out(required=false)
   @Valid
   private Booking booking;
   
   @In
   private User user;
   
   @In(create=true)
   private transient FacesMessages facesMessages;
      
   @In 
   private HotelSearching hotelSearch;
   
   @Begin
   public String selectHotel()
   {
      hotel = em.merge( hotelSearch.getSelectedHotel() );
      return "hotel";
   }
   
   public String bookHotel()
   {      
      booking = new Booking(hotel, user);
      Calendar calendar = Calendar.getInstance();
      booking.setCheckinDate( calendar.getTime() );
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      booking.setCheckoutDate( calendar.getTime() );
      
      return "book";
   }

   @IfInvalid(outcome=REDISPLAY)
   public String setBookingDetails()
   {
      if (booking==null || hotel==null) return "main";
      if ( !booking.getCheckinDate().before( booking.getCheckoutDate() ) )
      {
         facesMessages.add("Check out date must be later than check in date");
         return null;
      }
      else
      {
         return "confirm";
      }
   }

   @End
   public String confirm()
   {
      if (booking==null || hotel==null) return "main";
      em.persist(booking);
      facesMessages.add("Thank you, #{user.name}, your confimation number for #{hotel.name} is #{booking.id}");
      Events.instance().raiseEvent("bookingConfirmed");
      return "confirmed";
   }
   
   @End
   public String cancel()
   {
      return "main";
   }
   
   @Destroy @Remove
   public void destroy() {}

}