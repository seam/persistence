/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.seam.examples.booking.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.CreditCardNumber;
import org.jboss.seam.solder.core.Veto;

/**
 * <p>
 * <strong>Booking</strong> is the model/entity class that represents a hotel booking.
 * </p>
 *
 * @author Gavin King
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
@Entity
@Veto
public class Booking implements Serializable {
    private Long id;
    private User user;
    private Hotel hotel;
    private Date checkinDate;
    private Date checkoutDate;
    private String creditCardNumber;
    private CreditCardType creditCardType;
    private String creditCardName;
    private int creditCardExpiryMonth;
    private int creditCardExpiryYear;
    private boolean smoking;
    private int beds;

    public Booking() {
    }

    public Booking(Hotel hotel, User user, int daysFromNow, int nights) {
        this.hotel = hotel;
        this.user = user;
        creditCardName = user.getName();
        smoking = false;
        beds = 1;
        setReservationDates(daysFromNow, nights);
        creditCardExpiryMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Temporal(TemporalType.DATE)
    public Date getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(Date datetime) {
        checkinDate = datetime;
    }

    @NotNull
    @ManyToOne
    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    @NotNull
    @ManyToOne
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Temporal(TemporalType.DATE)
    public Date getCheckoutDate() {
        return checkoutDate;
    }

    public void setCheckoutDate(Date checkoutDate) {
        this.checkoutDate = checkoutDate;
    }

    public boolean isSmoking() {
        return smoking;
    }

    public void setSmoking(boolean smoking) {
        this.smoking = smoking;
    }

    // @Size(min = 1, max = 3)
    public int getBeds() {
        return beds;
    }

    public void setBeds(int beds) {
        this.beds = beds;
    }

    @NotNull(message = "Credit card number is required")
    @Size(min = 16, max = 16, message = "Credit card number must 16 digits long")
    @Digits(fraction = 0, integer = 16)
    @CreditCardNumber
    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    @NotNull(message = "Credit card type is required")
    @Enumerated(EnumType.STRING)
    public CreditCardType getCreditCardType() {
        return creditCardType;
    }

    public void setCreditCardType(CreditCardType creditCardType) {
        this.creditCardType = creditCardType;
    }

    @NotNull(message = "Credit card name is required")
    @Size(min = 3, max = 70, message = "Credit card name is required")
    public String getCreditCardName() {
        return creditCardName;
    }

    public void setCreditCardName(String creditCardName) {
        this.creditCardName = creditCardName;
    }

    /**
     * The credit card expiration month, represented using a 1-based numeric value (i.e., Jan = 1, Feb = 2, ...).
     *
     * @return 1-based numeric month value
     */
    public int getCreditCardExpiryMonth() {
        return creditCardExpiryMonth;
    }

    public void setCreditCardExpiryMonth(int creditCardExpiryMonth) {
        this.creditCardExpiryMonth = creditCardExpiryMonth;
    }

    /**
     * The credit card expiration year.
     *
     * @return numberic year value
     */
    public int getCreditCardExpiryYear() {
        return creditCardExpiryYear;
    }

    public void setCreditCardExpiryYear(int creditCardExpiryYear) {
        this.creditCardExpiryYear = creditCardExpiryYear;
    }

    @Transient
    public String getDescription() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return hotel == null ? null : hotel.getName() + ", " + df.format(getCheckinDate()) + " to "
                + df.format(getCheckoutDate());
    }

    @Transient
    public BigDecimal getTotal() {
        return hotel.getPrice().multiply(new BigDecimal(getNights()));
    }

    @Transient
    public int getNights() {
        return (int) (checkoutDate.getTime() - checkinDate.getTime()) / 1000 / 60 / 60 / 24;
    }

    /**
     * Initialize the check-in and check-out dates.
     *
     * @param daysFromNow Number of days the stay will begin from now
     * @param nights      Length of the stay in number of nights
     */
    public void setReservationDates(int daysFromNow, int nights) {
        Calendar refDate = Calendar.getInstance();
        refDate.set(refDate.get(Calendar.YEAR), refDate.get(Calendar.MONTH), refDate.get(Calendar.DAY_OF_MONTH) + daysFromNow,
                0, 0, 0);
        setCheckinDate(refDate.getTime());
        refDate.add(Calendar.DAY_OF_MONTH, nights);
        setCheckoutDate(refDate.getTime());
    }

    @Override
    public String toString() {
        return "Booking(" + user + ", " + hotel + ")";
    }
}
