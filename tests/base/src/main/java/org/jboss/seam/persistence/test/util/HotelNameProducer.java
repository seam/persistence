package org.jboss.seam.persistence.test.util;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class HotelNameProducer
{
   @Produces
   @Named("hotelName")
   public String getHotelName()
   {
      return "Hilton";
   }
}
