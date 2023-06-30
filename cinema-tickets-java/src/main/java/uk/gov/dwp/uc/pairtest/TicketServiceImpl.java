package uk.gov.dwp.uc.pairtest;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidAccountException;
import uk.gov.dwp.uc.pairtest.exception.InvalidNumberOfTicketsException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.InvalidTicketTypeException;
import uk.gov.dwp.uc.pairtest.exception.UnaccompaniedTicketsException;


public class TicketServiceImpl implements TicketService {
    @Inject
    public TicketPaymentService ticketPaymentService;
    @Inject
    public SeatReservationService seatReservationService;

    public static final int maxNoOfTicketsPerTransaction = 20;

    private static final int adultTicketPrice = 2000;
    private static final int childTicketPrice = 1000;
    private static final int infantTicketPrice = 0;

    /**
     * Should only have private methods other than the one below.
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        // Collate totals per type of ticket in requests, then apply checks and business rules using the totals
        // before requesting payment and reservations.

        // Check account number is valid before iterating requests
        if (accountId < 0) {
            throw new InvalidAccountException();
        }

        Map <TicketTypeRequest.Type, Integer> collatedTicketTotals = getTicketTypeTotals(ticketTypeRequests);
        Integer noOfTicketsRequiringAccompaniment = collatedTicketTotals.get(TicketTypeRequest.Type.INFANT) + collatedTicketTotals.get(TicketTypeRequest.Type.CHILD);
        Integer noOfTicketsThatAccompany = collatedTicketTotals.get(TicketTypeRequest.Type.ADULT);
        Integer noOfReservableSeats = collatedTicketTotals.get(TicketTypeRequest.Type.CHILD) + collatedTicketTotals.get(TicketTypeRequest.Type.ADULT);
        Integer noOfPayableTickets = collatedTicketTotals.get(TicketTypeRequest.Type.CHILD) + collatedTicketTotals.get(TicketTypeRequest.Type.ADULT);

        // Business rule: Maximum of 20 tickets
        // This could be in a single request, or spread across multiple requests, so we check against the total.
        if (noOfPayableTickets > maxNoOfTicketsPerTransaction) {
            throw new InvalidNumberOfTicketsException();
        }
        
        // Business rule: Child and Infant tickets cannot be purchased without Adult tickets.
        if (noOfTicketsRequiringAccompaniment > 0 && noOfTicketsThatAccompany == 0) {
            throw new UnaccompaniedTicketsException();
        }

        // At this point ticket requests are valid, so we progress with charging and then reserving.
        // Note the business rules do not explicitly exclude zero requests or zero tickets, this loop treats all equally.
        int total = 0;
        for (TicketTypeRequest.Type type :TicketTypeRequest.Type.values()) {
            int pricePerTicket = getPriceInPenceForTicketType(type);
            total += pricePerTicket * collatedTicketTotals.get(type);
        }
        
        ticketPaymentService.makePayment(accountId, total);
        seatReservationService.reserveSeat(accountId, noOfReservableSeats);
    }

    /**
     * Gets the price in pence for the type of ticket.
     * 
     * @param type TicketTypeRequest.Type
     * @return Price in pence
     * @throws InvalidTicketTypeException if the type is not recognised.
     */
    private int getPriceInPenceForTicketType(TicketTypeRequest.Type type) throws InvalidTicketTypeException {
        // Currently hard-coded values, replace in future with a call to a service
        if (type == TicketTypeRequest.Type.INFANT) {
            return infantTicketPrice;
        }
        if (type == TicketTypeRequest.Type.CHILD) {
            return childTicketPrice;
        }
        if (type == TicketTypeRequest.Type.ADULT) {
            return adultTicketPrice;
        }
        throw new InvalidTicketTypeException();
    }

    /**
     * Collates the total number of tickets by ticket type, with all types present regardless of whether there
     * are tickets requested of the type.
     * 
     * Negative numbers of tickets in requests are checked for to prevent inappropriate totals. Any negative values
     * found in a request will cause an InvalidNumberOfTicketsException.
     * 
     * @param ticketTypeRequests
     * @return Map keyed with TicketTypeRequest.Type to total number of tickets from all requests with that type.
     * @throws InvalidNumberOfTicketsException if a negative number of tickets specified in a request.
     */
    private Map <TicketTypeRequest.Type, Integer> getTicketTypeTotals(TicketTypeRequest... ticketTypeRequests) throws InvalidNumberOfTicketsException {
        Map <TicketTypeRequest.Type, Integer> totals = new HashMap <TicketTypeRequest.Type, Integer>(3);

        // Ensure all ticket types are present as keys
        for (TicketTypeRequest.Type ticketType : TicketTypeRequest.Type.values()) {
            totals.put(ticketType, 0);
        }
        // Calculate value totals
        TicketTypeRequest.Type type;
        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            if (ticketTypeRequest.getNoOfTickets() < 0) {
                throw new InvalidNumberOfTicketsException();
            }

            type = ticketTypeRequest.getTicketType();
            Integer total = totals.get(type);
            totals.put(type, (total == null) ? ticketTypeRequest.getNoOfTickets() : total + ticketTypeRequest.getNoOfTickets());
        }

        return totals;
    }
}
